<?php

namespace app\controllers;


use app\components\Rbac;
use app\models\Adelantos;
use app\models\AdelantosClientesView;
use app\models\AdelantosSearch;
use app\models\Agencia;
use app\models\ArchivosClientes;
use app\models\Catalogo;
use app\models\CatalogoItem;
use app\models\Cliente;
use app\models\ClienteBeneficiarioFinal;
use app\models\ClienteInformacionContacto;
use app\models\ClienteJunta;
use app\models\ClienteJuridico;
use app\models\ClienteSearch;
use app\models\DetaMovimientos;
use app\models\EmpleadoSearch;
use app\models\MovimientoAdelanto;
use app\models\Parametros;
use app\models\Usuario;
use app\services\AdelantoService;
use app\services\ClienteService;
use app\services\ClienteDocumentService;
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
use yii\db\Expression;
use yii\db\StaleObjectException;
use yii\filters\AccessControl;
use yii\helpers\ArrayHelper;
use yii\helpers\Json;
use yii\web\Controller;
use yii\web\NotFoundHttpException;
use yii\filters\VerbFilter;
use yii\web\Response;
use yii\web\UploadedFile;
use function PHPUnit\Framework\isNull;

/**
 * ClienteController implements the CRUD actions for Cliente model.
 */
class ClienteController extends Controller
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
                        Rbac::permiso(['verClientes'])
                    ],
                ],
                'verbs' => [
                    'class' => VerbFilter::class,
                    'actions' => [
                        'delete' => ['POST'],
                        'eliminar-documento' => ['POST'],
                    ],
                ],
            ]
        );
    }

    /**
     * Lists all Cliente models.
     *
     * @return string
     */
    public function actionIndex(): string
    {
        $searchModel = new ClienteSearch();
        $dataProvider = $searchModel->search($this->request->queryParams);
        return $this->render('index', [
            'searchModel' => $searchModel,
            'dataProvider' => $dataProvider,
        ]);
    }

    public function actionListadoModal(): string
    {
        $searchModel = new ClienteSearch(); // si usas filtro
        $dataProvider = $searchModel->search(Yii::$app->request->queryParams);

        return $this->renderAjax('listado_modal_datatable', [
            'models' => Cliente::find()->all(),
        ]);
    }

    public function actionListadoAdelantosModal2222(): string
    {
        $searchModel = new ClienteSearch();
        // Usa el search del searchModel para crear el dataProvider aplicando los queryParams
        $dataProvider = $searchModel->search(Yii::$app->request->queryParams);

        // IMPORTANTE: renderAjax para no cargar layout global
        return $this->renderAjax('listado_modal', [
            'searchModel' => $searchModel,
            'dataProvider' => $dataProvider,
        ]);
    }

    public function actionListadoAdelantosModal(): string
    {
        $models = AdelantosClientesView::find()->orderBy(['nombres' => SORT_ASC])->all();

        return $this->renderAjax('listado_modal_datatable', [
            'models' => $models,
        ]);
    }

    public function actionFind($idcliente): array
    {
        Yii::$app->response->format = Response::FORMAT_JSON;
        try {
            $idcliente = Yii::$app->request->get('idcliente');
            return ['success' => true, 'data' => $this->findModel($idcliente)];
        } catch (NotFoundHttpException $e) {
            return ['success' => false, 'message' => $e->getMessage()];
        }
    }

    public function actionAdelanto()
    {
        Yii::$app->response->format = Response::FORMAT_JSON;
        try {
            $idcliente = Yii::$app->request->get('idcliente');
            $service = new ClienteService();
            return ['success' => true, 'data' => $service->getClientesAdelantos($idcliente)];
        } catch (NotFoundHttpException $e) {
            return ['success' => false, 'message' => $e->getMessage()];
        }
    }

    /**
     * Displays a single Cliente model.
     * @param int $idcliente Idcliente
     * @return string
     * @throws NotFoundHttpException if the model cannot be found
     */
    public function actionView(int $idcliente): string
    {
        $adelantoService = new AdelantoService();
        $saldoTotal = $adelantoService->findAdelantosByIclienteAndActivos($idcliente)->sum('saldo');
        $adelantos = new ActiveDataProvider([
            'query' => $this->buscarAnticiposCliente($idcliente),
            'pagination' => false,
        ]);
        return $this->render('view', [
            'model' => $this->findModel($idcliente),
            'adelantos' => $adelantos,
            'saldoTotal' => $saldoTotal,
        ]);
    }

    public function actionReporteAdelantosPdf(int $idcliente): string
    {
        $cliente = $this->findModel($idcliente);
        $adelantos = $this->buscarAnticiposCliente($idcliente)->all();
        $content = $this->renderPartial('reporte-adelantos-pdf', [
            'cliente' => $cliente,
            'adelantos' => $adelantos,
        ]);
        $pdf = new Pdf([
            'mode' => Pdf::MODE_UTF8,
            'format' => Pdf::FORMAT_A4,
            'orientation' => Pdf::ORIENT_LANDSCAPE,
            'marginLeft' => 12,
            'marginRight' => 12,
            'marginTop' => 12,
            'marginBottom' => 15,
            'filename' => 'anticipos_cliente_' . $idcliente . '.pdf',
            'destination' => Pdf::DEST_DOWNLOAD,
            'content' => $content,
            'cssInline' => 'body{font-family: sans-serif;font-size:10pt;color:#263445}.brand{color:#1d4e78;font-size:10pt;font-weight:bold;letter-spacing:1px}h1{font-size:20pt;color:#17365d;margin:2mm 0}.meta{color:#526173;margin-bottom:7mm}.report{width:100%;border-collapse:collapse}.report th{background:#24476b;color:#fff;text-align:left}.report th,.report td{border:1px solid #ccd4df;padding:7px}.report .number{text-align:right;white-space:nowrap}.report tfoot td{background:#eaf0f8;font-weight:bold}.badge{font-weight:bold}.approved{color:#237a45}.pending{color:#956300}.muted{color:#687789}',
            'options' => ['title' => 'Reporte de anticipos del cliente'],
            'methods' => [
                'SetTitle' => 'Reporte de anticipos - ' . $cliente->nombres . ' ' . $cliente->apellidos,
                'SetAuthor' => 'Oro Best',
                'SetFooter' => 'Oro Best||Página {PAGENO} de {nbpg}',
            ],
        ]);
        return $pdf->render();
    }

    public function actionReporteAdelantosExcel(int $idcliente): Response
    {
        $cliente = $this->findModel($idcliente);
        $adelantos = $this->buscarAnticiposCliente($idcliente)->all();
        $spreadsheet = new \PhpOffice\PhpSpreadsheet\Spreadsheet();
        $sheet = $spreadsheet->getActiveSheet();
        $sheet->setTitle('Anticipos');
        $sheet->setCellValue('A1', 'ORO BEST | REPORTE DE ANTICIPOS');
        $sheet->mergeCells('A1:E1');
        $sheet->setCellValue('A2', 'Cliente: ' . trim($cliente->nombres . ' ' . $cliente->apellidos));
        $sheet->mergeCells('A2:E2');
        $sheet->setCellValue('A3', 'Código: ' . $cliente->codcliente . '    |    Cédula: ' . $cliente->cedula);
        $sheet->mergeCells('A3:E3');
        $sheet->setCellValue('A4', 'Generado: ' . date('d/m/Y H:i'));
        $sheet->mergeCells('A4:E4');
        $sheet->fromArray(['N.º', 'Fecha', 'Monto (C$)', 'Saldo (C$)', 'Estado'], null, 'A6');

        $fila = 7;
        $totalMonto = 0.0;
        $totalSaldo = 0.0;
        foreach ($adelantos as $adelanto) {
            $totalMonto += (float) $adelanto->monto;
            $totalSaldo += (float) $adelanto->saldo;
            $estado = match ((int) $adelanto->estado) {
                1 => 'Aprobado',
                2 => 'Pendiente',
                default => 'Desconocido',
            };
            $sheet->fromArray([
                (int) $adelanto->idadelanto,
                Yii::$app->formatter->asDatetime($adelanto->fecha, 'php:d/m/Y H:i'),
                (float) $adelanto->monto,
                (float) $adelanto->saldo,
                $estado,
            ], null, 'A' . $fila);
            $fila++;
        }
        $filaTotal = $fila;
        $sheet->setCellValue('A' . $filaTotal, 'TOTALES');
        $sheet->mergeCells('A' . $filaTotal . ':B' . $filaTotal);
        $sheet->setCellValue('C' . $filaTotal, $totalMonto);
        $sheet->setCellValue('D' . $filaTotal, $totalSaldo);
        $sheet->setCellValue('E' . $filaTotal, 'Registros: ' . count($adelantos));

        $sheet->getStyle('A1:E1')->getFont()->setBold(true)->setSize(16)->getColor()->setRGB('FFFFFF');
        $sheet->getStyle('A1:E1')->getFill()->setFillType(\PhpOffice\PhpSpreadsheet\Style\Fill::FILL_SOLID)->getStartColor()->setRGB('17365D');
        $sheet->getStyle('A1:E1')->getAlignment()->setVertical(\PhpOffice\PhpSpreadsheet\Style\Alignment::VERTICAL_CENTER);
        $sheet->getRowDimension(1)->setRowHeight(30);
        $sheet->getStyle('A2:A4')->getFont()->setBold(true)->getColor()->setRGB('334155');
        $sheet->getStyle('A6:E6')->getFont()->setBold(true)->getColor()->setRGB('FFFFFF');
        $sheet->getStyle('A6:E6')->getFill()->setFillType(\PhpOffice\PhpSpreadsheet\Style\Fill::FILL_SOLID)->getStartColor()->setRGB('4472C4');
        $sheet->getStyle('A6:E6')->getAlignment()->setHorizontal(\PhpOffice\PhpSpreadsheet\Style\Alignment::HORIZONTAL_CENTER);
        $sheet->getStyle('A' . $filaTotal . ':E' . $filaTotal)->getFont()->setBold(true);
        $sheet->getStyle('A' . $filaTotal . ':E' . $filaTotal)->getFill()->setFillType(\PhpOffice\PhpSpreadsheet\Style\Fill::FILL_SOLID)->getStartColor()->setRGB('D9EAF7');
        if ($fila > 7) {
            $sheet->getStyle('C7:D' . $filaTotal)->getNumberFormat()->setFormatCode('#,##0.00');
            $sheet->getStyle('A6:E' . ($filaTotal - 1))->getBorders()->getAllBorders()->setBorderStyle(\PhpOffice\PhpSpreadsheet\Style\Border::BORDER_THIN)->getColor()->setRGB('D7DEE8');
        }
        foreach (['A' => 12, 'B' => 22, 'C' => 20, 'D' => 20, 'E' => 18] as $column => $width) {
            $sheet->getColumnDimension($column)->setWidth($width);
        }
        $sheet->freezePane('A7');
        $sheet->setAutoFilter('A6:E' . max(6, $filaTotal - 1));

        ob_start();
        \PhpOffice\PhpSpreadsheet\IOFactory::createWriter($spreadsheet, 'Xlsx')->save('php://output');
        $content = ob_get_clean();
        return Yii::$app->response->sendContentAsFile(
            $content,
            'anticipos_cliente_' . $idcliente . '.xlsx',
            ['mimeType' => 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet']
        );
    }

    private function buscarAnticiposCliente(int $idcliente)
    {
        return Adelantos::find()
            ->where(['idcliente' => $idcliente])
            ->andWhere(['or', ['<>', 'estado', 0], ['estado' => null]])
            ->orderBy(['fecha' => SORT_DESC, 'idadelanto' => SORT_DESC]);
    }

    public function actionPrinter(int $idcliente, $fecha): Response|string
    {
        try {
            $cliente = $this->findModel($idcliente);
            $param = Parametros::find()->one();
            $movimientos = DetaMovimientos::find()
                ->alias('m')
                ->joinWith(['movimientoAdelantos  ma' => function ($q) {
                    $q->joinWith('adelantos a');
                }])
                ->where([
                    'a.idcliente' => $idcliente,
                    'm.idmovimiento' => $param->abono_adelanto
                ]);

            if (!empty($fecha)) {
                $inicio = $fecha . ' 00:00:00';
                $fin = date('Y-m-d H:i:s', strtotime($fecha . ' +1 day'));
                $movimientos->andWhere([
                    'and',
                    ['>=', 'm.fecha', $inicio],
                    ['<', 'm.fecha', $fin],
                ]);
            }
            $provider = new ActiveDataProvider();
            $provider->query = $movimientos;
            return $this->render('printer', [
                'cliente' => $cliente,
                'fecha' => $fecha,
                'movimientos' => $provider
            ]);
        } catch (NotFoundHttpException $e) {
            return $this->redirect(['index']);
        }
    }

    /**
     * @throws CrossReferenceException
     * @throws MpdfException
     * @throws InvalidConfigException
     * @throws PdfParserException
     * @throws NotFoundHttpException
     * @throws PdfTypeException
     */
    public function actionVoucherAbonoAdelanto($id, $idcliente): string
    {
        $model = DetaMovimientos::findOne($id);
        $adelantoService = new AdelantoService();
        $cliente = Cliente::findOne($idcliente);
        $saldo = $adelantoService->findAdelantosByIclienteAndActivos($idcliente)->sum('saldo');
        if ($model === null || $cliente === null) {
            throw new NotFoundHttpException('Registro no encontrado');
        }

        $content = $this->renderPartial('voucher_abono_adelanto', [
            'model' => $model,
            'cliente' => $cliente,
            'saldo' => $saldo,
        ]);

        $pdf = new Pdf([
            'mode' => Pdf::MODE_UTF8,
            'format' => Pdf::FORMAT_LETTER,
            'orientation' => Pdf::ORIENT_PORTRAIT,
            'destination' => Pdf::DEST_BROWSER,
            'marginLeft' => 8,
            'marginRight' => 8,
            'marginTop' => 6,
            'marginBottom' => 6,
            'content' => $content,
            'filename' => 'Comprobante_' . $model->iddetamovimientos . '.pdf',
            'cssFile' => '@vendor/kartik-v/yii2-mpdf/src/assets/kv-mpdf-bootstrap.min.css',
            'cssInline' => HelperServices::stylePrint(),
            'options' => [
                'title' => 'Comprobante',
            ],
            'methods' => [
                //'SetHeader' => [''],
                //'SetFooter' => null //[''],
            ],
        ]);

        return $pdf->render();
    }


    /**
     * Creates a new Cliente model.
     * If creation is successful, the browser will be redirected to the 'view' page.
     * @return string|Response
     */
    public function actionCreate(): Response|string
    {
        $model = new Cliente();
        $service = new ClienteService();
        $departamentos = $service->getDepartamentos();
        $tipo = $service->getTipoContribuyente();
        $origen = $service->getOrigenOro();
        $giroNegocio = $service->getGiroNegocio();
        $actividad = $service->getActividadPrincipal();

        // Modelos relacionados
        $clienteJuridico = new Clientejuridico();
        $beneficiarioFinal = new ClienteBeneficiarioFinal();
        $informacionContacto = new ClienteInformacionContacto();
        $juntaDirectiva = [];

        if ($this->request->isPost) {
            $transaction = Yii::$app->db->beginTransaction();
            try {
                // Cargar todos los modelos
                if (!$model->load($this->request->post())) {
                    throw new \RuntimeException('No se recibieron los datos del cliente.');
                }
                $model->documentosUpload = UploadedFile::getInstances($model, 'documentosUpload');
                if (!$model->validate()) {
                    throw new Exception('Corrija los errores del formulario.');
                }
                if ($model->departamento == null) {
                    throw new Exception('Debe seleccionar un departamento');
                }
                if ($model->ciudad == null) {
                    throw new Exception('Debe seleccionar una ciudad');
                }
                $usuario = Yii::$app->user->identity;
                $model->save(false);
                $agencia = Agencia::findOne($usuario->idagencia);
                $model->idcliente = $model->getPrimaryKey();
                $codigo = $agencia->codagencia . str_pad($model->idcliente, 6, "0", STR_PAD_LEFT);
                $model->codcliente = $codigo;
                $model->save(false);
                if ($clienteJuridico->load($this->request->post())) {
                    if (strlen($clienteJuridico->ruc) > 0) {
                        $clienteJuridico->idcliente = $model->getPrimaryKey();
                        $clienteJuridico->save(false);
                    }
                }
                if ($beneficiarioFinal->load($this->request->post())) {
                    if (strlen($beneficiarioFinal->nombre_completo) > 0) {
                        $beneficiarioFinal->idcliente = $model->getPrimaryKey();
                        $beneficiarioFinal->save(false);
                    }
                }
                if ($informacionContacto->load($this->request->post())) {
                    if (strlen($informacionContacto->nombre_legal) > 0) {
                        $informacionContacto->idcliente = $model->getPrimaryKey();
                        $informacionContacto->save(false);
                    }
                }

                // Cargar lista de Junta Directiva
                $juntaData = $this->request->post('ClienteJunta', []);
                foreach ($juntaData as $data) {
                    $junta = new ClienteJunta();
                    $junta->load(['ClienteJunta' => $data]);
                    $juntaDirectiva[] = $junta;
                }

                // 3️⃣ Guardar Junta Directiva
                if (count($juntaDirectiva) > 0) {
                    foreach ($juntaDirectiva as $j) {
                        $j->idcliente = $model->getPrimaryKey();
                        $j->save(false);
                    }
                }

                // Los documentos forman parte de la misma transacción del cliente.
                $service->guardarDocumentos($model);

                $transaction->commit();

                Yii::$app->session->setFlash('success', 'Cliente guardado correctamente.');
                return $this->redirect(['view', 'idcliente' => $model->idcliente]);
            } catch (\Throwable $e) {
                if ($transaction->isActive) {
                    $transaction->rollBack();
                }
                $model->setIsNewRecord(true);
                $model->idcliente = null;
                Yii::error($e->getMessage(), 'cliente');
                Yii::$app->session->setFlash('error', 'Error al guardar los datos: ' . $e->getMessage());
            }

        } else {
            $model->loadDefaultValues();
        }

        return $this->render('create', [
            'model' => $model,
            'clienteJuridico' => $clienteJuridico,
            'juntaDirectiva' => $juntaDirectiva,
            'beneficiarioFinal' => $beneficiarioFinal,
            'informacionContacto' => $informacionContacto,
            'departamentos' => $departamentos,
            'ciudades' => $model->departamento ? $service->getCiudadesMap((int) $model->departamento) : [],
            'tipo' => $tipo,
            'origen' => $origen,
            'giroNegocio' => $giroNegocio,
            'actividad' => $actividad,
        ]);
    }

    public function actionCiudades(): array
    {
        Yii::$app->response->format = Response::FORMAT_JSON;
        $service = new ClienteService();
        $id = $this->request->post('depdrop_parents')[0];
        $ciudades = $service->getCiudades($id);
        return $ciudades;
    }


    /**
     * @throws Exception
     * @throws NotFoundHttpException
     */
    public function actionUpdate(int $idcliente): Response|string
    {
        $model = $this->findModel($idcliente);
        $service = new ClienteService();
        $departamentos = $service->getDepartamentos();
        $ciudades = $service->getCiudadesMap($model->departamento);
        $tipo = $service->getTipoContribuyente();
        $origen = $service->getOrigenOro();
        $giroNegocio = $service->getGiroNegocio();
        $actividad = $service->getActividadPrincipal();
        // Modelos relacionados
        $clienteJuridico = Clientejuridico::find()->where(['idcliente' => $idcliente])->one() ?? new ClienteJuridico();
        $beneficiarioFinal = ClienteBeneficiarioFinal::find()->where(['idcliente' => $idcliente])->one() ?? new ClienteBeneficiarioFinal();
        $informacionContacto = ClienteInformacionContacto::find()->where(['idcliente' => $idcliente])->one() ?? new ClienteInformacionContacto();
        $juntaDirectiva = ClienteJunta::find()->where(['idcliente' => $idcliente])->all() ?? [];

        if ($this->request->isPost && $model->load($this->request->post())) {
            $model->documentosUpload = UploadedFile::getInstances($model, 'documentosUpload');
            $transaction = Yii::$app->db->beginTransaction();
            try {
                if (!$model->save()) {
                    throw new \RuntimeException('Corrija los errores del formulario.');
                }
                if (strlen($model->codcliente) <= 0) {
                    $usuario = Usuario::findOne($model->created_by);
                    $agencia = Agencia::findOne($usuario->idagencia);
                    $codigo = $agencia->codagencia . str_pad($model->idcliente, 6, "0", STR_PAD_LEFT);
                    $model->codcliente = $codigo;
                    $model->save(false);
                }
                $post = $this->request->post();

                // Cliente Jurídico
                if ($clienteJuridico->load($post)) {
                    $clienteJuridico->idcliente = $model->idcliente;
                    $clienteJuridico->save(false);
                }

                // Beneficiario final
                if ($beneficiarioFinal->load($post)) {
                    $beneficiarioFinal->idcliente = $model->idcliente;
                    $beneficiarioFinal->save(false);
                }

                // Información de contacto
                if ($informacionContacto->load($post)) {
                    $informacionContacto->idcliente = $model->idcliente;
                    $informacionContacto->save(false);
                }

                // Junta directiva (si viene en JSON o array)
                if (!empty($post['ClienteJunta'])) {
                    ClienteJunta::deleteAll(['idcliente' => $model->idcliente]); // limpiar previos
                    foreach ($post['ClienteJunta'] as $miembro) {
                        $jd = new ClienteJunta();
                        $jd->setAttributes($miembro);
                        $jd->idcliente = $model->idcliente;
                        $jd->save(false);
                    }
                }

                $service->guardarDocumentos($model);
                $transaction->commit();
                Yii::$app->session->setFlash('success', 'Cliente guardado correctamente.');
                return $this->redirect(['view', 'idcliente' => $model->idcliente]);
            } catch (\Throwable $e) {
                if ($transaction->isActive) {
                    $transaction->rollBack();
                }
                Yii::error($e->getMessage(), 'cliente');
                Yii::$app->session->setFlash('error', 'Error al guardar los datos: ' . $e->getMessage());
            }
        }


        return $this->render('update', [
            'model' => $model,
            'clienteJuridico' => $clienteJuridico,
            'juntaDirectiva' => $juntaDirectiva,
            'beneficiarioFinal' => $beneficiarioFinal,
            'informacionContacto' => $informacionContacto,
            'departamentos' => $departamentos,
            'ciudades' => $ciudades,
            'tipo' => $tipo,
            'origen' => $origen,
            'giroNegocio' => $giroNegocio,
            'actividad' => $actividad,
        ]);
    }

    /**
     * Deletes an existing Cliente model.
     * If deletion is successful, the browser will be redirected to the 'index' page.
     * @param int $idcliente Idcliente
     * @return Response
     * @throws NotFoundHttpException if the model cannot be found
     */
    public function actionDelete($idcliente)
    {
        $this->findModel($idcliente)->delete();

        return $this->redirect(['index']);
    }

    /**
     * @throws NotFoundHttpException
     */
    public function actionDescargarDocumento($id)
    {
        $archivo = ArchivosClientes::findOne($id);

        if (!$archivo) {
            throw new NotFoundHttpException('El documento no existe.');
        }

        if (!$archivo->documentos || !$archivo->documentos->idcliente) {
            throw new NotFoundHttpException('El cliente del documento no existe.');
        }
        [$path, $mime] = (new ClienteDocumentService())->image((int) $archivo->documentos->idcliente, (int) $archivo->idarchivo);
        Yii::$app->response->headers->set('X-Content-Type-Options', 'nosniff');
        Yii::$app->response->headers->set('Cache-Control', 'no-store');
        return Yii::$app->response->sendFile($path, $archivo->nombre_original, ['mimeType' => $mime]);
    }

    /**
     * @throws StaleObjectException
     * @throws \Throwable
     * @throws NotFoundHttpException
     */
    public function actionEliminarDocumento($id)
    {
        $archivo = ArchivosClientes::findOne($id);

        // Verificar que sea método POST
        if (!Yii::$app->request->isPost) {
            throw new \yii\web\MethodNotAllowedHttpException('Método no permitido.');
        }

        if (!$archivo) {
            throw new NotFoundHttpException('El documento no existe.');
        }

        // Obtener el cliente ANTES de eliminar el archivo
        $documento = $archivo->documentos; // Relación singular
        if (!$documento) {
            throw new NotFoundHttpException('El documento padre no existe.');
        }

        $cliente = $documento->cliente; // Relación con cliente
        if (!$cliente) {
            throw new NotFoundHttpException('El cliente no existe.');
        }

        $clienteId = $cliente->idcliente;

        try {
            (new ClienteDocumentService())->delete((int) $clienteId, (int) $archivo->idarchivo);
            Yii::$app->session->setFlash('success', 'Documento eliminado correctamente.');
        } catch (\Throwable $error) {
            Yii::error($error->getMessage(), 'cliente');
            if (Yii::$app->request->isAjax) {
                Yii::$app->response->statusCode = 500;
                return $this->asJson(['success' => false, 'message' => 'No se pudo eliminar el documento.']);
            }
            Yii::$app->session->setFlash('error', 'No se pudo eliminar el documento.');
        }
        if (Yii::$app->request->isAjax) {
            return $this->asJson(['success' => true]);
        }
        return $this->redirect(['view', 'idcliente' => $clienteId]);
    }

    /**
     * Finds the Cliente model based on its primary key value.
     * If the model is not found, a 404 HTTP exception will be thrown.
     * @param int $idcliente Idcliente
     * @return Cliente the loaded model
     * @throws NotFoundHttpException if the model cannot be found
     */
    protected function findModel(int $idcliente): Cliente
    {
        if (($model = Cliente::findOne(['idcliente' => $idcliente])) !== null) {
            return $model;
        }

        throw new NotFoundHttpException('The requested page does not exist.');
    }

    public function actionApiClientes()
    {
        Yii::$app->response->format = Response::FORMAT_JSON;
        return Cliente::find()->all();
    }
}
