package com.example.ui

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioEngine
import com.example.data.DAWDatabase
import com.example.data.DAWRepository
import com.example.data.DrumStepEntity
import com.example.data.NoteEntity
import com.example.data.ProjectEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class DAWTab {
    SEQUENCER,    // Step sequencer for Drum Machine
    PIANO_ROLL,   // Piano roll for Synth
    SYNTH_VST,    // Envelope/filter controls for VST
    MIXER         // Volume/Panning faders and delay/reverb sends
}

class DAWViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: DAWRepository
    val audioEngine = AudioEngine()

    // UI Workspace state
    private val _selectedTab = MutableStateFlow(DAWTab.SEQUENCER)
    val selectedTab: StateFlow<DAWTab> = _selectedTab.asStateFlow()

    // Playback state synchronized with engine
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _bpm = MutableStateFlow(120)
    val bpm: StateFlow<Int> = _bpm.asStateFlow()

    val currentStep: StateFlow<Int> = audioEngine.currentStepFlow

    // Peak levels for VU meters
    private val _leftChannelPeak = MutableStateFlow(0f)
    val leftChannelPeak: StateFlow<Float> = _leftChannelPeak.asStateFlow()

    private val _rightChannelPeak = MutableStateFlow(0f)
    val rightChannelPeak: StateFlow<Float> = _rightChannelPeak.asStateFlow()

    // Database / Projects list
    val projectsList: StateFlow<List<ProjectEntity>>

    // Current Project state
    private val _currentProjectId = MutableStateFlow<Long?>(null)
    val currentProjectId: StateFlow<Long?> = _currentProjectId.asStateFlow()

    private val _projectName = MutableStateFlow("New Project")
    val projectName: StateFlow<String> = _projectName.asStateFlow()

    // Synth state
    val synthWaveform = MutableStateFlow(0)
    val synthAttack = MutableStateFlow(0.1f)
    val synthDecay = MutableStateFlow(0.2f)
    val synthSustain = MutableStateFlow(0.7f)
    val synthRelease = MutableStateFlow(0.3f)
    val synthCutoff = MutableStateFlow(0.8f)
    val synthResonance = MutableStateFlow(0.3f)

    // Mixer volumes
    val volSynth = MutableStateFlow(0.8f)
    val volKick = MutableStateFlow(0.9f)
    val volSnare = MutableStateFlow(0.8f)
    val volHiHat = MutableStateFlow(0.7f)
    val volClap = MutableStateFlow(0.8f)
    val volMaster = MutableStateFlow(0.9f)

    // Mixer Panning
    val panSynth = MutableStateFlow(0.0f)
    val panKick = MutableStateFlow(0.0f)
    val panSnare = MutableStateFlow(-0.1f)
    val panHiHat = MutableStateFlow(0.2f)
    val panClap = MutableStateFlow(-0.2f)

    // Effects state
    val delayMix = MutableStateFlow(0.25f)
    val delayFeedback = MutableStateFlow(0.4f)
    val reverbMix = MutableStateFlow(0.15f)
    val reverbFeedback = MutableStateFlow(0.5f)

    // Sequencer Local UI Matrices (to trigger recompositions in Jetpack Compose)
    // Synth note grid: 25 keys x 16 steps
    val synthGridState = mutableStateListOf<MutableList<Boolean>>()
    // Drum note grid: 4 drums x 16 steps
    val drumGridState = mutableStateListOf<MutableList<Boolean>>()

    init {
        val database = DAWDatabase.getDatabase(application)
        repository = DAWRepository(database.dawDao())
        
        projectsList = repository.allProjects.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Initialize grid matrices
        resetGridStates()
        
        // Start peak meter polling loop
        viewModelScope.launch {
            while (true) {
                _leftChannelPeak.value = audioEngine.currentLeftLevel
                _rightChannelPeak.value = audioEngine.currentRightLevel
                delay(40) // ~25 FPS is excellent for level meters
            }
        }
    }

    private fun resetGridStates() {
        synthGridState.clear()
        for (note in 0..24) {
            val list = mutableStateListOf<Boolean>()
            for (step in 0..15) {
                list.add(false)
            }
            synthGridState.add(list)
        }

        drumGridState.clear()
        for (drum in 0..3) {
            val list = mutableStateListOf<Boolean>()
            for (step in 0..15) {
                list.add(false)
            }
            drumGridState.add(list)
        }
    }

    fun selectTab(tab: DAWTab) {
        _selectedTab.value = tab
    }

    fun togglePlayback() {
        audioEngine.togglePlayback()
        _isPlaying.value = audioEngine.isEnginePlaying()
    }

    fun stopPlayback() {
        audioEngine.stop()
        _isPlaying.value = false
    }

    fun setBpm(newBpm: Int) {
        _bpm.value = newBpm
        audioEngine.bpm = newBpm
    }

    // Toggle grid note (Piano Roll)
    fun toggleSynthNote(pitch: Int, step: Int) {
        if (pitch in 0..24 && step in 0..15) {
            val currentVal = synthGridState[pitch][step]
            synthGridState[pitch][step] = !currentVal
            // Update audio engine instantly
            audioEngine.synthSequence[pitch][step] = !currentVal
        }
    }

    // Toggle drum sequencer step
    fun toggleDrumStep(drumId: Int, step: Int) {
        if (drumId in 0..3 && step in 0..15) {
            val currentVal = drumGridState[drumId][step]
            drumGridState[drumId][step] = !currentVal
            // Update audio engine instantly
            audioEngine.drumSequence[drumId][step] = !currentVal
        }
    }

    fun updateSynthParams(
        waveform: Int? = null,
        attack: Float? = null,
        decay: Float? = null,
        sustain: Float? = null,
        release: Float? = null,
        cutoff: Float? = null,
        resonance: Float? = null
    ) {
        waveform?.let {
            synthWaveform.value = it
            audioEngine.synthWaveform = it
        }
        attack?.let {
            synthAttack.value = it
            audioEngine.synthAttack = it
        }
        decay?.let {
            synthDecay.value = it
            audioEngine.synthDecay = it
        }
        sustain?.let {
            synthSustain.value = it
            audioEngine.synthSustain = it
        }
        release?.let {
            synthRelease.value = it
            audioEngine.synthRelease = it
        }
        cutoff?.let {
            synthCutoff.value = it
            audioEngine.synthCutoff = it
        }
        resonance?.let {
            synthResonance.value = it
            audioEngine.synthResonance = it
        }
    }

    fun updateMixerParams(
        volS: Float? = null,
        volK: Float? = null,
        volSn: Float? = null,
        volH: Float? = null,
        volC: Float? = null,
        volM: Float? = null,
        panS: Float? = null,
        panK: Float? = null,
        panSn: Float? = null,
        panH: Float? = null,
        panC: Float? = null
    ) {
        volS?.let { volSynth.value = it; audioEngine.volSynth = it }
        volK?.let { volKick.value = it; audioEngine.volKick = it }
        volSn?.let { volSnare.value = it; audioEngine.volSnare = it }
        volH?.let { volHiHat.value = it; audioEngine.volHiHat = it }
        volC?.let { volClap.value = it; audioEngine.volClap = it }
        volM?.let { volMaster.value = it; audioEngine.volMaster = it }
        
        panS?.let { panSynth.value = it; audioEngine.panSynth = it }
        panK?.let { panKick.value = it; audioEngine.panKick = it }
        panSn?.let { panSnare.value = it; audioEngine.panSnare = it }
        panH?.let { panHiHat.value = it; audioEngine.panHiHat = it }
        panC?.let { panClap.value = it; audioEngine.panClap = it }
    }

    fun updateEffectsParams(
        delMix: Float? = null,
        delFeed: Float? = null,
        revMix: Float? = null,
        revFeed: Float? = null
    ) {
        delMix?.let { delayMix.value = it; audioEngine.delayMix = it }
        delFeed?.let { delayFeedback.value = it; audioEngine.delayFeedback = it }
        revMix?.let { reverbMix.value = it; audioEngine.reverbMix = it }
        revFeed?.let { reverbFeedback.value = it; audioEngine.reverbFeedback = it }
    }

    // Trigger synth note in real time (vst live playing)
    fun playLiveNoteOn(noteIndex: Int) {
        audioEngine.liveNoteOn(noteIndex)
    }

    fun playLiveNoteOff(noteIndex: Int) {
        audioEngine.liveNoteOff(noteIndex)
    }

    // Load full project from database
    fun loadProject(project: ProjectEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            // Stop playing first
            withContext(Dispatchers.Main) {
                stopPlayback()
            }
            
            val notes = repository.getNotesForProject(project.id)
            val drumSteps = repository.getDrumStepsForProject(project.id)

            withContext(Dispatchers.Main) {
                _currentProjectId.value = project.id
                _projectName.value = project.name
                setBpm(project.bpm)

                // Sync Synth and FX values
                updateSynthParams(
                    waveform = project.waveformType,
                    attack = project.attack,
                    decay = project.decay,
                    sustain = project.sustain,
                    release = project.release,
                    cutoff = project.cutoff,
                    resonance = project.resonance
                )

                // Sync Mixer volumes
                updateMixerParams(
                    volS = project.volumeSynth,
                    volK = project.volumeKick,
                    volSn = project.volumeSnare,
                    volH = project.volumeHiHat,
                    volC = project.volumeClap,
                    volM = project.volumeMaster,
                    panS = project.panSynth,
                    panK = project.panKick,
                    panSn = project.panSnare,
                    panH = project.panHiHat,
                    panC = project.panClap
                )

                // Sync FX volumes
                updateEffectsParams(
                    delMix = project.delayMix,
                    delFeed = project.delayFeedback,
                    revMix = project.reverbMix,
                    revFeed = project.reverbFeedback
                )

                // Reset arrays
                resetGridStates()
                for (p in 0..24) {
                    for (s in 0..15) {
                        audioEngine.synthSequence[p][s] = false
                    }
                }
                for (d in 0..3) {
                    for (s in 0..15) {
                        audioEngine.drumSequence[d][s] = false
                    }
                }

                // Hydrate grid note states
                for (note in notes) {
                    if (note.pitch in 0..24 && note.step in 0..15) {
                        synthGridState[note.pitch][note.step] = true
                        audioEngine.synthSequence[note.pitch][note.step] = true
                    }
                }

                // Hydrate drum trigger states
                for (step in drumSteps) {
                    if (step.drumId in 0..3 && step.step in 0..15) {
                        drumGridState[step.drumId][step.step] = true
                        audioEngine.drumSequence[step.drumId][step.step] = true
                    }
                }
                
                audioEngine.resetSequencer()
            }
        }
    }

    // Save project to database offline
    fun saveProject(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val projectToSave = ProjectEntity(
                id = _currentProjectId.value ?: 0L,
                name = name,
                bpm = bpm.value,
                attack = synthAttack.value,
                decay = synthDecay.value,
                sustain = synthSustain.value,
                release = synthRelease.value,
                cutoff = synthCutoff.value,
                resonance = synthResonance.value,
                waveformType = synthWaveform.value,
                volumeSynth = volSynth.value,
                volumeKick = volKick.value,
                volumeSnare = volSnare.value,
                volumeHiHat = volHiHat.value,
                volumeClap = volClap.value,
                volumeMaster = volMaster.value,
                panSynth = panSynth.value,
                panKick = panKick.value,
                panSnare = panSnare.value,
                panHiHat = panHiHat.value,
                panClap = panClap.value,
                delayMix = delayMix.value,
                delayFeedback = delayFeedback.value,
                reverbMix = reverbMix.value,
                reverbFeedback = reverbFeedback.value,
                timestamp = System.currentTimeMillis()
            )

            // Gather active notes to insert
            val notesList = ArrayList<NoteEntity>()
            for (pitch in 0..24) {
                for (step in 0..15) {
                    if (synthGridState[pitch][step]) {
                        notesList.add(NoteEntity(projectId = projectToSave.id, pitch = pitch, step = step))
                    }
                }
            }

            // Gather active drum triggers to insert
            val drumStepsList = ArrayList<DrumStepEntity>()
            for (drumId in 0..3) {
                for (step in 0..15) {
                    if (drumGridState[drumId][step]) {
                        drumStepsList.add(DrumStepEntity(projectId = projectToSave.id, drumId = drumId, step = step))
                    }
                }
            }

            val savedId = repository.saveProject(projectToSave, notesList, drumStepsList)
            
            withContext(Dispatchers.Main) {
                _currentProjectId.value = savedId
                _projectName.value = name
            }
        }
    }

    // Delete project from database
    fun deleteProject(projectId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteProject(projectId)
            withContext(Dispatchers.Main) {
                if (_currentProjectId.value == projectId) {
                    createNewProject()
                }
            }
        }
    }

    fun createNewProject() {
        _currentProjectId.value = null
        _projectName.value = "New Project"
        setBpm(120)
        resetGridStates()
        
        // Reset audio engine arrays
        for (p in 0..24) {
            for (s in 0..15) {
                audioEngine.synthSequence[p][s] = false
            }
        }
        for (d in 0..3) {
            for (s in 0..15) {
                audioEngine.drumSequence[d][s] = false
            }
        }
        
        // Default synth parameters
        updateSynthParams(0, 0.1f, 0.2f, 0.7f, 0.3f, 0.8f, 0.3f)
        
        // Default mixer parameters
        updateMixerParams(
            volS = 0.8f, volK = 0.9f, volSn = 0.8f, volH = 0.7f, volC = 0.8f, volM = 0.9f,
            panS = 0.0f, panK = 0.0f, panSn = -0.1f, panH = 0.2f, panC = -0.2f
        )
        
        // Default FX parameters
        updateEffectsParams(0.25f, 0.4f, 0.15f, 0.5f)
        
        audioEngine.resetSequencer()
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.stop()
    }
}
