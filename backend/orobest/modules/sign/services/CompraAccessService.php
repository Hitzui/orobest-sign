<?php

namespace app\modules\sign\services;

use app\modules\sign\components\ApiException;
use app\modules\sign\models\CompraFirma;
use yii\db\Expression;
use yii\db\Query;

class CompraAccessService
{
    private function query(int $agency): Query
    {
        return (new Query())->from(['p' => 'compra'])
            ->innerJoin(['c' => 'caja'], 'c.idcaja = p.idcaja')
            ->leftJoin(['cl' => 'cliente'], 'cl.idcliente = p.idcliente')
            ->select(['p.*', 'cliente_nombre' => new Expression("TRIM(CONCAT_WS(' ', cl.nombres, cl.apellidos))")])
            ->where(['c.idagencia' => $agency]);
    }

    public function find(int $id, int $agency): array
    {
        $row = $this->query($agency)->andWhere(['p.idcompra' => $id])->one();
        if (!$row) {
            throw new ApiException(404, 'purchase_not_found', 'Compra no encontrada en la agencia asignada.');
        }
        return $row;
    }

    public static function integer($value, string $field, int $minimum = 1, int $maximum = PHP_INT_MAX): int
    {
        if ((!is_string($value) && !is_int($value)) || !preg_match('/^(0|[1-9][0-9]*)$/D', (string) $value)
            || filter_var($value, FILTER_VALIDATE_INT) === false || (int) $value < $minimum || (int) $value > $maximum) {
            throw new ApiException(422, 'invalid_input', "El campo $field no es válido.");
        }
        return (int) $value;
    }

    public function listing(int $agency, array $params): array
    {
        foreach (['idagencia', 'idcaja', 'idusuario'] as $field) {
            if (array_key_exists($field, $params)) {
                throw new ApiException(422, 'scope_not_selectable', 'La agencia se obtiene de la sesión.');
            }
        }
        $page = self::integer($params['page'] ?? 1, 'page', 1, 1000000);
        $pageSize = self::integer($params['page_size'] ?? 20, 'page_size', 1, 100);
        $query = $this->query($agency)->andWhere(['p.estado' => 1]);
        $dates = [];
        foreach (['desde', 'hasta'] as $field) {
            if (isset($params[$field])) {
                $value = $params[$field];
                $date = is_string($value) ? \DateTimeImmutable::createFromFormat('!Y-m-d', $value) : false;
                if (!$date || $date->format('Y-m-d') !== $value) {
                    throw new ApiException(422, 'invalid_date', "La fecha $field debe usar YYYY-MM-DD.");
                }
                $dates[$field] = $date;
            }
        }
        if (isset($dates['desde'], $dates['hasta']) && $dates['desde'] > $dates['hasta']) {
            throw new ApiException(422, 'invalid_date_range', 'El intervalo de fechas no es válido.');
        }
        if (isset($dates['desde'])) {
            $query->andWhere(['>=', 'p.fecha', $dates['desde']->format('Y-m-d 00:00:00')]);
        }
        if (isset($dates['hasta'])) {
            $query->andWhere(['<', 'p.fecha', $dates['hasta']->modify('+1 day')->format('Y-m-d 00:00:00')]);
        }
        if (isset($params['q'])) {
            if (!is_string($params['q']) || mb_strlen($params['q']) > 100) {
                throw new ApiException(422, 'invalid_search', 'La búsqueda no es válida.');
            }
            $query->andWhere(['like', 'p.numcompra', trim($params['q'])]);
        }
        if (isset($params['firma'])) {
            if (!in_array($params['firma'], ['firmada', 'pendiente'], true)) {
                throw new ApiException(422, 'invalid_signature_filter', 'Usa firma=firmada o firma=pendiente.');
            }
            $exists = (new Query())->select(new Expression('1'))->from(['f' => 'compra_firma'])
                ->where('f.idcompra = p.idcompra');
            $query->andWhere([$params['firma'] === 'firmada' ? 'exists' : 'not exists', $exists]);
        }
        $total = (int) (clone $query)->count();
        $rows = $query->orderBy(['p.fecha' => SORT_DESC, 'p.idcompra' => SORT_DESC])
            ->offset(($page - 1) * $pageSize)->limit($pageSize)->all();
        $signatures = [];
        if ($rows) {
            $latestIds = (new Query())->select(new Expression('MAX(idfirma)'))->from('compra_firma')
                ->where(['idcompra' => array_column($rows, 'idcompra')])->groupBy('idcompra');
            foreach (CompraFirma::find()->where(['idfirma' => $latestIds])->all() as $signature) {
                $signatures[$signature->idcompra] = $signature->metadata();
            }
        }
        return [
            'items' => array_map(fn ($row) => $this->summary($row) + ['firma' => $signatures[$row['idcompra']] ?? null], $rows),
            'pagination' => [
                'page' => $page, 'page_size' => $pageSize, 'total' => $total,
                'total_pages' => (int) ceil($total / $pageSize),
            ],
        ];
    }

    public function summary(array $row): array
    {
        return [
            'idcompra' => (int) $row['idcompra'],
            'numcompra' => $row['numcompra'],
            'fecha' => (new \DateTimeImmutable($row['fecha'], new \DateTimeZone('America/Managua')))->format(DATE_ATOM),
            'estado' => (int) $row['estado'],
            'total' => $row['total'] === null ? null : (string) $row['total'],
            'cliente' => $row['idcliente'] === null ? null : [
                'idcliente' => (int) $row['idcliente'], 'nombre' => $row['cliente_nombre'],
            ],
        ];
    }

    public function revision(array $row): string
    {
        $purchase = array_intersect_key($row, array_flip([
            'idcompra', 'numcompra', 'idcliente', 'fecha', 'idcaja', 'estado', 'forma_pago',
            'efectivo', 'transferencia', 'adelanto', 'total', 'subtotal', 'descuento', 'updated_at',
        ]));
        $details = (new Query())->from('deta_compra')->select([
            'iddetacompra', 'linea', 'descripcion', 'kilataje', 'kilate', 'precio', 'peso', 'importe', 'lectura',
        ])->where(['idcompra' => $row['idcompra']])->orderBy(['iddetacompra' => SORT_ASC])->all();
        return hash('sha256', json_encode([$purchase, $details], JSON_THROW_ON_ERROR | JSON_UNESCAPED_UNICODE));
    }

    public function detail(int $id, int $agency): array
    {
        $row = $this->find($id, $agency);
        $revision = $this->revision($row);
        $latest = CompraFirma::find()->where(['idcompra' => $id])->orderBy(['version' => SORT_DESC])->one();
        return $this->summary($row) + [
            'revision' => $revision,
            'firma' => $latest ? $latest->metadata() + [
                'compra_modificada' => !hash_equals($latest->compra_revision, $revision),
            ] : null,
        ];
    }
}
