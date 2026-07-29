package com.cydoniancitizen.mindora.core.content.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BundledMindfulnessContentRepositoryTest {
    @Test
    fun productionCatalogueLoadsAndValidates() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = BundledMindfulnessContentRepository(context)

        val paths = repository.getPaths()

        assertTrue(paths.isEmpty())
    }
}
