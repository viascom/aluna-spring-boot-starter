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

package io.viascom.discord.bot.aluna.property

import org.springframework.boot.convert.DurationUnit
import java.time.Duration
import java.time.temporal.ChronoUnit

class AlunaThreadProperties {

    /**
     * Max amount of threads used for interaction execution.
     */
    var interactionExecutorCount: Int = 100

    /**
     * Duration of how long an interaction thread should be keep inactive before it gets destroyed.
     */
    @DurationUnit(ChronoUnit.SECONDS)
    var interactionExecutorTtl: Duration = Duration.ofSeconds(30)

    /**
     * Max amount of async executor threads. These threads are used by Aluna to handle internal async task.
     */
    var asyncExecutorCount: Int = 100

    /**
     * Duration of how long an async thread should be keep inactive before it gets destroyed.
     */
    @DurationUnit(ChronoUnit.SECONDS)
    var asyncExecutorTtl: Duration = Duration.ofSeconds(10)

    /**
     * Max amount of event waiter threads.
     */
    var eventWaiterThreadPoolCount: Int = 100

    /**
     * Duration of how long an event waiter thread should be keep inactive before it gets destroyed.
     */
    @DurationUnit(ChronoUnit.SECONDS)
    var eventWaiterThreadPoolTtl: Duration = Duration.ofSeconds(30)

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
}
