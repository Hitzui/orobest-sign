<?php

/** Integración en MySQL local efímero, exclusivamente con datos sintéticos. */
$webRoot = $argv[1] ?? 'C:/xampp/htdocs/orobest';
defined('YII_DEBUG') or define('YII_DEBUG', false);
defined('YII_ENV') or define('YII_ENV', 'test');
defined('YII_ENABLE_ERROR_HANDLER') or define('YII_ENABLE_ERROR_HANDLER', false);
require $webRoot . '/vendor/autoload.php';
require $webRoot . '/vendor/yiisoft/yii2/Yii.php';
$config = require $webRoot . '/config/db.php';
if (!preg_match('/^mysql:host=(localhost|127\.0\.0\.1)(;|$)/D', $config['dsn'])) {
    throw new RuntimeException('Las pruebas solo admiten MySQL local.');
}
$database = 'orobest_sign_test_' . bin2hex(random_bytes(5));
$admin = new PDO($config['dsn'], $config['username'], $config['password'], [PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION]);
$admin->exec("CREATE DATABASE `$database` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
$storage = sys_get_temp_dir() . '/orobest-sign-' . bin2hex(random_bytes(8));
$checks = 0;
function check(bool $condition, string $label): void
{
    global $checks;
    if (!$condition) {
        throw new RuntimeException('FAILED: ' . $label);
    }
    $checks++;
    echo "OK: $label\n";
}
function rejects(callable $operation, string $code): void
{
    try {
        $operation();
    } catch (app\modules\sign\components\ApiException $e) {
        check($e->errorCode === $code, $code);
        return;
    }
    throw new RuntimeException('Expected rejection: ' . $code);
}
function api(string $route, string $method, ?string $token, array $body = [], array $params = [])
{
    $app = Yii::$app;
    $request = new class extends yii\web\Request {
        public string $testMethod = 'GET';
        public function getMethod() { return $this->testMethod; }
    };
    $request->testMethod = $method;
    $request->setBodyParams($body);
    $request->headers->set('Accept', 'application/json');
    if ($token !== null) {
        $request->headers->set('Authorization', 'Bearer ' . $token);
    }
    $app->set('request', $request);
    $app->set('response', new yii\web\Response());
    $app->user->setIdentity(null);
    return $app->runAction($route, $params);
}
try {
    $config['dsn'] = preg_replace('/dbname=[^;]+/', 'dbname=' . $database, $config['dsn']);
    $sign = require dirname(__DIR__) . '/orobest/config/sign.php';
    $sign['modules']['sign']['storagePath'] = $storage;
    $app = new yii\web\Application(yii\helpers\ArrayHelper::merge([
        'id' => 'sign-integration', 'basePath' => $webRoot, 'vendorPath' => $webRoot . '/vendor',
        'components' => [
            'db' => $config,
            'request' => ['cookieValidationKey' => 'synthetic-test-only', 'scriptFile' => $webRoot . '/index.php', 'scriptUrl' => '/index.php'],
            'user' => ['identityClass' => app\models\Usuario::class, 'enableSession' => false],
        ],
    ], $sign));
    Yii::setAlias('@app/modules/sign', dirname(__DIR__) . '/orobest/modules/sign');
    $db = $app->db;
    foreach ([
        'CREATE TABLE agencia (idagencia INT PRIMARY KEY, codagencia VARCHAR(10), descripcion VARCHAR(150), activo TINYINT NOT NULL) ENGINE=InnoDB',
        'CREATE TABLE caja (idcaja INT PRIMARY KEY, idagencia INT, activo TINYINT) ENGINE=InnoDB',
        'CREATE TABLE usuario (idusuario INT PRIMARY KEY, username VARCHAR(255), nombre VARCHAR(255), password VARCHAR(255), activo BIT(1) NOT NULL, idagencia INT, idcaja INT) ENGINE=InnoDB',
        'CREATE TABLE cliente (idcliente INT PRIMARY KEY, nombres VARCHAR(255), apellidos VARCHAR(255)) ENGINE=InnoDB',
        'CREATE TABLE compra (idcompra INT PRIMARY KEY, numcompra VARCHAR(255), fecha DATETIME, idcaja INT, estado INT, idusuario INT, idcliente INT, forma_pago VARCHAR(255), efectivo DECIMAL(12,3), transferencia DECIMAL(12,3), adelanto DECIMAL(12,3), total DECIMAL(12,3), subtotal DECIMAL(12,3), descuento DECIMAL(12,3), updated_at DATETIME) ENGINE=InnoDB',
        'CREATE TABLE deta_compra (iddetacompra INT PRIMARY KEY, idcompra INT, linea INT, descripcion TEXT, kilataje VARCHAR(255), kilate DECIMAL(12,3), precio DECIMAL(12,3), peso DECIMAL(12,3), importe DECIMAL(12,3), lectura INT) ENGINE=InnoDB',
    ] as $sql) {
        $db->createCommand($sql)->execute();
    }
    require dirname(__DIR__) . '/orobest/modules/sign/migrations/m261015_000038_create_sign_tables.php';
    (new m261015_000038_create_sign_tables(['db' => $db]))->up();
    $db->createCommand()->batchInsert('agencia', ['idagencia', 'codagencia', 'descripcion', 'activo'], [[1, 'A', 'Agencia A', 1], [2, 'B', 'Agencia B', 1]])->execute();
    $db->createCommand()->batchInsert('caja', ['idcaja', 'idagencia', 'activo'], [[1, 1, 1], [2, 1, 1], [3, 2, 1]])->execute();
    $password = $app->security->generatePasswordHash('synthetic-password');
    $db->createCommand()->batchInsert('usuario', ['idusuario', 'username', 'nombre', 'password', 'activo', 'idagencia', 'idcaja'], [
        [1, 'cashier', 'Caja sintética', $password, 1, 1, 1], [2, 'other', 'Otra cuenta', $password, 1, 2, 3],
        [3, 'inactive', 'Inactiva', $password, 0, 1, 1],
        [4, 'duplicate', 'Duplicada A', $password, 1, 1, 1], [5, 'duplicate', 'Duplicada B', $password, 1, 1, 1],
    ])->execute();
    foreach ([[1, 1, 1, 1], [2, 2, 1, 2], [3, 3, 1, 2], [4, 1, 0, 1]] as [$id, $cashbox, $state, $creator]) {
        $db->createCommand()->insert('compra', [
            'idcompra' => $id, 'numcompra' => 'TEST-' . $id, 'fecha' => '2026-10-15 08:00:00',
            'idcaja' => $cashbox, 'estado' => $state, 'idusuario' => $creator, 'total' => '123.456', 'updated_at' => '2026-10-15 08:00:00',
        ])->execute();
    }
    $module = $app->getModule('sign');
    $auth = new app\modules\sign\services\AuthService($module);
    $session = $auth->login(['username' => 'cashier', 'password' => 'synthetic-password']);
    [$user, $token] = $auth->authenticate($session['access_token']);
    check((int) $user->idusuario === 1, 'usuario sin rol especial puede iniciar sesión');
    check($token->token_hash !== $session['access_token'], 'solo hash del token en base de datos');
    $token->updateAttributes(['expires_at' => gmdate('Y-m-d H:i:s', time() + 3600)]);
    [, $renewed] = $auth->authenticate($session['access_token']);
    check(strtotime($renewed->expires_at . ' UTC') > time() + 89 * 86400, 'sesión persistente renovada con el uso');
    rejects(fn () => $auth->login(['username' => 'inactive', 'password' => 'synthetic-password']), 'invalid_credentials');
    rejects(fn () => $auth->login(['username' => 'duplicate', 'password' => 'synthetic-password']), 'invalid_credentials');
    $access = new app\modules\sign\services\CompraAccessService();
    $listing = $access->listing(1, []);
    check(array_column($listing['items'], 'idcompra') === [2, 1], 'todas las cajas y creadores de la agencia');
    check($listing['items'][0]['total'] === '123.456', 'precisión decimal');
    check(str_ends_with($listing['items'][0]['fecha'], '-06:00'), 'fecha con zona de Managua');
    rejects(fn () => $access->find(3, 1), 'purchase_not_found');
    rejects(fn () => $access->listing(1, ['idagencia' => 2]), 'scope_not_selectable');
    rejects(fn () => $access->listing(1, ['desde' => '2026-02-31']), 'invalid_date');
    $service = new app\modules\sign\services\FirmaService($module);
    $image = imagecreatetruecolor(120, 60);
    imagefill($image, 0, 0, imagecolorallocate($image, 255, 255, 255));
    imageline($image, 5, 5, 100, 50, imagecolorallocate($image, 0, 0, 0));
    imagepng($image, $storage . '.png');
    imagedestroy($image);
    $png = $storage . '.png';
    $params = ['operation_id' => '11111111-1111-4111-8111-111111111111', 'compra_revision' => $access->detail(1, 1)['revision'], 'base_version' => 0];
    $first = $service->save(1, 1, 1, $params, $png);
    check($first['firma']['version'] === 1 && !$first['replayed'], 'primera firma');
    $retry = $service->save(1, 1, 1, $params, $png);
    check($retry['replayed'] && $retry['firma']['idfirma'] === $first['firma']['idfirma'], 'reintento idempotente');
    rejects(fn () => $service->save(3, 1, 1, $params, $png), 'purchase_not_found');
    $edit = array_replace($params, ['base_version' => 1, 'operation_id' => '22222222-2222-4222-8222-222222222222']);
    check($service->save(1, 1, 1, $edit, $png)['firma']['version'] === 2, 'editar crea versión nueva');
    [$oldPath] = $service->image(1, 1, 1);
    [$latestPath] = $service->image(1, 1);
    check($oldPath !== $latestPath && is_file($oldPath), 'historial conserva imágenes');
    check($service->latestImageForPrint(1) === $latestPath, 'comprobante obtiene la última imagen privada');
    check($service->latestImageForPrint(2) === null, 'compra sin firma conserva el espacio de firma manual');
    $stale = array_replace($params, ['operation_id' => '33333333-3333-4333-8333-333333333333']);
    rejects(fn () => $service->save(1, 1, 1, $stale, $png), 'signature_changed');
    $db->createCommand()->update('compra', ['total' => '999.000'], ['idcompra' => 1])->execute();
    $changed = array_replace($edit, ['base_version' => 2, 'operation_id' => '44444444-4444-4444-8444-444444444444']);
    rejects(fn () => $service->save(1, 1, 1, $changed, $png), 'purchase_changed');
    check($access->detail(1, 1)['firma']['compra_modificada'], 'detecta cambios de compra firmada');
    $inactive = array_replace($params, ['compra_revision' => $access->detail(4, 1)['revision'], 'operation_id' => '55555555-5555-4555-8555-555555555555']);
    rejects(fn () => $service->save(4, 1, 1, $inactive, $png), 'purchase_not_active');
    rejects(fn () => $service->image(1, 2), 'purchase_not_found');
    check($access->listing(1, ['firma' => 'firmada'])['pagination']['total'] === 1, 'filtro de firmadas');

    $result = api('sign/auth/login', 'POST', null, ['username' => 'cashier', 'password' => 'synthetic-password']);
    check($result['success'] && isset($result['data']['access_token']), 'login por controlador HTTP');
    $httpToken = $result['data']['access_token'];
    $result = api('sign/auth/me', 'GET', $httpToken);
    check($result['data']['agencia']['idagencia'] === 1, 'perfil autenticado HTTP');
    $result = api('sign/auth/login', 'GET', null);
    check($app->response->statusCode === 405, 'login rechaza método incorrecto');
    $result = api('sign/compras/index', 'GET', $httpToken);
    check(array_column($result['data']['items'], 'idcompra') === [2, 1], 'listado HTTP por agencia');
    $result = api('sign/compras/view', 'GET', $httpToken, [], ['idcompra' => 3]);
    check($app->response->statusCode === 404, 'detalle HTTP bloquea otra agencia');
    $uploadParams = ['operation_id' => '66666666-6666-4666-8666-666666666666', 'compra_revision' => $access->detail(2, 1)['revision'], 'base_version' => 0];
    $_FILES = ['firma' => ['name' => 'firma.png', 'type' => 'image/png', 'tmp_name' => $png, 'error' => UPLOAD_ERR_OK, 'size' => filesize($png)]];
    yii\web\UploadedFile::reset();
    $result = api('sign/compras/firma', 'POST', $httpToken, $uploadParams, ['idcompra' => 2]);
    check($result['success'] && $result['data']['firma']['version'] === 1, 'carga multipart mediante controlador');
    $binary = api('sign/compras/firma', 'GET', $httpToken, [], ['idcompra' => 2]);
    check($binary instanceof yii\web\Response && $binary->headers->get('Content-Type') === 'image/png', 'descarga PNG autenticada');
    check($binary->headers->get('Cache-Control') === 'no-store', 'imagen sin caché');
    if (is_array($binary->stream) && is_resource($binary->stream[0])) {
        fclose($binary->stream[0]);
    }
    $result = api('sign/auth/logout', 'POST', $httpToken);
    check($result['data']['logged_out'], 'logout HTTP');
    rejects(fn () => $auth->authenticate($httpToken), 'invalid_session');
    $db->createCommand()->update('agencia', ['activo' => 0], ['idagencia' => 1])->execute();
    rejects(fn () => $auth->authenticate($session['access_token']), 'agency_unavailable');
    $db->createCommand()->update('agencia', ['activo' => 1], ['idagencia' => 1])->execute();
    $db->createCommand()->update('usuario', ['idagencia' => 2], ['idusuario' => 1])->execute();
    [$movedUser] = $auth->authenticate($session['access_token']);
    check((int) $movedUser->idagencia === 2, 'sesión respeta la agencia actual tras reasignación');
    $db->createCommand()->update('usuario', ['idagencia' => 1], ['idusuario' => 1])->execute();
    $token->updateAttributes(['expires_at' => gmdate('Y-m-d H:i:s', time() - 10)]);
    rejects(fn () => $auth->authenticate($session['access_token']), 'invalid_session');
    $response = api('sign/auth/me', 'GET', $session['access_token']);
    check($app->response->statusCode === 401 && $response['error']['code'] === 'invalid_session', 'controlador HTTP devuelve error JSON');
    echo "PASS: $checks comprobaciones\n";
} finally {
    if (isset($app)) {
        $app->db->close();
    }
    $admin->exec("DROP DATABASE `$database`");
    if (is_dir($storage)) {
        yii\helpers\FileHelper::removeDirectory($storage);
    }
    if (is_file($storage . '.png')) {
        unlink($storage . '.png');
    }
}
