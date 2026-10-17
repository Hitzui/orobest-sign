<?php

namespace app\modules\sign\controllers;

use app\modules\sign\components\ApiException;
use app\modules\sign\services\ClienteDocumentService;
use app\modules\sign\services\CompraAccessService;
use Yii;
use yii\web\UploadedFile;

class ClientesController extends ApiController
{
    protected function verbs()
    {
        return [
            'index' => ['GET'],
            'view' => ['GET'],
            'documentos' => ['GET', 'POST'],
            'contenido' => ['GET'],
            'eliminar-documento' => ['DELETE'],
        ];
    }

    private function service(): ClienteDocumentService
    {
        return new ClienteDocumentService($this->module);
    }

    public function actionIndex()
    {
        return ['success' => true, 'data' => $this->service()->listing(Yii::$app->request->queryParams)];
    }

    public function actionView($idcliente)
    {
        $id = CompraAccessService::integer($idcliente, 'idcliente');
        return ['success' => true, 'data' => $this->service()->detail($id)];
    }

    public function actionDocumentos($idcliente)
    {
        $id = CompraAccessService::integer($idcliente, 'idcliente');
        $service = $this->service();
        if (Yii::$app->request->isGet) {
            return ['success' => true, 'data' => $service->documents($id)];
        }
        $file = UploadedFile::getInstanceByName('archivo');
        if (!$file || $file->error !== UPLOAD_ERR_OK || !$file->tempName) {
            $tooLarge = $file && in_array($file->error, [UPLOAD_ERR_INI_SIZE, UPLOAD_ERR_FORM_SIZE], true);
            throw new ApiException($tooLarge ? 413 : 422, $tooLarge ? 'document_too_large' : 'document_required', 'Se requiere un archivo en el campo archivo.');
        }
        $category = Yii::$app->request->post('tipo_documento', '');
        $operationId = Yii::$app->request->post('operation_id', '');
        $name = Yii::$app->request->post('nombre', $file->name);
        if (!is_string($category) || !is_string($operationId) || !is_string($name)) {
            throw new ApiException(422, 'invalid_document', 'Los datos del documento no son válidos.');
        }
        return ['success' => true, 'data' => $service->save($id, $category, $operationId, $name, $file->tempName, $file->name)];
    }

    public function actionContenido($idcliente, $idarchivo)
    {
        $id = CompraAccessService::integer($idcliente, 'idcliente');
        $fileId = CompraAccessService::integer($idarchivo, 'idarchivo');
        [$path, $mime] = $this->service()->image($id, $fileId);
        Yii::$app->response->headers->set('X-Content-Type-Options', 'nosniff');
        $record = \app\models\ArchivosClientes::findOne($fileId);
        return Yii::$app->response->sendFile($path, $record->nombre_original, [
            'mimeType' => $mime,
            'inline' => in_array($mime, ['image/jpeg', 'image/png', 'application/pdf', 'text/plain'], true),
        ]);
    }

    public function actionEliminarDocumento($idcliente, $idarchivo)
    {
        $id = CompraAccessService::integer($idcliente, 'idcliente');
        $fileId = CompraAccessService::integer($idarchivo, 'idarchivo');
        $this->service()->delete($id, $fileId);
        return ['success' => true, 'data' => ['deleted' => true]];
    }
}
