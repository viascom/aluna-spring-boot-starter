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

package io.viascom.discord.bot.aluna.bot.interaction.animal

import com.fasterxml.jackson.databind.ObjectMapper
import io.viascom.discord.bot.aluna.bot.DiscordSubCommand
import io.viascom.discord.bot.aluna.bot.Interaction
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import okhttp3.*
import java.awt.Color
import java.io.IOException

@Interaction
class BunnyCommand(
    private val objectMapper: ObjectMapper
) : DiscordSubCommand("bunny", "Show image of a bunny") {
    override fun execute(event: SlashCommandInteractionEvent) {
        //Acknowledge event and defer the reply
        event.deferReply().queue { interactionHook ->

            //Check http request
            val request = Request.Builder().url("https://api.bunnies.io/v2/loop/random/?media=gif,png").build()
            val call: Call = OkHttpClient.Builder().build().newCall(request)

            //Execute http request asynchronously
            call.enqueue(object : Callback {

                //On success
                override fun onResponse(call: Call, response: Response) {
                    //Parse json and extract image property
                    val image = objectMapper.readValue(response.body?.string() ?: "{}", BunnyImage::class.java).media.poster
                    response.close()

                    //Create a new embed
                    val builder = EmbedBuilder()
                        .setColor(Color.getHSBColor(0.08F, 0.60F, 0.63F))
                        .setImage(image)
                        .setFooter("Image provided by https://api.bunnies.io/")

                    //Send embed to the user
                    interactionHook.editOriginalEmbeds(builder.build()).queue()
                }

                //On failure
                override fun onFailure(call: Call, e: IOException) {
                    //Send message to the user that the request failed
                    interactionHook.editOriginal("Could not load bunny image \uD83D\uDE26").queue()
                }
            })
        }
    }

    class BunnyImage(
        val media: Media,
        val source: String
    ) {

        class Media(
            val gif: String,
            val poster: String
        )
    }
}
