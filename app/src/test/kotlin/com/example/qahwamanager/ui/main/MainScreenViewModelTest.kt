package com.example.qahwamanager.ui.main

import org.junit.Assert.assertNotNull
import org.junit.Test

class MainScreenViewModelTest {
  @Test
  fun viewModel_instantiationSucceeds() {
    val viewModel = MainScreenViewModel()
    assertNotNull(viewModel)
  }
}
