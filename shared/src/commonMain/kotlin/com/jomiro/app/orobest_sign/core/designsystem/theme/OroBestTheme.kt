package com.jomiro.app.orobest_sign.core.designsystem.theme

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.composables.ui.theme.ColorScheme
import com.composables.ui.theme.ComposablesTheme
import com.composables.ui.theme.LocalColorScheme
import com.composables.ui.theme.alphas
import com.composables.ui.theme.backgroundColor
import com.composables.ui.theme.borderColor
import com.composables.ui.theme.buttonShape
import com.composables.ui.theme.colors
import com.composables.ui.theme.controlColor
import com.composables.ui.theme.defaultIndication as defaultIndicationToken
import com.composables.ui.theme.defaultTextSelectionColors as selectionColorsToken
import com.composables.ui.theme.destructiveColor
import com.composables.ui.theme.dialogShape
import com.composables.ui.theme.disabledAlpha
import com.composables.ui.theme.fieldColor
import com.composables.ui.theme.fieldShape
import com.composables.ui.theme.indications
import com.composables.ui.theme.inverseIndication
import com.composables.ui.theme.largeShape
import com.composables.ui.theme.mediumShape
import com.composables.ui.theme.menuShape
import com.composables.ui.theme.mutedColor
import com.composables.ui.theme.onBackgroundColor
import com.composables.ui.theme.onControlColor
import com.composables.ui.theme.onDestructiveColor
import com.composables.ui.theme.onFieldColor
import com.composables.ui.theme.onPanelColor
import com.composables.ui.theme.onPrimaryColor
import com.composables.ui.theme.onSecondaryColor
import com.composables.ui.theme.onSelectedControlColor
import com.composables.ui.theme.overlayShadow
import com.composables.ui.theme.panelColor
import com.composables.ui.theme.primaryColor
import com.composables.ui.theme.raisedShadow
import com.composables.ui.theme.ringColor
import com.composables.ui.theme.scrimColor
import com.composables.ui.theme.secondaryColor
import com.composables.ui.theme.selectedControlColor
import com.composables.ui.theme.shadows
import com.composables.ui.theme.shapes
import com.composables.ui.theme.sheetShape
import com.composables.ui.theme.smallShape
import com.composables.ui.theme.switchSelectedTrackColor
import com.composables.ui.theme.switchThumbColor
import com.composables.ui.theme.switchTrackColor
import com.composables.ui.theme.textSelectionColors
import com.composables.ui.theme.thumbColor
import com.composeunstyled.LocalTextStyle
import com.composeunstyled.theme.Theme
import com.composeunstyled.theme.ThemeBuilder
import com.composeunstyled.theme.ThemeProperty
import com.composeunstyled.theme.ThemeToken
import com.composeunstyled.theme.buildTheme

private val BrandTheme = buildTheme {
    name = "OroBest Sign"
    defaultTextStyle = LocalTextStyle.current
    defaultContentColor = OroBestPalette.BrandBlue
    defaultIndication = LocalIndication.current
    val selectionColors = TextSelectionColors(OroBestPalette.BrandBlue, OroBestPalette.PaleBlue)
    defaultTextSelectionColors = selectionColors

    properties[colors] = mapOf(
        backgroundColor to OroBestPalette.White,
        onBackgroundColor to OroBestPalette.BrandBlue,
        panelColor to OroBestPalette.White,
        onPanelColor to OroBestPalette.BrandBlue,
        mutedColor to OroBestPalette.BrandBlue,
        primaryColor to OroBestPalette.BrandBlue,
        onPrimaryColor to OroBestPalette.White,
        secondaryColor to OroBestPalette.PaleBlue,
        onSecondaryColor to OroBestPalette.BrandBlue,
        controlColor to OroBestPalette.PaleBlue,
        onControlColor to OroBestPalette.BrandBlue,
        thumbColor to OroBestPalette.BrandBlue,
        switchTrackColor to OroBestPalette.SoftBlue,
        switchSelectedTrackColor to OroBestPalette.BrandBlue,
        switchThumbColor to OroBestPalette.White,
        selectedControlColor to OroBestPalette.PaleBlue,
        onSelectedControlColor to OroBestPalette.BrandBlue,
        destructiveColor to Theme[colors][destructiveColor],
        onDestructiveColor to Theme[colors][onDestructiveColor],
        borderColor to OroBestPalette.SoftBlue,
        fieldColor to OroBestPalette.White,
        onFieldColor to OroBestPalette.BrandBlue,
        scrimColor to OroBestPalette.BrandBlue.copy(alpha = 0.32f),
        ringColor to OroBestPalette.BrandBlue,
    )
    properties[textSelectionColors] = mapOf(selectionColorsToken to selectionColors)

    // Mantiene las formas, tamaños adaptativos y efectos de Composables UI.
    inherit(shapes, smallShape, mediumShape, largeShape, buttonShape, dialogShape, sheetShape, menuShape, fieldShape)
    inherit(shadows, raisedShadow, overlayShadow)
    inherit(alphas, disabledAlpha)
    inherit(indications, defaultIndicationToken, inverseIndication)
}

@Composable
private fun <T> ThemeBuilder.inherit(property: ThemeProperty<T>, vararg tokens: ThemeToken<T>) {
    properties[property] = tokens.associateWith { Theme[property][it] }
}

@Composable
fun OroBestTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalColorScheme provides ColorScheme.Light) {
        ComposablesTheme {
            BrandTheme(content)
        }
    }
}
