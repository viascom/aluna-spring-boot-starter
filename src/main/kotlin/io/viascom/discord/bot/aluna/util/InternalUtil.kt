/*
 * Copyright 2025 Viascom Ltd liab. Co
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

package io.viascom.discord.bot.aluna.util

import io.viascom.discord.bot.aluna.bot.DiscordSubCommandElement
import io.viascom.discord.bot.aluna.bot.InteractionScopedObject
import io.viascom.discord.bot.aluna.bot.SubCommandElement
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor


internal object InternalUtil {

    @JvmSynthetic
    internal fun <T : InteractionScopedObject> getSubCommandElements(clazz: T): List<KProperty1<out T, *>> {
        val fields = arrayListOf<KProperty1<out T, *>>()

        //Search in constructor
        (clazz::class.primaryConstructor ?: clazz::class.constructors.first()).parameters.forEach {
            if (it.findAnnotation<SubCommandElement>() != null &&
                (it.type.classifier as KClass<*>).isSubclassOf(DiscordSubCommandElement::class)
            ) {
                val field = clazz::class.memberProperties.firstOrNull { member -> member.name == it.name }
                    ?: throw IllegalArgumentException("Couldn't access ${it.name} parameter because it is not a property. To fix this, make sure that your parameter is defined as property.")
                fields.add(field)
            }
        }

        //Search properties
        clazz::class.memberProperties.filter {
            it.findAnnotation<SubCommandElement>() != null && (it.returnType.classifier as KClass<*>).isSubclassOf(DiscordSubCommandElement::class)
        }.forEach {
            fields.add(it)
        }

        return fields.distinctBy { it.name }
    }

}
