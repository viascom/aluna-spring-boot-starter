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

package io.viascom.discord.bot.aluna.property

class AlunaThreadProperties {

    /**
     * Max amount of scheduler threads for timeout handling on interaction observers.
     */
    var messagesToObserveScheduledThreadPool: Int = 2

    /**
     * Max amount of scheduler threads for timeout handling on discord scoped objects.
     */
    var scopedObjectsTimeoutScheduler: Int = 2

    /**
     * Max amount of scheduler threads for event waiter timeout handling.
     */
    var eventWaiterTimeoutScheduler: Int = 2

    /**
     * Max amount of threads for jda callback handling.
     */
    var jdaCallbackThreadPool: Int = 10

    /**
     * Max amount of threads for event publish handling.
     */
    var eventThreadPool: Int = 10

    /**
     * Amount of parallelism used for the interaction coroutine context.
     *
     * A value of -1 indicates that the level of parallelism is not explicitly set and will be determined by the system.
     */
    var interactionParallelism: Int = -1

    /**
     * Amount of parallelism used for the event coroutine context.
     *
     * A value of -1 indicates that the level of parallelism is not explicitly set and will be determined by the system.
     */
    var eventParallelism: Int = -1

    /**
     * Amount of parallelism used for the detached coroutine context.
     *
     * A value of -1 indicates that the level of parallelism is not explicitly set and will be determined by the system.
     */
    var detachedParallelism: Int = -1
}
