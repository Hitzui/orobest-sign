# Icono de OroBest Sign

![Icono de OroBest Sign](assets/icono-orobest-sign.png)

Identidad: documento blanco con trazo de firma y pluma diagonal sobre azul de marca. Se utilizan exclusivamente los cuatro colores de la [paleta aprobada](PALETA.md). El símbolo evita texto pequeño para conservar legibilidad en el lanzador y la barra de tareas.

## Fuentes editables

- [`assets/branding/orobest-sign.svg`](../assets/branding/orobest-sign.svg): diseño principal, en un lienzo de 128 × 128, escalable.
- [`assets/branding/orobest-sign-monochrome.svg`](../assets/branding/orobest-sign-monochrome.svg): variante de un solo color para iconos temáticos Android.

## Integración por plataforma

| Destino | Recursos |
| --- | --- |
| Android 24–25 | PNG normal y circular en mdpi, hdpi, xhdpi, xxhdpi y xxxhdpi |
| Android 26+ | Icono adaptativo con fondo azul y símbolo vectorial dentro de la zona segura |
| Android 33+ | Capa `monochrome` para los lanzadores con iconos temáticos activados |
| iOS | `AppIcon.appiconset/app-icon-1024.png`, 1024 × 1024 RGB, opaco y sin esquinas premarcadas |
| Ventana de escritorio | `desktopApp/src/main/resources/orobest-sign.png`, usado por `Window.icon` |
| Distribución Windows | `orobest-sign.ico`, con tamaños 16, 24, 32, 48, 64, 128 y 256 |
| Distribución macOS | `orobest-sign.icns`, con resoluciones hasta 1024 × 1024 |
| Distribución Linux | `orobest-sign.png`, 512 × 512 |

Las esquinas de iOS las aplica el sistema operativo. Las variantes de escritorio y Android anterior a los iconos adaptativos incluyen su propia forma redondeada. El nombre visible de la app en Android y en la ventana de escritorio es **OroBest Sign**.

## Regeneración

El script utiliza Python y Pillow únicamente como herramientas de diseño, sin añadir dependencias a la aplicación:

```powershell
python -m pip install Pillow==11.3.0
python tools/generate_app_icons.py
```

El generador lee los SVG y escribe los PNG, vectores Android, ICO, ICNS y la imagen de esta documentación. Admite los comandos SVG absolutos `M`, `L`, `H`, `V`, `Q`, `C` y `Z` utilizados por las fuentes actuales; no es un renderizador SVG general.

## Verificación

- APK Android debug compilado con los recursos nuevos.
- Aplicación Windows generada mediante `:desktopApp:createDistributable`; el icono se comprobó extrayéndolo del ejecutable resultante.
- Tamaños y lectura de ICO/ICNS/PNG verificados; PNG de iOS confirmado como RGB de 1024 × 1024 sin transparencia.
- El empaquetado y la apariencia nativos de iOS/macOS requieren macOS/Xcode. El empaquetado Linux se verifica en Linux.
