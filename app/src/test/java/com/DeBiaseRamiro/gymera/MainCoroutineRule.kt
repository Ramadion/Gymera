package com.DeBiaseRamiro.gymera

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * JUnit Rule que reemplaza el dispatcher Main con uno de test.
 * Usar en todos los tests de ViewModel que usan viewModelScope.
 *
 * Uso:
 *   @get:Rule val coroutineRule = MainCoroutineRule()
 *
 * UnconfinedTestDispatcher ejecuta coroutines de forma eager (sin necesidad
 * de advanceUntilIdle()) — ideal para tests de StateFlow y operaciones simples.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainCoroutineRule(
    val dispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}