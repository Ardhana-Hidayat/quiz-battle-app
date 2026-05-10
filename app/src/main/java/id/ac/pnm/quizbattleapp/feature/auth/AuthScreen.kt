package id.ac.pnm.quizbattleapp.feature.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var isLogin   by remember { mutableStateOf(true) }
    var name      by remember { mutableStateOf("") }
    var email     by remember { mutableStateOf("") }
    var password  by remember { mutableStateOf("") }
    var showPass  by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Navigasi setelah sukses
    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) onAuthSuccess()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .align(Alignment.Center),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Judul
            Text(
                text = if (isLogin) "Selamat Datang 👋" else "Buat Akun Baru",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (isLogin) "Masuk untuk mulai bermain"
                else "Daftar dan tantang pemain lain",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            // Field Nama (hanya saat Register)
            AnimatedVisibility(visible = !isLogin) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Tampilan") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = if (showPass) VisualTransformation.None
                else PasswordVisualTransformation(),
                trailingIcon = {
                    TextButton(onClick = { showPass = !showPass }) {
                        Text(if (showPass) "Sembunyikan" else "Tampilkan",
                            style = MaterialTheme.typography.labelSmall)
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Error message
            AnimatedVisibility(visible = uiState is AuthUiState.Error) {
                Text(
                    text = (uiState as? AuthUiState.Error)?.message.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.height(4.dp))

            // Tombol utama
            Button(
                onClick = {
                    if (isLogin) viewModel.login(email, password)
                    else viewModel.register(email, password, name)
                },
                enabled = uiState !is AuthUiState.Loading,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (uiState is AuthUiState.Loading)
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                else
                    Text(if (isLogin) "Masuk" else "Daftar", fontWeight = FontWeight.SemiBold)
            }

            // Toggle login / register
            TextButton(
                onClick = { isLogin = !isLogin; viewModel.resetState() },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    if (isLogin) "Belum punya akun? Daftar sekarang"
                    else "Sudah punya akun? Masuk"
                )
            }
        }
    }
}