<?php

/** Pruebas de imagen sintéticas: no usan base de datos ni firmas de clientes. */
require dirname(__DIR__) . '/orobest/modules/sign/services/FirmaService.php';
require dirname(__DIR__) . '/orobest/modules/sign/services/SignaturePrintRenderer.php';

use app\modules\sign\services\SignaturePrintRenderer;

$checks = 0;
function verify(bool $condition, string $label): void
{
    global $checks;
    if (!$condition) {
        throw new RuntimeException('FAILED: ' . $label);
    }
    $checks++;
    echo "OK: $label\n";
}
function pngBytes(GdImage $image): string
{
    ob_start();
    try {
        imagepng($image);
        return ob_get_contents();
    } finally {
        ob_end_clean();
    }
}
function rgbAt(GdImage $image, int $x, int $y): array
{
    $color = imagecolorsforindex($image, imagecolorat($image, $x, $y));
    return [$color['red'], $color['green'], $color['blue']];
}

$renderer = new SignaturePrintRenderer();
$image = imagecreatetruecolor(1600, 800);
imagefill($image, 0, 0, imagecolorallocate($image, 255, 255, 255));
$blue = imagecolorallocate($image, 65, 93, 138);
imagefilledrectangle($image, 200, 300, 800, 303, $blue);
imagefilledrectangle($image, 900, 500, 1200, 503, $blue);
imagefilledrectangle($image, 900, 530, 1200, 533, $blue);
imagefilledellipse($image, 1400, 200, 4, 4, $blue);
$original = pngBytes($image);
$originalHash = hash('sha256', $original);
$printed = $renderer->render($original);
verify($printed !== null, 'genera la variante de impresión de un PNG válido');
$result = imagecreatefromstring($printed);
verify(imagesx($result) === 1600 && imagesy($result) === 800, 'conserva dimensiones y proporción del original');
verify(rgbAt($result, 500, 301) === [0, 0, 0], 'convierte la tinta azul en negro sólido');
$thickness = 0;
for ($y = 0; $y < 800; $y++) {
    if (rgbAt($result, 500, $y) === [0, 0, 0]) {
        $thickness++;
    }
}
verify($thickness === 10, 'refuerza el trazo típico de 4 a 10 píxeles: unos 0,375 mm al imprimir a 60 mm');
verify(rgbAt($result, 196, 301) === [255, 255, 255] && rgbAt($result, 197, 301) === [0, 0, 0], 'expansión limitada sin desplazar el trazo');
verify(rgbAt($result, 1000, 515) === [255, 255, 255], 'mantiene separados trazos próximos sin rellenar sus espacios');
verify(rgbAt($result, 1400, 200) === [0, 0, 0], 'conserva puntos y trazos breves');
verify(imagecolorstotal($result) === 2 && rgbAt($result, 0, 0) === [255, 255, 255], 'salida de dos colores, sin grises ni fondo añadido');
verify(hash('sha256', $original) === $originalHash && rgbAt($image, 500, 301) === [65, 93, 138], 'preserva la firma original en azul');

$transparent = imagecreatetruecolor(160, 80);
imagealphablending($transparent, false);
imagesavealpha($transparent, true);
imagefill($transparent, 0, 0, imagecolorallocatealpha($transparent, 0, 0, 0, 127));
imagesetpixel($transparent, 80, 40, imagecolorallocatealpha($transparent, 65, 93, 138, 0));
$transparentResult = imagecreatefromstring($renderer->render(pngBytes($transparent)));
verify(rgbAt($transparentResult, 0, 0) === [255, 255, 255] && rgbAt($transparentResult, 80, 40) === [0, 0, 0], 'compone transparencias sobre blanco sin convertir el fondo en tinta');
verify($renderer->render('') === null && $renderer->render('not a png') === null, 'rechaza imágenes ausentes o corruptas sin imprimir contenido inválido');

$oversized = imagecreatetruecolor(2049, 20);
verify($renderer->render(pngBytes($oversized)) === null, 'limita dimensiones antes de decodificar la firma');

// Exportación opcional para inspeccionar una firma sintética, nunca datos personales.
if (isset($argv[1])) {
    $directory = $argv[1];
    if (!is_dir($directory) && !mkdir($directory, 0700, true)) {
        throw new RuntimeException('No se pudo crear el directorio de muestra.');
    }
    $sample = imagecreatetruecolor(1600, 800);
    imagefill($sample, 0, 0, imagecolorallocate($sample, 255, 255, 255));
    $sampleBlue = imagecolorallocate($sample, 65, 93, 138);
    imagesetthickness($sample, 4);
    $previous = null;
    for ($x = 200; $x < 1400; $x++) {
        $y = (int) round(360 + 65 * sin(($x - 200) / 28) + 35 * sin(($x - 200) / 11));
        if ($previous !== null) {
            imageline($sample, $x - 1, $previous, $x, $y, $sampleBlue);
        }
        $previous = $y;
    }
    imageline($sample, 200, 500, 1350, 420, $sampleBlue);
    $samplePng = pngBytes($sample);
    file_put_contents($directory . '/firma-antes.png', $samplePng);
    file_put_contents($directory . '/firma-impresion.png', $renderer->render($samplePng));
    imagedestroy($sample);
}

foreach ([$image, $result, $transparent, $transparentResult, $oversized] as $resource) {
    imagedestroy($resource);
}
echo "PASS: $checks comprobaciones de impresión de firmas.\n";
