package id.ac.pnm.quizbattleapp.feature.result

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.ac.pnm.quizbattleapp.data.model.GameResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    result: GameResult,
    onPlayAgain: () -> Unit,
    onHome: () -> Unit
) {
    val isOnline   = result.mode == "online"
    val winnerText = when {
        !isOnline          -> "Latihan Selesai!"
        result.isWinner    -> "Kamu Menang! 🎉"
        result.score == result.opponentScore -> "Seri!"
        else               -> "Kamu Kalah :("
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Hasil Kuis") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon hasil
            Icon(
                imageVector = if (!isOnline || result.isWinner)
                    Icons.Default.EmojiEvents
                else
                    Icons.Default.SentimentDissatisfied,
                contentDescription = null,
                tint   = if (!isOnline || result.isWinner) Color(0xFFFFC107) else Color.Gray,
                modifier = Modifier.size(80.dp)
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text       = winnerText,
                fontSize   = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign  = TextAlign.Center,
                color      = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(32.dp))

            // Card skor
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Skormu", style = MaterialTheme.typography.labelLarge)
                    Text(
                        text       = "${result.score}",
                        fontSize   = 56.sp,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text  = "${result.correctAnswers} / ${result.totalQuestions} jawaban benar",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Skor lawan (hanya mode online)
                    if (isOnline && result.opponentName.isNotBlank()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                        Text("Skor ${result.opponentName}", style = MaterialTheme.typography.labelLarge)
                        Text(
                            text       = "${result.opponentScore}",
                            fontSize   = 36.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick  = onPlayAgain,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Main Lagi")
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick  = onHome,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Kembali ke Beranda")
            }
        }
    }
}