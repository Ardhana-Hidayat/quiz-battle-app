package id.ac.pnm.quizbattleapp.feature.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.ac.pnm.quizbattleapp.data.model.GameResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoloScreen(
    onNavigateBack: () -> Unit,
    onGameFinished: (GameResult) -> Unit,
    viewModel: SoloViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.restartGame()
    }

    LaunchedEffect(uiState.isFinished, uiState.result) {
        val result = uiState.result
        if (uiState.isFinished && result != null) {
            onGameFinished(result)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Solo Training", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                uiState.questions.isNotEmpty() && !uiState.isFinished -> {
                    SoloQuizContent(
                        uiState  = uiState,
                        onAnswer = viewModel::answerQuestion
                    )
                }
            }
        }
    }
}

@Composable
private fun SoloQuizContent(
    uiState: SoloUiState,
    onAnswer: (Int) -> Unit
) {
    val currentQuestion = uiState.questions[uiState.currentQuestionIndex]
    val totalQuestions  = uiState.questions.size
    val currentIndex    = uiState.currentQuestionIndex

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(4.dp))

        // ── Progress header ───────────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text  = "Soal ${currentIndex + 1} dari $totalQuestions",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text       = "Skor: ${uiState.score}",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.primary
                )
            }

            // Badge skor dengan avatar-style seperti di HomeScreen
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = "${currentIndex + 1}/$totalQuestions",
                    style      = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // Progress bar
        LinearProgressIndicator(
            progress  = { (currentIndex + 1).toFloat() / totalQuestions },
            modifier  = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
        )

        // ── Kartu soal ────────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(16.dp),
            colors   = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Text(
                text       = currentQuestion.text,
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
                textAlign  = TextAlign.Center,
                color      = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier   = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            )
        }

        // ── Pilihan jawaban ───────────────────────────────────────────
        Text(
            text  = "Pilih jawaban:",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        currentQuestion.options.forEachIndexed { index, option ->
            Card(
                onClick   = { onAnswer(index) },
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Badge huruf opsi (A, B, C, D)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text       = ('A' + index).toString(),
                            style      = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text  = option,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}