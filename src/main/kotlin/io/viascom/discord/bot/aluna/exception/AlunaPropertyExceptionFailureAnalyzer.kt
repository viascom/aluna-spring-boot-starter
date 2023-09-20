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

package io.viascom.discord.bot.aluna.exception

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer
import org.springframework.boot.diagnostics.FailureAnalysis

internal class AlunaPropertyExceptionFailureAnalyzer : AbstractFailureAnalyzer<AlunaPropertiesException>() {
    override fun analyze(rootFailure: Throwable, cause: AlunaPropertiesException): FailureAnalysis {
        val description = "${cause.description}\n\n" +
                "\tProperty: ${cause.property}\n" +
                "\tValue: ${cause.value}\n" +
                "\tReason: ${cause.reason}"
        return FailureAnalysis(description, "Update your application configuration", cause)
    }
}
