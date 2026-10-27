<?php

namespace app\models;

use Exception;
use Yii;
use yii\db\ActiveRecord;
use yii\web\IdentityInterface;

/**
 * This is the model class for table "usuario".
 *
 * @property int $idusuario
 * @property bool $activo
 * @property bool $imprimir_sin_firma
 * @property string|null $email
 * @property string|null $nombre
 * @property string|null $password
 * @property string|null $titulo
 * @property string|null $username
 * @property string|null $created_at
 * @property string $updated_at
 * @property string|null $auth_key
 * @property string|null $access_token
 * @property int|null $idcaja
 * @property int|null $idagencia
 *
 * @property Caja $caja
 */
class Usuario extends ActiveRecord implements IdentityInterface
{

    public $password_new;   // atributo virtual, NO se guarda en BD

    public function scenarios(): array
    {
        $scenarios = parent::scenarios();

        // Campos permitidos en cada escenario
        $scenarios['create'] = $scenarios[self::SCENARIO_DEFAULT];
        $scenarios['create'][] = 'password_new';

        $scenarios['update'] = $scenarios[self::SCENARIO_DEFAULT];
        $scenarios['update'][] = 'password_new';

        return $scenarios;
    }


    /**
     * {@inheritdoc}
     */
    public static function tableName()
    {
        return 'usuario';
    }

    public static function findByUsername($username): ?Usuario
    {
        return static::findOne(['username' => $username]);
    }

    /**
     * Validates password
     *
     * @param string $password password to validate
     * @return bool if password provided is valid for current user
     */
    public function validatePassword(string $password): bool
    {
        return Yii::$app->getSecurity()->validatePassword($password, $this->password);
    }


    /**
     * {@inheritdoc}
     */
    public function rules()
    {
        return [
            [['email', 'nombre', 'password', 'titulo', 'username', 'auth_key', 'access_token', 'idcaja'], 'default', 'value' => null],
            [['activo'], 'default', 'value' => 0],
            [['activo'], 'boolean'],
            [['imprimir_sin_firma'], 'default', 'value' => 0],
            [['imprimir_sin_firma'], 'boolean'],
            [['created_at', 'updated_at'], 'safe'],
            ['password_new', 'required', 'on' => 'create'],
            [['password_new'], 'safe'],
            [['idcaja', 'idagencia'], 'integer'],
            [['email', 'nombre', 'password', 'titulo', 'username', 'auth_key', 'access_token'], 'string', 'max' => 255],
            [['idcaja'], 'exist', 'skipOnError' => true, 'targetClass' => Caja::class, 'targetAttribute' => ['idcaja' => 'idcaja']],
        ];
    }

    /**
     * {@inheritdoc}
     */
    public function attributeLabels()
    {
        return [
            'idusuario' => 'Idusuario',
            'activo' => 'Activo',
            'imprimir_sin_firma' => 'Permitir impresión sin firma',
            'email' => 'Email',
            'nombre' => 'Nombre',
            'password' => 'Password',
            'titulo' => 'Titulo',
            'username' => 'Username',
            'created_at' => 'Created At',
            'updated_at' => 'Updated At',
            'auth_key' => 'Auth Key',
            'access_token' => 'Access Token',
            'idcaja' => 'Caja',
            'idagencia' => 'Agencia',
        ];
    }

    /**
     * Gets query for [[Caja]].
     *
     * @return \yii\db\ActiveQuery|CajaQuery
     */
    public function getCaja(): CajaQuery|\yii\db\ActiveQuery
    {
        return $this->hasOne(Caja::class, ['idcaja' => 'idcaja']);
    }

    /**
     * {@inheritdoc}
     * @return UsuarioQuery the active query used by this AR class.
     */
    public static function find(): UsuarioQuery
    {
        return new UsuarioQuery(get_called_class());
    }

    /**
     * @throws Exception
     */
    public function beforeSave($insert): bool
    {
        if (parent::beforeSave($insert)) {
            if ($this->isNewRecord) {
                $this->auth_key = Yii::$app->security->generateRandomString();
            }
            if (!empty($this->password_new)) {
                $this->password = Yii::$app->security->generatePasswordHash($this->password_new);
            }
            return true;
        }
        return false;
    }

    public static function findIdentity($id): Usuario|IdentityInterface|null
    {
        return static::findone($id);
    }

    public static function findIdentityByAccessToken($token, $type = null): Usuario|IdentityInterface|null
    {
        return static::findOne(['access_token' => $token]);
    }

    public function getId(): int
    {
        return $this->idusuario;
    }

    public function getAuthKey(): ?string
    {
        return $this->auth_key;
    }

    public function validateAuthKey($authKey): bool
    {
        return $this->getAuthKey() === $authKey;
    }

    public function esAdministrador(): bool
    {
        return \Yii::$app->user->can('administrador');
    }
}
