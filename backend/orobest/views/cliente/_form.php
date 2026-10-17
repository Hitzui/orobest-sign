<?php


/** @var yii\web\View $this */
/** @var app\models\Cliente $model */
/** @var app\models\ClienteJuridico $clienteJuridico */

/** @var yii\widgets\ActiveForm $form */

use app\models\ArchivosClientes;
use app\models\ClienteJunta;
use kartik\depdrop\DepDrop;
use kartik\file\FileInput;
use kartik\form\ActiveForm;
use kartik\select2\Select2;
use yii\bootstrap5\Html;
use yii\helpers\Url;


if (!isset($model->actividad_principal)) {
    $model->actividad_principal = 177;
}
if (!isset($model->procedencia)) {
    $model->procedencia = 173;
}
if (!isset($model->tipo)) {
    $model->tipo = 170;
}
if (!isset($model->giro_negocio)) {
    $model->giro_negocio = 176;
}

/*if (!isset($model->departamento)){
    $model->departamento = 10;
}*/

$ciudadNombre = [];
if ($model->ciudad) {
    $ciudadNombre = $ciudades;
}
$js = <<<JS
let index = $("#junta-directiva-body tr").length;

$('#add-member').on('click', function() {
    let nuevoNombre = $('#nuevo-nombre');
    let nuevoCargo = $('#nuevo-cargo');
    let nombre = nuevoNombre.val().trim();
    let cargo = nuevoCargo.val().trim();

    if (!nombre || !cargo) {
        alert('Por favor, complete al menos el nombre y el cargo.');
        return;
    }

    // Crear nueva fila
    let nuevaFila = `
        <tr>
            <td><input type="text" name="ClienteJunta[\${index}][nombre_completo]" value="\${nombre}" class="form-control"></td>
            <td><input type="text" name="ClienteJunta[\${index}][cargo]" value="\${cargo}" class="form-control"></td>
            <td><button type="button" class="btn btn-danger btn-sm eliminar-fila"><i class="bi bi-trash3"></i></button></td>
        </tr>
    `;

    // Agregar al cuerpo de la tabla
    $('#junta-directiva-body').append(nuevaFila);
    index++;

    // Limpiar inputs
    nuevoNombre.val('');
    nuevoCargo.val('');

    // Foco en el primer campo
    nuevoNombre.focus();
});

// Eliminar fila
$(document).on('click', '.eliminar-fila', function() {
    $(this).closest('tr').remove();
});

$('.eliminar-documento-form').on('click', function() {
    const button = $(this);
    if (!window.confirm('¿Eliminar el documento "' + button.data('nombre') + '"?')) return;
    button.prop('disabled', true);
    const data = {};
    data[yii.getCsrfParam()] = yii.getCsrfToken();
    $.ajax({
        url: button.data('url'),
        type: 'POST',
        dataType: 'json',
        data: data,
        success: function(result) {
            if (result.success) button.closest('tr').remove();
            else window.alert(result.message || 'No se pudo eliminar el documento.');
        },
        error: function() { window.alert('No se pudo eliminar el documento. Vuelva a intentarlo.'); },
        complete: function() { button.prop('disabled', false); }
    });
});
JS;
$this->registerJs($js);
?>

<div class="container-fluid">

    <?php $form = ActiveForm::begin(['options' => ['enctype' => 'multipart/form-data']]); ?>
    <?= $form->errorSummary([$model, $clienteJuridico, $beneficiarioFinal, $informacionContacto]) ?>

    <!-- Nav tabs -->
    <ul class="nav nav-tabs" id="myTab" role="tablist">
        <li class="nav-item" role="presentation">
            <button class="nav-link active" id="home-tab" data-bs-toggle="tab" data-bs-target="#home"
                    type="button" role="tab" aria-controls="home" aria-selected="true">
                Datos Generales
            </button>
        </li>
        <li class="nav-item" role="presentation">
            <button class="nav-link" id="profile-tab" data-bs-toggle="tab" data-bs-target="#profile"
                    type="button" role="tab" aria-controls="profile" aria-selected="false">
                Persona Jurídica
            </button>
        </li>
        <li class="nav-item" role="presentation">
            <button class="nav-link" id="contact-tab" data-bs-toggle="tab" data-bs-target="#beneficiario"
                    type="button" role="tab" aria-controls="contact" aria-selected="false">
                Beneficiario Final
            </button>
        </li>
        <li class="nav-item" role="presentation">
            <button class="nav-link" id="contact-tab" data-bs-toggle="tab" data-bs-target="#contact"
                    type="button" role="tab" aria-controls="contact" aria-selected="false">
                Información de contacto
            </button>
        </li>
    </ul>

    <!-- Tab panes -->
    <div class="tab-content mt-3" id="myTabContent">
        <div class="tab-pane fade show active" id="home" role="tabpanel" aria-labelledby="home-tab">
            <div class="row">
                <div class="col">
                    <?= $form->field($model, 'nombres')->textInput(['maxlength' => true]) ?>
                </div>
                <div class="col">
                    <?= $form->field($model, 'apellidos')->textInput(['maxlength' => true]) ?>
                </div>
            </div>

            <?= $form->field($model, 'direccion')->textarea(['rows' => 6]) ?>

            <div class="row">
                <div class="col">
                    <?= $form->field($model, 'departamento')->widget(Select2::class, [
                            'data' => $departamentos,
                            'options' => ['placeholder' => 'Seleccione un departamento ...'],
                            'pluginOptions' => [
                                    'allowClear' => true
                            ],
                    ]) ?>
                </div>
                <div class="col">
                    <?php try {
                        echo $form->field($model, 'ciudad')->widget(DepDrop::class, [
                                'type' => DepDrop::TYPE_SELECT2,
                                'data' => $ciudadNombre,
                                'select2Options' => ['pluginOptions' => ['allowClear' => true]],
                                'options' => ['id' => 'ciudades-id'],
                                'pluginOptions' => [
                                        'depends' => ['cliente-departamento'],
                                        'placeholder' => 'Select...',
                                        'url' => Url::to(['/cliente/ciudades']),
                                ]
                        ]);
                    } catch (Exception $e) {
                        echo $e->getMessage();
                    } ?>
                </div>
            </div>

            <div class="row">
                <div class="col"> <?= $form->field($model, 'cedula')->textInput([
                            'maxlength' => true,
                            'oninput' => "this.value = this.value.toUpperCase().replace(/[^A-Z0-9]/g,'');"
                    ])
                    ?></div>
            </div>

            <div class="row">
                <div class="col"><?= $form->field($model, 'telefono')->textInput(['maxlength' => true]) ?></div>
                <div class="col"><?= $form->field($model, 'celular')->textInput(['maxlength' => true]) ?></div>
            </div>

            <div class="row">
                <div class="col">
                    <?= $form->field($model, 'actividad_principal')->widget(Select2::class, [
                            'data' => $actividad,
                            'options' => ['placeholder' => 'Seleccionar ...'],
                            'pluginOptions' => [
                                    'allowClear' => true
                            ],
                    ]) ?>
                </div>
                <div class="col">
                    <?= $form->field($model, 'procedencia')->widget(Select2::class, [
                            'data' => $origen,
                            'options' => ['placeholder' => 'Seleccionar ...'],
                            'pluginOptions' => [
                                    'allowClear' => true
                            ],
                    ]) ?>
                </div>
                <div class="col">
                    <?= $form->field($model, 'tipo')->widget(Select2::class, [
                            'data' => $tipo,
                            'options' => ['placeholder' => 'Seleccionar ...'],
                            'pluginOptions' => [
                                    'allowClear' => true
                            ],
                    ]) ?>
                </div>
                <div class="col">
                    <?= $form->field($model, 'giro_negocio')->widget(Select2::class, [
                            'data' => $giroNegocio,
                            'value' => 176,
                            'options' => ['placeholder' => 'Seleccionar ...'],
                            'pluginOptions' => [
                                    'allowClear' => true
                            ],
                    ]) ?>
                </div>
            </div>

            <div class="row">
                <div class="col"><?= $form->field($model, 'email') ?></div>
                <div class="col"><?= $form->field($model, 'bancos')->textInput() ?></div>
                <div class="col"><?= $form->field($model, 'cuenta1')->textInput(['maxlength' => true]) ?></div>
                <div class="col"><?= $form->field($model, 'cuenta2')->textInput(['maxlength' => true]) ?></div>
            </div>

            <?= $form->field($model, 'observaciones')->textarea(['rows' => 6]) ?>

            <!-- Campo para subir documentos -->
            <div class="panel panel-default">
                <div class="panel-heading">
                    <h3 class="panel-title">Documentos del Cliente</h3>
                </div>
                <div class="panel-body">
                    <?= $form->field($model, 'documentosUpload[]')->widget(FileInput::class, [
                            'options' => [
                                    'multiple' => true,
                                    'value' => '',
                                    'accept' => '.pdf,.doc,.docx,.jpg,.jpeg,.png,.txt',
                            ],
                            'pluginOptions' => [
                                    'previewFileType' => 'any',
                                    'showCaption' => true,
                                    'showRemove' => true,
                                    'showUpload' => false, // Importante: false para subida manual
                                    'browseClass' => 'btn btn-primary',
                                    'browseIcon' => '<i class="glyphicon glyphicon-folder-open"></i> ',
                                    'browseLabel' => 'Seleccionar archivos',
                                    'removeClass' => 'btn btn-danger',
                                    'removeIcon' => '<i class="glyphicon glyphicon-trash"></i> ',
                                    'mainClass' => 'input-group-lg',
                                    'maxFileCount' => 10,
                                    'maxFileSize' => 10240, // 10MB en KB
                                    'allowedFileExtensions' => ['pdf', 'doc', 'docx', 'jpg', 'jpeg', 'png', 'txt'],
                                    'overwriteInitial' => false,
                                    'fileActionSettings' => ['showUpload' => false],
                            ],
                    ])->label('Documentos del Cliente')
                            ->hint('Seleccione hasta 10 archivos a la vez. Se subirán al pulsar Guardar y se añadirán a los existentes, aunque tengan el mismo nombre. Formatos: PDF, DOC, DOCX, JPG, JPEG, PNG y TXT. Máximo: 10 MiB por archivo.'); ?>

                    <?php $archivos = $model->isNewRecord ? [] : (new \app\services\ClienteDocumentService())->files((int) $model->idcliente); ?>
                    <?php if ($archivos): ?>
                        <div class="existing-documents">
                            <h5>Documentos existentes:</h5>
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
                                            <td><?= Yii::$app->formatter->asShortSize($archivo->size) ?></td>
                                            <td><?= Html::encode($archivo->created_at) ?></td>
                                            <td>
                                                <?= Html::a('Descargar', ['descargar-documento', 'id' => $archivo->idarchivo], ['class' => 'btn btn-sm btn-primary', 'target' => '_blank']) ?>
                                                <?= Html::button('Eliminar', [
                                                    'class' => 'btn btn-sm btn-danger eliminar-documento-form',
                                                    'data-url' => Url::to(['eliminar-documento', 'id' => $archivo->idarchivo]),
                                                    'data-nombre' => $archivo->nombre_original,
                                                ]) ?>
                                            </td>
                                        </tr>
                                        <?php
                                    }
                                }
                                ?>
                                </tbody>
                            </table>
                        </div>
                    <?php endif; ?>
                </div>
            </div>

        </div>
        <div class="tab-pane fade" id="profile" role="tabpanel" aria-labelledby="profile-tab">
            <div class="row">
                <div class="col"><?= $form->field($clienteJuridico, 'razon_social')->textInput() ?></div>
                <div class="col"><?= $form->field($clienteJuridico, 'nombre_comercial')->textInput() ?></div>
            </div>
            <div class="row">
                <div class="col"><?= $form->field($clienteJuridico, 'ruc')->textInput() ?></div>
                <div class="col"><?= $form->field($clienteJuridico, 'constitucion_social')->textInput() ?></div>
            </div>
            <div class="row">
                <div class="col"><?= $form->field($clienteJuridico, 'direccion_matriz')->textInput() ?></div>
            </div>
            <div class="row">
                <h4>Junta Directiva</h4>
                <table class="table table-bordered" id="tabla-junta">
                    <thead>
                    <tr>
                        <th>Nombre</th>
                        <th>Cargo</th>
                        <th style="width:100px;">Acciones</th>
                    </tr>
                    </thead>
                    <tbody id="junta-directiva-body">
                    <?php if (count($juntaDirectiva) > 0):
                        foreach ($juntaDirectiva as $i => $miembro):
                            /** @var ClienteJunta $miembro */
                            if (is_null($miembro)) continue;
                            ?>
                            <tr>
                                <td><?= Html::textInput("ClienteJunta[$i][nombre_completo]", $miembro->nombre_completo, ['class' => 'form-control']) ?></td>
                                <td><?= Html::textInput("ClienteJunta[$i][cargo]", $miembro->cargo, ['class' => 'form-control']) ?></td>
                                <td>
                                    <button type="button" class="btn btn-danger btn-sm eliminar-fila"><i
                                                class="bi bi-trash3"></i></button>
                                </td>
                            </tr>
                        <?php
                        endforeach;
                    endif;
                    ?>
                    </tbody>
                    <tfoot>
                    <tr>
                        <td><input type="text" id="nuevo-nombre" class="form-control" placeholder="Nombre"></td>
                        <td><input type="text" id="nuevo-cargo" class="form-control" placeholder="Cargo"></td>
                        <td>
                            <button type="button" class="btn btn-success btn-sm" id="add-member"><i
                                        class="bi bi-plus-square"></i></button>
                        </td>
                    </tr>
                    </tfoot>
                </table>
            </div>
        </div>
        <div class="tab-pane fade" id="beneficiario" role="tabpanel" aria-labelledby="beneficiario-tab">
            <div class="row">
                <div class="col">
                    <?= $form->field($beneficiarioFinal, 'nombre_completo')->textInput() ?>
                </div>
                <div class="col">
                    <?= $form->field($beneficiarioFinal, 'identificacion')->textInput() ?>
                </div>
            </div>
            <div class="row">
                <div class="col">
                    <?= $form->field($beneficiarioFinal, 'nacionalidad')->textInput() ?>
                </div>
                <div class="col">
                    <?= $form->field($beneficiarioFinal, 'direccion')->textInput() ?>
                </div>
            </div>
        </div>
        <div class="tab-pane fade" id="contact" role="tabpanel" aria-labelledby="contact-tab">
            <div class="row">
                <div class="col"><?= $form->field($informacionContacto, 'nombre_legal')->textInput() ?></div>
            </div>
            <div class="row">
                <div class="col"><?= $form->field($informacionContacto, 'identificacion')->textInput() ?></div>
                <div class="col"><?= $form->field($informacionContacto, 'email')->textInput() ?></div>
            </div>
            <div class="row">
                <div class="col"><?= $form->field($informacionContacto, 'telefono')->textInput() ?></div>
                <div class="col"><?= $form->field($informacionContacto, 'celular')->textInput() ?></div>
            </div>
        </div>
    </div>


    <div class="form-group">
        <?= Html::submitButton('Guardar', ['class' => 'btn btn-success']) ?>
    </div>

    <?php ActiveForm::end(); ?>

</div>
