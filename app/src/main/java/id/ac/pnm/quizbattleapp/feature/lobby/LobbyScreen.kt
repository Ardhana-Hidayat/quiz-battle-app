package id.ac.pnm.quizbattleapp.feature.lobby

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.ac.pnm.quizbattleapp.data.model.GameRoom
import id.ac.pnm.quizbattleapp.data.model.RoomPlayer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LobbyScreen(
    onGameStart: (roomId: String) -> Unit,
    onBack: () -> Unit,
    viewModel: LobbyViewModel = hiltViewModel()
) {
    val state          by viewModel.state.collectAsStateWithLifecycle()
    val availableRooms by viewModel.availableRooms.collectAsStateWithLifecycle()
    // val showWaiting = state is LobbyUiState.Waiting || state is LobbyUiState.Ready
    // Navigasi ke game otomatis
    LaunchedEffect(state) {
        if (state is LobbyUiState.NavigateToGame)
            onGameStart((state as LobbyUiState.NavigateToGame).roomId)
    }

    // Tampilkan WaitingSheet saat Waiting / Ready
    val showWaiting = state is LobbyUiState.Waiting || state is LobbyUiState.Ready

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Online Battle", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        // FAB — buat room baru seperti tombol "+" WhatsApp
        floatingActionButton = {
            AnimatedVisibility(
                visible = !showWaiting && state !is LobbyUiState.Loading,
                enter   = fadeIn(),
                exit    = fadeOut()
            ) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.createRoom() },
                    icon    = { Icon(Icons.Default.Add, contentDescription = "Buat Room") },
                    text    = { Text("Buat Room", fontWeight = FontWeight.SemiBold) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor   = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                // ── Loading ────────────────────────────────────────────────
                state is LobbyUiState.Loading -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }

                // ── Waiting Room sheet (overlay) ───────────────────────────
                showWaiting -> {
                    val room = when (val s = state) {
                        is LobbyUiState.Waiting -> s.room
                        is LobbyUiState.Ready   -> s.room
                        else -> return@Box
                    }
                    WaitingRoomContent(
                        room      = room,
                        isReady   = state is LobbyUiState.Ready,
                        isCreator = room.player1.uid == viewModel.currentUid,
                        onStart   = { viewModel.startGame(room.roomId) },
                        onCancel      = { viewModel.backToList() },       // tombol back biasa
                        onCancelDelete = { viewModel.cancelAndDeleteRoom() }  // tombol "Batalkan Room"
                    )
                }

                // ── Daftar room tersedia ───────────────────────────────────
                else -> {
                    RoomListContent(
                        rooms      = availableRooms,
                        error      = (state as? LobbyUiState.Error)?.message,
                        onJoinRoom = { viewModel.joinRoom(it) }
                    )
                }
            }
        }
    }
}

// ── Daftar Room ──────────────────────────────────────────────────────────────
@Composable
private fun RoomListContent(
    rooms: List<GameRoom>,
    error: String?,
    onJoinRoom: (String) -> Unit
) {
    LazyColumn(
        modifier      = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start  = 16.dp,
            end    = 16.dp,
            top    = 12.dp,
            bottom = 88.dp  // beri ruang FAB
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Error banner
        if (error != null) {
            item {
                Card(
                    shape  = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text     = error,
                        color    = MaterialTheme.colorScheme.onErrorContainer,
                        style    = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        // Header
        item {
            Text(
                text  = "Room Tersedia",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text  = if (rooms.isEmpty()) "Belum ada room. Buat room baru lewat tombol + di bawah."
                        else "${rooms.size} room menunggu pemain",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
            Spacer(Modifier.height(4.dp))
        }

        // Empty state
        if (rooms.isEmpty()) {
            item { EmptyRoomState() }
        }

        // List room
        items(rooms, key = { it.roomId }) { room ->
            RoomCard(room = room, onJoin = { onJoinRoom(room.roomId) })
        }
    }
}

// ── Room Card ────────────────────────────────────────────────────────────────
@Composable
private fun RoomCard(room: GameRoom, onJoin: () -> Unit) {
    Card(
        onClick   = onJoin,
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
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Avatar host
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text  = room.player1.displayName.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Info room
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text       = room.player1.displayName,
                    style      = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text  = room.roomId,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Badge + tombol join
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text  = "1/2",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Text(
                    text  = "Ketuk untuk join →",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// ── Empty State ──────────────────────────────────────────────────────────────
@Composable
private fun EmptyRoomState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("🎮", style = MaterialTheme.typography.displaySmall)
            Text(
                text      = "Belum ada room aktif",
                style     = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Text(
                text      = "Tekan tombol + untuk membuat room baru\ndan ajak temanmu bergabung",
                style     = MaterialTheme.typography.bodySmall,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── Waiting Room Content ─────────────────────────────────────────────────────
@Composable
private fun WaitingRoomContent(
    room: GameRoom,
    isReady: Boolean,
    isCreator: Boolean,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    onCancelDelete: () -> Unit
) {
    val clipboard = LocalClipboardManager.current

    LazyColumn(
        modifier       = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Room ID Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(16.dp),
                colors   = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text  = "Room ID",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text       = room.roomId,
                            style      = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text  = "Bagikan ID ini ke temanmu",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                        )
                    }
                    IconButton(onClick = {
                        clipboard.setText(AnnotatedString(room.roomId))
                    }) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Salin",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // Label pemain
        item {
            Text(
                text       = "Pemain dalam Room",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Slot pemain
        item {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PlayerSlot(player = room.player1, modifier = Modifier.weight(1f))
                PlayerSlot(player = room.player2, modifier = Modifier.weight(1f))
            }
        }

        // Tombol aksi
        item {
            if (isCreator) {
                if (isReady) {
                    Button(
                        onClick  = onStart,
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Text("🚀  Mulai Battle!", fontWeight = FontWeight.Bold)
                    }
                } else {
                    OutlinedButton(
                        onClick  = {},
                        enabled  = false,
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Menunggu lawan masuk...")
                    }
                }
            } else {
                OutlinedButton(
                    onClick  = {},
                    enabled  = false,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text("Menunggu host memulai...")
                }
            }
        }

        // Tombol batalkan
        item {
            OutlinedButton(
                onClick  = onCancel,        // hanya kembali ke list, room tetap ada
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("← Kembali ke Daftar Room")
            }

            TextButton(
                onClick  = onCancelDelete,  // hapus room sekalian
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Batalkan & Hapus Room", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

// ── Player Slot ──────────────────────────────────────────────────────────────
@Composable
private fun PlayerSlot(player: RoomPlayer, modifier: Modifier = Modifier) {
    val filled = player.uid.isNotBlank()
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(
            containerColor = if (filled) MaterialTheme.colorScheme.secondaryContainer
                             else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier            = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (filled) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = if (filled) player.displayName.take(1).uppercase() else "?",
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color      = if (filled) MaterialTheme.colorScheme.onSecondary
                                 else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text       = if (filled) player.displayName else "Menunggu...",
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = if (filled) FontWeight.SemiBold else FontWeight.Normal,
                color      = if (filled) MaterialTheme.colorScheme.onSecondaryContainer
                             else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign  = TextAlign.Center
            )
        }
    }
}