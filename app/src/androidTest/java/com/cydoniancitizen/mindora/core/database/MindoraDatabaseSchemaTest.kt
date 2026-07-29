package com.cydoniancitizen.mindora.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MindoraDatabaseSchemaTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        MindoraDatabase::class.java,
    )

    @After
    fun deleteDatabase() {
        InstrumentationRegistry.getInstrumentation()
            .targetContext
            .deleteDatabase(DATABASE_NAME)
    }

    @Test
    fun exportedVersionOneSchemaCreatesDatabase() {
        helper.createDatabase(DATABASE_NAME, 1).close()
    }

    private companion object {
        const val DATABASE_NAME = "mindora-schema-test"
    }
}
