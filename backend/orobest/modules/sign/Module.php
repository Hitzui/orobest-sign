<?php

namespace app\modules\sign;

class Module extends \yii\base\Module
{
    public $controllerNamespace = 'app\modules\sign\controllers';
    public int $tokenTtlSeconds = 7776000;
    public string $storagePath = '@runtime/signatures';
}
