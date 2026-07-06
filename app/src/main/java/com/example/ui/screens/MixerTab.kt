package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.DAWViewModel
import com.example.ui.theme.*

@Composable
fun MixerTab(viewModel: DAWViewModel, modifier: Modifier = Modifier) {
    // Collect volume states
    val vSynth by viewModel.volSynth.collectAsState()
    val vKick by viewModel.volKick.collectAsState()
    val vSnare by viewModel.volSnare.collectAsState()
    val vHiHat by viewModel.volHiHat.collectAsState()
    val vClap by viewModel.volClap.collectAsState()
    val vMaster by viewModel.volMaster.collectAsState()

    // Collect panning states
    val pSynth by viewModel.panSynth.collectAsState()
    val pKick by viewModel.panKick.collectAsState()
    val pSnare by viewModel.panSnare.collectAsState()
    val pHiHat by viewModel.panHiHat.collectAsState()
    val pClap by viewModel.panClap.collectAsState()

    // Collect peak levels for visual meters
    val lPeak by viewModel.leftChannelPeak.collectAsState()
    val rPeak by viewModel.rightChannelPeak.collectAsState()

    // Collect FX parameters
    val delMix by viewModel.delayMix.collectAsState()
    val delFeed by viewModel.delayFeedback.collectAsState()
    val revMix by viewModel.reverbMix.collectAsState()
    val revFeed by viewModel.reverbFeedback.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ROW 1: Mixing Strips
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "CONSOLE MIXER",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Channel levels, stereo panning, and master summing bus",
                    fontSize = 11.sp,
                    color = GrayMuted
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Faders Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Synth strip
                    MixerStrip(
                        label = "SYNTH",
                        volume = vSynth,
                        pan = pSynth,
                        onVolumeChange = { viewModel.updateMixerParams(volS = it) },
                        onPanChange = { viewModel.updateMixerParams(panS = it) },
                        accentColor = NeonBlue,
                        modifier = Modifier.weight(1f),
                        testTagPrefix = "synth"
                    )

                    // 2. Kick strip
                    MixerStrip(
                        label = "KICK",
                        volume = vKick,
                        pan = pKick,
                        onVolumeChange = { viewModel.updateMixerParams(volK = it) },
                        onPanChange = { viewModel.updateMixerParams(panK = it) },
                        accentColor = NeonGreen,
                        modifier = Modifier.weight(1f),
                        testTagPrefix = "kick"
                    )

                    // 3. Snare strip
                    MixerStrip(
                        label = "SNARE",
                        volume = vSnare,
                        pan = pSnare,
                        onVolumeChange = { viewModel.updateMixerParams(volSn = it) },
                        onPanChange = { viewModel.updateMixerParams(panSn = it) },
                        accentColor = NeonBlue,
                        modifier = Modifier.weight(1f),
                        testTagPrefix = "snare"
                    )

                    // 4. Hi-hat strip
                    MixerStrip(
                        label = "HI-HAT",
                        volume = vHiHat,
                        pan = pHiHat,
                        onVolumeChange = { viewModel.updateMixerParams(volH = it) },
                        onPanChange = { viewModel.updateMixerParams(panH = it) },
                        accentColor = NeonOrange,
                        modifier = Modifier.weight(1f),
                        testTagPrefix = "hihat"
                    )

                    // 5. Clap strip
                    MixerStrip(
                        label = "CLAP",
                        volume = vClap,
                        pan = pClap,
                        onVolumeChange = { viewModel.updateMixerParams(volC = it) },
                        onPanChange = { viewModel.updateMixerParams(panC = it) },
                        accentColor = Color(0xFFE2E2E6),
                        modifier = Modifier.weight(1f),
                        testTagPrefix = "clap"
                    )

                    // Divider before Master
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(1.dp)
                            .background(Color.White.copy(alpha = 0.1f))
                    )

                    // 6. Master strip (with dual visual level meters)
                    Row(
                        modifier = Modifier
                            .weight(1.4f)
                            .fillMaxHeight()
                            .background(DarkCard, RoundedCornerShape(8.dp))
                            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Level meter block on the left
                        Row(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(28.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Left meter
                            VuMeter(peak = lPeak, modifier = Modifier.weight(1f))
                            // Right meter
                            VuMeter(peak = rPeak, modifier = Modifier.weight(1f))
                        }

                        // Master fader controls on right of master strip
                        Column(
                            modifier = Modifier.fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "MASTER",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonGreen
                            )

                            // Master fader (vertical slider)
                            Box(modifier = Modifier.weight(1f).padding(vertical = 12.dp)) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Slider(
                                        value = vMaster,
                                        onValueChange = { viewModel.updateMixerParams(volM = it) },
                                        valueRange = 0.0f..1.2f, // Master gain can boost slightly
                                        colors = SliderDefaults.colors(
                                            thumbColor = NeonGreen,
                                            activeTrackColor = NeonGreen,
                                            inactiveTrackColor = DarkBg
                                        ),
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .width(32.dp)
                                            .padding(vertical = 8.dp)
                                            .height(130.dp)
                                            .testTag("mixer_master_fader")
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = "${(vMaster * 100).toInt()}%",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonGreen
                                    )
                                }
                            }

                            Text(
                                text = "STEREO",
                                fontSize = 9.sp,
                                color = GrayMuted
                            )
                        }
                    }
                }
            }
        }

        // ROW 2: Master Digital Effects Sends Rack (Delay & Reverb)
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
                    text = "MASTER EFFECTS BUS (AUX SENDS)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Delay FX Rack
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(DarkCard, RoundedCornerShape(8.dp))
                            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "STEREO DELAY / ECHO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonBlue
                        )

                        // Delay Mix
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Wet Mix", fontSize = 10.sp, color = SilverText)
                                Text("${(delMix * 100).toInt()}%", fontSize = 10.sp, color = NeonBlue, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = delMix,
                                onValueChange = { viewModel.updateEffectsParams(delMix = it) },
                                valueRange = 0f..0.8f,
                                colors = SliderDefaults.colors(thumbColor = NeonBlue, activeTrackColor = NeonBlue, inactiveTrackColor = DarkBg)
                            )
                        }

                        // Delay Feedback
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Feedback (Echoes)", fontSize = 10.sp, color = SilverText)
                                Text("${(delFeed * 100).toInt()}%", fontSize = 10.sp, color = NeonBlue, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = delFeed,
                                onValueChange = { viewModel.updateEffectsParams(delFeed = it) },
                                valueRange = 0f..0.95f,
                                colors = SliderDefaults.colors(thumbColor = NeonBlue, activeTrackColor = NeonBlue, inactiveTrackColor = DarkBg)
                            )
                        }
                    }

                    // Reverb FX Rack
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(DarkCard, RoundedCornerShape(8.dp))
                            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "SPACE REVERB / ROOM",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonOrange
                        )

                        // Reverb Mix
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Space Mix", fontSize = 10.sp, color = SilverText)
                                Text("${(revMix * 100).toInt()}%", fontSize = 10.sp, color = NeonOrange, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = revMix,
                                onValueChange = { viewModel.updateEffectsParams(revMix = it) },
                                valueRange = 0f..0.8f,
                                colors = SliderDefaults.colors(thumbColor = NeonOrange, activeTrackColor = NeonOrange, inactiveTrackColor = DarkBg)
                            )
                        }

                        // Reverb Feedback (Room Size / Tail)
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Decay Tail", fontSize = 10.sp, color = SilverText)
                                Text("${(revFeed * 100).toInt()}%", fontSize = 10.sp, color = NeonOrange, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = revFeed,
                                onValueChange = { viewModel.updateEffectsParams(revFeed = it) },
                                valueRange = 0f..0.95f,
                                colors = SliderDefaults.colors(thumbColor = NeonOrange, activeTrackColor = NeonOrange, inactiveTrackColor = DarkBg)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MixerStrip(
    label: String,
    volume: Float,
    pan: Float,
    onVolumeChange: (Float) -> Unit,
    onPanChange: (Float) -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier,
    testTagPrefix: String = ""
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(DarkCard.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .border(BorderStroke(0.5.dp, Color.White.copy(alpha = 0.03f)), RoundedCornerShape(6.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Label
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor
        )

        // Stereo Pan Slider (L to R)
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val panText = if (pan == 0.0f) "C" else if (pan < 0f) "L${(pan * -10).toInt()}" else "R${(pan * 10).toInt()}"
            Text(text = "Pan: $panText", fontSize = 8.sp, color = GrayMuted)
            Slider(
                value = pan,
                onValueChange = onPanChange,
                valueRange = -1.0f..1.0f,
                colors = SliderDefaults.colors(
                    thumbColor = accentColor,
                    activeTrackColor = accentColor,
                    inactiveTrackColor = DarkBg
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .testTag("mixer_pan_${testTagPrefix}")
            )
        }

        // Volume Slider / Fader (Visualized as a nice compact vertical block)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f).padding(vertical = 8.dp)
        ) {
            Slider(
                value = volume,
                onValueChange = onVolumeChange,
                valueRange = 0.0f..1.0f,
                colors = SliderDefaults.colors(
                    thumbColor = accentColor,
                    activeTrackColor = accentColor,
                    inactiveTrackColor = DarkBg
                ),
                modifier = Modifier
                    .fillMaxHeight()
                    .width(32.dp)
                    .padding(vertical = 8.dp)
                    .height(110.dp)
                    .testTag("mixer_fader_${testTagPrefix}")
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "${(volume * 100).toInt()}%",
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color = SilverText
            )
        }
    }
}

// A beautiful glowing vertical stereo peak VU meter
@Composable
fun VuMeter(peak: Float, modifier: Modifier = Modifier) {
    // Smoothen the visual peak transitions
    val smoothPeak by animateFloatAsState(targetValue = peak, label = "VU Meter")

    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(DarkBg, RoundedCornerShape(2.dp))
            .border(BorderStroke(0.5.dp, Color.White.copy(alpha = 0.05f)), RoundedCornerShape(2.dp))
    ) {
        // VU Level Indicator Column
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(smoothPeak.coerceIn(0f, 1f))
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(1.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Red,        // clipping peaks
                            Color.Yellow,     // warnings
                            NeonGreen,        // healthy levels
                            NeonGreen
                        )
                    )
                )
        )
    }
}
