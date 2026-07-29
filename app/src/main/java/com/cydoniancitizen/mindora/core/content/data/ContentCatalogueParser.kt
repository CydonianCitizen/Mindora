package com.cydoniancitizen.mindora.core.content.data

import com.cydoniancitizen.mindora.core.content.model.MindfulnessPath
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull

internal object ContentCatalogueParser {
    private const val SUPPORTED_SCHEMA_VERSION = 1

    private val json = Json {
        classDiscriminator = "type"
        coerceInputValues = false
        explicitNulls = true
        ignoreUnknownKeys = false
        isLenient = false
        useAlternativeNames = false
    }

    fun parse(source: String): List<MindfulnessPath> {
        val root = try {
            json.parseToJsonElement(source) as? JsonObject
                ?: malformed("The catalogue root must be a JSON object.")
        } catch (error: SerializationException) {
            throw MalformedContentCatalogueException(error)
        }
        val schemaVersionValue = root["schemaVersion"] as? JsonPrimitive
        val schemaVersion = schemaVersionValue
            ?.takeUnless(JsonPrimitive::isString)
            ?.intOrNull
            ?: malformed("schemaVersion must be an integer.")
        if (schemaVersion != SUPPORTED_SCHEMA_VERSION) {
            throw UnsupportedContentSchemaException(schemaVersion)
        }

        val catalogue = try {
            json.decodeFromJsonElement<ContentCatalogueDto>(root)
        } catch (error: SerializationException) {
            throw MalformedContentCatalogueException(error)
        }

        return ContentCatalogueValidator.validateAndMap(catalogue)
    }

    private fun malformed(reason: String): Nothing {
        throw MalformedContentCatalogueException(SerializationException(reason))
    }
}
