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

package io.viascom.discord.bot.aluna.translation

import com.vdurmont.emoji.EmojiParser
import io.viascom.discord.bot.aluna.property.AlunaProperties
import org.springframework.context.MessageSource
import org.springframework.context.support.ReloadableResourceBundleMessageSource
import java.text.NumberFormat
import java.util.*

class DefaultMessageService(
    private val messageSource: MessageSource,
    private val reloadableMessageSource: ReloadableResourceBundleMessageSource,
    private val alunaProperties: AlunaProperties
) : MessageService {

    override fun get(key: String, locale: Locale, vararg args: String): String {
        val correctLocale = if (alunaProperties.translation.useEnGbForEnInProduction && locale == Locale.ENGLISH && alunaProperties.productionMode) {
            Locale.forLanguageTag("en-GB")
        } else {
            locale
        }

        var message = messageSource.getMessage(key, args, correctLocale)
        message = message.replace("''", "'")
        message = message.replace("``", "`")
        return EmojiParser.parseToUnicode(message)
    }

    override fun getWithDefault(key: String, locale: Locale, default: String, vararg args: String): String {
        val correctLocale = if (alunaProperties.translation.useEnGbForEnInProduction && locale == Locale.ENGLISH && alunaProperties.productionMode) {
            Locale.forLanguageTag("en-GB")
        } else {
            locale
        }
        var message = reloadableMessageSource.getMessage(key, args, default, correctLocale)
        message = message.replace("''", "'")
        message = message.replace("``", "`")
        return EmojiParser.parseToUnicode(message)
    }

    override fun formatNumber(number: Double, locale: Locale): String {
        return NumberFormat.getNumberInstance(locale).format(number)
    }

}
