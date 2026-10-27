<?php

use yii\db\Migration;

/** Guarda la imagen PNG de la huella del cliente como Base64 en la compra. */
class m261026_000040_add_huella_to_compra extends Migration
{
    public function safeUp()
    {
        $this->addColumn('{{%compra}}', 'huella', $this->longText()->null()->after('total'));
    }

    public function safeDown()
    {
        $this->dropColumn('{{%compra}}', 'huella');
    }
}
