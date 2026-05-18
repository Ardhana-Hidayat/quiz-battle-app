package id.ac.pnm.quizbattleapp.feature.game

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoloScreen(
    onNavigateBack: () -> Unit,
    viewModel: SoloViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Solo Training") },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.isFinished) {
                // Tampilan Selesai
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Kuis Selesai!", style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Skor Anda:", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "${uiState.score}",
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(onClick = { viewModel.restartGame() }) {
                        Text("Main Lagi")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = onNavigateBack) {
                        Text("Kembali ke Beranda")
                    }
                }
            } else {
                // Tampilan Soal
                val currentQuestion = uiState.questions[uiState.currentQuestionIndex]
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Soal ${uiState.currentQuestionIndex + 1}/${uiState.questions.size}")
                        Text("Skor: ${uiState.score}", fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Pertanyaan
                    Text(
                        text = currentQuestion.text,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.height(48.dp))
                    
                    // Opsi Jawaban
                    currentQuestion.options.forEachIndexed { index, option ->
                        Button(
                            onClick = { viewModel.answerQuestion(index) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Text(option)
                        }
                    }
                }
            }
        }
    }
}
