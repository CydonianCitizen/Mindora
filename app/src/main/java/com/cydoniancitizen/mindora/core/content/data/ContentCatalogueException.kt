package com.cydoniancitizen.mindora.core.content.data

sealed class ContentCatalogueException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

class ContentAssetException(cause: Throwable) : ContentCatalogueException(
    message = "Unable to read the bundled mindfulness catalogue asset.",
    cause = cause,
)

class MalformedContentCatalogueException(cause: Throwable) : ContentCatalogueException(
    message = "The bundled mindfulness catalogue contains malformed JSON: ${cause.message}",
    cause = cause,
)

class UnsupportedContentSchemaException(
    val schemaVersion: Int,
) : ContentCatalogueException(
    message = "Unsupported mindfulness catalogue schema version: $schemaVersion.",
)

class InvalidContentCatalogueException(
    reason: String,
) : ContentCatalogueException(
    message = "Invalid mindfulness catalogue: $reason",
)
