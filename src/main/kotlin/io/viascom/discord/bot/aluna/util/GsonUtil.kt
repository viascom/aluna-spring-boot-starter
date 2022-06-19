/*
 * Copyright 2022 Viascom Ltd liab. Co
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

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
