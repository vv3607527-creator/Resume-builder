package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeminiRepository
import com.example.data.model.Education
import com.example.data.model.Resume
import com.example.data.model.User
import com.example.data.model.WorkExperience
import com.example.data.repository.ResumeRepository
import com.example.data.repository.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class Screen {
    object Landing : Screen()
    object SignIn : Screen()
    object SignUp : Screen()
    object Home : Screen()
    data class Edit(val resumeId: Long) : Screen()
    data class Preview(val resumeId: Long) : Screen()
}

sealed class AiState {
    object Idle : AiState()
    object Loading : AiState()
    data class Success(val result: String) : AiState()
    data class Error(val message: String) : AiState()
}

class ResumeViewModel(
    private val resumeRepository: ResumeRepository,
    private val userRepository: UserRepository,
    private val geminiRepository: GeminiRepository
) : ViewModel() {

    // --- Navigation State ---
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Landing)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // --- User Session State ---
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _authLoading = MutableStateFlow<Boolean>(false)
    val authLoading: StateFlow<Boolean> = _authLoading.asStateFlow()

    // --- Saved Resumes Flow ---
    private val _savedResumes = MutableStateFlow<List<Resume>>(emptyList())
    val savedResumes: StateFlow<List<Resume>> = _savedResumes.asStateFlow()

    // --- Active Selected Resume ---
    private val _activeResume = MutableStateFlow<Resume?>(null)
    val activeResume: StateFlow<Resume?> = _activeResume.asStateFlow()

    // --- AI Generator State ---
    private val _aiState = MutableStateFlow<AiState>(AiState.Idle)
    val aiState: StateFlow<AiState> = _aiState.asStateFlow()

    private var resumesJob: Job? = null

    init {
        viewModelScope.launch {
            val activeUser = userRepository.getActiveUser()
            if (activeUser != null) {
                _currentUser.value = activeUser
                observeUserResumes(activeUser.id)
                _currentScreen.value = Screen.Home
            } else {
                _currentScreen.value = Screen.Landing
                // Guest profile resumes or default previews
                observeUserResumes(0L)
            }
        }
    }

    private fun observeUserResumes(userId: Long) {
        resumesJob?.cancel()
        resumesJob = viewModelScope.launch {
            if (userId == 0L) {
                resumeRepository.getResumesForUser(0L).collect { list ->
                    if (list.isEmpty()) {
                        populateDefaultResumes(0L)
                    } else {
                        _savedResumes.value = list
                    }
                }
            } else {
                resumeRepository.getResumesForUser(userId).collect { list ->
                    if (list.isEmpty()) {
                        populateDefaultResumes(userId)
                    } else {
                        _savedResumes.value = list
                    }
                }
            }
        }
    }

    private suspend fun populateDefaultResumes(userId: Long) {
        val defaultName = if (userId == 0L) "Alexander Wright" else "My Professional Resume"
        val defaultResumes = listOf(
            Resume(
                userId = userId,
                title = "Senior Software Engineer Resume",
                fullName = defaultName,
                email = "alexander.wright@techmail.com",
                phone = "+1 (555) 234-5678",
                website = "github.com/alexwright",
                location = "San Francisco, CA",
                summary = "Innovative and solutions-driven Senior Software Engineer with over 6 years of experience designing, building, and maintaining robust web and mobile applications. Expert in Kotlin, Jetpack Compose, and scalable microservices architectures.",
                workExperiences = listOf(
                    WorkExperience(
                        company = "NovaTech Solutions",
                        role = "Senior Android Developer",
                        duration = "2022 - Present",
                        description = "• Spearheaded redesign of flagship mobile app using Jetpack Compose, reducing UI render latency by 35%.\n• Pioneered clean architecture patterns across 4 multi-module Android projects, boosting test coverage to 85%.\n• Managed a team of 4 junior developers and established agile best practices."
                    ),
                    WorkExperience(
                        company = "Apex Systems",
                        role = "Software Engineer II",
                        duration = "2020 - 2022",
                        description = "• Developed core REST API endpoints and real-time messaging pipelines using Kotlin and Ktor.\n• Successfully migrated legacy database schemas to optimized SQLite/Room instances, speeding up local query execution times by 50%."
                    )
                ),
                educations = listOf(
                    Education(
                        school = "University of California, Berkeley",
                        degree = "B.S. in Computer Science",
                        duration = "2016 - 2020",
                        description = "Graduated with Honors. Specialized in Software Engineering & AI Systems."
                    )
                ),
                skills = listOf("Kotlin", "Jetpack Compose", "Android SDK", "Room Database", "Retrofit", "M3 Design", "Git", "CI/CD"),
                certifications = listOf("Google Certified Associate Android Developer", "AWS Certified Cloud Practitioner"),
                templateId = "modern"
            )
        )
        for (resume in defaultResumes) {
            resumeRepository.insertResume(resume)
        }
    }

    // --- Authentication Actions ---
    fun register(email: String, fullName: String, password: String) {
        if (email.isBlank() || fullName.isBlank() || password.isBlank()) {
            _authError.value = "All fields are required"
            return
        }
        _authLoading.value = true
        _authError.value = null
        viewModelScope.launch {
            val existing = userRepository.getUserByEmail(email)
            if (existing != null) {
                _authError.value = "Email is already registered"
                _authLoading.value = false
                return@launch
            }
            val newUser = User(
                email = email,
                fullName = fullName,
                passwordHash = password
            )
            val newId = userRepository.registerUser(newUser)
            val userWithId = newUser.copy(id = newId)
            userRepository.loginUser(userWithId)
            _currentUser.value = userWithId
            observeUserResumes(newId)
            _authLoading.value = false
            _currentScreen.value = Screen.Home
        }
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authError.value = "All fields are required"
            return
        }
        _authLoading.value = true
        _authError.value = null
        viewModelScope.launch {
            val user = userRepository.getUserByEmail(email)
            if (user == null || user.passwordHash != password) {
                _authError.value = "Invalid email or password"
                _authLoading.value = false
                return@launch
            }
            userRepository.loginUser(user)
            _currentUser.value = user.copy(isLoggedIn = true)
            observeUserResumes(user.id)
            _authLoading.value = false
            _currentScreen.value = Screen.Home
        }
    }

    fun logout() {
        _authLoading.value = true
        viewModelScope.launch {
            userRepository.logout()
            _currentUser.value = null
            _activeResume.value = null
            observeUserResumes(0L)
            _authLoading.value = false
            _currentScreen.value = Screen.Landing
        }
    }

    fun clearAuthError() {
        _authError.value = null
    }

    // --- Navigation ---
    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
        _aiState.value = AiState.Idle
        when (screen) {
            is Screen.Home -> {
                _activeResume.value = null
            }
            is Screen.Edit -> {
                viewModelScope.launch {
                    if (screen.resumeId == -1L) {
                        val currentUserId = _currentUser.value?.id ?: 0L
                        _activeResume.value = Resume(userId = currentUserId)
                    } else {
                        _activeResume.value = resumeRepository.getResumeById(screen.resumeId)
                    }
                }
            }
            is Screen.Preview -> {
                viewModelScope.launch {
                    _activeResume.value = resumeRepository.getResumeById(screen.resumeId)
                }
            }
            else -> {}
        }
    }

    // --- Active Resume Mutation ---
    fun updateActiveResume(resume: Resume) {
        _activeResume.value = resume
    }

    fun saveActiveResume() {
        val resume = _activeResume.value ?: return
        val currentUserId = _currentUser.value?.id ?: 0L
        val resumeWithUser = if (resume.userId == 0L && currentUserId != 0L) {
            resume.copy(userId = currentUserId)
        } else {
            resume
        }
        viewModelScope.launch {
            if (resumeWithUser.id == 0L) {
                val newId = resumeRepository.insertResume(resumeWithUser)
                _activeResume.value = resumeWithUser.copy(id = newId)
                navigateTo(Screen.Preview(newId))
            } else {
                resumeRepository.updateResume(resumeWithUser)
                navigateTo(Screen.Preview(resumeWithUser.id))
            }
        }
    }

    fun deleteResume(resume: Resume) {
        viewModelScope.launch {
            resumeRepository.deleteResume(resume)
            navigateTo(Screen.Home)
        }
    }

    // --- Gemini AI Actions ---

    fun generateAiSummary() {
        val resume = _activeResume.value ?: return
        _aiState.value = AiState.Loading

        viewModelScope.launch {
            val experiences = resume.workExperiences.joinToString("\n") {
                "Role: ${it.role} at ${it.company}\nDescription: ${it.description}"
            }
            val prompt = """
                Write a highly professional, compelling, and punchy 3-sentence executive summary for my resume.
                Name: ${resume.fullName}
                Skills: ${resume.skills.joinToString(", ")}
                Experiences:
                $experiences
                
                Keep it highly executive, use strong action-verbs, and tailor it specifically for high-end professional settings. Do not include greeting phrases or placeholders. Output ONLY the executive summary.
            """.trimIndent()

            val systemInstruction = "You are an expert resume writer and executive career coach. You write elegant, concise, and impact-oriented professional resumes."
            val result = geminiRepository.generateContent(prompt, systemInstruction)

            if (result.startsWith("Error:") || result.startsWith("API Call failed:")) {
                _aiState.value = AiState.Error(result)
            } else {
                _aiState.value = AiState.Success(result)
                _activeResume.value = _activeResume.value?.copy(summary = result)
            }
        }
    }

    fun enhanceWorkExperience(index: Int) {
        val resume = _activeResume.value ?: return
        val experience = resume.workExperiences.getOrNull(index) ?: return
        _aiState.value = AiState.Loading

        viewModelScope.launch {
            val prompt = """
                Rewrite the following work experience bullet points to be highly professional, metric-driven, and rich in action verbs.
                Role: ${experience.role} at ${experience.company}
                Original Description:
                ${experience.description}
                
                Rules:
                - Use the X-Y-Z formula (Accomplished [X] as measured by [Y], by doing [Z]).
                - Use strong engineering or business action verbs (e.g. Spearheaded, Engineered, Optimized, Pioneered).
                - Keep the format of bullet points (using • symbol).
                - Do NOT invent unrealistic degrees or companies, just polish the provided points beautifully.
                - Output ONLY the polished bullet points. No conversational text.
            """.trimIndent()

            val systemInstruction = "You are a senior technical recruiter and resume specialist who helps candidates format experience to stand out to FAANG and top Fortune 500 companies."
            val result = geminiRepository.generateContent(prompt, systemInstruction)

            if (result.startsWith("Error:") || result.startsWith("API Call failed:")) {
                _aiState.value = AiState.Error(result)
            } else {
                _aiState.value = AiState.Success("Successfully enhanced!")
                val updatedList = resume.workExperiences.toMutableList()
                updatedList[index] = experience.copy(description = result)
                _activeResume.value = _activeResume.value?.copy(workExperiences = updatedList)
            }
        }
    }

    fun recommendSkills(industryOrRole: String) {
        val resume = _activeResume.value ?: return
        if (industryOrRole.isBlank()) return
        _aiState.value = AiState.Loading

        viewModelScope.launch {
            val prompt = """
                Recommend a comma-separated list of exactly 8 highly relevant industry-standard skills for a candidate in the following role or industry:
                "$industryOrRole"
                
                Current listed skills: ${resume.skills.joinToString(", ")}
                
                Output ONLY the 8 skills as a simple comma-separated list. (e.g. Skill1, Skill2, Skill3...). No introduction, no markdown formatting other than commas.
            """.trimIndent()

            val systemInstruction = "You are an automated skills taxonomy assistant for modern career portals. You output only raw comma-separated values."
            val result = geminiRepository.generateContent(prompt, systemInstruction)

            if (result.startsWith("Error:") || result.startsWith("API Call failed:")) {
                _aiState.value = AiState.Error(result)
            } else {
                val newSkills = result.split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() && !resume.skills.contains(it) }
                
                if (newSkills.isNotEmpty()) {
                    val updatedSkills = resume.skills + newSkills
                    _activeResume.value = _activeResume.value?.copy(skills = updatedSkills)
                    _aiState.value = AiState.Success("Suggested skills added: ${newSkills.joinToString(", ")}")
                } else {
                    _aiState.value = AiState.Success("No new unique skills suggested.")
                }
            }
        }
    }

    fun optimizeResumeWithAi() {
        val resume = _activeResume.value ?: return
        _aiState.value = AiState.Loading

        viewModelScope.launch {
            val prompt = """
                Analyze the following resume details and generate optimized suggestions to make it stand out.
                Return your answer ONLY as a raw JSON object with the following structure, and NO other conversational text or formatting:
                {
                  "summary": "A highly professional, results-oriented 3-sentence summary featuring key achievements.",
                  "skillsToAdd": ["Skill1", "Skill2", "Skill3"],
                  "enhancedExperienceDescriptions": ["Enhanced bullets for first work experience...", "Enhanced bullets for second work experience..."]
                }

                Resume Details:
                Name: ${resume.fullName}
                Current Summary: ${resume.summary}
                Skills: ${resume.skills.joinToString(", ")}
                Work Experiences:
                ${resume.workExperiences.mapIndexed { idx, exp -> "Experience #$idx:\nRole: ${exp.role} at ${exp.company}\nDescription: ${exp.description}" }.joinToString("\n\n")}
            """.trimIndent()

            val systemInstruction = "You are an expert resume optimization engine. You analyze resumes and output optimized improvements in strict JSON format."
            val result = geminiRepository.generateJsonContent(prompt, systemInstruction)

            if (result.isBlank()) {
                _aiState.value = AiState.Error("Could not retrieve AI suggestions. Please check your internet connection or API key.")
            } else {
                _aiState.value = AiState.Success(result)
            }
        }
    }

    fun clearAiState() {
        _aiState.value = AiState.Idle
    }
}

class ResumeViewModelFactory(
    private val resumeRepository: ResumeRepository,
    private val userRepository: UserRepository,
    private val geminiRepository: GeminiRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ResumeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ResumeViewModel(resumeRepository, userRepository, geminiRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
