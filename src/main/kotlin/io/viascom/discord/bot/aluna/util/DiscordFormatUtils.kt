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

@file:JvmName("AlunaUtils")
@file:JvmMultifileClass

package io.viascom.discord.bot.aluna.util

import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime

enum class TimestampFormat(val format: String) {
    SHORT_TIME("t"),
    LONG_TIME("T"),
    SHORT_DATE("d"),
    LONG_DATE("D"),
    SHORT_DATE_TIME("f"),
    LONG_DATE_TIME("F"),
    RELATIVE_TIME("R")
}

/**
 * Convert to discord timestamp. This method will use the LocalDateTime at UTC.
 *
 * @param format Format of the timestamp
 * @return Discord timestamp
 */
fun LocalDateTime.toDiscordTimestamp(format: TimestampFormat = TimestampFormat.SHORT_DATE_TIME): String =
    "<t:${this.toUnixTimestamp()}:${format.format}>"

/**
 * Convert to discord timestamp.
 *
 * @param format Format of the timestamp
 * @return Discord timestamp
 */
fun OffsetDateTime.toDiscordTimestamp(format: TimestampFormat = TimestampFormat.SHORT_DATE_TIME): String =
    "<t:${this.toEpochSecond()}:${format.format}>"

/**
 * Convert to discord timestamp.
 *
 * @param format Format of the timestamp
 * @return Discord timestamp
 */
fun ZonedDateTime.toDiscordTimestamp(format: TimestampFormat = TimestampFormat.SHORT_DATE_TIME): String =
    "<t:${this.toEpochSecond()}:${format.format}>"

/**
 * Convert to unix timestamp. This method will use the LocalDateTime at UTC.
 *
 * @return Epoch Seconds
 */
fun LocalDateTime.toUnixTimestamp(): Long = this.toEpochSecond(ZoneOffset.UTC)
