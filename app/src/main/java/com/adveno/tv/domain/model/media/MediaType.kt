package com.adveno.tv.domain.model.media

import kotlinx.serialization.Serializable

/**
 * Domain media types supported by the Adveno digital signage network.
 */
@Serializable
enum class MediaType {
    IMAGE,
    VIDEO
}

