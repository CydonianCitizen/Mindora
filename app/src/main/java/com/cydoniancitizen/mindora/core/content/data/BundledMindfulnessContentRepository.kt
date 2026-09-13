package com.cydoniancitizen.mindora.core.content.data

import android.content.Context
import com.cydoniancitizen.mindora.core.content.MeditationLibraryRepository
import com.cydoniancitizen.mindora.core.content.MindfulnessContentRepository
import com.cydoniancitizen.mindora.core.content.model.LibraryMeditation
import com.cydoniancitizen.mindora.core.content.model.MindfulnessCatalogue
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
) : MindfulnessContentRepository, MeditationLibraryRepository {
    override suspend fun getPaths(): List<MindfulnessPath> = load().paths

    override suspend fun getMeditations(): List<LibraryMeditation> = load().meditations

    /**
     * Kept per language: the per-app language can change while the process lives, and the
     * catalogue has to follow it the way string resources do.
     */
    @Volatile
    private var cached: Pair<String, MindfulnessCatalogue>? = null

    private suspend fun load(): MindfulnessCatalogue {
        val language = context.resources.configuration.locales[0].language
        cached?.takeIf { it.first == language }?.let { return it.second }
        return withContext(Dispatchers.IO) {
            val source = try {
                context.assets
                    .open(CATALOGUE_ASSET)
                    .bufferedReader()
                    .use { reader -> reader.readText() }
            } catch (error: IOException) {
                throw ContentAssetException(error)
            }
            ContentCatalogueParser.parse(source, language)
        }.also { cached = language to it }
    }

    private companion object {
        const val CATALOGUE_ASSET = "content/mindfulness_catalog.json"
    }
}
