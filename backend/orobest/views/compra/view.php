<?php


/** @var yii\web\View $this */

/** @var app\models\Compra $model */

use kartik\detail\DetailView;
use kartik\grid\GridView;
use yii\bootstrap5\Html;

$this->title = $model->numcompra;
$this->params['breadcrumbs'][] = ['label' => 'Compras', 'url' => ['index']];
$this->params['breadcrumbs'][] = $this->title;
\yii\web\YiiAsset::register($this);

$disabled = $model->estado == 0;
?>
<div class="compra-view">
        <div>
                <?php if ($disabled) {
                        ?>
                        <div class="alert alert-danger" role="alert">
                                La compra esta anulada...
                        </div>
                        <?php
                } ?>
        </div>
        <p>
                <?= Html::a(
                        '<i class="bi bi-printer"></i> Imprimir',
                        ['print', 'idcompra' => $model->idcompra],
                        [
                                'id' => 'compra-print-button',
                                'class' => 'btn btn-success' . ($disabled ? 'disabled' : ''),
                                'aria-disabled' => $disabled ? 'true' : 'false',
                                'tabindex' => $disabled ? '-1' : null,
                                'onclick' => $disabled ? 'return false;' : null,
                                'target' => '_blank', // 🔹 abre en nueva pestaña
                                'data-check-url' => \yii\helpers\Url::to(['check-signature', 'idcompra' => $model->idcompra]),
                                'data-print-url' => \yii\helpers\Url::to(['print', 'idcompra' => $model->idcompra]),
                                'encode' => false     // 🔹 permite que el ícono <i> no se escape
                        ]
                ) ?>

                <?= Html::a(
                        'Actualizar',
                        ['update', 'idcompra' => $model->idcompra],
                        [
                                'class' => 'btn btn-primary ' . ($disabled ? 'disabled' : ''),
                                'aria-disabled' => $disabled ? 'true' : 'false',
                                'tabindex' => $disabled ? '-1' : null,
                                'onclick' => $disabled ? 'return false;' : null,
                        ]
                ) ?>

                <?= Html::a(
                        'Eliminar',
                        ['delete', 'idcompra' => $model->idcompra],
                        [
                                'class' => 'btn btn-danger ' . ($disabled ? 'disabled' : ''),
                                'aria-disabled' => $disabled ? 'true' : 'false',
                                'tabindex' => $disabled ? '-1' : null,
                                'onclick' => $disabled ? 'return false;' : null,
                                'data' => $disabled ? [] : [
                                        'confirm' => '¿Desea eliminar la compra realizada? Esta acción no se puede revertir',
                                        'method' => 'post',
                                ],
                        ]
                ) ?>
        </p>

        <div class="row">
                <?= DetailView::widget([
                        'model' => $model,
                        'attributes' => [
                                [
                                        'group' => true,
                                        'label' => 'Información de la compra',
                                        'rowOptions' => ['class' => 'table-info']
                                ],
                                [
                                        'columns' => [
                                                [
                                                        'attribute' => 'numcompra',
                                                        'valueColOptions' => ['style' => 'width:30%']
                                                ],
                                                [
                                                        'attribute' => 'fecha',
                                                        'value' => date('d/m/Y h:i A', strtotime($model->fecha)),
                                                        'valueColOptions' => ['style' => 'width:30%']
                                                ]
                                        ]
                                ],
                                [
                                        'columns' => [
                                                [
                                                        'label' => 'Cliente',
                                                        'value' => $model->cliente->nombres . ' ' . $model->cliente->apellidos,
                                                        'valueColOptions' => ['style' => 'width:30%']
                                                ],
                                                [
                                                        'label' => 'Identificacion',
                                                        'value' => $model->cliente->cedula,
                                                        'valueColOptions' => ['style' => 'width:30%']
                                                ]
                                        ]
                                ],
                                [
                                        'columns' => [
                                                [
                                                        'attribute' => 'efectivo',
                                                        'valueColOptions' => ['style' => 'width:30%']
                                                ],
                                                [
                                                        'attribute' => 'subtotal',
                                                        'valueColOptions' => ['style' => 'width:30%']
                                                ],
                                        ]
                                ],
                                [
                                        'columns' => [
                                                [
                                                        'attribute' => 'transferencia',
                                                        'valueColOptions' => ['style' => 'width:30%']
                                                ],
                                                [
                                                        'attribute' => 'descuento',
                                                        'valueColOptions' => ['style' => 'width:30%']
                                                ],
                                        ]
                                ],
                                [
                                        'columns' => [
                                                [
                                                        'attribute' => 'adelanto',
                                                        'valueColOptions' => ['style' => 'width:30%']
                                                ],
                                                [
                                                        'attribute' => 'total',
                                                        'valueColOptions' => ['style' => 'width:30%']
                                                ],
                                        ]
                                ],
                        ],
                ]) ?>
        </div>
        <br>
        <div class="row">
                <?php
                try {
                        echo GridView::widget(
                                [
                                        'id' => 'grid-detalle',
                                        'dataProvider' => $detalleCompras,
                                        'columns' => [
                                                'linea',
                                                'descripcion',
                                                //'kilataje',
                                                'kilate',
                                                'precio',
                                                'peso',
                                                'importe',
                                                'lectura',
                                        ],
                                ]
                        );
                } catch (Throwable $e) {
                        echo $e->getMessage();
                }
                ?>
        </div>
</div>
<?php
$this->registerJs(<<<'JS'
document.addEventListener('click', async function (event) {
    const button = event.target.closest('#compra-print-button');
    if (!button || button.classList.contains('disabled')) {
        return;
    }

    event.preventDefault();
    const printWindow = window.open('about:blank', '_blank');
    if (!printWindow) {
        await Swal.fire({
            icon: 'warning',
            title: 'Permita las ventanas emergentes',
            text: 'El navegador bloqueó la ventana para imprimir.'
        });
        return;
    }

    try {
        const response = await fetch(button.dataset.checkUrl, {
            headers: { 'X-Requested-With': 'XMLHttpRequest' },
            credentials: 'same-origin'
        });
        const result = await response.json();
        if (!response.ok || !result.success) {
            throw new Error(result.message || 'No se pudo verificar la firma de la compra.');
        }
        if (!result.permitido) {
            printWindow.close();
            await Swal.fire({
                icon: 'warning',
                title: 'Firma requerida',
                text: 'No se puede imprimir la compra porque falta la firma.'
            });
            return;
        }

        printWindow.location.href = button.dataset.printUrl;
    } catch (error) {
        printWindow.close();
        await Swal.fire({
            icon: 'error',
            title: 'No se pudo verificar la firma',
            text: error.message || 'Intente nuevamente.'
        });
    }
});
JS);
?>
