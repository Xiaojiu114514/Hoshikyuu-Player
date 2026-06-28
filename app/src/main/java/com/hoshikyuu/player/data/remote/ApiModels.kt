package com.hoshikyuu.player.data.remote

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive

/**
 * 可同時解析 Int 與 String 型別的 ID 序列化器。
 * 網易雲(wy)回傳數字 ID（如 5257138），QQ 回傳字串 ID（如 "0022QuVR1LcRHN"）。
 */
object FlexibleIdSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("FlexibleId", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: String) { encoder.encodeString(value) }
    override fun deserialize(decoder: Decoder): String {
        val json = decoder as? JsonDecoder
        return if (json != null) {
            val el = json.decodeJsonElement()
            if (el is JsonPrimitive) el.content else el.toString()
        } else {
            decoder.decodeString()
        }
    }
}

@Serializable
data class ApiResponse<T>(
    val code: Int,
    val msg: String,
    val data: T? = null
)

@Serializable
data class MusicSearchItem(
    val type: String,
    @Serializable(with = FlexibleIdSerializer::class) val id: String,
    val name: String,
    val album: String,
    val artist: String,
    @SerialName("pic_id") val picId: String = ""
)

@Serializable
data class SongDetail(
    val name: String = "",
    val album: String = "",
    val artist: String = "",
    val picid: String = "",
    val url: String = "",
    val pic: String = "",
    val lrc: String = ""
)

@Serializable
data class SongSearchRequest(
    val token: String,
    val name: String,
    val type: String = "wy",
    val page: Int = 1,
    val limit: Int = 10
)

@Serializable
data class SongDetailRequest(
    val token: String,
    val id: String,
    val type: String = "wy"
)
