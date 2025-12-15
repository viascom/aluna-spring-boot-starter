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

package io.viascom.discord.bot.aluna.bot.webhook

import io.viascom.discord.bot.aluna.configuration.condition.ConditionalOnJdaEnabled
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.IncomingWebhookClient
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.WebhookClient
import net.dv8tion.jda.api.sharding.ShardManager
import org.springframework.stereotype.Service

@Service
@ConditionalOnJdaEnabled
public class AlunaWebhookClient(private val shardManager: ShardManager) {

    /**
     * Connect to a webhook
     *
     * @param url Webhook url
     * @return Webhook client
     */
    public fun connect(url: String): IncomingWebhookClient {
        return WebhookClient<Message>.createClient(shardManager.shards.first(), url)
    }

    /**
     * Connect to a webhook
     *
     * @param url Webhook url
     * @param jda JDA instance
     * @return Webhook client
     */
    public fun connect(url: String, jda: JDA): IncomingWebhookClient {
        return WebhookClient<Message>.createClient(jda, url)
    }

}
