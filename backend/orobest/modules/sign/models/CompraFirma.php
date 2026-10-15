<?php

namespace app\modules\sign\models;

class CompraFirma extends \yii\db\ActiveRecord
{
    public static function tableName()
    {
        return 'compra_firma';
    }

    public function metadata(): array
    {
        return [
            'idfirma' => (int) $this->idfirma,
            'idcompra' => (int) $this->idcompra,
            'version' => (int) $this->version,
            'idusuario' => (int) $this->idusuario,
            'compra_revision' => $this->compra_revision,
            'sha256' => $this->sha256,
            'mime_type' => $this->mime_type,
            'size_bytes' => (int) $this->size_bytes,
            'width' => (int) $this->width,
            'height' => (int) $this->height,
            'created_at' => gmdate('Y-m-d\TH:i:s\Z', strtotime($this->created_at . ' UTC')),
        ];
    }
}
