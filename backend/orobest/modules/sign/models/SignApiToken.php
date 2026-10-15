<?php

namespace app\modules\sign\models;

class SignApiToken extends \yii\db\ActiveRecord
{
    public static function tableName()
    {
        return 'sign_api_token';
    }
}
