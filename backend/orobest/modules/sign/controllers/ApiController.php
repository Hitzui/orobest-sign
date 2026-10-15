<?php

namespace app\modules\sign\controllers;

use app\modules\sign\components\ApiException;
use app\modules\sign\components\SignBearerAuth;
use Yii;
use yii\web\HttpException;
use yii\web\Response;

abstract class ApiController extends \yii\rest\Controller
{
    public function init()
    {
        parent::init();
        Yii::$app->user->enableSession = false;
        Yii::$app->user->enableAutoLogin = false;
        Yii::$app->response->format = Response::FORMAT_JSON;
        Yii::$app->response->headers->set('Cache-Control', 'no-store');
    }

    public function behaviors()
    {
        $behaviors = parent::behaviors();
        unset($behaviors['rateLimiter']);
        $behaviors['authenticator'] = ['class' => SignBearerAuth::class];
        return $behaviors;
    }

    public function runAction($id, $params = [])
    {
        try {
            return parent::runAction($id, $params);
        } catch (HttpException $e) {
            Yii::$app->response->statusCode = $e->statusCode;
            return ['success' => false, 'error' => [
                'code' => $e instanceof ApiException ? $e->errorCode : 'http_' . $e->statusCode,
                'message' => $e->getMessage(),
            ]];
        } catch (\Throwable $e) {
            Yii::error('Sign API failure: ' . get_class($e), 'sign');
            Yii::$app->response->statusCode = 500;
            return ['success' => false, 'error' => [
                'code' => 'server_error', 'message' => 'No fue posible completar la operación.',
            ]];
        }
    }
}
