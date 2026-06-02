package id.ac.pnm.quizbattleapp.feature.game

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.ac.pnm.quizbattleapp.data.model.GameResult

@Composable
fun GameScreen(
    roomId: String,
    onGameFinished: (GameResult) -> Unit,
    viewModel: GameViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Navigasi ke result saat selesai
    LaunchedEffect(state.isFinished, state.result) {
        if (state.isFinished && state.result != null) {
            onGameFinished(state.result!!)
        }
    }

    when {
        state.isLoading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("Memuat soal...", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        state.error != null -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text  = state.error!!,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.padding(24.dp)
                )
            }
        }

        state.questions.isNotEmpty() -> {
            GameContent(state = state, onAnswer = viewModel::answerQuestion)
        }
    }
}

@Composable
private fun GameContent(
    state: GameUiState,
    onAnswer: (Int) -> Unit
) {
    val currentQuestion = state.questions[state.currentIndex]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // ── Scoreboard ────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            ScoreColumn(label = "Kamu", score = state.myScore)
            Text("VS", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            ScoreColumn(label = state.opponentName.ifBlank { "Lawan" }, score = state.opponentScore)
        }

        Spacer(Modifier.height(16.dp))

        // ── Progress & Timer ──────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                "Soal ${state.currentIndex + 1} / ${state.questions.size}",
                style = MaterialTheme.typography.labelLarge
            )

            // Timer — merah kalau ≤ 5 detik
            val timerColor = if (state.timeLeft <= 5)
                MaterialTheme.colorScheme.error
            else
                MaterialTheme.colorScheme.primary

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(50))
                    .background(timerColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text  = "${state.timeLeft}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 18.sp
                )
            }
        }

        LinearProgressIndicator(
            progress  = { (state.currentIndex.toFloat() + 1) / state.questions.size },
            modifier  = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clip(RoundedCornerShape(4.dp))
        )

        Spacer(Modifier.height(24.dp))

        // ── Pertanyaan ────────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(16.dp)
        ) {
            Text(
                text      = currentQuestion.text,
                style     = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
                textAlign  = TextAlign.Center,
                modifier   = Modifier.padding(24.dp)
            )
        }

        Spacer(Modifier.height(24.dp))

        // ── Pilihan Jawaban ───────────────────────────────────────────
        currentQuestion.options.forEachIndexed { index, option ->
            val isSelected  = state.selectedIndex == index
            val isCorrect   = index == currentQuestion.correctAnswerIndex
            val hasAnswered = state.selectedIndex != null

            val containerColor = when {
                hasAnswered && isCorrect  -> Color(0xFF4CAF50)
                hasAnswered && isSelected -> MaterialTheme.colorScheme.error
                else                     -> MaterialTheme.colorScheme.surface
            }

            val borderColor = when {
                hasAnswered && isCorrect  -> Color(0xFF4CAF50)
                hasAnswered && isSelected -> MaterialTheme.colorScheme.error
                else                     -> MaterialTheme.colorScheme.outline
            }

            OutlinedButton(
                onClick  = { if (!hasAnswered) onAnswer(index) },
                enabled  = !hasAnswered,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .border(
                        width = 2.dp,
                        color = borderColor,
                        shape = RoundedCornerShape(12.dp)
                    ),
                shape  = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor         = containerColor,
                    disabledContainerColor = containerColor
                )
            ) {
                Text(
                    text   = option,
                    color  = if (hasAnswered && (isCorrect || isSelected)) Color.White
                             else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun ScoreColumn(label: String, score: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(
            text       = "$score",
            fontWeight = FontWeight.Bold,
            fontSize   = 24.sp,
            color      = MaterialTheme.colorScheme.primary
        )
    }
}