<?php

use yii\db\Migration;

/** Permite autorizar cuentas específicas para imprimir compras sin firma. */
class m261022_000039_add_imprimir_sin_firma_to_usuario extends Migration
{
    public function safeUp()
    {
        $this->addColumn('usuario', 'imprimir_sin_firma', $this->boolean()->notNull()->defaultValue(false));
    }

    public function safeDown()
    {
        $this->dropColumn('usuario', 'imprimir_sin_firma');
    }
}
