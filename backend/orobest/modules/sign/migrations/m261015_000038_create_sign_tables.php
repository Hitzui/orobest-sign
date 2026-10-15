<?php

use yii\db\Migration;

/** Migración aislada de Sign: solo crea tablas auxiliares. */
class m261015_000038_create_sign_tables extends Migration
{
    public function safeUp()
    {
        $options = 'ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci';
        $this->createTable('sign_api_token', [
            'idtoken' => $this->primaryKey(),
            'idusuario' => $this->integer()->notNull(),
            'token_hash' => $this->char(64)->notNull(),
            'created_at' => $this->dateTime()->notNull(),
            'expires_at' => $this->dateTime()->notNull(),
            'last_used_at' => $this->dateTime()->null(),
            'revoked_at' => $this->dateTime()->null(),
        ], $options);
        $this->createIndex('uq_sign_token_hash', 'sign_api_token', 'token_hash', true);
        $this->createIndex('idx_sign_token_user', 'sign_api_token', 'idusuario');
        $this->addForeignKey('fk_sign_token_user', 'sign_api_token', 'idusuario', 'usuario', 'idusuario', 'RESTRICT', 'CASCADE');

        $this->createTable('compra_firma', [
            'idfirma' => $this->primaryKey(),
            'idcompra' => $this->integer()->notNull(),
            'version' => $this->integer()->notNull(),
            'idusuario' => $this->integer()->notNull(),
            'idagencia' => $this->integer()->notNull(),
            'operation_id' => $this->char(36)->notNull(),
            'compra_revision' => $this->char(64)->notNull(),
            'file_path' => $this->string(255)->notNull(),
            'sha256' => $this->char(64)->notNull(),
            'mime_type' => $this->string(50)->notNull()->defaultValue('image/png'),
            'size_bytes' => $this->integer()->notNull(),
            'width' => $this->integer()->notNull(),
            'height' => $this->integer()->notNull(),
            'created_at' => $this->dateTime()->notNull(),
        ], $options);
        $this->createIndex('uq_compra_firma_version', 'compra_firma', ['idcompra', 'version'], true);
        $this->createIndex('uq_compra_firma_operation', 'compra_firma', ['idusuario', 'operation_id'], true);
        $this->addForeignKey('fk_compra_firma_compra', 'compra_firma', 'idcompra', 'compra', 'idcompra', 'RESTRICT', 'CASCADE');
        $this->addForeignKey('fk_compra_firma_user', 'compra_firma', 'idusuario', 'usuario', 'idusuario', 'RESTRICT', 'CASCADE');
        $this->addForeignKey('fk_compra_firma_agency', 'compra_firma', 'idagencia', 'agencia', 'idagencia', 'RESTRICT', 'CASCADE');
    }

    public function safeDown()
    {
        $this->dropTable('compra_firma');
        $this->dropTable('sign_api_token');
    }
}
