package id.ac.pnm.quizbattleapp.feature.result

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    val isOnline = result.mode == "online"

    val (icon, iconTint, winnerText) = when {
        !isOnline -> Triple(
            Icons.Default.EmojiEvents,
            Color(0xFFFFC107),
            "Latihan Selesai!"
        )
        result.isWinner -> Triple(
            Icons.Default.EmojiEvents,
            Color(0xFFFFC107),
            "Kamu Menang! 🎉"
        )
        result.score == result.opponentScore -> Triple(
            Icons.Default.RemoveCircleOutline,
            MaterialTheme.colorScheme.primary,
            "Seri!"
        )
        else -> Triple(
            Icons.Default.SentimentDissatisfied,
            Color(0xFF9E9E9E),
            "Kamu Kalah :("
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hasil Kuis", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onHome) {
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // ── Ikon hasil ────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = icon,
                    contentDescription = null,
                    tint               = iconTint,
                    modifier           = Modifier.size(56.dp)
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── Teks hasil ────────────────────────────────────────────
            Text(
                text       = winnerText,
                fontSize   = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign  = TextAlign.Center,
                color      = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(28.dp))

            // ── Card skor ─────────────────────────────────────────────
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier            = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text  = "Skormu",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text       = "${result.score}",
                        fontSize   = 64.sp,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.primary,
                        textAlign  = TextAlign.Center,
                        modifier   = Modifier.fillMaxWidth()
                    )

                    Text(
                        text      = "${result.correctAnswers} / ${result.totalQuestions} jawaban benar",
                        style     = MaterialTheme.typography.bodyMedium,
                        color     = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )

                    // ── Skor lawan (hanya online) ─────────────────────
                    if (isOnline && result.opponentName.isNotBlank()) {
                        HorizontalDivider(
                            modifier  = Modifier.padding(vertical = 16.dp),
                            color     = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                        )

                        Text(
                            text  = "Skor ${result.opponentName}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text       = "${result.opponentScore}",
                            fontSize   = 40.sp,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.secondary,
                            textAlign  = TextAlign.Center,
                            modifier   = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // ── Tombol aksi ───────────────────────────────────────────
            Button(
                onClick  = onPlayAgain,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Main Lagi", fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick  = onHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Kembali ke Beranda")
            }
        }
    }
}