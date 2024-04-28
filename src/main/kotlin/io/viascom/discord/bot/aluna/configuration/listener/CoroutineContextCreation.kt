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

package io.viascom.discord.bot.aluna.configuration.listener

import io.viascom.discord.bot.aluna.AlunaDispatchers
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationContextInitializedEvent
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Service

@Service
class CoroutineContextCreation : ApplicationListener<ApplicationContextInitializedEvent> {

    private val logger: Logger = LoggerFactory.getLogger(CoroutineContextCreation::class.java)

    override fun onApplicationEvent(event: ApplicationContextInitializedEvent) {
        val interactionParallelism = event.applicationContext.environment.getProperty("aluna.thread.interaction-parallelism", Int::class.java, -1)
        val eventParallelism = event.applicationContext.environment.getProperty("aluna.thread.event-parallelism", Int::class.java, -1)
        val detachedParallelism = event.applicationContext.environment.getProperty("aluna.thread.detached-parallelism", Int::class.java, -1)

        if (interactionParallelism != -1 || eventParallelism != -1 || detachedParallelism != -1) {
            logger.info("Creating CoroutineContext with interactionParallelism=$interactionParallelism, eventParallelism=$eventParallelism, detachedParallelism=$detachedParallelism")
        }
        AlunaDispatchers.initScopes(interactionParallelism, eventParallelism, detachedParallelism)
    }

}
