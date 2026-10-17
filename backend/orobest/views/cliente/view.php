<?php


/** @var yii\web\View $this */
/** @var ActiveDataProvider $adelantos */

/** @var app\models\Cliente $model */

use app\models\Adelantos;
use app\models\ArchivosClientes;
use app\models\CatalogoItem;
use app\services\HelperServices;
use kartik\detail\DetailView;
use kartik\form\ActiveForm;
use kartik\grid\GridView;
use yii\bootstrap5\Html;
use yii\bootstrap5\Modal;
use yii\data\ActiveDataProvider;
use yii\web\YiiAsset;

$this->title = 'Datos Cliente';
$this->params['breadcrumbs'][] = ['label' => 'Clientes', 'url' => ['index']];
$this->params['breadcrumbs'][] = $this->title;
YiiAsset::register($this);


$script = <<< JS
$(document).ready(function() {
    // Configuración de SweetAlert2
    const swalWithBootstrapButtons = Swal.mixin({
        customClass: {
            confirmButton: 'btn btn-danger mx-2',
            cancelButton: 'btn btn-secondary mx-2'
        },
        buttonsStyling: false,
        showClass: {
            popup: 'animate__animated animate__fadeInDown'
        },
        hideClass: {
            popup: 'animate__animated animate__fadeOutUp'
        }
    });

    // Evento para eliminar documento
    $(document).on('click', '.btn-eliminar-swal', function(e) {
        e.preventDefault();
        
        var \$button = $(this);
        var id = \$button.data('id');
        var nombre = \$button.data('nombre');
        var size = \$button.data('size');
        var fecha = \$button.data('fecha');
        
        swalWithBootstrapButtons.fire({
            title: '¿Está seguro?',
            html: '<div class="text-start">' +
                  '<p>¿Está seguro de eliminar el siguiente documento?</p>' +
                  '<div class="alert alert-warning mt-2">' +
                  '<strong>Nombre:</strong> ' + $('<div>').text(nombre).html() + '<br>' +
                  '<strong>Tamaño:</strong> ' + size + '<br>' +
                  '<strong>Fecha:</strong> ' + fecha +
                  '</div>' +
                  '<small class="text-danger">Esta acción no se puede deshacer.</small>' +
                  '</div>',
            icon: 'warning',
            showCancelButton: true,
            confirmButtonText: '<i class="bi bi-trash3"></i> Sí, eliminar',
            cancelButtonText: '<i class="bi bi-x-circle"></i> Cancelar',
            reverseButtons: true,
            focusCancel: true,
            width: '500px',
            padding: '2rem',
            backdrop: true,
            allowOutsideClick: false,
            allowEscapeKey: false
        }).then((result) => {
            if (result.isConfirmed) {
                // Mostrar loading
                swalWithBootstrapButtons.fire({
                    title: 'Eliminando...',
                    text: 'Por favor espere',
                    allowOutsideClick: false,
                    allowEscapeKey: false,
                    didOpen: () => {
                        Swal.showLoading();
                    }
                });
                
                // Enviar el formulario
                var \$form = $('.form-eliminar-documento[data-id="' + id + '"]');
                $.ajax({
                    url: \$form.attr('action'),
                    type: 'POST',
                    data: \$form.serialize(),
                    dataType: 'json',
                    success: function(result) {
                        if (!result.success) {
                            Swal.fire('Error', result.message || 'No se pudo eliminar el documento.', 'error');
                            return;
                        }
                        // Cerrar loading
                        Swal.close();
                        
                        // Mostrar mensaje de éxito
                        Swal.fire({
                            title: '¡Eliminado!',
                            text: 'El documento ha sido eliminado correctamente.',
                            icon: 'success',
                            confirmButtonText: 'Aceptar',
                            timer: 2000,
                            timerProgressBar: true
                        }).then(() => {
                            // Recargar la página para ver los cambios
                            location.reload();
                        });
                    },
                    error: function() {
                        Swal.fire('Error', 'No se pudo eliminar el documento. Vuelva a intentarlo.', 'error');
                    }
                });
            }
        });
    });
});
JS;

$this->registerJs($script);

?>
<div class="cliente-view">

    <section>
        <?= Html::a('Actualizar', ['update', 'idcliente' => $model->idcliente], ['class' => 'btn btn-primary']) ?>
        <?= Html::a('Eliminar', ['delete', 'idcliente' => $model->idcliente], [
                'class' => 'btn btn-danger',
                'data' => [
                        'confirm' => 'Are you sure you want to delete this item?',
                        'method' => 'post',
                ],
        ]) ?>
        <?php
        Modal::begin([
                'title' => '<h5 class="mb-0">Aplicar abono</h5>',
                'toggleButton' => [
                        'label' => '<i class="bi bi-cash-stack"></i> Aplicar abono',
                        'class' => 'btn btn-primary',
                        'encodeLabel' => false,
                ],
        ]);

        // Form: enviará a action 'aplicar-abono' del controlador actual (ajustá la ruta si hace falta)
        echo Html::beginForm(['adelantos/aplicar-abono'], 'post', [
                'id' => 'aplicar-form',
                'autocomplete' => 'off',
        ]);

        echo Html::tag('p', 'Saldo total disponible: <strong>' . number_format($saldoTotal, 2, '.', ',') . ' C$</strong>', ['class' => 'mb-3', 'encode' => false]);

        // Input manual (sin model)
        echo '<div class="mb-3">';
        echo Html::input('hidden', 'saldoTotal', number_format($saldoTotal, 2, '.', ','));
        echo Html::input('hidden', 'idcliente', $model->idcliente);
        echo Html::label('Monto a aplicar', 'saldoAplicar', ['class' => 'form-label']);
        echo Html::input('number', 'saldoAplicar', null, [
                'id' => 'saldoAplicar',
                'class' => 'form-control',
                'placeholder' => 'Ingresar monto a aplicar',
                'min' => 0,
                'max' => $saldoTotal,
                'step' => '0.01',
                'inputmode' => 'decimal',
                'required' => true,
        ]);
        echo Html::tag('div', '', ['id' => 'saldoAplicar-error', 'class' => 'invalid-feedback', 'style' => 'display:none;']);
        echo '</div>';

        echo Html::tag('hr', '');

        echo Html::submitButton('<i class="bi bi-check2-circle"></i> Aplicar', [
                'class' => 'btn btn-success',
                'id' => 'btn-aplicar',
                'encode' => false,
        ]);

        echo Html::endForm();

        Modal::end();
        ?>
        <?= Html::a('Impresiones Recibos', ['printer', 'idcliente' => $model->idcliente, 'fecha' => date('Y-m-d')], ['class' => 'btn btn-info']) ?>
    </section>
    <br>
    <?= DetailView::widget([
            'model' => $model,
            'attributes' => [
                    'codcliente',
                    'nombres',
                    'apellidos',
                    'direccion:ntext',
                    [
                            'format' => 'raw',
                            'attribute' => 'departamento',
                            'value' => CatalogoItem::findOne($model->departamento)->nombre,
                    ],
                    [
                            'format' => 'raw',
                            'attribute' => 'ciudad',
                            'value' => CatalogoItem::findOne($model->ciudad)->nombre,
                    ],
                    'cedula',
                    'telefono',
                    'celular',
                    [
                            'format' => 'raw',
                            'attribute' => 'actividad_principal',
                            'value' => CatalogoItem::findOne($model->actividad_principal)->nombre,
                    ],
                    [
                            'format' => 'raw',
                            'attribute' => 'procedencia',
                            'value' => CatalogoItem::findOne($model->procedencia)->nombre,
                    ],
                    [
                            'format' => 'raw',
                            'attribute' => 'tipo',
                            'value' => CatalogoItem::findOne($model->tipo)->nombre,
                    ],
                    [
                            'format' => 'raw',
                            'attribute' => 'giro_negocio',
                            'value' => CatalogoItem::findOne($model->giro_negocio)->nombre,
                    ],
                    'observaciones:ntext',
                    'email:email',
                    'bancos',
                    'cuenta1',
                    'cuenta2',
            ],
    ]) ?>
    <p>&nbsp;</p>
    <div class="panel panel-default">
        <div class="panel-heading">
            <h3 class="panel-title">Documentos del Cliente</h3>
        </div>
        <div class="panel-body">
            <?php $archivos = (new \app\services\ClienteDocumentService())->files((int) $model->idcliente); ?>
            <?php if ($archivos): ?>
                <table class="table table-striped">
                    <thead>
                    <tr>
                        <th>Nombre del Documento</th>
                        <th>Tipo</th>
                        <th>Tamaño</th>
                        <th>Fecha</th>
                        <th>Acciones</th>
                    </tr>
                    </thead>
                    <tbody>
                    <?php
                    if (count($archivos) > 0) {
                        foreach ($archivos as $archivo) {
                            /** @var ArchivosClientes $archivo */
                            ?>
                            <tr>
                                <td><?= Html::encode($archivo->nombre_original) ?></td>
                                <td><?= Html::encode($archivo->tipo) ?></td>
                                <td><?= Html::encode($archivo->size) ?></td>
                                <td><?= Html::encode($archivo->created_at) ?></td>
                                <td>
                                    <?= Html::a('<i class="bi bi-cloud-download"></i>', ['descargar-documento', 'id' => $archivo->idarchivo], [
                                            'class' => 'btn btn-xs btn-primary',
                                            'target' => '_blank',
                                            'title' => 'Descargar documento'
                                    ]) ?>

                                    <?= Html::beginForm(['eliminar-documento', 'id' => $archivo->idarchivo], 'post', [
                                            'class' => 'd-inline form-eliminar-documento',
                                            'data-id' => $archivo->idarchivo
                                    ]) ?>
                                    <?= Html::hiddenInput('id', $archivo->idarchivo) ?>
                                    <?= Html::hiddenInput(Yii::$app->request->csrfParam, Yii::$app->request->csrfToken) ?>
                                    <button type="button" class="btn btn-xs btn-danger btn-eliminar-swal"
                                            data-id="<?= $archivo->idarchivo ?>"
                                            data-nombre="<?= Html::encode($archivo->nombre_original) ?>"
                                            data-size="<?= Yii::$app->formatter->asShortSize($archivo->size) ?>"
                                            data-fecha="<?= Yii::$app->formatter->asDateTime($archivo->created_at) ?>"
                                            title="Eliminar documento">
                                        <i class="bi bi-trash3"></i>
                                    </button>
                                    <?= Html::endForm() ?>
                                </td>
                            </tr>
                            <?php
                        }
                    }
                    ?>
                    </tbody>
                </table>
            <?php else: ?>
                <p class="text-muted">No hay documentos subidos para este cliente.</p>
            <?php endif; ?>
        </div>
    </div>
    <p>&nbsp;</p>
    <?= GridView::widget([
            'dataProvider' => $adelantos,
            'pjax' => true,
            'pjaxSettings' => [
                    'neverTimeout' => true,
            ],'headerContainer' => ['style' => 'top:50px', 'class' => 'kv-table-header'], // offset from top
            'floatHeader' => true, // table header floats when you scroll
            'floatPageSummary' => true, // table page summary floats when you scroll
            'floatFooter' => false, // disable floating of table footer
        // parameters from the demo form
            'responsive' => false,
            'bordered' => true,
            'striped' => false,
            'condensed' => true,
            'hover' => true,
            'showPageSummary' => true,
            'panel' => [
                    'after' => '',
                    'heading' => '<i class="fas fa-book"></i>  Anticipos',
                    'type' => 'primary',
                    'before' => '<div class="d-flex flex-wrap gap-2 mb-2">' .
                            Html::a('<i class="bi bi-file-earmark-pdf"></i> Descargar PDF', ['reporte-adelantos-pdf', 'idcliente' => $model->idcliente], ['class' => 'btn btn-danger btn-sm', 'encode' => false]) .
                            Html::a('<i class="bi bi-file-earmark-excel"></i> Descargar Excel', ['reporte-adelantos-excel', 'idcliente' => $model->idcliente], ['class' => 'btn btn-success btn-sm', 'encode' => false]) .
                            '</div>',
            ],
            'export' => false,
            'columns' => [
                    [
                            'class' => 'kartik\grid\SerialColumn',
                            'header' => '',
                            'pageSummary'=>'Total',
                            'pageSummaryOptions' => ['colspan' => 3],
                            'headerOptions' => ['class' => 'kartik-sheet-style']
                    ],

                //'idadelanto',
                //'idcliente',
                    [
                            'attribute' => 'nombreCliente',
                            'value' => function ($model) {
                                /** @var Adelantos @model */
                                return $model->cliente->nombres . ' ' . $model->cliente->apellidos;
                            }
                    ],
                //'idcaja',
                    'fecha',
                    [
                            'format' => ['decimal', 2],
                            'attribute' => 'monto',
                            'value' => function ($model) {
                                return $model->monto;
                            },
                            'pageSummary' => true, // Muestra el total
                            'pageSummaryFunc' => GridView::F_SUM, // Suma de la columna
                    ],
                    [
                            'format' => ['decimal', 2],
                            'attribute' => 'saldo',
                            'value' => function ($model) {
                                return $model->saldo;
                            },
                            'pageSummary' => true, // Muestra el total
                            'pageSummaryFunc' => GridView::F_SUM, // Suma de la columna
                    ],
                    [
                            'attribute' => 'estado',
                            'label' => 'Estado',
                            'format' => 'raw',
                            'value' => static function ($model) {
                                return match ((int) $model->estado) {
                                    1 => '<span class="badge bg-success">Aprobado</span>',
                                    2 => '<span class="badge bg-warning text-dark">Pendiente</span>',
                                    default => '<span class="badge bg-secondary">Desconocido</span>',
                                };
                            },
                    ],

            ],
    ]); ?>
</div>
