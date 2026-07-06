package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.DAWTab
import com.example.ui.DAWViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainStudioScreen(viewModel: DAWViewModel) {
    val isPlaying by viewModel.isPlaying.collectAsState()
    val bpm by viewModel.bpm.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val currentStep by viewModel.currentStep.collectAsState()
    val projectName by viewModel.projectName.collectAsState()
    val projectsList by viewModel.projectsList.collectAsState()
    
    val lPeak by viewModel.leftChannelPeak.collectAsState()
    val rPeak by viewModel.rightChannelPeak.collectAsState()

    var showSaveDialog by remember { mutableStateOf(false) }
    var saveNameInput by remember { mutableStateOf("") }
    var showProjectDropdown by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        containerColor = DarkBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ================== MASTER TRANSPORT & CONTROL PANEL ==================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left Block: Project Metadata
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.createNewProject() },
                        modifier = Modifier.testTag("action_new_project")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "New Project", tint = Color.White)
                    }

                    Box {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(DarkCard)
                                .clickable { showProjectDropdown = true }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = projectName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonGreen
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Select Project",
                                tint = NeonGreen,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Project Selector Dropdown
                        DropdownMenu(
                            expanded = showProjectDropdown,
                            onDismissRequest = { showProjectDropdown = false },
                            modifier = Modifier.background(DarkCard)
                        ) {
                            if (projectsList.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No Saved Projects", color = GrayMuted, fontSize = 12.sp) },
                                    onClick = { showProjectDropdown = false }
                                )
                            } else {
                                projectsList.forEach { project ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(project.name, color = Color.White, fontSize = 13.sp)
                                                Spacer(modifier = Modifier.width(16.dp))
                                                Text("${project.bpm} BPM", color = GrayMuted, fontSize = 11.sp)
                                            }
                                        },
                                        onClick = {
                                            viewModel.loadProject(project)
                                            showProjectDropdown = false
                                        },
                                        trailingIcon = {
                                            IconButton(
                                                onClick = {
                                                    viewModel.deleteProject(project.id)
                                                    showProjectDropdown = false
                                                },
                                                modifier = Modifier.size(20.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete",
                                                    tint = Color.Red.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    IconButton(
                        onClick = {
                            saveNameInput = projectName
                            showSaveDialog = true
                        },
                        modifier = Modifier.testTag("action_save_project")
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = "Save Project", tint = Color.White)
                    }
                }

                // Middle Block: Transport (Play/Stop/BPM)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Play/Stop Button
                    Button(
                        onClick = { viewModel.togglePlayback() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPlaying) NeonGreen else DarkCard
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("action_play_stop")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Stop" else "Play",
                            tint = if (isPlaying) DarkBg else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPlaying) "STOP" else "PLAY",
                            color = if (isPlaying) DarkBg else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Master BPM control
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(DarkCard)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "BPM:",
                            fontSize = 10.sp,
                            color = GrayMuted,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format("%03d", bpm),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        // Small BPM Sliders
                        Slider(
                            value = bpm.toFloat(),
                            onValueChange = { viewModel.setBpm(it.toInt()) },
                            valueRange = 40f..240f,
                            colors = SliderDefaults.colors(
                                thumbColor = NeonGreen,
                                activeTrackColor = NeonGreen,
                                inactiveTrackColor = DarkBg
                            ),
                            modifier = Modifier
                                .width(90.dp)
                                .height(20.dp)
                                .testTag("bpm_slider")
                        )
                    }
                }

                // Right Block: LED step position + Mini VU summations
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Step readout
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(DarkCard)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("STEP ", fontSize = 9.sp, color = GrayMuted, fontWeight = FontWeight.Bold)
                        Text(
                            text = String.format("%02d", if (isPlaying) currentStep + 1 else 1),
                            fontSize = 12.sp,
                            color = NeonOrange,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Small mini stereo LED strip for top feedback
                    Column(
                        modifier = Modifier
                            .height(28.dp)
                            .width(18.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(1.dp))
                                .background(if (isPlaying) NeonGreen.copy(alpha = maxOf(0.1f, lPeak)) else DarkCard)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(1.dp))
                                .background(if (isPlaying) NeonGreen.copy(alpha = maxOf(0.1f, rPeak)) else DarkCard)
                        )
                    }
                }
            }

            // ================== WORKBENCH TAB CONTROLLER (SEGMENTED MENU) ==================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkCard)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TabButton(
                    label = "DRUMS",
                    icon = Icons.Default.GridOn,
                    isSelected = selectedTab == DAWTab.SEQUENCER,
                    onClick = { viewModel.selectTab(DAWTab.SEQUENCER) },
                    accentColor = NeonOrange,
                    modifier = Modifier.weight(1f),
                    testTag = "tab_sequencer"
                )
                TabButton(
                    label = "PIANO ROLL",
                    icon = Icons.Default.MusicNote,
                    isSelected = selectedTab == DAWTab.PIANO_ROLL,
                    onClick = { viewModel.selectTab(DAWTab.PIANO_ROLL) },
                    accentColor = NeonBlue,
                    modifier = Modifier.weight(1f),
                    testTag = "tab_piano_roll"
                )
                TabButton(
                    label = "VST SYNTH",
                    icon = Icons.Default.Tune,
                    isSelected = selectedTab == DAWTab.SYNTH_VST,
                    onClick = { viewModel.selectTab(DAWTab.SYNTH_VST) },
                    accentColor = NeonBlue,
                    modifier = Modifier.weight(1f),
                    testTag = "tab_vst"
                )
                TabButton(
                    label = "MIXER",
                    icon = Icons.Default.VolumeUp,
                    isSelected = selectedTab == DAWTab.MIXER,
                    onClick = { viewModel.selectTab(DAWTab.MIXER) },
                    accentColor = NeonGreen,
                    modifier = Modifier.weight(1f),
                    testTag = "tab_mixer"
                )
            }

            // ================== ACTIVE WORKBENCH WORKSPACE PANE ==================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (selectedTab) {
                    DAWTab.SEQUENCER -> SequencerTab(viewModel = viewModel)
                    DAWTab.PIANO_ROLL -> PianoRollTab(viewModel = viewModel)
                    DAWTab.SYNTH_VST -> VSTSettingsTab(viewModel = viewModel)
                    DAWTab.MIXER -> MixerTab(viewModel = viewModel)
                }
            }
        }

        // ================== OFFLINE SAVE PROJECT DIALOG MODAL ==================
        if (showSaveDialog) {
            AlertDialog(
                onDismissRequest = { showSaveDialog = false },
                title = { Text("Save DAW Project", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Enter a unique name to save your synthesiser melodies and drum beats locally.", color = SilverText, fontSize = 12.sp)
                        OutlinedTextField(
                            value = saveNameInput,
                            onValueChange = { saveNameInput = it },
                            label = { Text("Project Name") },
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = DarkCard,
                                unfocusedContainerColor = DarkCard,
                                focusedLabelColor = NeonGreen,
                                focusedIndicatorColor = NeonGreen
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("save_name_field")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (saveNameInput.isNotBlank()) {
                                viewModel.saveProject(saveNameInput)
                                showSaveDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                    ) {
                        Text("Save Project", color = DarkBg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSaveDialog = false }) {
                        Text("Cancel", color = Color.White, fontSize = 12.sp)
                    }
                },
                containerColor = DarkSurface,
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
fun TabButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) DarkSurface else Color.Transparent)
            .border(
                BorderStroke(
                    1.dp,
                    if (isSelected) accentColor.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.03f)
                ),
                RoundedCornerShape(6.dp)
            )
            .clickable { onClick() }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) accentColor else GrayMuted,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else GrayMuted
            )
        }
        
        // Bottom active selection accent line
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(accentColor)
                    .align(Alignment.BottomCenter)
            )
        }
    }
}
