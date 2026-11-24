package com.medicalrep.manager.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

internal fun <T> Flow<T>.toListBlocking(): T = runBlocking { first() }
