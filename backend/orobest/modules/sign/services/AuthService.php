<?php

namespace app\modules\sign\services;

use app\models\Agencia;
use app\models\Usuario;
use app\modules\sign\components\ApiException;
use app\modules\sign\models\SignApiToken;
use app\modules\sign\Module;
use Yii;

class AuthService
{
    public function __construct(private Module $module)
    {
    }

    public function login(array $params): array
    {
        $username = $params['username'] ?? null;
        $password = $params['password'] ?? null;
        if (!is_string($username) || !is_string($password) || trim($username) === '' || $password === ''
            || strlen($username) > 255 || strlen($password) > 4096) {
            throw new ApiException(422, 'invalid_credentials_input', 'Usuario y contraseña son obligatorios.');
        }
        $users = Usuario::find()->where(['username' => trim($username), 'activo' => 1])->limit(2)->all();
        if (count($users) !== 1 || !$users[0]->password || !$users[0]->validatePassword($password)) {
            throw new ApiException(401, 'invalid_credentials', 'No fue posible validar el usuario y la contraseña.');
        }
        $user = $users[0];
        $this->agency($user);
        $plain = Yii::$app->security->generateRandomString(64);
        $token = new SignApiToken();
        $token->setAttributes([
            'idusuario' => $user->idusuario,
            'token_hash' => hash('sha256', $plain),
            'created_at' => gmdate('Y-m-d H:i:s'),
            'expires_at' => gmdate('Y-m-d H:i:s', time() + $this->module->tokenTtlSeconds),
        ], false);
        $token->save(false);
        return ['access_token' => $plain, 'token_type' => 'Bearer'] + $this->profile($user, $token);
    }

    public function authenticate(string $plain): array
    {
        if (!preg_match('/^[A-Za-z0-9_-]{64}$/D', $plain)) {
            throw new ApiException(401, 'invalid_session', 'La sesión no es válida.');
        }
        $token = SignApiToken::find()->where([
            'token_hash' => hash('sha256', $plain), 'revoked_at' => null,
        ])->andWhere(['>', 'expires_at', gmdate('Y-m-d H:i:s')])->one();
        $user = $token ? Usuario::findOne(['idusuario' => $token->idusuario, 'activo' => 1]) : null;
        if (!$token || !$user) {
            throw new ApiException(401, 'invalid_session', 'La sesión venció o fue revocada.');
        }
        $this->agency($user);
        // Ventana móvil de 90 días; no depende de los 10 minutos de sesión web.
        $token->updateAttributes([
            'last_used_at' => gmdate('Y-m-d H:i:s'),
            'expires_at' => gmdate('Y-m-d H:i:s', time() + $this->module->tokenTtlSeconds),
        ]);
        return [$user, $token];
    }

    public function agency(Usuario $user): Agencia
    {
        $agency = $user->idagencia ? Agencia::findOne(['idagencia' => $user->idagencia, 'activo' => 1]) : null;
        if (!$agency) {
            throw new ApiException(403, 'agency_unavailable', 'El usuario no tiene una agencia activa asignada.');
        }
        return $agency;
    }

    public function profile(Usuario $user, SignApiToken $token): array
    {
        $agency = $this->agency($user);
        return [
            'expires_at' => gmdate('Y-m-d\TH:i:s\Z', strtotime($token->expires_at . ' UTC')),
            'usuario' => [
                'idusuario' => (int) $user->idusuario,
                'username' => $user->username,
                'nombre' => $user->nombre,
            ],
            'agencia' => [
                'idagencia' => (int) $agency->idagencia,
                'codagencia' => $agency->codagencia,
                'descripcion' => $agency->descripcion,
            ],
        ];
    }
}
