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

package io.viascom.discord.bot.aluna.configuration

import com.fatboyindustrial.gsonjavatime.Converters
import com.google.gson.*
import com.google.gson.annotations.Expose
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.gson.GsonBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnClass(Gson::class)
open class GsonConfig {

    @Bean
    @ConditionalOnClass(Gson::class)
    open fun localDateTimeGsonBuilderCustomizer(): GsonBuilderCustomizer {
        return GsonBuilderCustomizer { gsonBuilder -> Converters.registerAll(gsonBuilder) }
    }

    @Bean
    @ConditionalOnClass(Gson::class)
    open fun exclusionStrategyGsonBuilderCustomizer(): GsonBuilderCustomizer {
        return object : GsonBuilderCustomizer {
            override fun customize(gsonBuilder: GsonBuilder) {
                gsonBuilder.addSerializationExclusionStrategy(object : ExclusionStrategy {
                    override fun shouldSkipField(f: FieldAttributes): Boolean {
                        return f.getAnnotation(Expose::class.java) != null && !f.getAnnotation(Expose::class.java).serialize;
                    }

                    override fun shouldSkipClass(clazz: Class<*>?): Boolean {
                        return false;
                    }
                })
            }
        }
    }

    @Bean
    @ConditionalOnClass(Gson::class)
    open fun registerByteArrayAdapter(): GsonBuilderCustomizer {
        return GsonBuilderCustomizer { gsonBuilder ->
            gsonBuilder.registerTypeAdapter(
                ByteArray::class.java,
                JsonSerializer<ByteArray?> { src, typeOfSrc, context -> JsonPrimitive(String(src)) })
                .registerTypeAdapter(
                    ByteArray::class.java,
                    JsonDeserializer<ByteArray?> { json, typeOfT, context ->
                        if (json == null) null else if (json.asString == null) null else json.asString.toByteArray()
                    })
        }
    }
}
