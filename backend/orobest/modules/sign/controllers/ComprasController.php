<?php

namespace app\modules\sign\controllers;

use app\modules\sign\components\ApiException;
use app\modules\sign\services\CompraAccessService;
use app\modules\sign\services\FirmaService;
use Yii;
use yii\web\UploadedFile;

class ComprasController extends ApiController
{
    protected function verbs()
    {
        return ['index' => ['GET'], 'view' => ['GET'], 'firma' => ['GET', 'POST']];
    }

    public function actionIndex()
    {
        return ['success' => true, 'data' => (new CompraAccessService())->listing(
            (int) Yii::$app->user->identity->idagencia, Yii::$app->request->queryParams
        )];
    }

    public function actionView($idcompra)
    {
        return ['success' => true, 'data' => (new CompraAccessService())->detail(
            CompraAccessService::integer($idcompra, 'idcompra'), (int) Yii::$app->user->identity->idagencia
        )];
    }

    public function actionFirma($idcompra)
    {
        $id = CompraAccessService::integer($idcompra, 'idcompra');
        $user = Yii::$app->user->identity;
        $service = new FirmaService($this->module);
        if (Yii::$app->request->isGet) {
            [$path, $signature] = $service->image($id, (int) $user->idagencia, Yii::$app->request->get('version'));
            Yii::$app->response->headers->set('X-Content-Type-Options', 'nosniff');
            return Yii::$app->response->sendFile($path, 'firma_' . $id . '_v' . $signature->version . '.png', [
                'mimeType' => 'image/png', 'inline' => true,
            ]);
        }
        // Comprobar alcance antes de procesar archivos de otra agencia.
        (new CompraAccessService())->find($id, (int) $user->idagencia);
        $file = UploadedFile::getInstanceByName('firma');
        if (!$file || $file->error !== UPLOAD_ERR_OK) {
            $tooLarge = $file && in_array($file->error, [UPLOAD_ERR_INI_SIZE, UPLOAD_ERR_FORM_SIZE], true);
            throw new ApiException($tooLarge ? 413 : 422, $tooLarge ? 'image_too_large' : 'image_required', 'Se requiere un archivo PNG en el campo firma.');
        }
        return ['success' => true, 'data' => $service->save(
            $id, (int) $user->idusuario, (int) $user->idagencia, Yii::$app->request->post(), $file->tempName
        )];
    }
}
