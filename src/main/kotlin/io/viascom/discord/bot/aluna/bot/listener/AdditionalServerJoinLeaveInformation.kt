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

package io.viascom.discord.bot.aluna.bot.listener

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.MessageEmbed.Field

/**
 * This interface can be used to add additional information to the join and leave message
 */
public interface AdditionalServerJoinLeaveInformation {

    /**
     * Returns a list of fields that will be added to the join message
     *
     * @param server the server that the bot joined
     * @return a list of fields
     */
    public fun getAdditionalServerJoinInformation(server: Guild): List<Field>

    /**
     * Returns a list of fields that will be added to the leave message
     *
     * @param server the server that the bot left
     * @return a list of fields
     */
    public fun getAdditionalServerLeaveInformation(server: Guild): List<Field>

}
