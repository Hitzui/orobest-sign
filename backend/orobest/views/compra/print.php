<?php
/** @var Compra $compra */

/** @var \app\models\Cliente $cliente */

use app\models\Compra;
use app\services\HelperServices;

if (count($cliente->clienteJuridicos)>0) {
    $clienteJuridico = $cliente->clienteJuridicos[0];
} else {
    $clienteJuridico = null;
}

$fecha = date('d/m/y H:i A', strtotime($compra->fecha));
$nombreCompleto = $compra->cliente->nombres . " " . $compra->cliente->apellidos;
$totalGramos = 0;
$totalOnzas = 0;
$logoPath = Yii::getAlias('@app/img/logo.png');
$logoContenido = is_file($logoPath) ? @file_get_contents($logoPath) : false;
$logoDataUri = $logoContenido !== false
    ? 'data:image/png;base64,' . base64_encode($logoContenido)
    : null;
?>
<!-- Encabezado -->
<div class="header">
    <table class="no-border" style="width: 100%; margin: 0;">
        <tr>
            <td style="width: 22%; text-align: left; vertical-align: middle;">
                <?php if ($logoDataUri): ?>
                    <img src="<?= $logoDataUri ?>" alt="Logo Oro Best" style="width: 105px; height: auto;">
                <?php endif; ?>
            </td>
            <td style="width: 78%; text-align: center; vertical-align: middle;">
                <div style="font-size: 16pt; font-weight: bold; line-height: 1.15;">Oro &amp; Best</div>
                <div style="font-size: 12pt; line-height: 1.2;">Mejores en Precio, Atención y Calidad</div>
                <div style="font-size: 10pt; line-height: 1.2;">ROTONDA BELLO HORIZONTE 1 C. Y MEDIA AL SUR TEL.: 86465077 - 86323334</div>
            </td>
        </tr>
    </table>
    <h3 style="font-size: 18pt; margin: 6px 0 8px;">COMPROBANTE DE COMPRA</h3>
</div>

<!-- Datos generales -->
<table class="no-border">
    <tr>
        <td><b>Sucursal:</b> <?= $agencia ?></td>
        <td><b>Compra No.:</b> <?= $compra->numcompra ?></td>
    </tr>
    <tr>
        <td><b>Cliente:</b> <?= $cliente->idcliente ?>
            &nbsp; <?= $clienteJuridico ? $clienteJuridico->nombre_comercial : $nombreCompleto ?></td>
        <td><b>Fecha:</b> <?= $fecha ?></td>
    </tr>
    <tr>
        <td><b><?= $clienteJuridico ? 'RUC': 'Cedula'?>:</b> <?= $clienteJuridico ? $clienteJuridico->ruc : $cliente->cedula ?></td>
        <td><b>Departamento:</b> <?= $departamento ?></td>
    </tr>
    <tr>
        <td><b>Dirección:</b> <?= $clienteJuridico ? $clienteJuridico->direccion_matriz : $cliente->direccion ?></td>
        <td><b>Teléfono:</b> <?= $cliente->telefono ?></td>
    </tr>
</table>

<!-- Tabla detalle -->
<table>
    <thead>
    <tr>
        <th>Kilataje</th>
        <th>Lectura</th>
        <th>Descripción</th>
        <th>Precio</th>
        <th>P. N gr.</th>
        <th>P. N Onz.</th>
        <th>Importe C$</th>
    </tr>
    </thead>
    <tbody>
    <?php foreach ($compra->detaCompras as $d):
        $onzas = ($d->peso / 31.1035) * ($d->kilate / 24);
        $totalOnzas += $onzas;
        $totalGramos += $d->peso;
        ?>
        <tr style="font-size: 8pt !important">
            <td style="width: 12%"><?= $d->kilataje ?></td>
            <td style="width: 12%"><?= $d->lectura ?></td>
            <td style="width: 30%"><?= $d->descripcion ?></td>
            <td style="width: 12%" class="right"><?= HelperServices::formatoNumero($d->precio) ?></td>
            <td style="width: 12%" class="right"><?= HelperServices::formatoNumero($d->peso) ?></td>
            <td style="width: 12%" class="right"><?= HelperServices::formatoNumero($onzas) ?></td>
            <td style="width: 12%" class="right"><?= HelperServices::formatoNumero($d->importe) ?></td>
        </tr>
    <?php endforeach; ?>
    <tr>
        <td colspan="4">Totales:</td>
        <td class="right"><b><?= HelperServices::formatoNumero($totalGramos) ?></b></td>
        <td class="right"><b><?= HelperServices::formatoNumero($totalOnzas) ?></b></td>
        <td class="right"><b>C$<?= HelperServices::formatoNumero($compra->total) ?></b></td>
    </tr>
    </tbody>
</table>

<!-- Totales -->
<table class="no-border" style="font-size: 10px;">
    <tr>
        <td><b>Atendido por:</b> <?= $usuario->nombre ?></td>
        <td class="right"><b>TOTALES:</b>

        </td>
    </tr>
</table>

<div class="bold">
    <!--  Aqui va el total en letras  -->
</div>

<!-- Declaración -->


<!-- Firma -->
<table style="width: 100%; border: none;">
    <tr>
        <td style="width: 50%; text-align: left; border: none; font-size: 10px">
            <div class="footer">
                <p style="text-align: justify">
                    <b>DECLARACIÓN JURÍDICA DE PROPIEDAD:</b><br>
                    Declaro bajo juramento que soy el legítimo propietario de los bienes que estoy vendiendo y
                    que son detallados en este COMPROBANTE DE COMPRA motivo por el cual eximo de cualquier
                    responsabilidad a <b>ORO BEST</b>.
                </p>
            </div>
        </td>
        <td style="width: 50%; text-align: center; border: none;">
            <div style="margin: 0 auto; text-align: center;">
                <?php if (!empty($firmaDataUri)): ?>
                    <div style="text-align: center; margin: 0 0 2mm;">
                        <img src="<?= $firmaDataUri ?>" alt="Firma del Cliente" style="width: 60mm; height: auto;">
                    </div>
                <?php else: ?>
                    <div style="margin: 0 0 2mm;">_________________________________</div>
                <?php endif; ?>
                <div style="line-height: 1.35;">
                    Firma del Cliente<br>
                    <?= $fecha ?>
                </div>
            </div>
        </td>
    </tr>
</table>
