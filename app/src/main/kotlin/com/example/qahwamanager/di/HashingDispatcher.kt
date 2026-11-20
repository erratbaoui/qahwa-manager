package com.example.qahwamanager.di

import javax.inject.Qualifier

/** The dispatcher PBKDF2 runs on: [kotlinx.coroutines.Dispatchers.Default] in the app, the test scheduler in tests. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class HashingDispatcher
