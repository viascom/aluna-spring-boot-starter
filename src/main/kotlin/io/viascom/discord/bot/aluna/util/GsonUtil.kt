@file:JvmName("AlunaUtils")
@file:JvmMultifileClass

package io.viascom.discord.bot.aluna.util

import com.google.gson.Gson
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties

fun <T> Gson.fromJson(value: ByteArray, type: Class<T>): T = this.fromJson(String(value), type)
fun Gson.toJsonByteArray(value: Any?): ByteArray = this.toJson(value).toByteArray()
fun <T : Any> Gson.convertValue(value: Any?, type: KClass<T>): T = fromJson(toJsonByteArray(value), type.java)
fun Gson.checkFields(data: Any, preferredObject: KClass<*>): Boolean {
    val jsonObject = this.toJsonTree(data).asJsonObject
    val allowedFields = preferredObject.memberProperties.map { it.name }
    return jsonObject.keySet().all { it in allowedFields }
}
