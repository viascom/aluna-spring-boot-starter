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

package io.viascom.discord.bot.aluna.model

import io.viascom.discord.bot.aluna.util.toHex
import java.awt.Color

class ReleaseNotes(
    val title: String = "Release Notes - {now:LONG_DATE_TIME}",
    val description: String = "",
    val color: String = Color.BLUE.toHex(),
    val thumbnail: String? = null,
    val image: String? = null,
    val creator: String = "",
    val newCommands: List<String> = arrayListOf(),
    val newFeatures: List<String> = arrayListOf(),
    val bugFixes: List<String> = arrayListOf(),
    val internalChanges: List<String> = arrayListOf()
)
