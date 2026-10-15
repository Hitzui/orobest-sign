<?php

/** Configuración independiente del módulo; no contiene credenciales. */
return [
    'modules' => [
        'sign' => [
            'class' => app\modules\sign\Module::class,
            'tokenTtlSeconds' => 90 * 24 * 3600,
            // En hosting puede configurarse una ruta absoluta fuera de public_html.
            'storagePath' => getenv('OROBEST_SIGN_STORAGE_PATH') ?: '@runtime/signatures',
        ],
    ],
    'components' => [
        'urlManager' => [
            'rules' => [
                'api/sign/v1/auth/<action:login|logout|me>' => 'sign/auth/<action>',
                'api/sign/v1/compras/<idcompra:\d+>/firma' => 'sign/compras/firma',
                'api/sign/v1/compras/<idcompra:\d+>' => 'sign/compras/view',
                'api/sign/v1/compras' => 'sign/compras/index',
            ],
        ],
    ],
];
