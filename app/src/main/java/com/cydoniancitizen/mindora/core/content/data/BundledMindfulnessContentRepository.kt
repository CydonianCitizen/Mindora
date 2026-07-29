package com.cydoniancitizen.mindora.core.content.data

import android.content.Context
import com.cydoniancitizen.mindora.core.content.MindfulnessContentRepository
import com.cydoniancitizen.mindora.core.content.model.MindfulnessPath
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class BundledMindfulnessContentRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : MindfulnessContentRepository {
    override suspend fun getPaths(): List<MindfulnessPath> = withContext(Dispatchers.IO) {
        val source = try {
            context.assets
                .open(CATALOGUE_ASSET)
                .bufferedReader()
                .use { reader -> reader.readText() }
        } catch (error: IOException) {
            throw ContentAssetException(error)
        }

        ContentCatalogueParser.parse(source)
    }

    private companion object {
        const val CATALOGUE_ASSET = "content/mindfulness_catalog.json"
    }
}
