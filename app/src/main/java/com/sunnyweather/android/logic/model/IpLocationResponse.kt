package com.sunnyweather.android.logic.model

import com.google.gson.*
import com.google.gson.annotations.JsonAdapter
import java.lang.reflect.Type

/**
 * 高德 IP 定位响应。
 * 注意：部分字段（province、city、adcode、rectangle）在 IP 无法精确定位时
 * 可能返回 JSON 空数组 [] 而非字符串，因此使用 @JsonAdapter 自定义解析。
 */
@JsonAdapter(IpLocationResponse.Deserializer::class)
data class IpLocationResponse(
    val status: String,     // "1"=成功, "0"=失败
    val info: String,       // 状态说明
    val infocode: String,   // 状态码，"10000"=正确
    val province: String,   // 省份名称（可能为空）
    val city: String,       // 城市名称（可能为空）
    val adcode: String,     // adcode 编码（可能为空）
    val rectangle: String   // 矩形区域范围（可能为空）
) {
    /**
     * 自定义 Gson 反序列化器，处理高德 API 中
     * 字符串字段可能返回 []（空数组）的边界情况。
     */
    class Deserializer : JsonDeserializer<IpLocationResponse> {
        override fun deserialize(
            json: JsonElement,
            typeOfT: Type?,
            context: JsonDeserializationContext?
        ): IpLocationResponse {
            val obj = json.asJsonObject
            return IpLocationResponse(
                status    = obj.safeString("status"),
                info      = obj.safeString("info"),
                infocode  = obj.safeString("infocode"),
                province  = obj.safeString("province"),
                city      = obj.safeString("city"),
                adcode    = obj.safeString("adcode"),
                rectangle = obj.safeString("rectangle")
            )
        }
    }
}

/** 安全读取字符串字段：JSON 数组（如 []）转为空字符串 */
private fun JsonObject.safeString(key: String): String {
    val element = this[key] ?: return ""
    return when {
        element.isJsonArray  -> ""  // 处理高德返回 [] 的情况
        element.isJsonNull   -> ""
        else                 -> element.asString
    }
}
