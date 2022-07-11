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

package io.viascom.discord.bot.aluna.util

import java.security.SecureRandom
import java.util.*
import kotlin.math.ceil

/**
 * Nano id utils ported to Kotlin from https://github.com/aventrix/jnanoid
 */
object NanoId {

    /**
     * The default random number generator used by this class.
     * Creates cryptographically strong NanoId Strings.
     */
    private val DEFAULT_NUMBER_GENERATOR = SecureRandom()

    /**
     * The default alphabet used by this class.
     * Creates url-friendly NanoId Strings using 64 unique symbols.
     */
    private val DEFAULT_ALPHABET = "_-0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray()

    /**
     * The default size used by this class.
     * Creates NanoId Strings with slightly more unique values than UUID v4.
     */
    private const val DEFAULT_SIZE = 21

    /**
     * Retrieve a NanoId String.
     *
     * The string is generated using the given random number generator.
     *
     * @param alphabet The symbols used in the NanoId String.
     * @param size     The number of symbols in the NanoId String.
     * @param random   The random number generator.
     * @return A randomly generated NanoId String.
     */
    @JvmOverloads
    fun generate(alphabet: CharArray = DEFAULT_ALPHABET, size: Int = DEFAULT_SIZE, random: Random = DEFAULT_NUMBER_GENERATOR): String {
        require(!(alphabet.isEmpty() || alphabet.size >= 256)) { "alphabet must contain between 1 and 255 symbols." }
        require(size > 0) { "size must be greater than zero." }

        if(alphabet.size == 1){
            return repeat(alphabet[0], size)
        }

        //floor(log2(x)) = 31 - numberOfLeadingZeros(x)
        val mask = (2 shl (Integer.SIZE - 1 - Integer.numberOfLeadingZeros(alphabet.size - 1))) - 1
        val step = ceil(1.6 * mask * size / alphabet.size).toInt()
        val idBuilder = StringBuilder(size)
        val bytes = ByteArray(step)
        while (true) {
            random.nextBytes(bytes)
            for (i in 0 until step) {
                val alphabetIndex = bytes[i].toInt() and mask
                if (alphabetIndex < alphabet.size) {
                    idBuilder.append(alphabet[alphabetIndex])
                    if (idBuilder.length == size) {
                        return idBuilder.toString()
                    }
                }
            }
        }
    }

    private fun repeat(c: Char, size: Int): String {
        val builder = java.lang.StringBuilder(size)
        for (i in 0 until size) {
            builder.append(c)
        }
        return builder.toString()
    }
}