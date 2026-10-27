<?php

use yii\helpers\Html;
use yii\widgets\DetailView;

/** @var yii\web\View $this */
/** @var app\models\Usuario $model */

$this->title = 'Usuario: #'.$model->idusuario;
$this->params['breadcrumbs'][] = ['label' => 'Usuarios', 'url' => ['index']];
$this->params['breadcrumbs'][] = $this->title;
\yii\web\YiiAsset::register($this);
?>
<div class="usuario-view">

    <p>
        <?= Html::a('Update', ['update', 'idusuario' => $model->idusuario], ['class' => 'btn btn-primary']) ?>
        <?= Html::a('Delete', ['delete', 'idusuario' => $model->idusuario], [
            'class' => 'btn btn-danger',
            'data' => [
                'confirm' => 'Are you sure you want to delete this item?',
                'method' => 'post',
            ],
        ]) ?>
    </p>

    <?= DetailView::widget([
        'model' => $model,
        'attributes' => [
            'idusuario',
            'activo:boolean',
            'imprimir_sin_firma:boolean',
            'email:email',
            'nombre',
            'password',
            'titulo',
            'username',
            'created_at',
            'updated_at',
            'auth_key',
            'access_token',
            'idcaja',
            'idagencia',
        ],
    ]) ?>

</div>
