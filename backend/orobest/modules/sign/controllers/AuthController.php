<?php

namespace app\modules\sign\controllers;

use app\modules\sign\services\AuthService;
use Yii;

class AuthController extends ApiController
{
    public function behaviors()
    {
        $behaviors = parent::behaviors();
        $behaviors['authenticator']['except'] = ['login'];
        return $behaviors;
    }

    protected function verbs()
    {
        return ['login' => ['POST'], 'logout' => ['POST'], 'me' => ['GET']];
    }

    public function actionLogin()
    {
        return ['success' => true, 'data' => (new AuthService($this->module))->login(Yii::$app->request->bodyParams)];
    }

    public function actionMe()
    {
        return ['success' => true, 'data' => (new AuthService($this->module))->profile(
            Yii::$app->user->identity, $this->getBehavior('authenticator')->session
        )];
    }

    public function actionLogout()
    {
        $this->getBehavior('authenticator')->session->updateAttributes(['revoked_at' => gmdate('Y-m-d H:i:s')]);
        return ['success' => true, 'data' => ['logged_out' => true]];
    }
}
