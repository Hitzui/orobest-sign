<?php

/** Aplica exclusivamente migraciones de Sign y comprueba el esquema preexistente. */
$webRoot = $argv[1] ?? 'C:/xampp/htdocs/orobest';
$dbConfig = require $webRoot . '/config/db.php';
if (!preg_match('/^mysql:host=(localhost|127\.0\.0\.1)(;|$)/D', $dbConfig['dsn'])) {
    throw new RuntimeException('Este comando solo permite la base MySQL local.');
}
defined('YII_DEBUG') or define('YII_DEBUG', false);
defined('YII_ENV') or define('YII_ENV', 'prod');
require $webRoot . '/vendor/autoload.php';
require $webRoot . '/vendor/yiisoft/yii2/Yii.php';
$app = new yii\console\Application(require $webRoot . '/config/console.php');
$before = [];
foreach (['usuario', 'agencia', 'caja', 'compra'] as $table) {
    $before[$table] = $app->db->createCommand("SHOW CREATE TABLE `$table`")->queryOne()['Create Table'];
}
$app->request->setParams(['migrate/up', '--migrationPath=@app/modules/sign/migrations', '--interactive=0']);
$exit = $app->run();
foreach ($before as $table => $schema) {
    if ($schema !== $app->db->createCommand("SHOW CREATE TABLE `$table`")->queryOne()['Create Table']) {
        throw new RuntimeException('Cambió una tabla preexistente: ' . $table);
    }
}
echo "Esquema de usuario, agencia, caja y compra sin cambios.\n";
exit($exit);
