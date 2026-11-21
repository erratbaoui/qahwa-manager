package com.example.qahwamanager.ui.cloud

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.qahwamanager.R
import com.example.qahwamanager.data.cloud.CloudAuthRepository
import com.example.qahwamanager.ui.common.QahwaCard
import com.example.qahwamanager.ui.common.QahwaPrimaryButton
import com.example.qahwamanager.ui.common.QahwaTextField
import com.example.qahwamanager.ui.common.QahwaTopBarActions
import com.example.qahwamanager.ui.theme.QahwaTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CloudAuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class CloudAuthViewModel @Inject constructor(
    private val authRepository: CloudAuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CloudAuthUiState())
    val uiState: StateFlow<CloudAuthUiState> = _uiState.asStateFlow()

    fun signIn(email: String, password: String) {
        if (_uiState.value.isLoading) return
        _uiState.value = CloudAuthUiState(isLoading = true)
        viewModelScope.launch {
            runCatching { authRepository.signIn(email, password) }
                .onSuccess { _uiState.value = CloudAuthUiState() }
                .onFailure { err ->
                    _uiState.value = CloudAuthUiState(errorMessage = err.localizedMessage ?: "Sign-in failed")
                }
        }
    }

    fun signUp(displayName: String, email: String, password: String) {
        if (_uiState.value.isLoading) return
        _uiState.value = CloudAuthUiState(isLoading = true)
        viewModelScope.launch {
            runCatching { authRepository.signUp(email, password, displayName) }
                .onSuccess { _uiState.value = CloudAuthUiState() }
                .onFailure { err ->
                    _uiState.value = CloudAuthUiState(errorMessage = err.localizedMessage ?: "Sign-up failed")
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

@Composable
fun CloudAuthScreen(
    viewModel: CloudAuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val qColors = QahwaTheme.colors

    var isSignUp by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val canSubmit = email.isNotBlank() && password.length >= 6 && (!isSignUp || name.isNotBlank()) && !uiState.isLoading

    Scaffold(containerColor = qColors.background) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                QahwaTopBarActions()
            }

            Spacer(Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.CloudSync,
                    contentDescription = null,
                    tint = qColors.primaryAccent,
                )
                Text(
                    text = stringResource(R.string.cloud_auth_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = qColors.textPrimary,
                )
                Text(
                    text = stringResource(R.string.cloud_auth_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = qColors.textSecondary,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = !isSignUp,
                        onClick = {
                            isSignUp = false
                            viewModel.clearError()
                        },
                        label = { Text(stringResource(R.string.sign_in)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = qColors.cardSurface,
                            labelColor = qColors.textSecondary,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = !isSignUp,
                            borderColor = qColors.divider,
                            selectedBorderColor = Color.Transparent,
                        ),
                    )
                    FilterChip(
                        selected = isSignUp,
                        onClick = {
                            isSignUp = true
                            viewModel.clearError()
                        },
                        label = { Text(stringResource(R.string.create_account)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = qColors.cardSurface,
                            labelColor = qColors.textSecondary,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSignUp,
                            borderColor = qColors.divider,
                            selectedBorderColor = Color.Transparent,
                        ),
                    )
                }

                QahwaCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (isSignUp) {
                            QahwaTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = stringResource(R.string.owner_name),
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words,
                                    imeAction = ImeAction.Next,
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        QahwaTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = stringResource(R.string.email),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        QahwaTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = stringResource(R.string.password),
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        uiState.errorMessage?.let { err ->
                            Text(
                                text = err,
                                style = MaterialTheme.typography.bodySmall,
                                color = qColors.error,
                            )
                        }

                        if (uiState.isLoading) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                CircularProgressIndicator(color = qColors.primaryAccent)
                            }
                        } else {
                            QahwaPrimaryButton(
                                text = stringResource(if (isSignUp) R.string.create_account else R.string.sign_in),
                                onClick = {
                                    if (isSignUp) viewModel.signUp(name, email, password)
                                    else viewModel.signIn(email, password)
                                },
                                enabled = canSubmit,
                                leadingIcon = if (isSignUp) Icons.Default.PersonAdd else Icons.Default.Login,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}
