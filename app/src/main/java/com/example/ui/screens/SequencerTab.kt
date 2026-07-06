package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.DAWViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SequencerTab(viewModel: DAWViewModel, modifier: Modifier = Modifier) {
    val currentStep by viewModel.currentStep.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    
    val drumNames = listOf("KICK 808", "SNARE analog", "HI-HAT metal", "CLAP retro")
    val drumColors = listOf(NeonGreen, NeonBlue, NeonOrange, Color(0xFFE2E2E6))

    Card(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "DRUM SEQUENCER",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "16-Step Analog Pattern Sequencer",
                        fontSize = 12.sp,
                        color = GrayMuted
                    )
                }
                
                Button(
                    onClick = {
                        for (d in 0..3) {
                            for (s in 0..15) {
                                if (viewModel.drumGridState[d][s]) {
                                    viewModel.toggleDrumStep(d, s)
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkCard),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear Sequence",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Clear Sequence", color = Color.White, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Step numbers / indicator headers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 110.dp), // align with step grid
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (step in 0..15) {
                    val isActivePlayhead = isPlaying && currentStep == step
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (step + 1).toString(),
                            fontSize = 10.sp,
                            fontWeight = if (isActivePlayhead) FontWeight.Bold else FontWeight.Normal,
                            color = if (isActivePlayhead) NeonGreen else GrayMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sequencer Rows
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                for (drumId in 0..3) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .background(DarkCard, RoundedCornerShape(8.dp))
                            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Instrument Control Left Block
                        Row(
                            modifier = Modifier
                                .width(100.dp)
                                .padding(end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = drumNames[drumId],
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.weight(1f)
                            )
                            
                            // Preview button
                            IconButton(
                                onClick = {
                                    // Trigger drum synth directly for live preview
                                    when (drumId) {
                                        0 -> viewModel.audioEngine.liveNoteOn(0) // preview kick
                                        1 -> viewModel.audioEngine.liveNoteOn(1) // preview snare
                                        2 -> viewModel.audioEngine.liveNoteOn(2) // preview hihat
                                        3 -> viewModel.audioEngine.liveNoteOn(3) // preview clap
                                    }
                                    // Instant preview synthesis trigger
                                    coroutineScope.launch {
                                        delay(50)
                                        when (drumId) {
                                            0 -> viewModel.audioEngine.liveNoteOff(0)
                                            1 -> viewModel.audioEngine.liveNoteOff(1)
                                            2 -> viewModel.audioEngine.liveNoteOff(2)
                                            3 -> viewModel.audioEngine.liveNoteOff(3)
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .size(24.dp)
                                    .testTag("preview_${drumId}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Preview Instrument",
                                    tint = drumColors[drumId],
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Steps grid
                        for (step in 0..15) {
                            val isTriggered = viewModel.drumGridState[drumId][step]
                            val isCurrentPlayhead = isPlaying && currentStep == step
                            
                            // Visual beats grouping (4-step groups: 1-4, 5-8, 9-12, 13-16)
                            val beatGroup = step / 4
                            val baseStepColor = if (beatGroup % 2 == 0) {
                                Color.White.copy(alpha = 0.05f)
                            } else {
                                Color.White.copy(alpha = 0.1f)
                            }

                            val borderAlpha = if (isCurrentPlayhead) 0.8f else 0.1f
                            val borderColor = if (isCurrentPlayhead) NeonGreen else Color.White.copy(alpha = borderAlpha)

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(vertical = 8.dp, horizontal = 2.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (isTriggered) drumColors[drumId] else baseStepColor
                                    )
                                    .border(
                                        BorderStroke(
                                            if (isCurrentPlayhead) 2.dp else 1.dp,
                                            borderColor
                                        ),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .clickable {
                                        viewModel.toggleDrumStep(drumId, step)
                                    }
                                    .testTag("pad_${drumId}_${step}")
                            ) {
                                // Subtle active glow dot inside pad
                                if (isCurrentPlayhead && !isTriggered) {
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(NeonGreen)
                                            .align(Alignment.Center)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(NeonGreen))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Kick", fontSize = 10.sp, color = GrayMuted)
                Spacer(modifier = Modifier.width(16.dp))
                Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(NeonBlue))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Snare", fontSize = 10.sp, color = GrayMuted)
                Spacer(modifier = Modifier.width(16.dp))
                Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(NeonOrange))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Hi-Hat", fontSize = 10.sp, color = GrayMuted)
                Spacer(modifier = Modifier.width(16.dp))
                Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFE2E2E6)))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Clap", fontSize = 10.sp, color = GrayMuted)
            }
        }
    }
}
