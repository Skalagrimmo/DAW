package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.DAWViewModel
import com.example.ui.theme.*

@Composable
fun VSTSettingsTab(viewModel: DAWViewModel, modifier: Modifier = Modifier) {
    val waveformType by viewModel.synthWaveform.collectAsState()
    val attack by viewModel.synthAttack.collectAsState()
    val decay by viewModel.synthDecay.collectAsState()
    val sustain by viewModel.synthSustain.collectAsState()
    val release by viewModel.synthRelease.collectAsState()
    
    val cutoff by viewModel.synthCutoff.collectAsState()
    val resonance by viewModel.synthResonance.collectAsState()

    val waveforms = listOf("SINE", "TRIANGLE", "SAWTOOTH", "SQUARE")

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ROW 1: Waveform & Envelope Graph
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Waveform Selector Panel (Left)
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(200.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "VIRTUAL OSCILLATOR (VST)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonOrange
                        )
                        Text(
                            text = "Waveform Sound Source",
                            fontSize = 11.sp,
                            color = GrayMuted
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        waveforms.forEachIndexed { index, name ->
                            val isSelected = waveformType == index
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(28.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isSelected) NeonOrange else DarkCard)
                                    .border(
                                        BorderStroke(1.dp, if (isSelected) Color.Transparent else Color.White.copy(alpha = 0.05f)),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .clickable {
                                        viewModel.updateSynthParams(waveform = index)
                                    }
                                    .testTag("wave_select_${index}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = name,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) DarkBg else SilverText
                                )
                            }
                        }
                    }
                }
            }

            // Envelope ADSR Visualizer Graph (Right)
            Card(
                modifier = Modifier
                    .weight(1.5f)
                    .height(200.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "ACTIVE ENVELOPE (ADSR)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonBlue
                    )
                    Text(
                        text = "Dynamic volume trajectory visualizer",
                        fontSize = 11.sp,
                        color = GrayMuted
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Draw the ADSR envelope path interactively!
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(DarkBg, RoundedCornerShape(8.dp))
                            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.03f)), RoundedCornerShape(8.dp))
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            
                            // Map attack, decay, sustain, release to widths
                            val totalSegments = attack + decay + 0.5f + release
                            val scale = w / totalSegments

                            val x0 = 0f
                            val y0 = h

                            val x1 = (attack * scale).coerceAtMost(w * 0.3f)
                            val y1 = 0f

                            val x2 = x1 + (decay * scale).coerceAtMost(w * 0.3f)
                            val y2 = h * (1f - sustain)

                            val x3 = (x2 + (0.5f * scale)).coerceAtMost(w * 0.8f)
                            val y3 = y2

                            val x4 = (x3 + (release * scale)).coerceAtMost(w)
                            val y4 = h

                            val path = Path().apply {
                                moveTo(x0, y0)
                                lineTo(x1, y1)
                                lineTo(x2, y2)
                                lineTo(x3, y3)
                                lineTo(x4, y4)
                            }

                            // Draw shadow fill under envelope curve
                            val fillPath = Path().apply {
                                addPath(path)
                                lineTo(x4, h)
                                lineTo(x0, h)
                                close()
                            }
                            
                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(NeonBlue.copy(alpha = 0.3f), Color.Transparent)
                                )
                            )

                            // Draw the stroke envelope line
                            drawPath(
                                path = path,
                                color = NeonBlue,
                                style = Stroke(width = 3.dp.toPx())
                            )

                            // Draw node circles
                            drawCircle(color = Color.White, radius = 4.dp.toPx(), center = Offset(x1, y1))
                            drawCircle(color = Color.White, radius = 4.dp.toPx(), center = Offset(x2, y2))
                            drawCircle(color = Color.White, radius = 4.dp.toPx(), center = Offset(x3, y3))
                        }
                    }
                }
            }
        }

        // ROW 2: Sliders
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "VST PARAMETERS",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                // Envelopes block
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Volume Envelope (ADSR)", fontSize = 11.sp, color = NeonBlue, fontWeight = FontWeight.Bold)

                    // Attack Slider
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Attack Time (Speed of build-up)", fontSize = 11.sp, color = SilverText)
                            Text("${(attack * 1000).toInt()} ms", fontSize = 11.sp, color = NeonBlue, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = attack,
                            onValueChange = { viewModel.updateSynthParams(attack = it) },
                            valueRange = 0.005f..1.5f,
                            colors = SliderDefaults.colors(thumbColor = NeonBlue, activeTrackColor = NeonBlue, inactiveTrackColor = DarkCard)
                        )
                    }

                    // Decay Slider
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Decay Time (Time to settle to sustain)", fontSize = 11.sp, color = SilverText)
                            Text("${(decay * 1000).toInt()} ms", fontSize = 11.sp, color = NeonBlue, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = decay,
                            onValueChange = { viewModel.updateSynthParams(decay = it) },
                            valueRange = 0.005f..1.5f,
                            colors = SliderDefaults.colors(thumbColor = NeonBlue, activeTrackColor = NeonBlue, inactiveTrackColor = DarkCard)
                        )
                    }

                    // Sustain Slider
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Sustain Level (Holding volume level)", fontSize = 11.sp, color = SilverText)
                            Text("${(sustain * 100).toInt()}%", fontSize = 11.sp, color = NeonBlue, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = sustain,
                            onValueChange = { viewModel.updateSynthParams(sustain = it) },
                            valueRange = 0.0f..1.0f,
                            colors = SliderDefaults.colors(thumbColor = NeonBlue, activeTrackColor = NeonBlue, inactiveTrackColor = DarkCard)
                        )
                    }

                    // Release Slider
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Release Time (Fading duration on release)", fontSize = 11.sp, color = SilverText)
                            Text("${(release * 1000).toInt()} ms", fontSize = 11.sp, color = NeonBlue, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = release,
                            onValueChange = { viewModel.updateSynthParams(release = it) },
                            valueRange = 0.005f..2.0f,
                            colors = SliderDefaults.colors(thumbColor = NeonBlue, activeTrackColor = NeonBlue, inactiveTrackColor = DarkCard)
                        )
                    }
                }

                Divider(color = Color.White.copy(alpha = 0.05f))

                // Cutoff and Resonance filter block
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Low Pass Resonant Filter", fontSize = 11.sp, color = NeonOrange, fontWeight = FontWeight.Bold)

                    // Cutoff frequency
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Cutoff Frequency (High-end sweep)", fontSize = 11.sp, color = SilverText)
                            Text("${(80f + cutoff * cutoff * 8000f).toInt()} Hz", fontSize = 11.sp, color = NeonOrange, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = cutoff,
                            onValueChange = { viewModel.updateSynthParams(cutoff = it) },
                            valueRange = 0.0f..1.0f,
                            colors = SliderDefaults.colors(thumbColor = NeonOrange, activeTrackColor = NeonOrange, inactiveTrackColor = DarkCard)
                        )
                    }

                    // Resonance peak
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Resonance Peak (Analog filter warmth)", fontSize = 11.sp, color = SilverText)
                            Text("${(resonance * 100).toInt()}%", fontSize = 11.sp, color = NeonOrange, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = resonance,
                            onValueChange = { viewModel.updateSynthParams(resonance = it) },
                            valueRange = 0.0f..1.0f,
                            colors = SliderDefaults.colors(thumbColor = NeonOrange, activeTrackColor = NeonOrange, inactiveTrackColor = DarkCard)
                        )
                    }
                }
            }
        }
    }
}
