package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.Education
import com.example.data.model.Resume
import com.example.data.model.WorkExperience
import com.example.viewmodel.AiState
import com.example.viewmodel.Screen
import com.example.viewmodel.ResumeViewModel
import org.json.JSONObject
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// --- Premium Dark Theme Colors ---
val DarkBg = Color(0xFF0A0A0A)
val SurfaceDark = Color(0xFF141414)
val SurfaceCard = Color(0xFF1E1E1E)
val AccentCream = Color(0xFFFFDBCB)
val AccentIceBlue = Color(0xFFD1E4FF)
val TextLight = Color(0xFFF5F2ED)
val TextMuted = Color(0x99F5F2ED)
val BorderColor = Color(0x1AFFFFFF)

data class OptimizationResult(
    val summary: String,
    val skillsToAdd: List<String>,
    val enhancedExperienceDescriptions: List<String>
)

fun parseOptimization(jsonStr: String): OptimizationResult? {
    return try {
        var cleaned = jsonStr.trim()
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.substringAfter("```json").substringAfter("```").substringBeforeLast("```")
        }
        cleaned = cleaned.trim()
        val json = JSONObject(cleaned)
        val summary = json.optString("summary", "")
        
        val skills = mutableListOf<String>()
        val skillsArray = json.optJSONArray("skillsToAdd")
        if (skillsArray != null) {
            for (i in 0 until skillsArray.length()) {
                skills.add(skillsArray.getString(i))
            }
        }
        
        val experiences = mutableListOf<String>()
        val expArray = json.optJSONArray("enhancedExperienceDescriptions")
        if (expArray != null) {
            for (i in 0 until expArray.length()) {
                experiences.add(expArray.getString(i))
            }
        }
        
        OptimizationResult(summary, skills, experiences)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ResumeViewModel,
    modifier: Modifier = Modifier
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val resumes by viewModel.savedResumes.collectAsState()
    val activeResume by viewModel.activeResume.collectAsState()
    val aiState by viewModel.aiState.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        when (val screen = currentScreen) {
            is Screen.Home -> {
                DashboardView(
                    viewModel = viewModel,
                    resumes = resumes
                )
            }
            is Screen.Edit -> {
                if (activeResume != null) {
                    EditResumeView(
                        viewModel = viewModel,
                        resume = activeResume!!
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AccentCream)
                    }
                }
            }
            else -> {}
        }

        // --- Immersive Global AI Loading Overlay ---
        if (aiState is AiState.Loading) {
            AiProcessingOverlay()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardView(
    viewModel: ResumeViewModel,
    resumes: List<Resume>
) {
    val currentUser by viewModel.currentUser.collectAsState()

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "AI CO-PILOT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 3.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "ResuMe",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Light,
                                letterSpacing = 1.sp
                            ),
                            color = TextLight
                        )
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        if (currentUser != null) {
                            IconButton(
                                onClick = { viewModel.logout() },
                                modifier = Modifier.testTag("logout_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Logout,
                                    contentDescription = "Log Out",
                                    tint = AccentCream
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(1.dp, BorderColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Powered",
                                tint = AccentCream,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = DarkBg)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.navigateTo(Screen.Edit(-1L)) },
                containerColor = AccentCream,
                contentColor = DarkBg,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("create_resume_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Create")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Build Resume",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 80.dp, start = 16.dp, end = 16.dp, top = 16.dp)
        ) {
            // Curatorial Banner Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF2C1E1A), Color(0xFF131722))
                            )
                        )
                        .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
                        .padding(24.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI",
                                tint = AccentCream,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AI PORTFOLIO ASSISTANT",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentCream,
                                letterSpacing = 2.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (currentUser != null) {
                                "Hello, ${currentUser!!.fullName}! Draft, refine, and tailor multiple professional resumes using state-of-the-art Gemini AI model pipelines."
                            } else {
                                "Draft, refine, and tailor multiple professional resumes using state-of-the-art Gemini AI model pipelines."
                            },
                            fontSize = 15.sp,
                            fontFamily = FontFamily.Serif,
                            color = TextLight,
                            lineHeight = 22.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "YOUR RESUMES",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
                )
            }

            if (resumes.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = "No Resumes",
                                tint = TextMuted,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No resumes saved yet",
                                fontSize = 16.sp,
                                color = TextLight,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap the button below to build your first AI-enhanced resume.",
                                fontSize = 12.sp,
                                color = TextMuted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                        }
                    }
                }
            } else {
                items(resumes) { resume ->
                    ResumeCard(
                        resume = resume,
                        onPreview = { viewModel.navigateTo(Screen.Preview(resume.id)) },
                        onEdit = { viewModel.navigateTo(Screen.Edit(resume.id)) },
                        onDelete = { viewModel.deleteResume(resume) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun ResumeCard(
    resume: Resume,
    onPreview: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = resume.title.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentCream,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (resume.fullName.isBlank()) "Untitled Candidate" else resume.fullName,
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = TextLight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Styled Template Indicator Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = resume.templateId.uppercase(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentIceBlue
                    )
                }
            }

            if (resume.summary.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = resume.summary,
                    fontSize = 13.sp,
                    color = TextMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = BorderColor)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = getFormattedDate(resume.timestamp),
                    fontSize = 10.sp,
                    color = TextMuted
                )

                Row {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color.Red.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = TextLight,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Button(
                        onClick = onPreview,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = "PREVIEW",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentCream
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditResumeView(
    viewModel: ResumeViewModel,
    resume: Resume
) {
    var title by remember { mutableStateOf(resume.title) }
    var fullName by remember { mutableStateOf(resume.fullName) }
    var email by remember { mutableStateOf(resume.email) }
    var phone by remember { mutableStateOf(resume.phone) }
    var website by remember { mutableStateOf(resume.website) }
    var location by remember { mutableStateOf(resume.location) }
    var summary by remember { mutableStateOf(resume.summary) }
    var selectedTemplate by remember { mutableStateOf(resume.templateId) }

    // Dynamic Lists (work/edu/skills/certs)
    val workList = remember { mutableStateListOf<WorkExperience>().apply { addAll(resume.workExperiences) } }
    val eduList = remember { mutableStateListOf<Education>().apply { addAll(resume.educations) } }
    var skillsInput by remember { mutableStateOf(resume.skills.joinToString(", ")) }
    var certsInput by remember { mutableStateOf(resume.certifications.joinToString(", ")) }

    // --- AI Suggestions Handling ---
    var showSuggestionsDialog by remember { mutableStateOf(false) }
    var parsedSuggestions by remember { mutableStateOf<OptimizationResult?>(null) }
    val currentAiState by viewModel.aiState.collectAsState()

    LaunchedEffect(currentAiState) {
        if (currentAiState is AiState.Success) {
            val result = (currentAiState as AiState.Success).result
            val parsed = parseOptimization(result)
            if (parsed != null) {
                parsedSuggestions = parsed
                showSuggestionsDialog = true
            }
        }
    }

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "EDIT RESUME",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextLight,
                        letterSpacing = 1.5.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.Home) },
                        modifier = Modifier.testTag("edit_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextLight
                        )
                    }
                },
                actions = {
                    // Optimize with AI Master Button
                    IconButton(
                        onClick = {
                            val tempResume = resume.copy(
                                title = title,
                                fullName = fullName,
                                email = email,
                                phone = phone,
                                website = website,
                                location = location,
                                summary = summary,
                                templateId = selectedTemplate,
                                workExperiences = workList.toList(),
                                educations = eduList.toList(),
                                skills = skillsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                                certifications = certsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                                timestamp = System.currentTimeMillis()
                            )
                            viewModel.updateActiveResume(tempResume)
                            viewModel.optimizeResumeWithAi()
                        },
                        modifier = Modifier.testTag("optimize_resume_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Optimize with AI",
                            tint = AccentCream
                        )
                    }

                    TextButton(
                        onClick = {
                            val finalResume = resume.copy(
                                title = title,
                                fullName = fullName,
                                email = email,
                                phone = phone,
                                website = website,
                                location = location,
                                summary = summary,
                                templateId = selectedTemplate,
                                workExperiences = workList.toList(),
                                educations = eduList.toList(),
                                skills = skillsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                                certifications = certsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                                timestamp = System.currentTimeMillis()
                            )
                            viewModel.updateActiveResume(finalResume)
                            viewModel.saveActiveResume()
                        },
                        modifier = Modifier.testTag("save_resume_button")
                    ) {
                        Text(
                            text = "SAVE",
                            fontWeight = FontWeight.Bold,
                            color = AccentCream
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
            contentPadding = PaddingValues(20.dp)
        ) {
            // General Details
            item {
                SectionHeader(title = "GENERAL INFORMATION")
                StyledTextField(value = title, onValueChange = { title = it }, label = "Resume Identifier / Title")
                Spacer(modifier = Modifier.height(12.dp))
                StyledTextField(value = fullName, onValueChange = { fullName = it }, label = "Full Name")
                Spacer(modifier = Modifier.height(12.dp))
                StyledTextField(value = email, onValueChange = { email = it }, label = "Email Address")
                Spacer(modifier = Modifier.height(12.dp))
                StyledTextField(value = phone, onValueChange = { phone = it }, label = "Phone Number")
                Spacer(modifier = Modifier.height(12.dp))
                StyledTextField(value = website, onValueChange = { website = it }, label = "Website / Portfolio (e.g. GitHub)")
                Spacer(modifier = Modifier.height(12.dp))
                StyledTextField(value = location, onValueChange = { location = it }, label = "Location (e.g. San Francisco, CA)")
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Summary Section with Gemini Autowriter
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader(title = "EXECUTIVE SUMMARY")
                    AssistChip(
                        onClick = {
                            val tempResume = resume.copy(
                                fullName = fullName,
                                skills = skillsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                                workExperiences = workList.toList()
                            )
                            viewModel.updateActiveResume(tempResume)
                            viewModel.generateAiSummary()
                        },
                        label = { Text("AI Auto-Write", fontSize = 11.sp, color = AccentCream) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI summary",
                                tint = AccentCream,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(containerColor = Color.White.copy(alpha = 0.05f))
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                
                // Track state to update summary from ViewModel if user uses AI
                val activeResumeFromVm by viewModel.activeResume.collectAsState()
                LaunchedEffect(activeResumeFromVm?.summary) {
                    activeResumeFromVm?.summary?.let {
                        if (it.isNotBlank() && it != summary) {
                            summary = it
                        }
                    }
                }

                StyledTextField(
                    value = summary,
                    onValueChange = { summary = it },
                    label = "Professional Summary",
                    singleLine = false,
                    modifier = Modifier.height(120.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Work Experience Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader(title = "WORK EXPERIENCE")
                    IconButton(onClick = { workList.add(WorkExperience()) }) {
                        Icon(imageVector = Icons.Default.AddCircle, contentDescription = "Add Work", tint = AccentCream)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(workList.size) { idx ->
                val work = workList[idx]
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ROLE #${idx + 1}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentIceBlue
                            )
                            Row {
                                // AI Bullet Enhancer
                                IconButton(
                                    onClick = {
                                        val tempResume = resume.copy(workExperiences = workList.toList())
                                        viewModel.updateActiveResume(tempResume)
                                        viewModel.enhanceWorkExperience(idx)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "Enhance points",
                                        tint = AccentCream,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(onClick = { workList.removeAt(idx) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete work",
                                        tint = Color.Red.copy(alpha = 0.6f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        
                        // Observe enhanced item from ViewModel
                        val activeResumeFromVm by viewModel.activeResume.collectAsState()
                        LaunchedEffect(activeResumeFromVm?.workExperiences) {
                            activeResumeFromVm?.workExperiences?.getOrNull(idx)?.let { updatedWork ->
                                if (updatedWork.description != work.description) {
                                    workList[idx] = updatedWork
                                }
                            }
                        }

                        StyledTextField(value = work.company, onValueChange = { workList[idx] = work.copy(company = it) }, label = "Company Name")
                        Spacer(modifier = Modifier.height(8.dp))
                        StyledTextField(value = work.role, onValueChange = { workList[idx] = work.copy(role = it) }, label = "Job Title / Role")
                        Spacer(modifier = Modifier.height(8.dp))
                        StyledTextField(value = work.duration, onValueChange = { workList[idx] = work.copy(duration = it) }, label = "Duration (e.g. 2021 - Present)")
                        Spacer(modifier = Modifier.height(8.dp))
                        StyledTextField(
                            value = work.description,
                            onValueChange = { workList[idx] = work.copy(description = it) },
                            label = "Key Achievements (Bullet points)",
                            singleLine = false,
                            modifier = Modifier.height(100.dp)
                        )
                    }
                }
            }

            // Education Section
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader(title = "EDUCATION")
                    IconButton(onClick = { eduList.add(Education()) }) {
                        Icon(imageVector = Icons.Default.AddCircle, contentDescription = "Add Edu", tint = AccentCream)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(eduList.size) { idx ->
                val edu = eduList[idx]
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "EDUCATION #${idx + 1}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentIceBlue
                            )
                            IconButton(onClick = { eduList.removeAt(idx) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete education",
                                    tint = Color.Red.copy(alpha = 0.6f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        StyledTextField(value = edu.school, onValueChange = { eduList[idx] = edu.copy(school = it) }, label = "School / University")
                        Spacer(modifier = Modifier.height(8.dp))
                        StyledTextField(value = edu.degree, onValueChange = { eduList[idx] = edu.copy(degree = it) }, label = "Degree / Major")
                        Spacer(modifier = Modifier.height(8.dp))
                        StyledTextField(value = edu.duration, onValueChange = { eduList[idx] = edu.copy(duration = it) }, label = "Duration (e.g. 2016 - 2020)")
                        Spacer(modifier = Modifier.height(8.dp))
                        StyledTextField(value = edu.description, onValueChange = { eduList[idx] = edu.copy(description = it) }, label = "Details / Honors (Optional)")
                    }
                }
            }

            // Skills & Certs
            item {
                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader(title = "SKILLS (COMMA SEPARATED)")
                    
                    // Skill Suggester Prompt
                    var activeJobTitle = fullName
                    if (workList.isNotEmpty()) {
                        activeJobTitle = workList.first().role
                    }
                    AssistChip(
                        onClick = {
                            val tempResume = resume.copy(
                                skills = skillsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            )
                            viewModel.updateActiveResume(tempResume)
                            viewModel.recommendSkills(activeJobTitle)
                        },
                        label = { Text("Suggest For Role", fontSize = 11.sp, color = AccentCream) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI suggest skills",
                                tint = AccentCream,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(containerColor = Color.White.copy(alpha = 0.05f))
                    )
                }
                
                // Update skills input if recommended
                val activeResumeFromVm by viewModel.activeResume.collectAsState()
                LaunchedEffect(activeResumeFromVm?.skills) {
                    activeResumeFromVm?.skills?.let {
                        val joined = it.joinToString(", ")
                        if (joined.isNotBlank() && joined != skillsInput) {
                            skillsInput = joined
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                StyledTextField(value = skillsInput, onValueChange = { skillsInput = it }, label = "e.g. Kotlin, Compose, AWS, Git")

                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader(title = "CERTIFICATIONS")
                StyledTextField(value = certsInput, onValueChange = { certsInput = it }, label = "e.g. AWS Solutions Architect, PMP")

                Spacer(modifier = Modifier.height(24.dp))
                SectionHeader(title = "AESTHETIC STYLE TEMPLATE")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("modern", "serif", "minimalist", "creative").forEach { template ->
                        val isSelected = selectedTemplate == template
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) AccentCream else Color.White.copy(alpha = 0.05f))
                                .clickable { selectedTemplate = template }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = template.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) DarkBg else TextLight
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // --- Interactive Master AI Suggestions Dialog ---
    if (showSuggestionsDialog && parsedSuggestions != null) {
        val sugg = parsedSuggestions!!
        AlertDialog(
            onDismissRequest = {
                showSuggestionsDialog = false
                viewModel.clearAiState()
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI",
                        tint = AccentCream,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "AI Optimization suggestions",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextLight
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Optimized Summary Suggestion
                    if (!sugg.summary.isNullOrBlank()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                            modifier = Modifier.fillMaxWidth().border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "SUGGESTED EXECUTIVE SUMMARY",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentCream
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = sugg.summary,
                                    fontSize = 13.sp,
                                    color = TextLight,
                                    lineHeight = 18.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        summary = sugg.summary
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text("Apply Summary", color = AccentCream, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    // Skills To Add Suggestion
                    if (!sugg.skillsToAdd.isNullOrEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                            modifier = Modifier.fillMaxWidth().border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "SUGGESTED SKILLS TO ADD",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentCream
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                // Chips
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    sugg.skillsToAdd.forEach { s ->
                                        val alreadyAdded = skillsInput.split(",").map { it.trim().lowercase() }.contains(s.lowercase())
                                        AssistChip(
                                            onClick = {
                                                if (!alreadyAdded) {
                                                    skillsInput = if (skillsInput.isBlank()) s else "$skillsInput, $s"
                                                }
                                            },
                                            label = { Text(s, color = if (alreadyAdded) TextMuted else AccentIceBlue) },
                                            leadingIcon = {
                                                if (alreadyAdded) {
                                                    Icon(Icons.Default.Check, null, modifier = Modifier.size(12.dp), tint = TextMuted)
                                                } else {
                                                    Icon(Icons.Default.Add, null, modifier = Modifier.size(12.dp), tint = AccentIceBlue)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Work Experience Suggestion
                    if (!sugg.enhancedExperienceDescriptions.isNullOrEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                            modifier = Modifier.fillMaxWidth().border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "ENHANCED WORK BULLET POINTS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentCream
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                sugg.enhancedExperienceDescriptions.forEachIndexed { idx, desc ->
                                    val workCompany = workList.getOrNull(idx)?.company ?: "Role #${idx + 1}"
                                    Column(modifier = Modifier.padding(bottom = 12.dp)) {
                                        Text(workCompany, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AccentIceBlue)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(desc, fontSize = 12.sp, color = TextLight, lineHeight = 16.sp)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = {
                                                if (idx < workList.size) {
                                                    workList[idx] = workList[idx].copy(description = desc)
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.align(Alignment.End)
                                        ) {
                                            Text("Apply to role", color = AccentCream, fontSize = 11.sp)
                                        }
                                    }
                                    if (idx < sugg.enhancedExperienceDescriptions.size - 1) {
                                        Divider(color = BorderColor, modifier = Modifier.padding(vertical = 8.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuggestionsDialog = false
                        viewModel.clearAiState()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCream)
                ) {
                    Text("Apply & Close", color = DarkBg, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = SurfaceDark
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = TextMuted,
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(vertical = 6.dp)
    )
}

@Composable
fun StyledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    singleLine: Boolean = true,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextMuted, fontSize = 12.sp) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AccentCream,
            unfocusedBorderColor = BorderColor,
            focusedTextColor = TextLight,
            unfocusedTextColor = TextLight,
            cursorColor = AccentCream,
            focusedContainerColor = SurfaceDark,
            unfocusedContainerColor = SurfaceDark
        ),
        shape = RoundedCornerShape(12.dp),
        singleLine = singleLine,
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
    )
}

@Composable
fun AiProcessingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = AccentCream, strokeWidth = 3.dp)
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "GEMINI AI CO-PILOT",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = AccentCream,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Weaving career narratives, refining bullet points...",
                fontSize = 13.sp,
                color = TextMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun getFormattedDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
