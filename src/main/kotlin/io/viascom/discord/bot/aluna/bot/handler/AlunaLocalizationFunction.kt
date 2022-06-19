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

package io.viascom.discord.bot.aluna.bot.handler
//
//import io.viascom.discord.bot.aluna.util.DiscordLocalization
//import net.dv8tion.jda.api.interactions.commands.LocalizationFunction
//import org.slf4j.Logger
//import org.slf4j.LoggerFactory
//import org.springframework.context.MessageSource
//import org.springframework.context.NoSuchMessageException
//import org.springframework.context.support.MessageSourceAccessor
//import java.util.*
//
//class AlunaLocalizationFunction(
//    messageSource: MessageSource
//) : LocalizationFunction {
//
//    private val messageSourceAccessor: MessageSourceAccessor = MessageSourceAccessor(messageSource)
//
//    val logger: Logger = LoggerFactory.getLogger(javaClass)
//
//    override fun apply(localizationKey: String): MutableMap<Locale, String> {
//        val map: MutableMap<Locale, String> = hashMapOf()
//        val key = "interaction.$localizationKey"
//        logger.debug("Search translation for $key")
//        DiscordLocalization.values().forEach {
//            val i18nName = try {
//                messageSourceAccessor.getMessage(key, it.locale)
//            } catch (e: NoSuchMessageException) {
//                null
//            }
//            if (i18nName != null && i18nName != key) {
//                map[it.locale] = i18nName
//            }
//        }
//
//        return map
//    }
//}
