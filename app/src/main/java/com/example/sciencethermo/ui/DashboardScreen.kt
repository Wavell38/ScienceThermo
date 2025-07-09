package com.example.sciencethermo.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.example.sciencethermo.ui.theme.TestAppTheme
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Opacity

/**
 * Écran principal affichant température, humidité et JSON brut.
 * @param temperature          texte à afficher (ex « 27.4 °C »)
 * @param humidity             texte à afficher (ex « 46 % »)
 * @param rawJson              dernière trame reçue (formatée)
 * @param temperatureValueC    valeur numérique (°C) pour le dégradé de fond
 */
@Composable
fun DashboardScreen(
    temperature: String,
    humidity: String,
    rose: String,
    vapSat: String,
    vapReal: String,
    humAbs: String,
    rapport: String,
    enthalpie: String,
    rawJson: String,
    temperatureValueC: Double? = null,
) {
    /* -------- Couleur de fond dynamique (bleu → jaune → orange) -------- */
    val t = temperatureValueC ?: 20.0
    val colorCold = Color(0xFF2196F3)
    val colorMid  = Color(0xFFFFF176)
    val colorHot  = Color(0xFFFF7043)
    val ratio     = ((t / 40.0).coerceIn(0.0, 1.0)).toFloat()
    val bgColor   = lerpColor(lerpColor(colorCold, colorMid, ratio), colorHot, ratio)
    val bgBrush   = Brush.verticalGradient(
        listOf(
            lerpColor(bgColor, Color.White, 0.3f),
            lerpColor(bgColor, Color.White, 0.7f)   // même teinte, 40 % plus clair
        )
    )

    /* -------------------------- Contenu -------------------------- */
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Thermostat,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = Color(red = 250, green = 103, blue = 60)
                )
                Spacer(Modifier.width(25.dp))
                Text(temperature, fontSize = 48.sp, fontWeight = FontWeight.Bold)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Opacity,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = Color(red = 89, green = 60, blue = 250)
                )
                Spacer(Modifier.width(25.dp))
                Text(humidity, fontSize = 48.sp, fontWeight = FontWeight.Bold)
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(4.dp))
                Text("Point de rosée : $rose", fontSize = 12.sp, fontWeight = FontWeight.Normal)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(4.dp))
                Text("Pression vapeur saturante : $vapSat", fontSize = 12.sp, fontWeight = FontWeight.Normal)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(4.dp))
                Text("Pression vapeur réelle : $vapReal", fontSize = 12.sp, fontWeight = FontWeight.Normal)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(4.dp))
                Text("Humidité absolue : $humAbs", fontSize = 12.sp, fontWeight = FontWeight.Normal)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Rapport de mélange : $rapport", fontSize = 12.sp, fontWeight = FontWeight.Normal)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(4.dp))
                Text("Enthalpie de l’air : $enthalpie", fontSize = 12.sp, fontWeight = FontWeight.Normal)
            }
            Spacer(Modifier.height(50.dp))
        }
    }
}

/* -------------------------------------------------------------- */
private fun lerpColor(a: Color, b: Color, t: Float): Color =
    Color(
        red   = a.red   + (b.red   - a.red)   * t,
        green = a.green + (b.green - a.green) * t,
        blue  = a.blue  + (b.blue  - a.blue)  * t,
        alpha = 1f
    )

/* ------------------------------ Preview ------------------------------ */
@Preview(showBackground = true, name = "Dashboard – 18 °C")
@Composable fun PreviewDashboard18() {
    TestAppTheme {
        DashboardScreen(
            temperature = "18.0 °C",
            humidity = "42.0  %",
            rose = "18.5 °C",
            vapSat = "Test",
            vapReal = "test",
            humAbs = "test",
            rapport = "test",
            enthalpie = "test",
            rawJson = "{\"T\":18.0,\"RH\":42.0}",
            temperatureValueC = 18.0
        )
    }
}

@Preview(showBackground = true, name = "Dashboard – 32 °C", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable fun PreviewDashboard32() {
    TestAppTheme {
        DashboardScreen(
            temperature = "32.0 °C",
            humidity = "70.0  %",
            rose = "18.5 °C",
            vapSat = "Test",
            vapReal = "test",
            humAbs = "test",
            rapport = "test",
            enthalpie = "test",
            rawJson = "{\"T\":32.0,\"RH\":70.0}",
            temperatureValueC = 32.0
        )
    }
}
