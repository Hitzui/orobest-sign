<?php

namespace app\modules\sign\services;

use app\modules\sign\components\ApiException;
use app\modules\sign\models\CompraFirma;
use app\modules\sign\Module;
use Yii;
use yii\helpers\FileHelper;

class FirmaService
{
    public const MAX_BYTES = 2097152;

    public function __construct(private Module $module)
    {
    }

    private function normalizePng(string $path): array
    {
        if (!is_file($path) || filesize($path) > self::MAX_BYTES) {
            throw new ApiException(413, 'image_too_large', 'La firma no puede superar 2 MiB.');
        }
        $size = @getimagesize($path);
        if (!$size || $size[2] !== IMAGETYPE_PNG || $size[0] < 1 || $size[1] < 1 || $size[0] > 2048 || $size[1] > 1024) {
            throw new ApiException(422, 'invalid_image', 'Se requiere PNG de hasta 2048 × 1024 píxeles.');
        }
        $source = @imagecreatefrompng($path);
        if (!$source) {
            throw new ApiException(422, 'invalid_image', 'La imagen PNG no puede leerse.');
        }
        $image = imagecreatetruecolor($size[0], $size[1]);
        imagefill($image, 0, 0, imagecolorallocate($image, 255, 255, 255));
        imagecopy($image, $source, 0, 0, 0, 0, $size[0], $size[1]);
        ob_start();
        try {
            imagepng($image, null, 6);
            $png = ob_get_contents();
        } finally {
            ob_end_clean();
            imagedestroy($source);
            imagedestroy($image);
        }
        if (strlen($png) > self::MAX_BYTES) {
            throw new ApiException(413, 'image_too_large', 'La firma procesada supera 2 MiB.');
        }
        return [$png, $size[0], $size[1]];
    }

    public function save(int $id, int $user, int $agency, array $params, string $uploadedPath): array
    {
        $operation = $params['operation_id'] ?? '';
        $revision = $params['compra_revision'] ?? '';
        if (!is_string($operation) || !preg_match('/^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/iD', $operation)
            || !is_string($revision) || !preg_match('/^[0-9a-f]{64}$/D', $revision)) {
            throw new ApiException(422, 'invalid_operation', 'Se requieren operation_id UUID y compra_revision válidos.');
        }
        $base = CompraAccessService::integer($params['base_version'] ?? null, 'base_version', 0, 2147483646);
        [$png, $width, $height] = $this->normalizePng($uploadedPath);
        $hash = hash('sha256', $png);
        $operation = strtolower($operation);
        $access = new CompraAccessService();
        $access->find($id, $agency);
        $db = Yii::$app->db;
        $transaction = $db->beginTransaction();
        $written = null;
        try {
            // Serializa cargas de la misma compra, incluida la primera firma.
            $db->createCommand('SELECT idcompra FROM compra WHERE idcompra = :id FOR UPDATE', [':id' => $id])->queryScalar();
            $row = $access->find($id, $agency);
            $previous = CompraFirma::findOne(['idusuario' => $user, 'operation_id' => $operation]);
            if ($previous) {
                if ((int) $previous->idcompra !== $id || (int) $previous->version !== $base + 1
                    || !hash_equals($previous->sha256, $hash) || !hash_equals($previous->compra_revision, $revision)) {
                    throw new ApiException(409, 'operation_conflict', 'El identificador de operación ya tiene otro contenido.');
                }
                $transaction->commit();
                return ['firma' => $previous->metadata(), 'replayed' => true];
            }
            if ((int) $row['estado'] !== 1) {
                throw new ApiException(409, 'purchase_not_active', 'La compra está anulada o no admite firma.');
            }
            if (!hash_equals($access->revision($row), $revision)) {
                throw new ApiException(409, 'purchase_changed', 'La compra cambió. Consúltala nuevamente antes de firmar.');
            }
            $current = (int) CompraFirma::find()->where(['idcompra' => $id])->max('version');
            if ($current !== $base) {
                throw new ApiException(409, 'signature_changed', 'La firma cambió en otro dispositivo. Recarga la compra.');
            }
            $relative = $id . '/' . bin2hex(random_bytes(24)) . '.png';
            $root = $this->storageRoot();
            FileHelper::createDirectory($root . '/' . $id, 0700);
            $written = $root . '/' . $relative;
            if (file_put_contents($written, $png, LOCK_EX) !== strlen($png)) {
                throw new \RuntimeException('Unable to write signature');
            }
            chmod($written, 0600);
            $signature = new CompraFirma();
            $signature->setAttributes([
                'idcompra' => $id, 'version' => $base + 1, 'idusuario' => $user, 'idagencia' => $agency,
                'operation_id' => $operation, 'compra_revision' => $revision, 'file_path' => $relative,
                'sha256' => $hash, 'mime_type' => 'image/png', 'size_bytes' => strlen($png),
                'width' => $width, 'height' => $height, 'created_at' => gmdate('Y-m-d H:i:s'),
            ], false);
            $signature->save(false);
            $transaction->commit();
            return ['firma' => $signature->metadata(), 'replayed' => false];
        } catch (\Throwable $e) {
            if ($transaction->isActive) {
                $transaction->rollBack();
            }
            if ($written && is_file($written)) {
                unlink($written);
            }
            if ($e instanceof \yii\db\IntegrityException) {
                throw new ApiException(409, 'operation_conflict', 'La operación ya fue registrada; consulta la compra.');
            }
            throw $e;
        }
    }

    private function storageRoot(): string
    {
        $root = $this->storagePath();
        FileHelper::createDirectory($root, 0700);
        // Protección Apache también para instalaciones cuya raíz pública es @app.
        $deny = $root . '/.htaccess';
        if (!is_file($deny) && file_put_contents($deny, "<IfModule mod_authz_core.c>\nRequire all denied\n</IfModule>\n<IfModule !mod_authz_core.c>\nDeny from all\n</IfModule>\n") === false) {
            throw new \RuntimeException('Unable to protect signature storage');
        }
        return $root;
    }

    private function storagePath(): string
    {
        return Yii::getAlias($this->module->storagePath);
    }

    public function image(int $id, int $agency, $version = null): array
    {
        (new CompraAccessService())->find($id, $agency);
        $query = CompraFirma::find()->where(['idcompra' => $id]);
        if ($version !== null) {
            $query->andWhere(['version' => CompraAccessService::integer($version, 'version')]);
        }
        $signature = $query->orderBy(['version' => SORT_DESC])->one();
        $expectedPath = '/^' . preg_quote((string) $id, '/') . '\/[a-f0-9]{48}\.png$/D';
        if (!$signature || !preg_match($expectedPath, $signature->file_path)) {
            throw new ApiException(404, 'signature_not_found', 'Firma no encontrada.');
        }
        $path = $this->storageRoot() . '/' . $signature->file_path;
        if (!is_file($path)) {
            throw new ApiException(404, 'signature_not_found', 'La imagen de la firma no está disponible.');
        }
        return [$path, $signature];
    }

    /**
     * Obtiene la ruta de la última imagen para el comprobante web.
     * El controlador de impresión debe comprobar previamente el acceso a la compra.
     */
    public function latestImageForPrint(int $id): ?string
    {
        $signature = CompraFirma::find()->where(['idcompra' => $id])
            ->orderBy(['version' => SORT_DESC])->one();
        $expectedPath = '/^' . preg_quote((string) $id, '/') . '\/[a-f0-9]{48}\.png$/D';
        if (!$signature || !preg_match($expectedPath, $signature->file_path)) {
            return null;
        }
        $path = $this->storagePath() . '/' . $signature->file_path;
        return is_file($path) ? $path : null;
    }
}
