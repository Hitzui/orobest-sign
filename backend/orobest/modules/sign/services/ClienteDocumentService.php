<?php

namespace app\modules\sign\services;

use app\modules\sign\Module;

/** Adaptador de Sign: web y móvil comparten validación, almacenamiento y lectura. */
class ClienteDocumentService extends \app\services\ClienteDocumentService
{
    public function __construct(Module $module)
    {
        parent::__construct($module->clientDocumentsStoragePath);
    }
}
