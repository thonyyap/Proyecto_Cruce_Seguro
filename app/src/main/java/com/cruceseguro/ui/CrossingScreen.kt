package com.cruceseguro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cruceseguro.controller.MainUiState
import com.cruceseguro.controller.SimulationMotion
import com.cruceseguro.decision.ProximityLevel
import com.cruceseguro.model.AppMode
import com.cruceseguro.model.Availability
import com.cruceseguro.ui.theme.AccessibleBlack
import com.cruceseguro.ui.theme.AccessibleWhite
import com.cruceseguro.ui.theme.ErrorRed
import com.cruceseguro.ui.theme.SafetyYellow
import com.cruceseguro.ui.theme.SuccessGreen
import com.cruceseguro.ui.theme.WarningOrange
import java.util.Locale

@Composable
fun CrossingScreen(
    state: MainUiState,
    onModeChange: (AppMode) -> Unit,
    onSimulatedDistanceChange: (Double) -> Unit,
    onAutoApproach: () -> Unit,
    onMoveAway: () -> Unit,
    onStop: () -> Unit,
    onCreateCrossing: () -> Boolean,
    onRequestLocation: () -> Unit,
    onRequestCamera: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Cruce Seguro Accesible", style = MaterialTheme.typography.titleLarge)
            Text(
                "Prototipo de apoyo. Verifique siempre el tránsito antes de cruzar.",
                color = SafetyYellow,
            )
            ModeSelector(state.mode, onModeChange)
            DistanceCard(state)
            ConditionsCard(state)
            if (state.mode == AppMode.SIMULATED) {
                SimulationControls(state, onSimulatedDistanceChange, onAutoApproach, onMoveAway, onStop)
            } else {
                RealModeControls(state, onCreateCrossing, onRequestLocation, onRequestCamera)
            }
            DebugSection(state)
            Spacer(Modifier.height(16.dp))
        }

        if (state.criticalAlertVisible) CriticalOverlay()
    }
}

@Composable
private fun ModeSelector(
    mode: AppMode,
    onModeChange: (AppMode) -> Unit,
) {
    AccessibleCard(title = "Modo de funcionamiento") {
        AppMode.entries.forEach { option ->
            val label = if (option == AppMode.SIMULATED) "Simulado" else "Real con GPS"
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .requiredHeightIn(min = 56.dp)
                        .semantics { role = Role.RadioButton },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = mode == option, onClick = { onModeChange(option) })
                TextButton(onClick = { onModeChange(option) }) { Text(label) }
            }
        }
    }
}

@Composable
private fun DistanceCard(state: MainUiState) {
    val distanceText = state.distanceMeters?.let { String.format(Locale.getDefault(), "%.1f m", it) } ?: "Sin cruce"
    val levelText =
        when (state.proximityLevel) {
            ProximityLevel.CRITICAL -> "Crítico"
            ProximityLevel.NEAR -> "Cercano"
            ProximityLevel.PREVENTIVE -> "Preventivo"
            ProximityLevel.CLEAR -> "Fuera de zona"
            ProximityLevel.UNAVAILABLE -> "No disponible"
        }
    AccessibleCard(title = "Distancia al cruce") {
        Text(
            distanceText,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Distancia al cruce: $distanceText" },
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            color = levelColor(state.proximityLevel),
        )
        Text("Nivel: $levelText", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    }
}

@Composable
private fun ConditionsCard(state: MainUiState) {
    AccessibleCard(title = "Condiciones de decisión") {
        ConditionRow("Caminando", state.walking)
        ConditionRow("Cerca del cruce", state.nearCrossing)
        ConditionRow("Acercándose", state.approaching)
    }
}

@Composable
private fun ConditionRow(
    label: String,
    active: Boolean,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .semantics { contentDescription = "$label: ${if (active) "sí" else "no"}" },
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label)
        Text(if (active) "SÍ" else "NO", color = if (active) Color(0xFF77DD77) else Color(0xFFFF8A80), fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SimulationControls(
    state: MainUiState,
    onDistanceChange: (Double) -> Unit,
    onAutoApproach: () -> Unit,
    onMoveAway: () -> Unit,
    onStop: () -> Unit,
) {
    AccessibleCard(title = "Simulación de distancia") {
        Slider(
            value = state.simulatedDistanceMeters.toFloat(),
            onValueChange = { onDistanceChange(it.toDouble()) },
            valueRange = 0f..50f,
            modifier =
                Modifier.semantics {
                    contentDescription = "Distancia simulada, ${state.simulatedDistanceMeters.toInt()} metros"
                },
        )
        LargeButton("Auto-acercar", state.simulationMotion == SimulationMotion.APPROACHING, onAutoApproach)
        LargeButton("Alejar", state.simulationMotion == SimulationMotion.MOVING_AWAY, onMoveAway)
        LargeButton("Parar", state.simulationMotion == SimulationMotion.STOPPED, onStop)
        Text("El acelerómetro y el giroscopio siguen siendo reales.")
    }
}

@Composable
private fun RealModeControls(
    state: MainUiState,
    onCreateCrossing: () -> Boolean,
    onRequestLocation: () -> Unit,
    onRequestCamera: () -> Unit,
) {
    AccessibleCard(title = "Modo real") {
        Text("GPS: ${availabilityText(state.gpsStatus)}")
        Text("Precisión: ${state.gpsAccuracyMeters?.let { "${it.toInt()} m" } ?: "sin dato"}")
        LargeButton("Crear cruce aquí", state.crossingCreated) { onCreateCrossing() }
        if (state.gpsStatus == Availability.PERMISSION_REQUIRED) {
            LargeButton("Conceder permiso de ubicación", false, onRequestLocation)
        }
        LargeButton("Habilitar flash opcional", false, onRequestCamera)
        Text(if (state.crossingCreated) "Cruce temporal registrado." else "Todavía no hay un cruce registrado.")
    }
}

@Composable
private fun DebugSection(state: MainUiState) {
    var expanded by remember { mutableStateOf(false) }
    AccessibleCard(title = "Depuración") {
        LargeButton(if (expanded) "Ocultar datos de debug" else "Mostrar datos de debug", expanded) { expanded = !expanded }
        if (expanded) {
            Icon(Icons.Default.BugReport, contentDescription = null, tint = SafetyYellow)
            Text("Acelerómetro: ${availabilityText(state.accelerometerStatus)}")
            Text("Variación: ${format(state.accelerationVariation)} m/s²")
            Text("Giroscopio: ${availabilityText(state.gyroscopeStatus)}")
            Text("Giro X/Y/Z: ${format(state.gyroscopeX)} / ${format(state.gyroscopeY)} / ${format(state.gyroscopeZ)} rad/s")
            Text("Distancia: ${state.distanceMeters?.let(::format) ?: "sin dato"} m")
            Text("Precisión GPS: ${state.gpsAccuracyMeters?.let { format(it.toDouble()) } ?: "sin dato"} m")
        }
    }
}

@Composable
private fun AccessibleCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = SafetyYellow)
            HorizontalDivider(color = Color.DarkGray)
            content()
        }
    }
}

@Composable
private fun LargeButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier =
            Modifier
                .fillMaxWidth()
                .requiredHeightIn(min = 56.dp),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = if (selected) SuccessGreen else SafetyYellow,
                contentColor = if (selected) AccessibleWhite else AccessibleBlack,
            ),
    ) {
        Text(label, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CriticalOverlay() {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(AccessibleBlack)
                .semantics {
                    liveRegion = LiveRegionMode.Assertive
                    contentDescription = "Alerta crítica. Cruce peatonal inmediato. Deténgase y verifique antes de cruzar."
                },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = SafetyYellow, modifier = Modifier.height(96.dp))
            Text("¡ATENCIÓN!", color = SafetyYellow, fontSize = 46.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                "Cruce peatonal inmediato.\nDeténgase y verifique antes de cruzar.",
                color = AccessibleWhite,
                fontSize = 28.sp,
                lineHeight = 38.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private fun availabilityText(status: Availability): String =
    when (status) {
        Availability.AVAILABLE -> "disponible"
        Availability.UNAVAILABLE -> "no disponible"
        Availability.PERMISSION_REQUIRED -> "requiere permiso"
        Availability.DISABLED -> "desactivado"
    }

private fun levelColor(level: ProximityLevel): Color =
    when (level) {
        ProximityLevel.CRITICAL -> ErrorRed
        ProximityLevel.NEAR -> WarningOrange
        ProximityLevel.PREVENTIVE -> SafetyYellow
        ProximityLevel.CLEAR -> Color(0xFF77DD77)
        ProximityLevel.UNAVAILABLE -> AccessibleWhite
    }

private fun format(value: Double): String = String.format(Locale.getDefault(), "%.2f", value)
