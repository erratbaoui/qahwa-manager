package com.example.qahwamanager.ui.main

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

// Legacy ViewModel — superseded by feature-specific ViewModels in ui/home, ui/menu, etc.
@HiltViewModel
class MainScreenViewModel @Inject constructor() : ViewModel()
