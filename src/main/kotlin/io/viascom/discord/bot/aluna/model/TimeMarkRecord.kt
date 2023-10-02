/*
 * Copyright 2023 Viascom Ltd liab. Co
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

package io.viascom.discord.bot.aluna.model

import io.viascom.discord.bot.aluna.AlunaDispatchers
import io.viascom.discord.bot.aluna.util.round
import kotlinx.coroutines.withContext
import kotlin.time.Duration
import kotlin.time.TimeSource.Monotonic.ValueTimeMark

data class TimeMarkRecord(val step: TimeMarkStep, val mark: ValueTimeMark)

enum class TimeMarkStep {
    START,
    INITIALIZED,
    OWNER_CHECKED,
    NEEDED_USER_PERMISSIONS,
    NEEDED_BOT_PERMISSIONS,
    LOAD_DATA_BEFORE_ADDITIONAL_REQUIREMENTS,
    CHECK_FOR_ADDITIONAL_COMMAND_REQUIREMENTS,
    CHECK_COOLDOWN,
    LOAD_ADDITIONAL_DATA,
    ASYNC_TASKS_STARTED,
    RUN_EXECUTE,

    HANDLE_SUB_COMMAND_EXECUTION,
    LOAD_DYNAMIC_SUB_COMMAND_ELEMENTS,
    CHECK_SUB_COMMAND_PATH,
    SUB_COMMAND_INITIALIZED,
    SUB_COMMAND_RUN_EXECUTE,
    CHECK_SECOND_SUB_COMMAND_PATH,
    SECOND_SUB_COMMAND_INITIALIZED,
    SECOND_SUB_COMMAND_RUN_EXECUTE,

    ON_EXECUTION_EXCEPTION,
    EXIT_COMMAND
}

infix fun TimeMarkStep.at(mark: ValueTimeMark) = TimeMarkRecord(this, mark)

suspend fun ArrayList<TimeMarkRecord>.printTimeMarks(name: String): String = withContext(AlunaDispatchers.Detached) {
    var result = "\n"
    result += "$name\n"
    result += "===================================\n"
    val total = (this@printTimeMarks.last().mark - this@printTimeMarks.first().mark)
    this@printTimeMarks.windowed(2).forEach { (previous, current) ->
        val duration = current.mark - previous.mark
        result += "${current.step} in $duration (${(duration.inWholeNanoseconds / total.inWholeNanoseconds.toDouble() * 100).round(1)}%)\n"
    }

    result += "===================================\n"
    result += "Total: $total"

    return@withContext result
}

fun ArrayList<TimeMarkRecord>.getDuration(): Duration =
    this@getDuration.last().mark - this@getDuration.first().mark

fun ArrayList<TimeMarkRecord>.getDuration(possibleStartMarks: List<TimeMarkStep>, possibleEndMarks: List<TimeMarkStep>): Duration? {
    val start = this@getDuration.lastOrNull { it.step in possibleStartMarks }?.mark ?: return null
    val end = this@getDuration.lastOrNull { it.step in possibleEndMarks }?.mark ?: return null
    return end - start
}

@JvmSynthetic
internal fun ArrayList<TimeMarkRecord>.getDurationRunExecute(): Duration? = getDuration(
    listOf(TimeMarkStep.ASYNC_TASKS_STARTED),
    listOf(TimeMarkStep.RUN_EXECUTE, TimeMarkStep.SUB_COMMAND_RUN_EXECUTE, TimeMarkStep.SECOND_SUB_COMMAND_RUN_EXECUTE)
)

@JvmSynthetic
internal fun ArrayList<TimeMarkRecord>.getDurationNeededUserPermissions(): Duration? = getDuration(
    listOf(TimeMarkStep.INITIALIZED, TimeMarkStep.OWNER_CHECKED), listOf(TimeMarkStep.NEEDED_USER_PERMISSIONS)
)

@JvmSynthetic
internal fun ArrayList<TimeMarkRecord>.getDurationNeededBotPermissions(): Duration? =
    getDuration(listOf(TimeMarkStep.NEEDED_USER_PERMISSIONS), listOf(TimeMarkStep.NEEDED_BOT_PERMISSIONS))

@JvmSynthetic
internal fun ArrayList<TimeMarkRecord>.getDurationLoadDataBeforeAdditionalRequirements(): Duration? =
    getDuration(listOf(TimeMarkStep.NEEDED_BOT_PERMISSIONS), listOf(TimeMarkStep.LOAD_DATA_BEFORE_ADDITIONAL_REQUIREMENTS))

@JvmSynthetic
internal fun ArrayList<TimeMarkRecord>.getDurationCheckForAdditionalCommandRequirements(): Duration? =
    getDuration(listOf(TimeMarkStep.LOAD_DATA_BEFORE_ADDITIONAL_REQUIREMENTS), listOf(TimeMarkStep.CHECK_FOR_ADDITIONAL_COMMAND_REQUIREMENTS))

@JvmSynthetic
internal fun ArrayList<TimeMarkRecord>.getDurationLoadAdditionalData(): Duration? =
    getDuration(listOf(TimeMarkStep.CHECK_COOLDOWN, TimeMarkStep.CHECK_FOR_ADDITIONAL_COMMAND_REQUIREMENTS), listOf(TimeMarkStep.LOAD_ADDITIONAL_DATA))
