package com.example.ui.screens

import android.view.MotionEvent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.DAWViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PianoRollTab(viewModel: DAWViewModel, modifier: Modifier = Modifier) {
    val currentStep by viewModel.currentStep.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    
    // Note name array from C4 (index 0) to C6 (index 24)
    val noteNames = listOf(
        "C4", "C#4", "D4", "D#4", "E4", "F4", "F#4", "G4", "G#4", "A4", "A#4", "B4",
        "C5", "C#5", "D5", "D#5", "E5", "F5", "F#5", "G5", "G#5", "A5", "A#5", "B5",
        "C6"
    )
    
    // Indices of black keys (0-indexed from C4)
    val blackKeyIndices = setOf(1, 3, 6, 8, 10, 13, 15, 18, 20, 22)
    
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = 8) // center on mid-octave

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
                        text = "PIANO ROLL & VST SYNTH",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Touch keyboard keys to play live VST Synth, program melody grid on right",
                        fontSize = 12.sp,
                        color = GrayMuted
                    )
                }
                
                Button(
                    onClick = {
                        for (p in 0..24) {
                            for (s in 0..15) {
                                if (viewModel.synthGridState[p][s]) {
                                    viewModel.toggleSynthNote(p, s)
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkCard),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear Notes",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Clear Notes", color = Color.White, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Step Header indices
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 120.dp), // alignment with grid
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (step in 0..15) {
                    val isActivePlayhead = isPlaying && currentStep == step
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 1.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (step + 1).toString(),
                            fontSize = 10.sp,
                            fontWeight = if (isActivePlayhead) FontWeight.Bold else FontWeight.Normal,
                            color = if (isActivePlayhead) NeonBlue else GrayMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Scrollable Piano Roll area
            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // C6 down to C4 looks traditional (high pitches at top)
                    items(25) { reverseIndex ->
                        val noteIndex = 24 - reverseIndex
                        val isBlack = blackKeyIndices.contains(noteIndex)
                        val noteName = noteNames[noteIndex]
                        
                        // We track touch state of each key for live preview feedback
                        var isKeyPressed by remember { mutableStateOf(false) }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // --- PIANO KEY (LEFT) ---
                            Box(
                                modifier = Modifier
                                    .width(110.dp)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                                    .background(
                                        if (isKeyPressed) {
                                            NeonBlue.copy(alpha = 0.8f)
                                        } else if (isBlack) {
                                            Color(0xFF1E2433) // Black Key styling
                                        } else {
                                            Color(0xFFE6ECF5) // White Key styling
                                        }
                                    )
                                    .border(
                                        BorderStroke(
                                            1.dp,
                                            if (isBlack) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.15f)
                                        ),
                                        RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp)
                                    )
                                    .pointerInteropFilter { motionEvent ->
                                        when (motionEvent.action) {
                                            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                                                isKeyPressed = true
                                                viewModel.playLiveNoteOn(noteIndex)
                                            }
                                            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                                                isKeyPressed = false
                                                viewModel.playLiveNoteOff(noteIndex)
                                            }
                                        }
                                        true
                                    }
                                    .testTag("key_${noteIndex}"),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = noteName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isKeyPressed) DarkBg else if (isBlack) SilverText else DarkBg,
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                                
                                // Accent dot for black keys
                                if (isBlack && !isKeyPressed) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(RoundedCornerShape(2.5.dp))
                                            .background(NeonOrange)
                                            .align(Alignment.CenterEnd)
                                            .padding(end = 8.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // --- MELODY STEP GRID (RIGHT) ---
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(DarkCard.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                    .border(BorderStroke(0.5.dp, Color.White.copy(alpha = 0.03f)), RoundedCornerShape(4.dp))
                                    .padding(vertical = 2.dp)
                            ) {
                                for (step in 0..15) {
                                    val isTriggered = viewModel.synthGridState[noteIndex][step]
                                    val isCurrentPlayhead = isPlaying && currentStep == step
                                    
                                    val beatGroup = step / 4
                                    val cellBgColor = if (isTriggered) {
                                        NeonBlue
                                    } else {
                                        if (beatGroup % 2 == 0) {
                                            Color.White.copy(alpha = 0.02f)
                                        } else {
                                            Color.White.copy(alpha = 0.05f)
                                        }
                                    }

                                    val borderAlpha = if (isCurrentPlayhead) 0.8f else 0.05f
                                    val borderColor = if (isCurrentPlayhead) NeonBlue else Color.White.copy(alpha = borderAlpha)

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .padding(horizontal = 1.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(cellBgColor)
                                            .border(
                                                BorderStroke(
                                                    if (isCurrentPlayhead) 1.5.dp else 0.5.dp,
                                                    borderColor
                                                ),
                                                RoundedCornerShape(3.dp)
                                            )
                                            .clickable {
                                                viewModel.toggleSynthNote(noteIndex, step)
                                            }
                                            .testTag("synth_cell_${noteIndex}_${step}")
                                    ) {
                                        // Highlight current active step grid line
                                        if (isCurrentPlayhead && !isTriggered) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(2.dp)
                                                    .background(NeonBlue.copy(alpha = 0.5f))
                                                    .align(Alignment.Center)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
