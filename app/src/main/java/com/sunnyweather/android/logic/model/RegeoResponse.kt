package com.sunnyweather.android.logic.model

import com.google.gson.*
import com.google.gson.annotations.JsonAdapter
import java.lang.reflect.Type

/**
 * 高德逆地理编码（regeo）响应。
 * 将经纬度转换为城市名，作为 IP 定位失败的备用方案。
 * 同样使用 @JsonAdapter 处理字段可能返回 [] 的边界情况。
 */
@JsonAdapter(RegeoResponse.Deserializer::class)
data class RegeoResponse(
    val status: String,
    val info: String,
    val infocode: String,
    val regeocode: Regeocode?
) {
    data class Regeocode(
        val addressComponent: AddressComponent?
    ) {
        data class AddressComponent(
            val province: String,
            val city: String
        )
    }

    class Deserializer : JsonDeserializer<RegeoResponse> {
        override fun deserialize(
            json: JsonElement,
            typeOfT: Type?,
            context: JsonDeserializationContext?
        ): RegeoResponse {
            val obj = json.asJsonObject
            val regeocodeObj = obj.getAsJsonObject("regeocode")
            val addressComponentObj = regeocodeObj?.getAsJsonObject("addressComponent")

            return RegeoResponse(
                status   = obj.safeString("status"),
                info     = obj.safeString("info"),
                infocode = obj.safeString("infocode"),
                regeocode = if (addressComponentObj != null) {
                    Regeocode(
                        addressComponent = RegeoResponse.Regeocode.AddressComponent(
                            province = addressComponentObj.safeString("province"),
                            city     = addressComponentObj.safeString("city")
                        )
                    )
                } else null
            )
        }
    }
}

/** 安全读取字符串字段：JSON 数组（如 []）转为空字符串 */
private fun JsonObject.safeString(key: String): String {
    val element = this[key] ?: return ""
    return when {
        element.isJsonArray  -> ""
        element.isJsonNull   -> ""
        else                 -> element.asString
    }
}
