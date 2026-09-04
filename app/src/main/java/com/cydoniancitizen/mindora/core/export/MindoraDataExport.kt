package com.cydoniancitizen.mindora.core.export

import android.content.Context
import android.net.Uri
import com.cydoniancitizen.mindora.BuildConfig
import com.cydoniancitizen.mindora.core.preferences.MindoraPreferencesRepository
import com.cydoniancitizen.mindora.core.preferences.model.MindoraPreferences
import com.cydoniancitizen.mindora.core.session.MindfulnessSessionRepository
import com.cydoniancitizen.mindora.core.session.model.MindfulnessSession
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * The shape of the export, versioned separately from the app.
 *
 * A file someone keeps for years outlives the version that wrote it, so the reader needs to know
 * which layout it is looking at without guessing from the app version that produced it.
 */
private const val EXPORT_FORMAT = 1

@Serializable
internal data class ExportDocument(
    val application: String,
    val format: Int,
    val appVersionName: String,
    val appVersionCode: Int,
    val exportedAt: String,
    val preferences: ExportPreferences,
    val sessions: List<ExportSession>,
)

@Serializable
internal data class ExportPreferences(
    val weeklyGoalMinutes: Int?,
    val dailyReminderEnabled: Boolean,
    val dailyReminderTime: String,
)

@Serializable
internal data class ExportSession(
    val id: String,
    val type: String,
    val status: String,
    val sourcePathId: String?,
    val sourceStepId: String?,
    val startedAt: String,
    val activeDurationSeconds: Long,
    val plannedDurationSeconds: Long?,
)

private val exportJson = Json {
    // The file is meant to be opened and read by the person who owns it, not only by a parser.
    prettyPrint = true
    encodeDefaults = true
}

/**
 * Builds the export document. Pure on purpose: the timestamp and the app version arrive as
 * arguments so the exact bytes written can be asserted in a test.
 */
internal fun buildExportJson(
    appVersionName: String,
    appVersionCode: Int,
    exportedAt: Instant,
    preferences: MindoraPreferences,
    sessions: List<MindfulnessSession>,
): String = exportJson.encodeToString(
    ExportDocument(
        application = "Mindora",
        format = EXPORT_FORMAT,
        appVersionName = appVersionName,
        appVersionCode = appVersionCode,
        exportedAt = exportedAt.toString(),
        preferences = ExportPreferences(
            weeklyGoalMinutes = preferences.weeklyGoalMinutes,
            dailyReminderEnabled = preferences.dailyReminderEnabled,
            dailyReminderTime = preferences.dailyReminderTime.toString(),
        ),
        sessions = sessions.map { session ->
            ExportSession(
                id = session.id,
                type = session.type.name,
                status = session.status.name,
                sourcePathId = session.sourcePathId,
                sourceStepId = session.sourceStepId,
                startedAt = session.startedAt.toString(),
                activeDurationSeconds = session.activeDuration.seconds,
                plannedDurationSeconds = session.plannedDuration?.seconds,
            )
        },
    ),
)

/** The name offered in the system's save dialog. */
internal fun defaultExportFileName(today: LocalDate = LocalDate.now()): String =
    "mindora-data-$today.json"

/** Writes everything the app holds about its user to a destination the user chose. */
interface MindoraDataExporter {
    suspend fun exportTo(destination: Uri)
}

/**
 * The destination comes from the system's own create-document picker, so the export needs no
 * storage permission and lands wherever the person chose — which is the point of an app that keeps
 * their history on device and nowhere else.
 */
class ContentResolverDataExporter @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val sessionRepository: MindfulnessSessionRepository,
    private val preferencesRepository: MindoraPreferencesRepository,
) : MindoraDataExporter {
    override suspend fun exportTo(destination: Uri) {
        val document = buildExportJson(
            appVersionName = BuildConfig.VERSION_NAME,
            appVersionCode = BuildConfig.VERSION_CODE,
            exportedAt = Instant.now(),
            preferences = preferencesRepository.preferences.first(),
            sessions = sessionRepository.observeSessions().first(),
        )
        withContext(Dispatchers.IO) {
            val stream = context.contentResolver.openOutputStream(destination)
                ?: error("Export destination could not be opened for writing.")
            stream.use { it.write(document.toByteArray()) }
        }
    }
}
