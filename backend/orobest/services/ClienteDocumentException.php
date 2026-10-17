<?php

namespace app\services;

class ClienteDocumentException extends \yii\web\HttpException
{
    public string $errorCode;

    public function __construct(int $status, string $errorCode, string $message)
    {
        $this->errorCode = $errorCode;
        parent::__construct($status, $message);
    }
}
