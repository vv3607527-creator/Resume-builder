package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.Resume
import com.example.viewmodel.Screen
import com.example.viewmodel.ResumeViewModel
import com.example.viewmodel.AiState
import kotlinx.coroutines.launch

data class ResumeThemeConfig(
    val bgColor: Color,
    val cardColor: Color,
    val textColor: Color,
    val mutedColor: Color,
    val accentColor: Color,
    val fontFamily: FontFamily,
    val dividerColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(
    viewModel: ResumeViewModel,
    modifier: Modifier = Modifier
) {
    val resumeState by viewModel.activeResume.collectAsState()
    val scrollState = rememberScrollState()
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // AI Career Assistant State
    var showAiAssistant by remember { mutableStateOf(false) }
    var aiTargetJob by remember { mutableStateOf("") }
    var aiOutputText by remember { mutableStateOf("") }
    var isGeneratingAiTool by remember { mutableStateOf(false) }
    var activeAiToolType by remember { mutableStateOf("cover") } // "cover", "interview", "coach"
    var coachQuery by remember { mutableStateOf("") }

    if (resumeState == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBg),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = AccentCream)
        }
        return
    }

    val resume = resumeState!!

    // Define templates based on templateId
    val theme = when (resume.templateId) {
        "serif" -> ResumeThemeConfig(
            bgColor = Color(0xFFFAF7F0), // Warm Cream Paper
            cardColor = Color(0xFFFFFFFF),
            textColor = Color(0xFF1C1A17), // Rich Charcoal text
            mutedColor = Color(0xFF6B645D), // Warm slate
            accentColor = Color(0xFF8D6E63), // Clay accent
            fontFamily = FontFamily.Serif,
            dividerColor = Color(0xFFE5DFD5)
        )
        "minimalist" -> ResumeThemeConfig(
            bgColor = Color(0xFFF6F6F6), // Pure off-white
            cardColor = Color(0xFFFFFFFF),
            textColor = Color(0xFF1D1D1F), // Apple Charcoal
            mutedColor = Color(0xFF86868B), // Mid gray
            accentColor = Color(0xFF333333), // Slate black
            fontFamily = FontFamily.SansSerif,
            dividerColor = Color(0xFFE5E5E7)
        )
        "creative" -> ResumeThemeConfig(
            bgColor = Color(0xFF0D0B14), // Deep Purple
            cardColor = Color(0xFF171424), // Indigo Card
            textColor = Color(0xFFF5F2ED),
            mutedColor = Color(0xFFA59EB5),
            accentColor = Color(0xFFFF8A65), // Coral accent
            fontFamily = FontFamily.SansSerif,
            dividerColor = Color(0x33FF8A65)
        )
        else -> ResumeThemeConfig( // "modern"
            bgColor = Color(0xFF0F172A), // Slate Dark Blue
            cardColor = Color(0xFF1E293B), // Card
            textColor = Color(0xFFF8FAFC), // White Slate
            mutedColor = Color(0xFF94A3B8), // Muted Slate
            accentColor = Color(0xFF38BDF8), // Light Blue Accent
            fontFamily = FontFamily.Monospace,
            dividerColor = Color(0x3338BDF8)
        )
    }

    Scaffold(
        containerColor = theme.bgColor,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "PREVIEW RESUME",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.mutedColor,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = resume.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.textColor,
                            maxLines = 1
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.Home) },
                        modifier = Modifier.testTag("preview_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = theme.textColor
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val plainText = buildString {
                                appendLine("=== ${resume.fullName.uppercase()} ===")
                                appendLine("Email: ${resume.email} | Phone: ${resume.phone}")
                                appendLine("Website: ${resume.website} | Location: ${resume.location}")
                                appendLine()
                                appendLine("--- PROFESSIONAL SUMMARY ---")
                                appendLine(resume.summary)
                                appendLine()
                                appendLine("--- EXPERIENCE ---")
                                resume.workExperiences.forEach {
                                    appendLine("${it.role} at ${it.company} (${it.duration})")
                                    appendLine(it.description)
                                    appendLine()
                                }
                                appendLine("--- EDUCATION ---")
                                resume.educations.forEach {
                                    appendLine("${it.degree} - ${it.school} (${it.duration})")
                                    if (it.description.isNotBlank()) appendLine(it.description)
                                }
                                appendLine()
                                appendLine("--- SKILLS ---")
                                appendLine(resume.skills.joinToString(", "))
                            }
                            clipboardManager.setText(AnnotatedString(plainText))
                            scope.launch {
                                snackbarHostState.showSnackbar("Resume copied to clipboard!")
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Resume Text",
                            tint = theme.textColor
                        )
                    }
                    IconButton(onClick = { viewModel.navigateTo(Screen.Edit(resume.id)) }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Resume",
                            tint = theme.textColor
                        )
                    }
                    IconButton(onClick = { viewModel.deleteResume(resume) }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color.Red.copy(alpha = 0.7f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAiAssistant = !showAiAssistant },
                containerColor = AccentCream,
                contentColor = DarkBg,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Assistant"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI Career Tools", fontWeight = FontWeight.Bold)
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main content block splitting view & the sliding assistant panel
            Box(modifier = Modifier.weight(1f)) {
                // Rendered Styled Resume
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(theme.cardColor)
                            .border(1.dp, theme.dividerColor, RoundedCornerShape(16.dp))
                            .padding(24.dp)
                    ) {
                        Column {
                            // Resume Header Info
                            Text(
                                text = if (resume.fullName.isBlank()) "Untitled Candidate" else resume.fullName,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = theme.textColor,
                                fontFamily = theme.fontFamily
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = listOf(resume.email, resume.phone, resume.location)
                                    .filter { it.isNotBlank() }
                                    .joinToString("  •  "),
                                fontSize = 11.sp,
                                color = theme.mutedColor,
                                fontFamily = theme.fontFamily
                            )
                            if (resume.website.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = resume.website,
                                    fontSize = 11.sp,
                                    color = theme.accentColor,
                                    fontFamily = theme.fontFamily
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))
                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(theme.dividerColor))
                            Spacer(modifier = Modifier.height(18.dp))

                            // Summary Section
                            if (resume.summary.isNotBlank()) {
                                Text(
                                    text = "PROFESSIONAL SUMMARY",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = theme.accentColor,
                                    letterSpacing = 1.5.sp,
                                    fontFamily = theme.fontFamily
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = resume.summary,
                                    fontSize = 13.sp,
                                    color = theme.textColor,
                                    lineHeight = 20.sp,
                                    fontFamily = theme.fontFamily
                                )
                                Spacer(modifier = Modifier.height(22.dp))
                            }

                            // Work Experience
                            if (resume.workExperiences.isNotEmpty()) {
                                Text(
                                    text = "WORK EXPERIENCE",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = theme.accentColor,
                                    letterSpacing = 1.5.sp,
                                    fontFamily = theme.fontFamily
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                resume.workExperiences.forEachIndexed { index, exp ->
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = exp.role,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = theme.textColor,
                                                    fontFamily = theme.fontFamily
                                                )
                                                Text(
                                                    text = exp.company,
                                                    fontSize = 13.sp,
                                                    color = theme.mutedColor,
                                                    fontFamily = theme.fontFamily
                                                )
                                            }
                                            Text(
                                                text = exp.duration,
                                                fontSize = 12.sp,
                                                color = theme.mutedColor,
                                                fontWeight = FontWeight.SemiBold,
                                                fontFamily = theme.fontFamily
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = exp.description,
                                            fontSize = 13.sp,
                                            color = theme.textColor,
                                            lineHeight = 18.sp,
                                            fontFamily = theme.fontFamily
                                        )
                                    }
                                    if (index < resume.workExperiences.size - 1) {
                                        Spacer(modifier = Modifier.height(18.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                            }

                            // Education Section
                            if (resume.educations.isNotEmpty()) {
                                Text(
                                    text = "EDUCATION",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = theme.accentColor,
                                    letterSpacing = 1.5.sp,
                                    fontFamily = theme.fontFamily
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                resume.educations.forEach { edu ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = edu.school,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = theme.textColor,
                                                fontFamily = theme.fontFamily
                                            )
                                            Text(
                                                text = edu.degree,
                                                fontSize = 13.sp,
                                                color = theme.mutedColor,
                                                fontFamily = theme.fontFamily
                                            )
                                        }
                                        Text(
                                            text = edu.duration,
                                            fontSize = 12.sp,
                                            color = theme.mutedColor,
                                            fontFamily = theme.fontFamily
                                        )
                                    }
                                    if (edu.description.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = edu.description,
                                            fontSize = 12.sp,
                                            color = theme.textColor,
                                            fontStyle = FontStyle.Italic,
                                            fontFamily = theme.fontFamily
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(14.dp))
                                }
                            }

                            // Skills Chips
                            if (resume.skills.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "SKILLS & EXPERTISE",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = theme.accentColor,
                                    letterSpacing = 1.5.sp,
                                    fontFamily = theme.fontFamily
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    mainAxisSpacing = 8.dp,
                                    crossAxisSpacing = 8.dp
                                ) {
                                    resume.skills.forEach { skill ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(theme.accentColor.copy(alpha = 0.1f))
                                                .border(1.dp, theme.accentColor.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = skill,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = theme.textColor,
                                                fontFamily = theme.fontFamily
                                            )
                                        }
                                    }
                                }
                            }

                            // Certifications Section
                            if (resume.certifications.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    text = "CERTIFICATIONS",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = theme.accentColor,
                                    letterSpacing = 1.5.sp,
                                    fontFamily = theme.fontFamily
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                resume.certifications.forEach { cert ->
                                    Text(
                                        text = "• $cert",
                                        fontSize = 13.sp,
                                        color = theme.textColor,
                                        fontFamily = theme.fontFamily,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(60.dp))
                }

                // AI Assistant Sliding Bottom Drawer
                androidx.compose.animation.AnimatedVisibility(
                    visible = showAiAssistant,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.6f))
                            .clickable { showAiAssistant = false }
                    ) {
                        Card(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(520.dp)
                                .clickable(enabled = false) {}, // Prevent dismiss click inside card
                            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp)
                            ) {
                                // Panel header handle bar
                                Box(
                                    modifier = Modifier
                                        .size(40.dp, 4.dp)
                                        .clip(CircleShape)
                                        .background(BorderColor)
                                        .align(Alignment.CenterHorizontally)
                                )
                                Spacer(modifier = Modifier.height(18.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = "AI Tools",
                                            tint = AccentCream,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "AI CAREER TOOLBOX",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextLight,
                                            letterSpacing = 1.5.sp
                                        )
                                    }
                                    IconButton(onClick = { showAiAssistant = false }) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Navigation Tabs for the toolbox
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    listOf("cover" to "Cover Letter", "interview" to "Interview Prep", "coach" to "Career Coach").forEach { (tabId, label) ->
                                        val isTabActive = activeAiToolType == tabId
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isTabActive) AccentCream else Color.White.copy(alpha = 0.05f))
                                                .clickable { activeAiToolType = tabId }
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isTabActive) DarkBg else TextLight
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                // Panel Sub-views
                                when (activeAiToolType) {
                                    "cover" -> {
                                        Text(
                                            text = "Target Job Role or Company:",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextMuted
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        OutlinedTextField(
                                            value = aiTargetJob,
                                            onValueChange = { aiTargetJob = it },
                                            placeholder = { Text("e.g. Senior Android Developer at Google", color = TextMuted, fontSize = 13.sp) },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = AccentCream,
                                                unfocusedBorderColor = BorderColor,
                                                focusedTextColor = TextLight,
                                                unfocusedTextColor = TextLight
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    "coach" -> {
                                        Text(
                                            text = "Ask anything to your Career Coach:",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextMuted
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        OutlinedTextField(
                                            value = coachQuery,
                                            onValueChange = { coachQuery = it },
                                            placeholder = { Text("e.g. How can I explain a 6-month employment gap?", color = TextMuted, fontSize = 13.sp) },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = AccentCream,
                                                unfocusedBorderColor = BorderColor,
                                                focusedTextColor = TextLight,
                                                unfocusedTextColor = TextLight
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    "interview" -> {
                                        Text(
                                            text = "Press generate below to construct a tailored list of Technical & Behavioral Interview prep questions based on your unique resume experience.",
                                            fontSize = 12.sp,
                                            color = TextMuted,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = {
                                        isGeneratingAiTool = true
                                        scope.launch {
                                            val prompt = when (activeAiToolType) {
                                                "cover" -> """
                                                    Write an elegant, persuasive cover letter for ${resume.fullName} applying for the position of "$aiTargetJob".
                                                    
                                                    Resume Details:
                                                    Name: ${resume.fullName}
                                                    Summary: ${resume.summary}
                                                    Skills: ${resume.skills.joinToString(", ")}
                                                    Experiences:
                                                    ${resume.workExperiences.joinToString("\n") { "${it.role} at ${it.company}: ${it.description}" }}
                                                    
                                                    Format it clearly, make it highly persuasive, show excitement for the role, and explain why their skills fit perfectly. Ensure professional tone.
                                                """.trimIndent()
                                                "interview" -> """
                                                    Generate exactly 5 tailored interview preparation questions for ${resume.fullName}. Combine hard technical skills and behavioral items based on their experiences:
                                                    
                                                    Resume Details:
                                                    Summary: ${resume.summary}
                                                    Skills: ${resume.skills.joinToString(", ")}
                                                    Experiences:
                                                    ${resume.workExperiences.joinToString("\n") { "${it.role} at ${it.company}: ${it.description}" }}
                                                    
                                                    Provide precise questions, and 1-2 sentence golden tips on how they should answer based on their resume highlights.
                                                """.trimIndent()
                                                else -> """
                                                    You are an elite executive career coach. Answer the following candidate query relative to their resume:
                                                    Candidate Query: "$coachQuery"
                                                    
                                                    Candidate Resume Details:
                                                    Name: ${resume.fullName}
                                                    Summary: ${resume.summary}
                                                    Skills: ${resume.skills.joinToString(", ")}
                                                    Experiences:
                                                    ${resume.workExperiences.joinToString("\n") { "${it.role} at ${it.company}: ${it.description}" }}
                                                    
                                                    Keep the advice highly actionable, empowering, objective, and professional.
                                                """.trimIndent()
                                            }

                                            val systemInstruction = "You are a professional recruiting director and executive coach. You output formatted, premium corporate materials."
                                            val result = viewModel.recommendSkills(prompt) // Triggers loading but we can call repo directly
                                            
                                            // Call repo directly to avoid state collision
                                            val response = viewModel.let {
                                                com.example.data.api.GeminiRepository().generateContent(prompt, systemInstruction)
                                            }

                                            aiOutputText = response
                                            isGeneratingAiTool = false
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentCream),
                                    shape = RoundedCornerShape(12.dp),
                                    enabled = !isGeneratingAiTool
                                ) {
                                    if (isGeneratingAiTool) {
                                        CircularProgressIndicator(color = DarkBg, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    } else {
                                        Text(
                                            text = when (activeAiToolType) {
                                                "cover" -> "Generate Cover Letter"
                                                "interview" -> "Generate Interview Questions"
                                                else -> "Ask Career Coach"
                                            }.uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            color = DarkBg,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Output text field
                                if (aiOutputText.isNotBlank()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "GENERATED OUTPUT:",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AccentIceBlue
                                        )
                                        IconButton(
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString(aiOutputText))
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("AI Output copied!")
                                                }
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy Output", tint = AccentCream, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.Black.copy(alpha = 0.3f))
                                            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                                            .verticalScroll(rememberScrollState())
                                            .padding(14.dp)
                                    ) {
                                        Text(
                                            text = aiOutputText,
                                            fontSize = 12.sp,
                                            color = TextLight,
                                            lineHeight = 18.sp
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Your custom AI workspace will compile here.",
                                            fontSize = 12.sp,
                                            color = TextMuted
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

@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    mainAxisSpacing: androidx.compose.ui.unit.Dp = 0.dp,
    crossAxisSpacing: androidx.compose.ui.unit.Dp = 0.dp,
    content: @Composable () -> Unit
) {
    androidx.compose.ui.layout.Layout(
        modifier = modifier,
        content = content
    ) { measurables, constraints ->
        val placeables = measurables.map { measurable ->
            measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
        }

        val rows = mutableListOf<List<androidx.compose.ui.layout.Placeable>>()
        var currentRow = mutableListOf<androidx.compose.ui.layout.Placeable>()
        var currentRowWidth = 0

        placeables.forEach { placeable ->
            val spacing = if (currentRow.isEmpty()) 0 else mainAxisSpacing.roundToPx()
            if (currentRowWidth + spacing + placeable.width <= constraints.maxWidth) {
                currentRow.add(placeable)
                currentRowWidth += spacing + placeable.width
            } else {
                rows.add(currentRow)
                currentRow = mutableListOf(placeable)
                currentRowWidth = placeable.width
            }
        }
        if (currentRow.isNotEmpty()) {
            rows.add(currentRow)
        }

        var totalHeight = 0
        rows.forEachIndexed { index, row ->
            val spacing = if (index == 0) 0 else crossAxisSpacing.roundToPx()
            val rowHeight = row.maxOfOrNull { it.height } ?: 0
            totalHeight += spacing + rowHeight
        }

        layout(constraints.maxWidth, totalHeight) {
            var y = 0
            rows.forEach { row ->
                val rowHeight = row.maxOfOrNull { it.height } ?: 0
                var x = 0
                row.forEachIndexed { index, placeable ->
                    val spacing = if (index == 0) 0 else mainAxisSpacing.roundToPx()
                    placeable.placeRelative(x + spacing, y)
                    x += spacing + placeable.width
                }
                y += rowHeight + crossAxisSpacing.roundToPx()
            }
        }
    }
}
