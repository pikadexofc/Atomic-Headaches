package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Note
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Custom Theme colors
val SlateBg = Color(0xFF040406)      // Jet black deep space canvas
val SlateCard = Color(0xFF0C0C12)    // Slate absolute dark gray card surface
val CreamWhite = Color(0xFFEADDC9)   // Warm parchment branding color
val NeonCyan = Color(0xFF22D3EE)     // Cyber blue orb aura
val NeonViolet = Color(0xFF8B5CF6)   // Mystic amethyst orb aura
val MutedText = Color(0xFF9CA3AF)    // High contrast scale text

private val LOCATIONS = listOf("Home", "Campus", "Outside", "Lab", "Transit")
private val MOODS = listOf("Calm", "Focused", "Tired", "Reflective", "Restless", "Clear", "Analytical", "Skeptical")
private val FILTERS = listOf("All", "Today", "Home", "Campus", "Outside", "Mood", "Analytics")

@Composable
fun AtomicHeadachesApp(viewModel: NoteViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateBg)
    ) {
        // Dynamic Glowing Background Orbs
        AmbientOrb(
            color = NeonCyan,
            size = 280.dp,
            alpha = 0.12f,
            offsetX = (-60).dp,
            offsetY = (-80).dp,
            delayMs = 0
        )
        AmbientOrb(
            color = NeonViolet,
            size = 240.dp,
            alpha = 0.10f,
            alignment = Alignment.TopEnd,
            offsetX = 60.dp,
            offsetY = 120.dp,
            delayMs = 1200
        )
        AmbientOrb(
            color = Color.White,
            size = 260.dp,
            alpha = 0.06f,
            alignment = Alignment.BottomCenter,
            offsetX = 0.dp,
            offsetY = 100.dp,
            delayMs = 2400
        )

        // Overlay textures
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.02f),
                            Color.Transparent,
                            Color.White.copy(alpha = 0.01f)
                        )
                    )
                )
        )

        val showOnboarding by viewModel.showOnboarding
        val onboardingDone by viewModel.onboardingDone
        val composerOpen by viewModel.composerOpen
        val detailOpenId by viewModel.detailOpenId
        val analyticsOpen by viewModel.analyticsOpen
        val notesList by viewModel.notes.collectAsState()
        val authCompleted by viewModel.authCompleted
        val authSkipped by viewModel.authSkipped
        val bookReaderOpen by viewModel.bookReaderOpen

        // 1. Deciding what primary screen / panel container is visible
        if (!authCompleted && !authSkipped) {
            com.example.ui.auth.AuthScreen(
                onAuthSuccess = { email, displayName, photoUrl ->
                    viewModel.completeAuth(email, displayName, photoUrl)
                },
                onSkipAuth = {
                    viewModel.skipAuth()
                },
                playSound = { freq, dur -> viewModel.playSound(freq, dur) }
            )
        } else if (!onboardingDone && showOnboarding) {
            OnboardingOverlay(
                onFinish = { viewModel.completeOnboarding() },
                playSound = { freq, dur -> viewModel.playSound(freq, dur) }
            )
        } else if (bookReaderOpen) {
            com.example.ui.reader.BookReader(
                notes = notesList,
                onClose = { viewModel.bookReaderOpen.value = false },
                playSound = { freq, dur -> viewModel.playSound(freq, dur) }
            )
        } else {
            // Main App stream
            MainStream(
                viewModel = viewModel,
                notesList = notesList
            )

            // Compose Overlay Sheets
            AnimatedVisibility(
                visible = composerOpen,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                viewModel.draft.value?.let { activeDraft ->
                    ComposerSheet(
                        draft = activeDraft,
                        viewModel = viewModel,
                        onSave = { viewModel.saveDraft() },
                        onClose = {
                            viewModel.composerOpen.value = false
                            viewModel.draft.value = null
                            viewModel.editingId.value = null
                            viewModel.focusMode.value = false
                            viewModel.playSound(500f, 0.12f)
                        }
                    )
                }
            }

            AnimatedVisibility(
                visible = detailOpenId != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                val activeNote = notesList.find { it.id == detailOpenId }
                if (activeNote != null) {
                    DetailSheet(
                        note = activeNote,
                        viewModel = viewModel,
                        onClose = { viewModel.detailOpenId.value = null },
                        onEdit = { viewModel.openEdit(activeNote) },
                        onDelete = { viewModel.deleteNote(activeNote.id) }
                    )
                }
            }

            AnimatedVisibility(
                visible = analyticsOpen,
                enter = fadeIn() + scaleIn(initialScale = 0.98f),
                exit = fadeOut() + scaleOut(targetScale = 0.98f),
                modifier = Modifier.fillMaxSize()
            ) {
                AnalyticsDashboard(
                    notes = notesList,
                    viewModel = viewModel,
                    onClose = { viewModel.analyticsOpen.value = false }
                )
            }
        }
    }
}

@Composable
fun AmbientOrb(
    color: Color,
    size: Dp,
    alpha: Float,
    alignment: Alignment = Alignment.TopStart,
    offsetX: Dp = 0.dp,
    offsetY: Dp = 0.dp,
    delayMs: Int = 0
) {
    // Generate a beautiful, glowing mesh orb fully in native Compose using Canvas radial brush
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {}
    ) {
        Box(
            modifier = Modifier
                .align(alignment)
                .offset(x = offsetX, y = offsetY)
                .size(size)
                .blur(48.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            color.copy(alpha = alpha),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
    }
}

// PREMIUM BUTTONS
@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    textColor: Color = Color.White,
    borderColor: Color = Color.White.copy(alpha = 0.1f),
    backgroundColor: Color = Color.White.copy(alpha = 0.04f),
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = textColor
        ),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, borderColor),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxHeight()
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = textColor
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            content()
        }
    }
}

@Composable
fun GlassAction(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    active: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    val bg = if (active) Color.White else Color.White.copy(alpha = 0.08f)
    val textC = if (active) Color.Black else Color.White
    val borderC = if (active) Color.White else Color.White.copy(alpha = 0.1f)

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = bg,
            contentColor = textC
        ),
        shape = RoundedCornerShape(32.dp),
        border = BorderStroke(1.dp, borderC),
        contentPadding = PaddingValues(horizontal = 32.dp),
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(32.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxHeight()
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = textC
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            content()
        }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            letterSpacing = 3.sp,
            color = Color.White.copy(alpha = 0.35f)
        ),
        modifier = modifier
    )
}

@Composable
fun Badge(text: String) {
    Box(
        modifier = Modifier
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(
            text = text.uppercase(),
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp,
                letterSpacing = 2.sp,
                color = MutedText.copy(alpha = 0.8f)
            )
        )
    }
}

@Composable
fun MiniPill(
    active: Boolean,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .border(
                1.dp,
                if (active) Color.White else Color.White.copy(alpha = 0.04f),
                RoundedCornerShape(24.dp)
            )
            .background(
                if (active) Color.White else Color.White.copy(alpha = 0.03f),
                RoundedCornerShape(24.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = TextStyle(
                fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = 14.sp,
                letterSpacing = (-0.5).sp,
                color = if (active) Color.Black else Color.White.copy(alpha = 0.85f)
            )
        )
    }
}

// ONBOARDING
data class CinematicLine(val eyebrow: String, val title: String, val body: String)

val cinematicLines = listOf(
    CinematicLine(
        eyebrow = "Atomic headaches",
        title = "Write before the moment disappears.",
        body = "A private journal for fast capture. Timestamp, day, and location are attached automatically."
    ),
    CinematicLine(
        eyebrow = "Automatic context",
        title = "The note keeps its own memory.",
        body = "Time, date, and place stay with the entry. The writing stays at the center."
    ),
    CinematicLine(
        eyebrow = "Immediate entry",
        title = "What is on your mind right now?",
        body = "The editor opens ready. No setup. No explanation. Just the cursor."
    ),
    CinematicLine(
        eyebrow = "Subtle confirmation",
        title = "Saved at 17:20.",
        body = "A quiet confirmation, then back to the timeline."
    ),
    CinematicLine(
        eyebrow = "Timeline",
        title = "A private archive that gets out of the way.",
        body = "Recent notes arrive cleanly, with one strong action always in reach."
    )
)

val demoCards = listOf(
    Pair("Summer", "No items"),
    Pair("Routine", "4 items")
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingOverlay(
    onFinish: () -> Unit,
    playSound: (Float, Float) -> Unit
) {
    var index by remember { mutableStateOf(0) }
    var paused by remember { mutableStateOf(false) }
    val currentLine = cinematicLines[index]
    val finalSlide = index == cinematicLines.size - 1

    // Slide auto-play tick
    LaunchedEffect(index, paused) {
        if (paused) return@LaunchedEffect
        // Tick durations similar to React
        val duration = if (index == 2) 3200L else 2600L
        delay(duration)
        playSound(700f, 0.15f)
        if (finalSlide) {
            delay(1400L)
            playSound(1000f, 0.25f)
            onFinish()
        } else {
            index = (index + 1).coerceAtMost(cinematicLines.size - 1)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(NeonCyan, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "REALTIME SYNCHRONY",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 2.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    )
                }

                Text(
                    text = "9:41 AM",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                )
            }

            // Cinematic identity
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    SectionLabel("JOURNAL OVERTURE")
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "ATOMIC\nHEADACHES",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Black,
                            fontSize = 32.sp,
                            lineHeight = 30.sp,
                            color = Color.White,
                            letterSpacing = (-1).sp
                        )
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Mute / pause trigger
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                            .background(Color.White.copy(alpha = 0.05f))
                            .clickable {
                                playSound(600f, 0.12f)
                                paused = !paused
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (paused) Icons.Default.Mic else Icons.Default.MoreVert,
                            contentDescription = null,
                            tint = if (paused) NeonCyan else Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Chevron Next trigger
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                            .background(Color.White.copy(alpha = 0.05f))
                            .clickable {
                                playSound(900f, 0.12f)
                                if (finalSlide) {
                                    onFinish()
                                } else {
                                    index = (index + 1).coerceAtMost(cinematicLines.size - 1)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            // Slide Index tick indicators
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(2.dp))
            ) {
                val completionRatio = (index + 1).toFloat() / cinematicLines.size
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(completionRatio)
                        .background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(2.dp))
                )
            }

            // Cinematic Deck
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
            ) {
                AnimatedContent(
                    targetState = index,
                    transitionSpec = {
                        (fadeIn() + slideInVertically(initialOffsetY = { 30 }))
                            .togetherWith(fadeOut() + slideOutVertically(targetOffsetY = { -30 }))
                    },
                    modifier = Modifier.fillMaxSize()
                ) { targetIndex ->
                    if (targetIndex < 4) {
                        // Graphic slides
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(32.dp))
                                .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(32.dp))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    SectionLabel(currentLine.eyebrow)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = currentLine.title,
                                        style = TextStyle(
                                            fontFamily = FontFamily.SansSerif,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 28.sp,
                                            lineHeight = 32.sp,
                                            color = Color.White,
                                            letterSpacing = (-0.5).sp
                                        ),
                                        modifier = Modifier.fillMaxWidth(0.9f)
                                    )
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Box(
                                        modifier = Modifier
                                            .width(64.dp)
                                            .height(1.dp)
                                            .background(CreamWhite.copy(alpha = 0.3f))
                                    )
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Text(
                                        text = currentLine.body,
                                        style = TextStyle(
                                            fontFamily = FontFamily.SansSerif,
                                            fontWeight = FontWeight.Light,
                                            fontSize = 15.sp,
                                            lineHeight = 24.sp,
                                            color = Color.White.copy(alpha = 0.6f)
                                        ),
                                        modifier = Modifier.fillMaxWidth(0.9f)
                                    )
                                }

                                // Interactive display widgets inside Onboarding Slide
                                when (targetIndex) {
                                    0 -> {
                                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                OnboardingWidgetRow(Icons.Default.AccessTime, "Timestamp")
                                                OnboardingWidgetRow(Icons.Default.CalendarToday, "Day")
                                            }
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                                                    .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .background(Color.White.copy(alpha = 0.08f), CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Default.LocationOn, null, tint = CreamWhite, modifier = Modifier.size(18.dp))
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    text = "Location captured quietly",
                                                    style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White.copy(alpha = 0.85f))
                                                )
                                            }
                                        }
                                    }
                                    1 -> {
                                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            listOf(
                                                "Saved time appears instantly.",
                                                "Day is attached without a prompt.",
                                                "Location is summarized broadly."
                                            ).forEach { line ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                                                    .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                                                    .padding(16.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .background(CreamWhite, CircleShape)
                                                    )
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Text(
                                                        text = line,
                                                        style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White.copy(alpha = 0.85f))
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    2 -> {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                                                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                                                .padding(16.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "FOCUS MODE",
                                                    style = TextStyle(
                                                        fontFamily = FontFamily.Monospace,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 10.sp,
                                                        letterSpacing = 2.sp,
                                                        color = Color.White.copy(alpha = 0.7f)
                                                    )
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = "ACTIVE",
                                                        style = TextStyle(
                                                            fontFamily = FontFamily.Monospace,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 9.sp,
                                                            letterSpacing = 1.sp,
                                                            color = CreamWhite
                                                        )
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(16.dp))
                                                    .padding(16.dp)
                                            ) {
                                                Text("PROMPT", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 9.sp, letterSpacing = 2.sp, color = Color.White.copy(alpha = 0.4f)))
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text("What is on your mind?", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp, letterSpacing = (-0.5).sp, color = Color.White))
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Text(
                                                    text = "CURSOR IS ALREADY ACTIVE.",
                                                    style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, letterSpacing = 1.sp, color = Color.White.copy(alpha = 0.3f))
                                                )
                                            }
                                        }
                                    }
                                    3 -> {
                                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                                                    .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                                                    .padding(16.dp)
                                            ) {
                                                Text("SAVED STATUS", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color.White.copy(alpha = 0.4f)))
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text("17:20", style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 28.sp, color = CreamWhite))
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text("HOME · TODAY · CONFIRMED", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f)))
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                listOf("Home", "Campus", "Outside").forEach { tag ->
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                                                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(14.dp))
                                                            .padding(vertical = 12.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(tag.uppercase(), style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f)))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Workouts list preview deck
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(32.dp))
                                .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(32.dp))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        SectionLabel("HOME BASE")
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("WORKOUTS", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 26.sp, color = Color.White))
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                                                .background(Color.White.copy(alpha = 0.05f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.FilterList, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                                                .background(Color.White.copy(alpha = 0.05f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    demoCards.forEach { card ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(130.dp)
                                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                                                .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(24.dp))
                                                .padding(14.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.fillMaxSize(),
                                                verticalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Icon(Icons.Default.AutoAwesome, null, tint = CreamWhite, modifier = Modifier.size(16.dp))
                                                    Text("AUTO", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color.White.copy(alpha = 0.3f)))
                                                }
                                                Column {
                                                    Text(card.first.uppercase(), style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color.White))
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(card.second.uppercase(), style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f)))
                                                }
                                            }
                                        }
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                                        .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(24.dp))
                                        .padding(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Cardio 365".uppercase(), style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White))
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text("Every 4 days".uppercase(), style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f)))
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("0", style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = CreamWhite))
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White.copy(alpha = 0.1f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }

                                // Active Track tray
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                                        .background(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.White.copy(alpha = 0.06f),
                                                    Color.White.copy(alpha = 0.03f)
                                                )
                                            ),
                                            shape = RoundedCornerShape(24.dp)
                                        )
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("CURRENT SESSION", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color.White.copy(alpha = 0.35f)))
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("17:20", style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = CreamWhite))
                                            Text("Legs focus · In progress", style = TextStyle(fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f)))
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .background(CreamWhite, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.ArrowOutward, null, tint = Color.Black, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Carousel dots
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                cinematicLines.forEachIndexed { i, _ ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .background(
                                if (i <= index) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.15f),
                                RoundedCornerShape(2.dp)
                            )
                    )
                }
            }

            // Onboarding Bottom action slider buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                GlassAction(
                    onClick = {
                        playSound(1000f, 0.25f)
                        onFinish()
                    },
                    icon = Icons.Default.ArrowOutward,
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("onboarding_continue")
                ) {
                    Text("Continue", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = (-0.5).sp))
                }

                GlassAction(
                    onClick = {
                        playSound(1100f, 0.25f)
                        onFinish()
                    },
                    icon = Icons.Default.AutoAwesome,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("onboarding_enter")
                ) {
                    Text("Enter", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = (-0.5).sp))
                }
            }
        }
    }
}

@Composable
fun RowScope.OnboardingWidgetRow(icon: ImageVector, label: String) {
    Row(
        modifier = Modifier
            .weight(1f)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(Color.White.copy(alpha = 0.08f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = CreamWhite, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f))
        )
    }
}

// MAIN TIMELINE SCREEN
@Composable
fun MainStream(
    viewModel: NoteViewModel,
    notesList: List<Note>
) {
    val search by viewModel.search
    val filter by viewModel.filter
    val focusMode by viewModel.focusMode
    val syncPulse by viewModel.syncPulse

    val context = LocalContext.current
    var showProfileSettingsDialog by remember { mutableStateOf(false) }

    // Setup Intent Launcher for recoverable Google Authentication dialogs
    val authResolutionLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.performDriveSync(context)
        }
    }

    // Setup Choose Document launcher to import backup files
    val importLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            viewModel.importBookFromFile(context, uri) { succeeded ->
                if (succeeded) {
                    android.widget.Toast.makeText(context, "Journal Book imported successfully!", android.widget.Toast.LENGTH_LONG).show()
                } else {
                    android.widget.Toast.makeText(context, "Import failed: File format not recognized.", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Process filters and query matching useMemo counterparts
    val filteredNotes = remember(notesList, search, filter) {
        val lower = search.trim().lowercase()
        notesList.filter { note ->
            val matchesSearch = lower.isEmpty() ||
                "${note.title} ${note.body} ${note.locationLabel} ${note.mood} ${note.preset}".lowercase().contains(lower)
            val matchesFilter = filter == "All" ||
                (filter == "Today" && viewModel.groupLabel(note.createdAt) == "Today") ||
                (filter == "Mood" && note.mood.isNotEmpty()) ||
                note.locationLabel == filter || note.mood == filter
            matchesSearch && matchesFilter
        }
    }

    // Grouping
    val groupedNotes = remember(filteredNotes) {
        val map = linkedMapOf<String, MutableList<Note>>()
        filteredNotes.forEach { note ->
            val label = viewModel.groupLabel(note.createdAt)
            if (!map.containsKey(label)) {
                map[label] = mutableListOf()
            }
            map[label]?.add(note)
        }
        map.entries.toList()
    }

    val latestNote = notesList.firstOrNull()

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
            // Status row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(NeonCyan, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "09:41 AM",
                        style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Book Reader Icon Trigger
                    Icon(
                        imageVector = Icons.Default.AutoStories,
                        contentDescription = "Read journal notes like a book",
                        tint = CreamWhite,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable {
                                viewModel.playSound(850f, 0.15f)
                                viewModel.bookReaderOpen.value = true
                            }
                            .testTag("read_book_trigger")
                    )

                    // Profile / Sync Settings Toggle
                    val isAuthCompleted by viewModel.authCompleted
                    Icon(
                        imageVector = if (isAuthCompleted) Icons.Default.CloudQueue else Icons.Default.AccountCircle,
                        contentDescription = "Verify Google accounts or local shares",
                        tint = if (isAuthCompleted) NeonCyan else Color.White.copy(alpha = 0.5f),
                        modifier = Modifier
                            .size(20.dp)
                            .clickable {
                                viewModel.playSound(650f, 0.12f)
                                showProfileSettingsDialog = true
                            }
                            .testTag("sync_settings_trigger")
                    )

                    // Audio Toggle
                    val soundOn by viewModel.audioState
                    Icon(
                        imageVector = if (soundOn) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        contentDescription = "Toggle Soundtrack",
                        tint = if (soundOn) CreamWhite else Color.White.copy(alpha = 0.3f),
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { viewModel.toggleGlobalAudio() }
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(5.dp).background(Color.White.copy(alpha = 0.7f), CircleShape))
                        Box(modifier = Modifier.size(width = 20.dp, height = 5.dp).background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(3.dp)))
                        Box(modifier = Modifier.size(width = 12.dp, height = 5.dp).background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(3.dp)))
                    }
                }
            }

            // Stream Title Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    SectionLabel("ATOMIC HEADACHES")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (focusMode) "Focus mode" else "Notes",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Black,
                            fontSize = 32.sp,
                            color = Color.White,
                            letterSpacing = (-0.5).sp
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (focusMode) "One primary action. Everything else steps back."
                        else if (notesList.isEmpty()) "Start small. Keep the signal."
                        else "One more line is enough.",
                        style = TextStyle(fontWeight = FontWeight.Light, fontSize = 14.sp, color = Color.White.copy(alpha = 0.5f))
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Filter list trigger
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                            .background(Color.White.copy(alpha = 0.05f))
                            .clickable {
                                viewModel.playSound(600f, 0.12f)
                                viewModel.filter.value = if (viewModel.filter.value == "All") "Today" else "All"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.FilterList, null, tint = Color.White.copy(alpha = 0.82f), modifier = Modifier.size(18.dp))
                    }

                    // Plus composer trigger
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .border(1.dp, CreamWhite.copy(alpha = 0.3f), CircleShape)
                            .background(CreamWhite.copy(alpha = 0.1f))
                            .clickable {
                                viewModel.openNewNote("Home")
                            }
                            .testTag("new_note_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, null, tint = CreamWhite, modifier = Modifier.size(20.dp))
                    }
                }
            }

            // Filters selector row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 12.dp)
            ) {
                items(FILTERS) { item ->
                    MiniPill(
                        active = filter == item,
                        text = item,
                        onClick = {
                            viewModel.playSound(650f, 0.12f)
                            if (item == "Analytics") {
                                viewModel.analyticsOpen.value = true
                            } else {
                                viewModel.filter.value = item
                            }
                        }
                    )
                }
            }

            // Search Bar Component
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(12.dp))
                BasicTextField(
                    value = search,
                    onValueChange = { viewModel.search.value = it },
                    textStyle = TextStyle(color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Light),
                    singleLine = true,
                    cursorBrush = SolidColor(CreamWhite),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("search_input"),
                    decorationBox = { innerTextField ->
                        if (search.isEmpty()) {
                            Text(
                                "Search notes, mood, location...",
                                style = TextStyle(color = Color.White.copy(alpha = 0.2f), fontSize = 15.sp, fontWeight = FontWeight.Light)
                            )
                        }
                        innerTextField()
                    }
                )
            }

            // Metric tracking stats
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filteredNotes.size} ARTIFACTS",
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, letterSpacing = 1.sp, color = Color.White.copy(alpha = 0.4f))
                )
                Text(
                    text = if (syncPulse > 0) "SYNCED [$syncPulse]" else "READY",
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, letterSpacing = 1.sp, color = Color.White.copy(alpha = 0.4f))
                )
            }

            // Note List Items Stream
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                if (notesList.isEmpty()) {
                    // Empty Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
                            .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(24.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            SectionLabel("EMPTY STATE")
                            Text("Begin first entry.", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color.White.copy(alpha = 0.9f)))
                            Text(
                                "The app is ready. Timestamp, day, and location will attach automatically.",
                                style = TextStyle(fontWeight = FontWeight.Light, fontSize = 14.sp, color = Color.White.copy(alpha = 0.5f))
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                LOCATIONS.take(3).forEach { loc ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                                            .clickable { viewModel.openNewNote(loc) }
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(loc.uppercase(), style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f)))
                                    }
                                }
                            }
                        }
                    }
                } else if (groupedNotes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
                            .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(24.dp))
                            .padding(24.dp)
                    ) {
                        Column {
                            Text("No matches.", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White.copy(alpha = 0.9f)))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Change the filter or search terms.", style = TextStyle(fontWeight = FontWeight.Light, fontSize = 14.sp, color = Color.White.copy(alpha = 0.5f)))
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        groupedNotes.forEach { (group, items) ->
                            item {
                                Text(
                                    text = group.uppercase(),
                                    style = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        letterSpacing = 3.sp,
                                        color = CreamWhite
                                    ),
                                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                                )
                            }
                            items(items) { note ->
                                NoteCard(
                                    note = note,
                                    viewModel = viewModel,
                                    onOpen = { viewModel.detailOpenId.value = note.id }
                                )
                            }
                        }
                    }
                }
            }

            // Bottom capture tray block
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(30.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.06f),
                                Color.White.copy(alpha = 0.03f)
                            )
                        ),
                        shape = RoundedCornerShape(30.dp)
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            SectionLabel("CURRENT CAPTURE LAYER")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (latestNote != null) viewModel.formatTime(latestNote.createdAt) else "17:20",
                                style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = CreamWhite)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (latestNote != null) "${latestNote.title.ifEmpty { "Untitled" }} · ${latestNote.locationLabel}".uppercase() else "LEGS FOCUS · IN PROGRESS",
                                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.1f))
                                .clickable { viewModel.openNewNote("Home") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ArrowOutward, null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(22.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .clickable {
                                    viewModel.playSound(650f, 0.12f)
                                    viewModel.focusMode.value = !viewModel.focusMode.value
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (focusMode) "Exit focus" else "Enter focus",
                                style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 2.sp, color = Color.White.copy(alpha = 0.8f))
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(22.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .clickable { viewModel.openNewNote("Home") },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "New note",
                                style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 2.sp, color = Color.White.copy(alpha = 0.8f))
                            )
                        }
                    }
                }
            }
        }
    }
}

// NOTE CARD timeline item representation
@Composable
fun NoteCard(
    note: Note,
    viewModel: NoteViewModel,
    onOpen: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.02f)
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = note.title.ifEmpty { "Drawing" },
                        style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, letterSpacing = (-0.5).sp, color = Color.White.copy(alpha = 0.95f))
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${viewModel.formatTime(note.createdAt)}  ·  ${viewModel.groupLabel(note.createdAt)}  ·  ${note.locationLabel}".uppercase(),
                        style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.White.copy(alpha = 0.40f))
                    )
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                        .background(Color.White.copy(alpha = 0.04f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ArrowOutward, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = note.body.ifEmpty { "Empty draft item" },
                style = TextStyle(fontWeight = FontWeight.Light, fontSize = 15.sp, lineHeight = 22.sp, color = Color.White.copy(alpha = 0.7f)),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            if (note.mood.isNotEmpty() || note.preset.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (note.mood.isNotEmpty()) {
                        Badge(text = note.mood)
                    }
                    if (note.preset.isNotEmpty()) {
                        Badge(text = note.preset)
                    }
                }
            }
        }
    }
}

// COMPOSER SHEET (Write, Context, Mood tabs)
@Composable
fun ComposerSheet(
    draft: Note,
    viewModel: NoteViewModel,
    onSave: () -> Unit,
    onClose: () -> Unit
) {
    var activeTab by remember { mutableStateOf("write") }
    val canSave = draft.title.trim().isNotEmpty() || draft.body.trim().isNotEmpty()

    Scaffold(
        containerColor = SlateBg,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    SectionLabel("WORKSPACE CONTEXT")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("FOCUS DRAFT", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color.White))
                }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                        .background(Color.White.copy(alpha = 0.05f))
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }

            // Tabs navigator row
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    Pair("write", "Write"),
                    Pair("context", "Context"),
                    Pair("tone", "Mood")
                ).forEach { (key, name) ->
                    val active = activeTab == key
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(if (active) Color.White else Color.Transparent)
                            .clickable {
                                viewModel.playSound(650f, 0.12f)
                                activeTab = key
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = name,
                            style = TextStyle(
                                fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = if (active) Color.Black else Color.White.copy(alpha = 0.6f)
                            )
                        )
                    }
                }
            }

            // Main Tab Workspace
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                when (activeTab) {
                    "write" -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Title block
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(28.dp))
                                    .padding(20.dp)
                            ) {
                                Text("PROMPT / HEADER", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.White.copy(alpha = 0.4f)))
                                Spacer(modifier = Modifier.height(10.dp))
                                BasicTextField(
                                    value = draft.title,
                                    onValueChange = { viewModel.updateDraftTitle(it) },
                                    textStyle = TextStyle(color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold),
                                    singleLine = true,
                                    cursorBrush = SolidColor(CreamWhite),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("draft_title_input"),
                                    decorationBox = { innerTextField ->
                                        if (draft.title.isEmpty()) {
                                            Text(
                                                "What is on your mind?",
                                                style = TextStyle(color = Color.White.copy(alpha = 0.2f), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                            )
                                        }
                                        innerTextField()
                                    }
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "CURSOR ACTIVE. FOCUS ON LOGICAL SUBSTANCE.",
                                    style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.White.copy(alpha = 0.3f))
                                )
                            }

                            // Body block
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(28.dp))
                                    .padding(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("ENTRY MATRIX", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.White.copy(alpha = 0.4f)))
                                    Box(
                                        modifier = Modifier
                                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                                            .background(Color.White.copy(alpha = 0.05f))
                                            .clickable { viewModel.useDeviceLocation() }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "DEVIATE TO GPS",
                                            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, letterSpacing = 1.sp, color = CreamWhite)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                BasicTextField(
                                    value = draft.body,
                                    onValueChange = { viewModel.updateDraftBody(it) },
                                    textStyle = TextStyle(color = Color.White.copy(alpha = 0.85f), fontSize = 16.sp, fontWeight = FontWeight.Light, lineHeight = 24.sp),
                                    cursorBrush = SolidColor(CreamWhite),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 100.dp)
                                        .testTag("draft_body_input"),
                                    decorationBox = { innerTextField ->
                                        if (draft.body.isEmpty()) {
                                            Text(
                                                "Begin capturing raw details. Keep the noise low.",
                                                style = TextStyle(color = Color.White.copy(alpha = 0.2f), fontSize = 16.sp, fontWeight = FontWeight.Light)
                                            )
                                        }
                                        innerTextField()
                                    }
                                )
                            }

                            // Intuition presets
                            Column {
                                SectionLabel("INTUITION PRESETS")
                                Spacer(modifier = Modifier.height(10.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(listOf(
                                        Triple("Morning thought", "Morning thought", "Clear, private, direct."),
                                        Triple("Idea", "Idea", "Capture the shape before it fades."),
                                        Triple("Reminder", "Reminder", "One sentence is enough."),
                                        Triple("Execution", "Execution Parameter", "Track operational velocity."),
                                        Triple("Retrospective", "Retrospective Analysis", "Evaluate structural shifts.")
                                    )) { (label, seed, hint) ->
                                        val active = draft.preset == label
                                        Box(
                                            modifier = Modifier
                                                .width(170.dp)
                                                .clip(RoundedCornerShape(24.dp))
                                                .border(
                                                    1.dp,
                                                    if (active) Color.White else Color.White.copy(alpha = 0.08f),
                                                    RoundedCornerShape(24.dp)
                                                )
                                                .background(
                                                    if (active) Color.White else Color.White.copy(alpha = 0.05f),
                                                    RoundedCornerShape(24.dp)
                                                )
                                                .clickable {
                                                    viewModel.playSound(800f, 0.12f)
                                                    viewModel.updateDraftPreset(label, seed, hint)
                                                }
                                                .padding(16.dp)
                                        ) {
                                            Column {
                                                Text(
                                                    text = label,
                                                    style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (active) Color.Black else Color.White)
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = hint,
                                                    style = TextStyle(fontWeight = FontWeight.Light, fontSize = 11.sp, color = if (active) Color.Black.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.40f))
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "context" -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(28.dp))
                                    .padding(20.dp)
                            ) {
                                Text("CHRONOLOGY", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.White.copy(alpha = 0.4f)))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(viewModel.formatTime(draft.createdAt), style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = CreamWhite))
                                Text(viewModel.formatDay(draft.createdAt).uppercase(), style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f)))
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(28.dp))
                                    .padding(20.dp)
                            ) {
                                Text("GEOGRAPHICAL MAP", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.White.copy(alpha = 0.4f)))
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    LOCATIONS.take(3).forEach { loc ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(44.dp)
                                                .clip(RoundedCornerShape(22.dp))
                                                .background(
                                                    if (draft.locationLabel == loc) Color.White else Color.White.copy(alpha = 0.05f)
                                                )
                                                .clickable {
                                                    viewModel.playSound(650f, 0.12f)
                                                    viewModel.updateDraftLocation(loc)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                loc,
                                                style = TextStyle(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = if (draft.locationLabel == loc) Color.Black else Color.White
                                                )
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    LOCATIONS.drop(3).forEach { loc ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(44.dp)
                                                .clip(RoundedCornerShape(22.dp))
                                                .background(
                                                    if (draft.locationLabel == loc) Color.White else Color.White.copy(alpha = 0.05f)
                                                )
                                                .clickable {
                                                    viewModel.playSound(650f, 0.12f)
                                                    viewModel.updateDraftLocation(loc)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                loc,
                                                style = TextStyle(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = if (draft.locationLabel == loc) Color.Black else Color.White
                                                )
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.LocationOn, null, tint = CreamWhite, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = (if (draft.deviceLocation.isNotEmpty()) "${draft.locationLabel} · ${draft.deviceLocation}" else draft.locationLabel).uppercase(),
                                        style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(28.dp))
                                    .padding(20.dp)
                            ) {
                                Text("RECENCY INDICES", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.White.copy(alpha = 0.4f)))
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("just now", "today", "yesterday").forEach { tag ->
                                        Badge(text = tag)
                                    }
                                }
                            }
                        }
                    }
                    "tone" -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(28.dp))
                                    .padding(20.dp)
                            ) {
                                Text("SENSORY MOOD CHIP", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.White.copy(alpha = 0.4f)))
                                Spacer(modifier = Modifier.height(12.dp))
                                MOODS.chunked(3).forEach { rowMoods ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rowMoods.forEach { mood ->
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(44.dp)
                                                    .clip(RoundedCornerShape(22.dp))
                                                    .background(
                                                        if (draft.mood == mood) Color.White else Color.White.copy(alpha = 0.05f)
                                                    )
                                                    .clickable {
                                                        viewModel.playSound(650f, 0.12f)
                                                        viewModel.updateDraftMood(mood)
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    mood,
                                                    style = TextStyle(
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        color = if (draft.mood == mood) Color.Black else Color.White
                                                    )
                                                )
                                            }
                                        }
                                        if (rowMoods.size < 3) {
                                            Spacer(modifier = Modifier.weight((3 - rowMoods.size).toFloat()))
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "OPTIONAL ATTRIBUTE. ZERO SETUP REQUIRED.",
                                    style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.White.copy(alpha = 0.3f))
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(28.dp))
                                    .padding(20.dp)
                            ) {
                                Text("METRIC ENVELOPE PREVIEW", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.White.copy(alpha = 0.4f)))
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(22.dp))
                                        .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(22.dp))
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = draft.mood.ifEmpty { "Unset" }.uppercase(),
                                            style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 17.sp, color = CreamWhite)
                                        )
                                        Text(
                                            text = "Synchronized tone model",
                                            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.White.copy(alpha = 0.40f))
                                        )
                                    }
                                    Icon(Icons.Default.NightsStay, null, tint = NeonViolet, modifier = Modifier.size(22.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Buttons Actions Slate
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                GlassAction(
                    onClick = {
                        if (canSave) onSave()
                    },
                    icon = Icons.Default.Check,
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("save_note_button")
                ) {
                    Text("Save note", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = (-0.5).sp))
                }

                GlassAction(
                    onClick = {
                        viewModel.playSound(500f, 0.12f)
                        onClose()
                    },
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Back", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = (-0.5).sp))
                }
            }
        }
    }
}

// NOTE DETAIL VIEW
@Composable
fun DetailSheet(
    note: Note,
    viewModel: NoteViewModel,
    onClose: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Scaffold(
        containerColor = SlateBg,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    SectionLabel("LOGGED ARTIFACT")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = note.title.ifEmpty { "Drawing" },
                        style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color.White.copy(alpha = 0.95f)),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                        .background(Color.White.copy(alpha = 0.05f))
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }

            // Body Display
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(28.dp))
                        .padding(20.dp)
                ) {
                    Text("DRAFT CONTENT", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.White.copy(alpha = 0.4f)))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = note.body.ifEmpty { "No content." },
                        style = TextStyle(fontWeight = FontWeight.Light, fontSize = 16.sp, lineHeight = 24.sp, color = Color.White.copy(alpha = 0.8f))
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(24.dp))
                            .padding(16.dp)
                    ) {
                        Text("GPS ANCHOR", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color.White.copy(alpha = 0.4f)))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(note.locationLabel.uppercase(), style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = CreamWhite))
                        if (note.deviceLocation.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(note.deviceLocation, style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color.White.copy(alpha = 0.3f)))
                        }
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(24.dp))
                            .padding(16.dp)
                    ) {
                        Text("SENSORY PARAM", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color.White.copy(alpha = 0.4f)))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(note.mood.ifEmpty { "Unset" }.uppercase(), style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp, color = NeonViolet))
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(24.dp))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("TIMEFRAME", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.White.copy(alpha = 0.4f)))
                    Text(
                        text = "${viewModel.formatTime(note.createdAt)}  ·  ${viewModel.formatDay(note.createdAt)}".uppercase(),
                        style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                    )
                }
            }

            // Bottom Actions Slate
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                GlassAction(
                    onClick = onEdit,
                    icon = Icons.Default.Edit,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("edit_note_button")
                ) {
                    Text("Edit", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = (-0.5).sp))
                }

                GlassAction(
                    onClick = onDelete,
                    icon = Icons.Default.Delete,
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, Color.Red.copy(alpha = 0.2f), RoundedCornerShape(32.dp))
                        .testTag("delete_note_button")
                ) {
                    Text("Delete", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = (-0.5).sp, color = Color.Red.copy(alpha = 0.8f)))
                }
            }
        }
    }
}

// ANALYTICS PANEL
@Composable
fun AnalyticsDashboard(
    notes: List<Note>,
    viewModel: NoteViewModel,
    onClose: () -> Unit
) {
    val total = notes.size
    val topLocation = remember(notes) {
        notes.groupBy { it.locationLabel }
            .maxByOrNull { it.value.size }?.key ?: "None"
    }
    val topMood = remember(notes) {
        notes.filter { it.mood.isNotEmpty() }
            .groupBy { it.mood }
            .maxByOrNull { it.value.size }?.key ?: "None"
    }

    Scaffold(
        containerColor = SlateBg,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    SectionLabel("SYSTEM ANALYTICS")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("METRICS DECK", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color.White))
                }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                        .background(Color.White.copy(alpha = 0.05f))
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }

            // Metrics Cards Column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(28.dp))
                        .padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total Composed Artifacts", style = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 13.sp, color = Color.White.copy(alpha = 0.4f)))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("$total", style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 36.sp, color = CreamWhite))
                        }
                        Icon(Icons.Default.TrendingUp, null, tint = NeonCyan, modifier = Modifier.size(36.dp))
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(28.dp))
                            .padding(20.dp)
                    ) {
                        Text("Dominant Anchor", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.White.copy(alpha = 0.4f)))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(topLocation.uppercase(), style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 17.sp, color = CreamWhite))
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(28.dp))
                            .padding(20.dp)
                    ) {
                        Text("Prevailing Mood", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.White.copy(alpha = 0.4f)))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(topMood.uppercase(), style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 17.sp, color = NeonViolet))
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(28.dp))
                        .padding(20.dp)
                ) {
                    SectionLabel("CHRONOLOGICAL HISTORY")
                    Spacer(modifier = Modifier.height(16.dp))
                    if (notes.isEmpty()) {
                        Text("No data logged.", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Color.White.copy(alpha = 0.3f)))
                    } else {
                        notes.take(5).forEachIndexed { i, note ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = viewModel.formatDay(note.createdAt).uppercase(),
                                    style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f))
                                )
                                Text(
                                    text = note.title.ifEmpty { "Drawing" },
                                    style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White.copy(alpha = 0.82f)),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (i < 4 && i < notes.size - 1) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(Color.White.copy(alpha = 0.04f))
                                )
                            }
                        }
                    }
                }
            }

            // Back Action button
            Spacer(modifier = Modifier.height(16.dp))
            GlassAction(
                onClick = onClose,
                icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Return to Stream", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = (-0.5).sp))
            }
        }
    }
}

// ----------------------------------------------------
// ACCOUNT SETTINGS DIALOG (GOOGLE SYNC & IMPORT/EXPORT)
// ----------------------------------------------------
@Composable
fun AccountSettingsDialog(
    viewModel: NoteViewModel,
    onDismiss: () -> Unit,
    onTriggerImport: () -> Unit,
    onTriggerResolution: (android.content.Intent) -> Unit,
    playSound: (Float, Float) -> Unit
) {
    val context = LocalContext.current
    val authCompleted by viewModel.authCompleted
    val userEmail by viewModel.userEmail
    val userDisplayName by viewModel.userDisplayName
    val syncStatus by viewModel.syncStatus

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Cloud Sync & Portability",
                style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (authCompleted) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(CreamWhite),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (userDisplayName ?: "K").take(1).uppercase(),
                                    style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                                )
                            }
                            Column {
                                Text(
                                    userDisplayName ?: "Journal Keeper",
                                    style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                                )
                                Text(
                                    userEmail ?: "",
                                    style = TextStyle(fontSize = 12.sp, color = MutedText)
                                )
                            }
                        }
                    }

                    if (syncStatus.isNotEmpty()) {
                        Text(
                            text = syncStatus,
                            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = CreamWhite),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }

                    Button(
                        onClick = {
                            playSound(950f, 0.15f)
                            viewModel.performDriveSync(context) { intent ->
                                onTriggerResolution(intent)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CreamWhite, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("sync_drive_now")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CloudSync, null, modifier = Modifier.size(16.dp))
                            Text("Sync with Google Drive", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.05f)),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Warning, null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                            Column {
                                Text(
                                    "Guest Account (No Cloud Sync)",
                                    style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "Your notes are only stored on this device. Deleting the app deletes all of your data.",
                                    style = TextStyle(fontSize = 11.sp, color = MutedText)
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            playSound(800f, 0.15f)
                            onDismiss()
                            viewModel.sharedPrefs.edit().remove("auth_skipped").apply()
                            viewModel.authSkipped.value = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CreamWhite, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("dialog_google_link")
                    ) {
                        Text("Connect Google Account", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                Divider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            playSound(700f, 0.12f)
                            viewModel.exportBookToFile(context) { error ->
                                if (error != null) {
                                    android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_LONG).show()
                                } else {
                                    android.widget.Toast.makeText(context, "Journal exported successfully!", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        modifier = Modifier.weight(1f).testTag("dialog_export_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Download, null, modifier = Modifier.size(14.dp))
                            Text("Export JSON", fontSize = 12.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            playSound(700f, 0.12f)
                            onTriggerImport()
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        modifier = Modifier.weight(1f).testTag("dialog_import_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Upload, null, modifier = Modifier.size(14.dp))
                            Text("Import JSON", fontSize = 12.sp)
                        }
                    }
                }

                if (authCompleted) {
                    Divider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)
                    TextButton(
                        onClick = {
                            playSound(300f, 0.25f)
                            viewModel.logOut(context)
                            onDismiss()
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally).testTag("dialog_logout_button")
                    ) {
                        Text("Sign Out Account", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("dialog_close")) {
                Text("Close", color = CreamWhite, fontSize = 13.sp)
            }
        },
        containerColor = SlateCard,
        titleContentColor = Color.White,
        textContentColor = MutedText
    )
}
