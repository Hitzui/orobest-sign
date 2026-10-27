<?php

namespace app\modules\sign\services;

/** Copia de impresión: negro sólido y refuerzo del trazo, sin modificar el PNG guardado. */
class SignaturePrintRenderer
{
    // La plantilla imprime la firma a 60 mm. Se añaden 0,10 mm por cada lado
    // del trazo para que sobreviva a la impresión matricial sin empastar la láser.
    private const PRINT_WIDTH_MM = 60;
    private const EXPANSION_MM = 0.10;
    private const INK_THRESHOLD = 200000; // Luminancia 0..255 multiplicada por 1000.

    public function render(string $png): ?string
    {
        if ($png === '' || strlen($png) > FirmaService::MAX_BYTES) {
            return null;
        }
        $size = @getimagesizefromstring($png);
        if (!$size || $size[2] !== IMAGETYPE_PNG || $size[0] < 1 || $size[1] < 1
            || $size[0] > 2048 || $size[1] > 1024) {
            return null;
        }
        $source = @imagecreatefromstring($png);
        if (!$source) {
            return null;
        }
        [$width, $height] = $size;
        $flat = imagecreatetruecolor($width, $height);
        // Componer primero sobre blanco también admite PNG con transparencia.
        imagefill($flat, 0, 0, imagecolorallocate($flat, 255, 255, 255));
        imagecopy($flat, $source, 0, 0, 0, 0, $width, $height);
        $output = imagecreate($width, $height);
        imagecolorallocate($output, 255, 255, 255);
        $black = imagecolorallocate($output, 0, 0, 0);
        $radius = max(1, (int) round($width * self::EXPANSION_MM / self::PRINT_WIDTH_MM));
        $spreads = [];
        for ($dy = -$radius; $dy <= $radius; $dy++) {
            $spreads[$dy] = (int) floor(sqrt($radius * $radius - $dy * $dy));
        }
        try {
            for ($y = 0; $y < $height; $y++) {
                $start = null;
                for ($x = 0; $x <= $width; $x++) {
                    $ink = false;
                    if ($x < $width) {
                        $color = imagecolorat($flat, $x, $y);
                        $luminance = 299 * (($color >> 16) & 255) + 587 * (($color >> 8) & 255) + 114 * ($color & 255);
                        $ink = $luminance < self::INK_THRESHOLD;
                    }
                    if ($ink && $start === null) {
                        $start = $x;
                    } elseif (!$ink && $start !== null) {
                        // Dilatar los segmentos con un disco pequeño conserva extremos
                        // redondeados y evita recorrer un vecindario por cada píxel.
                        foreach ($spreads as $dy => $spread) {
                            $row = $y + $dy;
                            if ($row >= 0 && $row < $height) {
                                imageline($output, max(0, $start - $spread), $row, min($width - 1, $x - 1 + $spread), $row, $black);
                            }
                        }
                        $start = null;
                    }
                }
            }
            ob_start();
            try {
                return imagepng($output, null, 6) ? ob_get_contents() : null;
            } finally {
                ob_end_clean();
            }
        } finally {
            imagedestroy($source);
            imagedestroy($flat);
            imagedestroy($output);
        }
    }
}
