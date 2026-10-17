<?php

namespace app\models;

use Yii;

/**
 * This is the model class for table "cliente".
 *
 * @property int $idcliente
 * @property string $nombres
 * @property string $apellidos
 * @property string|null $direccion
 * @property int|null $departamento
 * @property int|null $ciudad
 * @property string|null $cedula
 * @property string|null $telefono
 * @property string|null $celular
 * @property int|null $actividad_principal
 * @property string|null $procedencia
 * @property int|null $tipo
 * @property string|null $giro_negocio
 * @property string|null $observaciones
 * @property string|null $email
 * @property int|null $bancos
 * @property string|null $cuenta1
 * @property string|null $cuenta2
 * @property int|null $created_by
 * @property string $created_at
 * @property int|null $updated_by
 * @property string $updated_at
 * @property int|null $pep
 * @property string|null $cargo_pep
 * @property string|null $familiares_pep
 * @property string|null $codcliente
 *
 * @property Adelantos[] $adelantos
 * @property ClienteBeneficiarioFinal[] $clienteBeneficiarioFinals
 * @property ClienteInformacionContacto[] $clienteInformacionContactos
 * @property ClienteJunta[] $clienteJuntas
 * @property ClienteJuridico[] $clienteJuridicos
 * @property Compra[] $compras
 * @property Documentos[] $documentos
 */
class Cliente extends \app\models\BaseModel
{

    public $documentosUpload;


    /**
     * {@inheritdoc}
     */
    public static function tableName()
    {
        return 'cliente';
    }

    /**
     * {@inheritdoc}
     */
    public function rules()
    {
        return [
            [['direccion', 'departamento', 'ciudad', 'cedula', 'telefono', 'celular', 'actividad_principal', 'procedencia', 'tipo', 'giro_negocio', 'observaciones', 'email', 'bancos', 'cuenta1', 'cuenta2', 'created_by', 'updated_by', 'cargo_pep', 'familiares_pep'], 'default', 'value' => null],
            [['pep'], 'default', 'value' => 0],
            [['nombres', 'apellidos'], 'required'],
            [['direccion', 'observaciones', 'familiares_pep'], 'string'],
            [['departamento', 'ciudad', 'actividad_principal', 'tipo', 'bancos', 'created_by', 'updated_by', 'pep'], 'integer'],
            [['created_at', 'updated_at'], 'safe'],
            [['nombres', 'apellidos', 'procedencia', 'giro_negocio', 'cuenta1', 'cuenta2', 'codcliente', 'cargo_pep'], 'string', 'max' => 255],
            [['cedula'], 'string', 'max' => 14],
            ['cedula', 'unique', 'message' => 'La cédula ingresada ya está registrada.'],
            ['cedula', 'trim'],
// Normaliza: quita símbolos y fuerza MAYÚSCULA
            [
                'cedula',
                'filter',
                'filter' => function ($value) {
                    $value = preg_replace('/[^0-9A-Za-z]/', '', $value);
                    return mb_strtoupper($value, 'UTF-8');
                }
            ],
// Validación formal de formato
            [
                'cedula',
                'match',
                'pattern' => '/^[0-9]{13}[A-Z]$/',
                'message' => 'La cédula debe tener 13 dígitos seguidos de una letra en mayúscula. Ejemplo: 0010101010011L'
            ],
            [['telefono', 'celular'], 'string', 'max' => 20],
            [['email'], 'string', 'max' => 50],
            [['documentosUpload'], 'file',
                'skipOnEmpty' => true,
                'maxFiles' => \app\services\ClienteDocumentService::MAX_FILES,
                'maxSize' => \app\services\ClienteDocumentService::MAX_BYTES,
                'extensions' => \app\services\ClienteDocumentService::EXTENSIONS,
                'checkExtensionByMimeType' => false, // El servicio compartido inspecciona el contenido real.
            ],
        ];
    }

    /**
     * {@inheritdoc}
     */
    public function attributeLabels()
    {
        return [
            'idcliente' => 'Idcliente',
            'nombres' => 'Nombres',
            'apellidos' => 'Apellidos',
            'direccion' => 'Direccion',
            'departamento' => 'Departamento',
            'ciudad' => 'Ciudad',
            'cedula' => 'Cedula',
            'telefono' => 'Telefono',
            'celular' => 'Celular',
            'actividad_principal' => 'Actividad Principal',
            'procedencia' => 'Procedencia',
            'tipo' => 'Tipo',
            'giro_negocio' => 'Giro Negocio',
            'observaciones' => 'Observaciones',
            'email' => 'Email',
            'bancos' => 'Bancos',
            'cuenta1' => 'Cuenta1',
            'cuenta2' => 'Cuenta2',
            'created_by' => 'Created By',
            'created_at' => 'Created At',
            'updated_by' => 'Updated By',
            'updated_at' => 'Updated At',
            'pep' => 'Pep',
            'cargo_pep' => 'Cargo Pep',
            'familiares_pep' => 'Familiares Pep',
            'documentosUpload' => 'Documentos del Cliente',
            'codcliente' => 'Cod Cliente',
        ];
    }

    /**
     * Gets query for [[Adelantos]].
     *
     * @return \yii\db\ActiveQuery|AdelantosQuery
     */
    public function getAdelantos()
    {
        return $this->hasMany(Adelantos::class, ['idcliente' => 'idcliente']);
    }

    /**
     * Gets query for [[ClienteBeneficiarioFinals]].
     *
     * @return \yii\db\ActiveQuery|ClienteBeneficiarioFinalQuery
     */
    public function getClienteBeneficiarioFinals()
    {
        return $this->hasMany(ClienteBeneficiarioFinal::class, ['idcliente' => 'idcliente']);
    }

    /**
     * Gets query for [[ClienteInformacionContactos]].
     *
     * @return \yii\db\ActiveQuery|ClienteInformacionContactoQuery
     */
    public function getClienteInformacionContactos()
    {
        return $this->hasMany(ClienteInformacionContacto::class, ['idcliente' => 'idcliente']);
    }

    /**
     * Gets query for [[ClienteJuntas]].
     *
     * @return \yii\db\ActiveQuery|ClienteJuntaQuery
     */
    public function getClienteJuntas()
    {
        return $this->hasMany(ClienteJunta::class, ['idcliente' => 'idcliente']);
    }

    /**
     * Gets query for [[ClienteJuridicos]].
     *
     * @return \yii\db\ActiveQuery|ClienteJuridicoQuery
     */
    public function getClienteJuridicos()
    {
        return $this->hasMany(ClienteJuridico::class, ['idcliente' => 'idcliente']);
    }

    /**
     * Gets query for [[Compras]].
     *
     * @return \yii\db\ActiveQuery|CompraQuery
     */
    public function getCompras()
    {
        return $this->hasMany(Compra::class, ['idcliente' => 'idcliente']);
    }

    /**
     * Gets query for [[Documentos]].
     *
     * @return \yii\db\ActiveQuery|DocumentosQuery
     */
    public function getDocumentos()
    {
        return $this->hasOne(Documentos::class, ['idcliente' => 'idcliente']);
    }

    /**
     * {@inheritdoc}
     * @return ClienteQuery the active query used by this AR class.
     */
    public static function find(): ClienteQuery
    {
        return new ClienteQuery(get_called_class());
    }

}
