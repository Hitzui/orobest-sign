<?php

namespace app\services;

use app\models\AdelantosClientesView;
use app\models\CatalogoItem;
use app\models\Cliente;
use yii\helpers\ArrayHelper;

class ClienteService extends BaseServices
{

    public function getActividadPrincipal(): array
    {
        return self::getCatalogoItem('actividad_principal');
    }

    public function getGiroNegocio(): array
    {
        return self::getCatalogoItem('giro_negocio');
    }

    public function getOrigenOro(): array
    {
        return self::getCatalogoItem('origen_oro');
    }

    public function getTipoContribuyente(): array
    {
        return self::getcatalogoItem('tipo_contribuyente');
    }

    public function getDepartamentos(): array
    {
        $departamentos = CatalogoItem::find()->where(['catalogo_id' => $this->parametros->departamentos])->all();
        return ArrayHelper::map($departamentos, 'idcatalog_item', 'nombre');
    }

    public function getCiudades(int $id): array
    {
        $ciudades = CatalogoItem::find()
            ->select(['idcatalog_item AS id', 'nombre AS name'])
            ->where([
                'catalogo_id' => $this->parametros->ciudades,
                'parent_id' => $id
            ])
            ->asArray()
            ->all();

        return ['output' => $ciudades, 'selected' => ''];
    }

    public function getCiudadesMap(int $id): array
    {
        return self::getcatalogoItem('ciudades');
    }

    public function getClientesAdelantos($idcliente): ?AdelantosClientesView
    {
        return AdelantosClientesView::find()->where(['idcliente' => $idcliente])->one();
    }

    public function getClientesArrayMap()
    {
        return ArrayHelper::map(
            Cliente::find()->all(),
            'idcliente',
            function ($cliente) {
                return $cliente->nombres . ' ' . $cliente->apellidos;
            }
        );
    }


    public function guardarDocumentos(Cliente $cliente): bool
    {
        (new ClienteDocumentService())->saveUploads($cliente, $cliente->documentosUpload ?? []);
        return true;
    }

    /**
     * Eliminar documentos (opcional, para update)
     */
    public function eliminarDocumentos(Cliente $cliente)
    {
        $service = new ClienteDocumentService();
        foreach ($service->files((int) $cliente->idcliente) as $archivo) {
            $service->delete((int) $cliente->idcliente, (int) $archivo->idarchivo);
        }
    }
}
