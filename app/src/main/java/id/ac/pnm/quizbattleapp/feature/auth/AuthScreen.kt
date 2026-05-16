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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var isLogin  by remember { mutableStateOf(true) }
    var name     by remember { mutableStateOf("") }
    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }

    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state) {
        if (state is AuthState.Success) onAuthSuccess()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isLogin) "Selamat Datang Ã°Å¸â€˜â€¹" else "Buat Akun",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(24.dp))

        AnimatedVisibility(!isLogin) {
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Nama") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = email, onValueChange = { email = it },
            label = { Text("Email") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password, onValueChange = { password = it },
            label = { Text("Password") }, singleLine = true,
            visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                TextButton(onClick = { showPass = !showPass }) {
                    Text(if (showPass) "Sembunyikan" else "Tampilkan",
                        style = MaterialTheme.typography.labelSmall)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        // Error
        AnimatedVisibility(state is AuthState.Error) {
            Text(
                text  = (state as? AuthState.Error)?.message.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                if (isLogin) viewModel.login(email, password)
                else viewModel.register(email, password, name)
            },
            enabled  = state !is AuthState.Loading,
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            if (state is AuthState.Loading)
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary)
            else
                Text(if (isLogin) "Masuk" else "Daftar", fontWeight = FontWeight.SemiBold)
        }

        TextButton(onClick = { isLogin = !isLogin; viewModel.resetState() }) {
            Text(if (isLogin) "Belum punya akun? Daftar" else "Sudah punya akun? Masuk")
        }
    }
}