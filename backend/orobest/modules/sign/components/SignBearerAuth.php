<?php

namespace app\modules\sign\components;

use app\modules\sign\models\SignApiToken;
use app\modules\sign\services\AuthService;
use Yii;

class SignBearerAuth extends \yii\filters\auth\HttpBearerAuth
{
    public ?SignApiToken $session = null;

    public function authenticate($user, $request, $response)
    {
        $header = $request->headers->get('Authorization', '');
        if (!preg_match('/^Bearer\s+(\S+)$/iD', $header, $matches)) {
            throw new ApiException(401, 'session_required', 'Se requiere una sesión de OroBest Sign.');
        }
        [$identity, $this->session] = (new AuthService(Yii::$app->getModule('sign')))->authenticate($matches[1]);
        $user->setIdentity($identity);
        return $identity;
    }
}
