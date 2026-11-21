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
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.qahwamanager.R
import com.example.qahwamanager.data.cloud.CafeWorkspaceRepository
import com.example.qahwamanager.ui.common.QahwaCard
import com.example.qahwamanager.ui.common.QahwaPrimaryButton
import com.example.qahwamanager.ui.common.QahwaSecondaryButton
import com.example.qahwamanager.ui.common.QahwaTextField
import com.example.qahwamanager.ui.common.QahwaTopBarActions
import com.example.qahwamanager.ui.theme.QahwaTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CafeWorkspaceUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class CafeWorkspaceViewModel @Inject constructor(
    private val workspaceRepository: CafeWorkspaceRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CafeWorkspaceUiState())
    val uiState: StateFlow<CafeWorkspaceUiState> = _uiState.asStateFlow()

    fun createCafe(name: String) {
        if (_uiState.value.isLoading) return
        _uiState.value = CafeWorkspaceUiState(isLoading = true)
        viewModelScope.launch {
            runCatching { workspaceRepository.createCafe(name) }
                .onSuccess { _uiState.value = CafeWorkspaceUiState() }
                .onFailure { err ->
                    _uiState.value = CafeWorkspaceUiState(errorMessage = err.localizedMessage ?: "Could not create coffee shop")
                }
        }
    }

    fun joinCafe(code: String) {
        if (_uiState.value.isLoading) return
        _uiState.value = CafeWorkspaceUiState(isLoading = true)
        viewModelScope.launch {
            runCatching { workspaceRepository.joinCafeByCode(code) }
                .onSuccess { _uiState.value = CafeWorkspaceUiState() }
                .onFailure { err ->
                    _uiState.value = CafeWorkspaceUiState(errorMessage = err.localizedMessage ?: "Invalid invite code")
                }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            workspaceRepository.signOutAndClearLocal()
        }
    }
}

@Composable
fun CafeWorkspaceScreen(
    viewModel: CafeWorkspaceViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val qColors = QahwaTheme.colors

    var cafeName by remember { mutableStateOf("") }
    var inviteCode by remember { mutableStateOf("") }

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

            Spacer(Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Storefront,
                    contentDescription = null,
                    tint = qColors.primaryAccent,
                )
                Text(
                    text = stringResource(R.string.cafe_workspace_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = qColors.textPrimary,
                )
                Text(
                    text = stringResource(R.string.cafe_workspace_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = qColors.textSecondary,
                )

                uiState.errorMessage?.let { err ->
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodySmall,
                        color = qColors.error,
                    )
                }

                if (uiState.isLoading) {
                    CircularProgressIndicator(color = qColors.primaryAccent)
                }

                // Create a new coffee shop
                QahwaCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.create_coffee_shop),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = qColors.textPrimary,
                        )
                        QahwaTextField(
                            value = cafeName,
                            onValueChange = { cafeName = it },
                            label = stringResource(R.string.coffee_shop_name),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Done,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        QahwaPrimaryButton(
                            text = stringResource(R.string.create_coffee_shop),
                            onClick = { viewModel.createCafe(cafeName) },
                            enabled = cafeName.isNotBlank() && !uiState.isLoading,
                            leadingIcon = Icons.Default.AddBusiness,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                // Join an existing coffee shop via invite code
                QahwaCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.join_coffee_shop),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = qColors.textPrimary,
                        )
                        QahwaTextField(
                            value = inviteCode,
                            onValueChange = { inviteCode = it.uppercase() },
                            label = stringResource(R.string.invite_code),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Characters,
                                imeAction = ImeAction.Done,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        QahwaSecondaryButton(
                            text = stringResource(R.string.join_coffee_shop),
                            onClick = { viewModel.joinCafe(inviteCode) },
                            enabled = inviteCode.trim().length >= 8 && !uiState.isLoading,
                            leadingIcon = Icons.Default.GroupAdd,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                TextButton(onClick = viewModel::signOut, enabled = !uiState.isLoading) {
                    Text(stringResource(R.string.sign_out), color = qColors.textSecondary)
                }
            }
        }
    }
}
