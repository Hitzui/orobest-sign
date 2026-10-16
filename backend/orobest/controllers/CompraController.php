<?php

namespace app\controllers;


use app\components\Rbac;
use app\models\Agencia;
use app\models\CatalogoItem;
use app\models\Compra;
use app\models\CompraSearch;
use app\models\DetaCompra;
use app\models\Usuario;
use app\modules\sign\Module as SignModule;
use app\modules\sign\services\FirmaService;
use app\services\CajaService;
use app\services\CompraService;
use app\services\HelperServices;
use kartik\mpdf\Pdf;
use Mpdf\MpdfException;
use setasign\Fpdi\PdfParser\CrossReference\CrossReferenceException;
use setasign\Fpdi\PdfParser\PdfParserException;
use setasign\Fpdi\PdfParser\Type\PdfTypeException;
use Yii;
use yii\base\InvalidConfigException;
use yii\data\ActiveDataProvider;
use yii\db\Exception;
use yii\db\StaleObjectException;
use yii\filters\AccessControl;
use yii\web\Controller;
use yii\web\NotFoundHttpException;
use yii\filters\VerbFilter;
use yii\web\Response;

/**
 * CompraController implements the CRUD actions for Compra model.
 */
class CompraController extends Controller
{
    /**
     * @inheritDoc
     */
    public function behaviors(): array
    {
        return array_merge(
            parent::behaviors(),
            [
                'access' => [
                    'class' => AccessControl::class,
                    'rules' => [
                        Rbac::permiso(['verCompras'])
                    ],
                ],
                'verbs' => [
                    'class' => VerbFilter::class,
                    'actions' => [
                        'delete' => ['POST'],
                    ],
                ],
            ]
        );
    }

    /**
     * Lists all Compra models.
     *
     *
     */
    public function actionIndex(): Response|string
    {
        $searchModel = new CompraSearch();
        $params = $this->request->queryParams;
        if (is_null(Yii::$app->user->identity)) {
            return $this->redirect(['site/login']);
        }
        $idCajaUsuario = Yii::$app->user->identity->idcaja;

        // Sobrescribir o agregar el idcaja al parámetro de búsqueda
        $params['CompraSearch']['idcaja'] = $idCajaUsuario;

        // Usar los parámetros modificados
        $dataProvider = $searchModel->search($params);

        return $this->render('index', [
            'searchModel' => $searchModel,
            'dataProvider' => $dataProvider,
        ]);
    }


    public function actionView($idcompra)
    {
        $compra = $this->findModel($idcompra);
        if (is_null($compra)) {
            return $this->redirect(['index']);
        }
        $detalleCompras = DetaCompra::find()->where(["idcompra" => $idcompra]);
        $dataMovimientos = new ActiveDataProvider([
            'query' => $detalleCompras,
        ]);
        return $this->render('view', [
            'model' => $compra,
            'detalleCompras' => $dataMovimientos,
        ]);
    }

    public function actionPrint($idcompra): Response|string
    {
        if (Yii::$app->user->isGuest) {
            return $this->redirect(['site/login']);
        }
        //$this->layout = 'print';
        $compra = $this->findModel($idcompra);
        if (is_null($compra)) {
            return $this->redirect(['index']);
        }
        $cliente = $compra->cliente;
        $departamento = CatalogoItem::find()->where(['idcatalog_item' => $cliente->departamento])->one();
        $ciudad = CatalogoItem::find()->where(['idcatalog_item' => $cliente->ciudad])->one();
        if (is_null($departamento)) {
            $departamento = "";
        } else {
            $departamento = $departamento->nombre;
        }
        if (is_null($ciudad)) {
            $ciudad = "";
        } else {
            $ciudad = $ciudad->nombre;
        }
        $usuario = Usuario::findOne($compra->idusuario);
        if (is_null($usuario)) {
            return $this->redirect(['site/login']);
        }
        $agencia = Agencia::findOne($usuario->idagencia);
        $firmaDataUri = null;
        // La impresión no pasa por una ruta del módulo Sign, así que puede seguir descargado.
        $signModule = Yii::$app->getModule('sign');
        if ($signModule instanceof SignModule) {
            $firmaPath = (new FirmaService($signModule))->latestImageForPrint((int) $compra->idcompra);
            if ($firmaPath !== null) {
                $firmaContenido = @file_get_contents($firmaPath);
                if ($firmaContenido !== false) {
                    $firmaDataUri = 'data:image/png;base64,' . base64_encode($firmaContenido);
                }
            }
        }

        $content = $this->renderPartial('print', [
            'compra' => $compra,
            'cliente'=>$cliente,
            'departamento' => $departamento,
            'ciudad' => $ciudad,
            'agencia' => $agencia->descripcion,
            'usuario'=>$usuario,
            'firmaDataUri' => $firmaDataUri,
        ]);

        $pdf = new Pdf([
            'mode' => Pdf::MODE_UTF8,
            'format' => Pdf::FORMAT_LETTER,
            'orientation' => Pdf::ORIENT_PORTRAIT,
            'marginLeft' => 8,
            'marginRight' => 8,
            'marginTop' => 6,
            'marginBottom' => 6,
            'filename' => 'compra_' . $compra->numcompra . '_' . strtotime($compra->fecha).'.pdf',
            'destination' => Pdf::DEST_BROWSER, // abre directo en el navegador
            //'cssFile' => '@vendor/kartik-v/yii2-mpdf/src/assets/kv-mpdf-bootstrap.min.css',
            // any css to be embedded if required
            'cssInline' => HelperServices::stylePrint(),
            'content' => $content,
            'options' => ['title' => 'Comprobante de compra'],
            'methods' => [
                //'SetHeader'=>['Mi Empresa||{PAGENO}'],
                //'SetFooter' => ['|Gracias por su compra|'],
                'SetDefaultFont'=>'Arial'
            ]
        ]);

        try {
            return $pdf->render();
        } catch (MpdfException|CrossReferenceException|PdfTypeException|PdfParserException|InvalidConfigException $e) {
            Yii::$app->session->setFlash('error', $e->getMessage());
            return $this->redirect(['compra/view', 'idcompra' => $compra->idcompra]);
        }
    }

    /**
     * Creates a new Compra model.
     * If creation is successful, the browser will be redirected to the 'view' page.
     *
     */
    public function actionCreate(): array|string
    {
        $model = new Compra();
        $cajaService = new CajaService();
        $caja = $cajaService->getDatosCaja();
        $agencia = $cajaService->getDatosAgencia();
        $compraService = new CompraService();
        $invoiceNumber = $compraService->getInvoiceNumber();
        $listaPrecio = $compraService->getPrecios();


        return $this->render('create', [
            'model' => $model,
            'numero' => $invoiceNumber,
            'caja' => $caja,
            'agencia' => $agencia,
            'listaPrecio' => $listaPrecio
        ]);
    }

    private function handleCompra(string $action): array
    {
        Yii::$app->response->format = Response::FORMAT_JSON;

        if (!$this->request->isPost) {
            return ['success' => false, 'message' => 'Los datos deben ser enviados desde el formulario vía POST'];
        }

        $compraService = new CompraService();
        $detaCompra = $this->request->post('DetalleCompra');

        if (is_null($detaCompra)) {
            return ['success' => false, 'message' => 'Debe especificar el detalle de la compra para continuar'];
        }

        $compra = new Compra($this->request->post('Compra'));

        /*if (!$compra->validate()) {
            return ['success' => false, 'message' => 'Debe indicar los campos de la compra para continuar'];
        }*/

        if ($action === 'create') {
            return $compraService->create($compra, $detaCompra);
        } elseif ($action === 'update') {
            return $compraService->update($compra, $detaCompra);
        }

        return ['success' => false, 'message' => 'Acción no soportada'];
    }

    public function actionSave($method): array
    {
        return match ($method) {
            'create' => $this->handleCompra('create'),
            'update' => $this->handleCompra('update'),
            default => ['success' => false, 'message' => 'Acción no soportada'],
        };
    }

    /**
     * Updates an existing Compra model.
     * If update is successful, the browser will be redirected to the 'view' page.
     * @param int $idcompra Idcompra
     * @return string|Response
     * @throws Exception if the model cannot be found
     */
    public function actionUpdate(int $idcompra): Response|string
    {
        $model = $this->findModel($idcompra);
        if (is_null($model)) {
            return $this->redirect(['index']);
        }
        $cajaService = new CajaService();
        $caja = $cajaService->getDatosCaja();
        $agencia = $cajaService->getDatosAgencia();
        $compraService = new CompraService();
        $invoiceNumber = $model->numcompra;
        $listaPrecio = $compraService->getPrecios();
        $detalleCompras = $model->detaCompras;
        $cliente = $model->cliente;

        return $this->render('update', [
            'model' => $model,
            'numero' => $invoiceNumber,
            'caja' => $caja,
            'agencia' => $agencia,
            'listaPrecio' => $listaPrecio,
            'cliente' => $cliente,
            'detalle' => $detalleCompras
        ]);
    }

    /***
     * <p>Anular la compra, no eliminarla</p>
     * @param $idcompra
     * @return array
     */
    public function actionDelete($idcompra): array
    {
        Yii::$app->response->format = Response::FORMAT_JSON;
        $compraService = new CompraService();
        return $compraService->delete($idcompra);
    }


    protected function findModel($idcompra): ?Compra
    {
        return Compra::findOne(['idcompra' => $idcompra]);
    }
}
