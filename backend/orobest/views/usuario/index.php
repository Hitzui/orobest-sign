<?php

use app\models\Agencia;
use app\models\Caja;
use app\models\Usuario;
use kartik\grid\GridView;
use yii\bootstrap5\Html;
use yii\helpers\ArrayHelper;
use yii\helpers\Url;
use yii\grid\ActionColumn;


/** @var yii\web\View $this */
/** @var app\models\UsuarioSearch $searchModel */
/** @var yii\data\ActiveDataProvider $dataProvider */

$this->title = 'Usuarios';
$this->params['breadcrumbs'][] = $this->title;
?>
<div class="usuario-index">

    <h1><?= Html::encode($this->title) ?></h1>

    <p>
        <?= Html::a('Create Usuario', ['create'], ['class' => 'btn btn-success']) ?>
    </p>

    <?php // echo $this->render('_search', ['model' => $searchModel]); ?>

    <?= GridView::widget([
            'dataProvider' => $dataProvider,
            'filterModel' => $searchModel,
            'columns' => [
                    ['class' => 'yii\grid\SerialColumn'],

                //'idusuario',
                'username',
                    [
                            'attribute' => 'nombre_agencia',
                            'format' => 'raw',
                            'label' => 'Agencia',
                            'value' => function ($model) {
                                $agencia = Agencia::findOne($model->idagencia);
                                return Html::a($agencia->descripcion, ['agencia/view', 'idagencia' => $model->idagencia]);
                            },
                            'filterType' => GridView::FILTER_SELECT2,
                            'filter' => ArrayHelper::map(Agencia::find()->all(), 'codagencia', 'descripcion'),
                            'filterWidgetOptions' => [
                                    'pluginOptions' => ['allowClear' => true],
                            ],
                            'filterInputOptions' => ['placeholder' => 'Seleccione agencia...'],
                    ],
                    [
                            'attribute' => 'idcaja',
                            'format' => 'raw',
                            'value' => function ($model, $key, $index, $column) {
                                $caja = Caja::findOne($model->idcaja);
                                if (is_null($caja)) {
                                    return Html::decode('<b>No tiene Caja asignada</b>');
                                }
                                return Html::a($caja->descripcion, Url::to(['caja/view', 'idcaja' => $model->idcaja]));
                            }
                    ],
                    'activo:boolean',
                    'imprimir_sin_firma:boolean',
                    //'email:email',
                    'nombre',
                    //'password',
                //'titulo',
                //'username',
                //'created_at',
                //'updated_at',
                //'auth_key',
                //'access_token',
                //'idcaja',
                //'idagencia',
                    [
                            'class' => ActionColumn::className(),
                            'urlCreator' => function ($action, Usuario $model, $key, $index, $column) {
                                return Url::toRoute([$action, 'idusuario' => $model->idusuario]);
                            }
                    ],
            ],
    ]); ?>


</div>
