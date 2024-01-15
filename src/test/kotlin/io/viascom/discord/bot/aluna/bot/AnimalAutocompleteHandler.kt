/*
 * Copyright 2024 Viascom Ltd liab. Co
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

package io.viascom.discord.bot.aluna.bot

import io.viascom.discord.bot.aluna.AlunaDispatchers
import io.viascom.discord.bot.aluna.bot.handler.AutoComplete
import io.viascom.discord.bot.aluna.bot.handler.AutoCompleteHandler
import io.viascom.discord.bot.aluna.bot.interaction.SetPreferredAnimalCommand
import io.viascom.discord.bot.aluna.util.getOptionAsString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@AutoComplete
class AnimalAutocompleteHandler : AutoCompleteHandler(SetPreferredAnimalCommand::class.java, "animal") {

    val tempFlow = MutableStateFlow<CommandAutoCompleteInteractionEvent?>(null)

    var isLoading = false
    var data: List<String>? = null

    override fun onRequest(event: CommandAutoCompleteInteractionEvent) {
        logger.info(this.uniqueId)

        if (data == null && !isLoading) {
            isLoading = true
            logger.info("first request")
            AlunaDispatchers.InteractionScope.launch {
                val httpClient = OkHttpClient.Builder()
                    .connectTimeout(20, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(120, TimeUnit.SECONDS)
                    .build()


                httpClient.newCall(Request.Builder().get().url("http://httpbin.org/delay/4").build()).execute()
                logger.info("data loaded")
                data = arrayListOf("fox", "bunny", "deer")
            }
            return
        }

        tempFlow.update { event }
    }

    suspend fun handleInput() {
        tempFlow.debounce {
            if (data != null) return@debounce 0.toDuration(DurationUnit.SECONDS)
            (2000.toDuration(DurationUnit.MILLISECONDS)) //Wait longer to show the user we are loading data
        }.collect { event ->
            if (event == null || data == null) return@collect

            logger.info("check input: ${event.getOptionAsString("animal") ?: ""}")

            val filtered = data!!.filter { it.contains(event.getOptionAsString("animal") ?: "") }
            event.replyChoiceStrings(filtered).queue()
        }
    }

    init {
        AlunaDispatchers.InteractionScope.launch { handleInput() }
    }

}
