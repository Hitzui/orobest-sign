<?php

namespace app\services;

use app\services\ClienteDocumentException as ApiException;
use app\models\ArchivosClientes;
use app\models\Cliente;
use app\models\Documentos;
use Yii;
use yii\helpers\FileHelper;
use yii\db\Query;
use yii\web\UploadedFile;

class ClienteDocumentService
{
    public const MAX_BYTES = 10 * 1024 * 1024;
    public const MAX_FILES = 10;
    public const EXTENSIONS = 'pdf, doc, docx, jpg, jpeg, png, txt';
    private const CATEGORIES = ['cedula_frente', 'cedula_reverso', 'otro'];

    public function __construct(private ?string $storage = null)
    {
    }

    public function listing(array $params): array
    {
        $page = $this->pageInteger($params['page'] ?? 1, 1000000);
        $pageSize = $this->pageInteger($params['page_size'] ?? 20, 100);
        $query = Cliente::find()->alias('c');
        if (array_key_exists('q', $params)) {
            if (!is_string($params['q']) || mb_strlen($params['q']) > 100) {
                throw new ApiException(422, 'invalid_search', 'La búsqueda de clientes no es válida.');
            }
            $term = trim($params['q']);
            if ($term !== '') {
                $query->andWhere(['or', ['like', 'c.nombres', $term], ['like', 'c.apellidos', $term], ['like', 'c.cedula', $term], ['like', 'c.idcliente', $term]]);
            }
        }
        $total = (int) (clone $query)->count();
        $rows = $query->select(['c.idcliente', 'c.nombres', 'c.apellidos', 'c.cedula', 'c.telefono', 'c.celular'])
            ->orderBy(['c.nombres' => SORT_ASC, 'c.apellidos' => SORT_ASC, 'c.idcliente' => SORT_ASC])
            ->offset(($page - 1) * $pageSize)->limit($pageSize)->asArray()->all();
        return [
            'items' => array_map(fn ($row) => $this->summary($row), $rows),
            'pagination' => ['page' => $page, 'page_size' => $pageSize, 'total' => $total, 'total_pages' => (int) ceil($total / $pageSize)],
        ];
    }

    public function find(int $id): Cliente
    {
        $client = Cliente::findOne($id);
        if (!$client) {
            throw new ApiException(404, 'client_not_found', 'Cliente no encontrado.');
        }
        return $client;
    }

    private function summary(array|Cliente $client): array
    {
        $get = static fn (string $key) => is_array($client) ? ($client[$key] ?? null) : $client->$key;
        return [
            'idcliente' => (int) $get('idcliente'),
            'nombres' => (string) $get('nombres'),
            'apellidos' => (string) $get('apellidos'),
            'cedula' => $get('cedula'),
            'telefono' => $get('telefono'),
            'celular' => $get('celular'),
        ];
    }

    public function detail(int $id): array
    {
        return $this->summary($this->find($id));
    }

    public function documents(int $id): array
    {
        $this->find($id);
        $documentIds = Documentos::find()->select('id')->where(['idcliente' => $id]);
        $files = ArchivosClientes::find()->where(['iddocumento' => $documentIds])
            ->orderBy(['idarchivo' => SORT_DESC])->all();
        return ['items' => array_map(fn (ArchivosClientes $file) => $this->metadata($file, $id), $files)];
    }

    public function save(int $clientId, string $category, string $operationId, string $displayName, string $uploadedPath, ?string $originalName = null): array
    {
        $client = $this->find($clientId);
        if (!in_array($category, self::CATEGORIES, true)) {
            throw new ApiException(422, 'invalid_document_type', 'El tipo de documento no es válido.');
        }
        if (!preg_match('/^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/iD', $operationId)) {
            throw new ApiException(422, 'invalid_operation', 'Se requiere un operation_id UUID válido.');
        }
        [$mime, $extension] = $this->inspect($uploadedPath, $originalName ?? $displayName);
        $displayName = trim(preg_replace('/[\x00-\x1F\x7F]/u', '', basename(str_replace('\\', '/', $displayName))));
        if ($category !== 'otro') {
            $displayName = $category === 'cedula_frente' ? 'Cédula - Frente' : 'Cédula - Reverso';
        } else {
            $displayName = preg_replace('/\.(jpe?g|png|pdf|docx?|txt)$/i', '', $displayName);
        }
        if ($displayName === '' || mb_strlen($displayName) > 240) {
            throw new ApiException(422, 'invalid_document_name', 'El nombre del documento no es válido.');
        }
        $operationId = strtolower($operationId);
        $relativePath = $clientId . '/' . $category . '/' . $operationId . '.' . $extension;
        $root = $this->storageRoot();
        $destination = $root . '/' . $relativePath;
        $db = Yii::$app->db;
        $transaction = $db->beginTransaction();
        $written = false;
        try {
            $db->createCommand('SELECT idcliente FROM cliente WHERE idcliente = :id FOR UPDATE', [':id' => $clientId])->queryScalar();
            $existing = ArchivosClientes::find()->where(['like', 'ruta', $clientId . '/' . $category . '/' . $operationId . '.%', false])->one();
            if ($existing) {
                $existingPath = $this->resolveFilePath($clientId, $existing->ruta);
                $existingHash = $existingPath ? hash_file('sha256', $existingPath) : false;
                $uploadHash = hash_file('sha256', $uploadedPath);
                if (!$existingHash || !$uploadHash || !hash_equals($existingHash, $uploadHash)) {
                    throw new ApiException(409, 'operation_conflict', 'El identificador de operación ya se utilizó con otro archivo.');
                }
                $transaction->commit();
                return ['documento' => $this->metadata($existing, $clientId), 'replayed' => true];
            }
            $header = Documentos::find()->where(['idcliente' => $clientId])->orderBy(['id' => SORT_ASC])->one();
            if (!$header) {
                $header = new Documentos();
                $header->titulo = 'Documentos de cliente ' . $client->idcliente;
                $header->idcliente = $clientId;
                if (!$header->save()) {
                    throw new \RuntimeException('No se pudo crear el registro de documentos del cliente.');
                }
            }
            FileHelper::createDirectory(dirname($destination), 0700);
            // Modo exclusivo: incluso un archivo huérfano nunca se sobrescribe.
            $output = @fopen($destination, 'xb');
            if ($output === false) {
                throw new ApiException(409, 'operation_conflict', 'Ya existe un archivo para esta operación.');
            }
            $written = true;
            try {
                $input = fopen($uploadedPath, 'rb');
                if ($input === false) {
                    throw new \RuntimeException('No se pudo leer el documento del cliente.');
                }
                try {
                    if (stream_copy_to_stream($input, $output) !== filesize($uploadedPath)) {
                        throw new \RuntimeException('No se pudo guardar el documento completo.');
                    }
                } finally {
                    fclose($input);
                }
            } finally {
                fclose($output);
            }
            chmod($destination, 0600);
            $record = new ArchivosClientes();
            $record->setAttributes([
                'iddocumento' => $header->id,
                'nombre_original' => $displayName . '.' . $extension,
                'ruta' => $relativePath,
                'tipo' => $mime,
                'size' => filesize($destination),
                'created_at' => gmdate('Y-m-d H:i:s'),
            ], false);
            if (!$record->save()) {
                throw new \RuntimeException('No se pudieron guardar los datos del documento.');
            }
            $this->cleanupOnRollback($destination);
            $transaction->commit();
            return ['documento' => $this->metadata($record, $clientId), 'replayed' => false];
        } catch (\Throwable $error) {
            if ($transaction->isActive) {
                $transaction->rollBack();
            }
            if ($written && is_file($destination)) {
                unlink($destination);
            }
            throw $error;
        }
    }

    public function inspect(string $uploadedPath, string $name): array
    {
        if (!is_file($uploadedPath) || filesize($uploadedPath) < 1 || filesize($uploadedPath) > self::MAX_BYTES) {
            throw new ApiException(413, 'document_too_large', 'Cada documento debe tener contenido y pesar como máximo 10 MiB.');
        }
        $mime = (new \finfo(FILEINFO_MIME_TYPE))->file($uploadedPath);
        $extension = match ($mime) {
            'image/jpeg' => 'jpg',
            'image/png' => 'png',
            'application/pdf' => 'pdf',
            'text/plain' => 'txt',
            'application/msword', 'application/CDFV2' => strtolower(pathinfo($name, PATHINFO_EXTENSION)) === 'doc' ? 'doc' : null,
            'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 'application/zip' => $this->isDocx($uploadedPath) ? 'docx' : null,
            default => null,
        };
        if ($extension === null) {
            throw new ApiException(422, 'invalid_document', 'Formatos permitidos: PDF, DOC, DOCX, JPG, JPEG, PNG y TXT.');
        }
        if (str_starts_with($mime, 'image/')) {
            $dimensions = @getimagesize($uploadedPath);
            if (!$dimensions || $dimensions[0] < 1 || $dimensions[1] < 1 || $dimensions[0] > 8000 || $dimensions[1] > 8000) {
                throw new ApiException(422, 'invalid_document', 'La imagen del documento no tiene dimensiones válidas.');
            }
        } elseif ($extension === 'pdf' && file_get_contents($uploadedPath, false, null, 0, 5) !== '%PDF-') {
            throw new ApiException(422, 'invalid_document', 'El archivo PDF no es válido.');
        }
        $mime = match ($extension) {
            'doc' => 'application/msword',
            'docx' => 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
            default => $mime,
        };
        return [$mime, $extension];
    }

    public function image(int $clientId, int $fileId): array
    {
        $this->find($clientId);
        $file = (new Query())->from(['a' => 'archivos_clientes'])
            ->innerJoin(['d' => 'documentos'], 'd.id = a.iddocumento')
            ->where(['a.idarchivo' => $fileId, 'd.idcliente' => $clientId])->one();
        if (!$file) {
            throw new ApiException(404, 'document_not_found', 'Documento no encontrado.');
        }
        $path = $this->resolveFilePath($clientId, $file['ruta']);
        if ($path === null) {
            throw new ApiException(404, 'document_not_found', 'El archivo no está disponible.');
        }
        try {
            [$mime] = $this->inspect($path, $file['nombre_original']);
        } catch (ClienteDocumentException $error) {
            // Los archivos históricos siguen siendo descargables aunque no sean previsualizables.
            $mime = 'application/octet-stream';
        }
        return [$path, $mime];
    }

    private function metadata(ArchivosClientes $file, int $clientId): array
    {
        $category = $this->categoryFromPath($clientId, $file->ruta) ?? 'otro';
        return [
            'idarchivo' => (int) $file->idarchivo,
            'tipo_documento' => $category,
            'nombre' => $file->nombre_original,
            'mime_type' => $file->tipo,
            'size_bytes' => (int) $file->size,
            'created_at' => $file->created_at,
            'disponible' => $this->resolveFilePath($clientId, $file->ruta) !== null,
        ];
    }

    private function categoryFromPath(int $clientId, string $path): ?string
    {
        $pattern = '~^' . preg_quote((string) $clientId, '~') . '/(cedula_frente|cedula_reverso|otro)/[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}\.(jpg|png|pdf|docx?|txt)$~D';
        return preg_match($pattern, $path, $matches) ? $matches[1] : null;
    }

    private function resolveFilePath(int $clientId, string $relative): ?string
    {
        if ($this->categoryFromPath($clientId, $relative) !== null) {
            $root = $this->storagePath() . '/' . $clientId;
            $relative = substr($relative, strlen((string) $clientId) + 1);
        } elseif (preg_match('~^uploads/clientes/' . preg_quote((string) $clientId, '~') . '/[^/]+$~D', $relative)) {
            // Compatibilidad de lectura para adjuntos históricos del formulario web.
            $root = Yii::getAlias('@app/uploads/clientes/' . $clientId);
            $relative = basename($relative);
        } else {
            return null;
        }
        $candidate = realpath($root . '/' . $relative);
        $realRoot = realpath($root);
        if (!$candidate || !$realRoot || !is_file($candidate)) {
            return null;
        }
        $prefix = rtrim($realRoot, DIRECTORY_SEPARATOR) . DIRECTORY_SEPARATOR;
        return str_starts_with($candidate, $prefix) ? $candidate : null;
    }

    private function storagePath(): string
    {
        if ($this->storage !== null) {
            return Yii::getAlias($this->storage);
        }
        $module = Yii::$app->getModule('sign');
        return Yii::getAlias($this->storage ?? ($module->clientDocumentsStoragePath ?? (getenv('OROBEST_CLIENT_DOCUMENTS_PATH') ?: '@runtime/client-documents')));
    }

    private function storageRoot(): string
    {
        $root = $this->storagePath();
        FileHelper::createDirectory($root, 0700);
        $deny = $root . '/.htaccess';
        if (!is_file($deny) && file_put_contents($deny, "<IfModule mod_authz_core.c>\nRequire all denied\n</IfModule>\n<IfModule !mod_authz_core.c>\nDeny from all\n</IfModule>\n") === false) {
            throw new \RuntimeException('Unable to protect client document storage');
        }
        return $root;
    }

    public function files(int $clientId): array
    {
        return ArchivosClientes::find()->where(['iddocumento' => Documentos::find()->select('id')->where(['idcliente' => $clientId])])
            ->orderBy(['idarchivo' => SORT_DESC])->all();
    }

    public function saveUploads(Cliente $client, array $files): void
    {
        if ($files === []) {
            return;
        }
        if (count($files) > self::MAX_FILES) {
            throw new ApiException(422, 'too_many_documents', 'Puede subir hasta 10 documentos a la vez.');
        }
        foreach ($files as $file) {
            if (!$file instanceof UploadedFile || $file->error !== UPLOAD_ERR_OK) {
                throw new ApiException(422, 'invalid_document', 'No se recibió el archivo completo. Vuelva a seleccionarlo.');
            }
            $this->inspect($file->tempName, $file->name);
        }
        $transaction = Yii::$app->db->beginTransaction();
        try {
            foreach ($files as $file) {
                $uuid = random_bytes(16);
                $uuid[6] = chr((ord($uuid[6]) & 0x0f) | 0x40);
                $uuid[8] = chr((ord($uuid[8]) & 0x3f) | 0x80);
                $hex = bin2hex($uuid);
                $operation = substr($hex, 0, 8) . '-' . substr($hex, 8, 4) . '-' . substr($hex, 12, 4) . '-' . substr($hex, 16, 4) . '-' . substr($hex, 20);
                $this->save((int) $client->idcliente, 'otro', $operation, $file->name, $file->tempName);
            }
            $transaction->commit();
        } catch (\Throwable $error) {
            $transaction->rollBack();
            throw $error;
        }
    }

    public function delete(int $clientId, int $fileId): void
    {
        $this->find($clientId);
        $transaction = Yii::$app->db->beginTransaction();
        $staged = null;
        $path = null;
        try {
            Yii::$app->db->createCommand('SELECT idcliente FROM cliente WHERE idcliente = :id FOR UPDATE', [':id' => $clientId])->queryScalar();
            $file = ArchivosClientes::find()->where(['idarchivo' => $fileId, 'iddocumento' => Documentos::find()->select('id')->where(['idcliente' => $clientId])])->one();
            if ($file) {
                $path = $this->resolveFilePath($clientId, $file->ruta);
                if ($path !== null) {
                    $staged = $path . '.delete-' . bin2hex(random_bytes(8));
                    if (!rename($path, $staged)) {
                        throw new \RuntimeException('No se pudo eliminar el archivo físico.');
                    }
                }
                if ($file->delete() === false) {
                    throw new \RuntimeException('No se pudo eliminar el registro del documento.');
                }
            }
            $transaction->commit();
        } catch (\Throwable $error) {
            $transaction->rollBack();
            if ($staged !== null && is_file($staged)) {
                rename($staged, $path);
            }
            throw $error;
        }
        if ($staged !== null && is_file($staged) && !unlink($staged)) {
            Yii::warning('No se pudo limpiar un documento eliminado.', 'cliente');
        }
    }

    private function isDocx(string $path): bool
    {
        $zip = new \ZipArchive();
        if ($zip->open($path) !== true) {
            return false;
        }
        try {
            return $zip->locateName('[Content_Types].xml') !== false && $zip->locateName('word/document.xml') !== false;
        } finally {
            $zip->close();
        }
    }

    private function pageInteger($value, int $max): int
    {
        $number = filter_var($value, FILTER_VALIDATE_INT, ['options' => ['min_range' => 1, 'max_range' => $max]]);
        if ($number === false) {
            throw new ApiException(422, 'invalid_pagination', 'La paginación no es válida.');
        }
        return $number;
    }

    private function cleanupOnRollback(string $path): void
    {
        $db = Yii::$app->db;
        $rollback = $commit = null;
        $detach = static function () use ($db, &$rollback, &$commit): void {
            $db->off(\yii\db\Connection::EVENT_ROLLBACK_TRANSACTION, $rollback);
            $db->off(\yii\db\Connection::EVENT_COMMIT_TRANSACTION, $commit);
        };
        $rollback = static function () use ($path, $detach): void {
            if (is_file($path)) {
                unlink($path);
            }
            $detach();
        };
        $commit = static function () use ($detach): void { $detach(); };
        $db->on(\yii\db\Connection::EVENT_ROLLBACK_TRANSACTION, $rollback);
        $db->on(\yii\db\Connection::EVENT_COMMIT_TRANSACTION, $commit);
    }
}
