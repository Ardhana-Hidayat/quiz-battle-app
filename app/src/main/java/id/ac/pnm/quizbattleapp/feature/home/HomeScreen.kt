package id.ac.pnm.quizbattleapp.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.OnlinePrediction
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.ac.pnm.quizbattleapp.data.model.GameMode
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOnlineBattle: () -> Unit,
    onSoloTraining: () -> Unit,
    onLeaderboard: () -> Unit,
    onHistory: () -> Unit,
    onLogout: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showLogoutDialog by remember { mutableStateOf(false) }
    val gameModes = GameMode.entries.toList()

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title   = { Text("Keluar?") },
            text    = { Text("Kamu akan keluar dari akun ini.") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    viewModel.logout()
                    onLogout()
                }) { Text("Keluar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Batal") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quiz Battle", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 20.dp)
        ) {

            item {
                ProfileCard(
                    displayName = uiState.user?.displayName ?: "Pemain",
                    email       = uiState.user?.email ?: ""
                )
            }

            item {
                Text(
                    text  = "Pilih Mode atau Fitur",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            items(gameModes) { mode ->
                GameModeCard(
                    mode = mode,
                    onClick = {
                        when (mode) {
                            GameMode.ONLINE_BATTLE -> onOnlineBattle()
                            GameMode.SOLO_TRAINING -> onSoloTraining()
                            GameMode.LEADERBOARD   -> onLeaderboard()
                            GameMode.HISTORY       -> onHistory()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun GameModeCard(mode: GameMode, onClick: () -> Unit) {
    val (containerColor, contentColor) = when (mode) {
        GameMode.ONLINE_BATTLE -> Color(0xFFD6EAFF) to Color(0xFF1A5FA8)
        GameMode.SOLO_TRAINING -> Color(0xFFD8F5E4) to Color(0xFF1B7A45) 
        GameMode.LEADERBOARD   -> Color(0xFFFFF3CC) to Color(0xFF8A6200)  
        GameMode.HISTORY       -> Color(0xFFECDFFF) to Color(0xFF5B2D9E)  
    }

    val vectorIcon = when (mode) {
        GameMode.ONLINE_BATTLE -> Icons.Default.OnlinePrediction
        GameMode.SOLO_TRAINING -> Icons.Default.Psychology
        GameMode.LEADERBOARD   -> Icons.Default.Leaderboard
        GameMode.HISTORY       -> Icons.Default.History
    }

    Card(
        onClick  = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(contentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = vectorIcon,
                    contentDescription = mode.label,
                    tint = contentColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text  = mode.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                Text(
                    text  = mode.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.75f)
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Buka Mode",
                tint = contentColor
            )
        }
    }
}

@Composable
fun ProfileCard(displayName: String, email: String) {

    val initials = displayName
        .trim()
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }
        .ifBlank { "?" }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color(0xFFD6EAFF)),  // biru soft senada
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1A5FA8)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = initials,
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text       = "Halo, $displayName!",
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color      = Color(0xFF1A5FA8)
                )
                Text(
                    text  = email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF1A5FA8).copy(alpha = 0.7f)
                )
            }
        }
    }
}