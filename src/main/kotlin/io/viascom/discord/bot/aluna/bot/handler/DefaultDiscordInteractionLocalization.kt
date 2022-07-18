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

package io.viascom.discord.bot.aluna.bot.handler

import io.viascom.discord.bot.aluna.property.AlunaDebugProperties
import io.viascom.discord.bot.aluna.property.AlunaProperties
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.localization.LocalizationFunction
import net.dv8tion.jda.api.interactions.commands.localization.ResourceBundleLocalizationFunction
import net.dv8tion.jda.internal.utils.Checks
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import javax.annotation.Nonnull

class DefaultDiscordInteractionLocalization(
    private val alunaProperties: AlunaProperties
) : DiscordInteractionLocalization {

    private var cachedLocalizationFunction: LocalizationFunction? = null

    @JvmSynthetic
    internal var missingTranslations: HashMap<DiscordLocale, ArrayList<String>> = hashMapOf()

    override fun getLocalizationFunction(): LocalizationFunction {
        if (cachedLocalizationFunction == null) {
            val localizationFunctionBuilder = AlunaResourceBundleLocalizationFunction.fromBundles(
                alunaProperties.translation.basePath ?: "i18n/messages",
                *alunaProperties.translation.supportedDiscordLocals.toTypedArray()
            ).build(alunaProperties, missingTranslations)

            cachedLocalizationFunction = localizationFunctionBuilder
        }

        return cachedLocalizationFunction!!
    }

    override fun getMissingTranslationKeys(): HashMap<DiscordLocale, ArrayList<String>> {
        return missingTranslations
    }

    class AlunaResourceBundleLocalizationFunction private constructor(
        private val bundles: Set<Bundle>,
        private val alunaProperties: AlunaProperties,
        private val missingTranslations: HashMap<DiscordLocale, ArrayList<String>>
    ) :
        LocalizationFunction {

        private val logger: Logger = LoggerFactory.getLogger(javaClass)

        @Nonnull
        override fun apply(@Nonnull localizationKey: String): Map<DiscordLocale, String> {
            if (alunaProperties.debug.showTranslationKeys == AlunaDebugProperties.ShowTranslationKeys.ALL) {
                logger.debug("Load localization for key: $localizationKey")
            }

            val map: MutableMap<DiscordLocale, String> = HashMap()
            for (bundle in bundles) {
                val resourceBundle = bundle.resourceBundle
                if (resourceBundle.containsKey(localizationKey)) {
                    map[bundle.targetLocale] = resourceBundle.getString(localizationKey)
                } else {
                    if (!missingTranslations.containsKey(bundle.targetLocale)) {
                        missingTranslations[bundle.targetLocale] = arrayListOf()
                    }
                    missingTranslations[bundle.targetLocale]!!.add(localizationKey)
                }
            }

            if (alunaProperties.debug.showTranslationKeys == AlunaDebugProperties.ShowTranslationKeys.ONLY_MISSING &&
                map.isEmpty()
            ) {
                logger.debug("Found missing localization for key: $localizationKey")
            }

            return map
        }

        /**
         * Builder for [ResourceBundleLocalizationFunction]
         * <br></br>Use the factory methods in [ResourceBundleLocalizationFunction] to create instances of this builder
         *
         * @see ResourceBundleLocalizationFunction.fromBundle
         * @see ResourceBundleLocalizationFunction.fromBundles
         */
        class Builder {
            private val bundles: MutableSet<Bundle> = HashSet()

            /**
             * Adds a resource bundle to this builder
             *
             *
             * You can see [.fromBundle] for an example
             *
             * @param  resourceBundle
             * The [ResourceBundle] to get the localized strings from
             *
             * @param  locale
             * The [DiscordLocale] of the resources
             *
             * @throws IllegalArgumentException
             *
             *  * If the resource bundle is null
             *  * If the locale is null
             *  * If the locale is [DiscordLocale.UNKNOWN]
             *
             *
             * @return This builder for chaining convenience
             *
             * @see .fromBundle
             */
            @Nonnull
            fun addBundle(@Nonnull resourceBundle: ResourceBundle, @Nonnull locale: DiscordLocale): Builder {
                Checks.notNull(resourceBundle, "Resource bundle")
                Checks.notNull(locale, "Locale")
                Checks.check(locale != DiscordLocale.UNKNOWN, "Cannot use UNKNOWN DiscordLocale")
                bundles.add(Bundle(locale, resourceBundle))
                return this
            }

            /**
             * Adds a resource bundle to this builder
             * <br></br>This will insert the resource bundles with the specified name, with each specified locale.
             *
             *
             * You can see [.fromBundles] for an example
             *
             * @param  baseName
             * The base name of the resource bundle, for example, the base name of `"MyBundle_fr_FR.properties"` would be `"MyBundle"`
             *
             * @param  locales
             * The locales to get from the resource bundle
             *
             * @throws IllegalArgumentException
             *
             *  * If the base name is null
             *  * If the locales or one of the locale is null
             *  * If one of the locale is [DiscordLocale.UNKNOWN]
             *
             *
             * @return This builder for chaining convenience
             * @see .fromBundles
             */
            @Nonnull
            fun addBundles(@Nonnull baseName: String?, @Nonnull vararg locales: DiscordLocale): Builder {
                Checks.notNull(baseName, "Base name")
                Checks.noneNull(locales, "Locale")
                for (locale in locales) {
                    Checks.check(locale != DiscordLocale.UNKNOWN, "Cannot use UNKNOWN DiscordLocale")
                    val resourceBundle = ResourceBundle.getBundle(baseName, Locale.forLanguageTag(locale.locale))
                    bundles.add(Bundle(locale, resourceBundle))
                }
                return this
            }

            /**
             * Builds the resource bundle localization function.
             *
             * @return The new [ResourceBundleLocalizationFunction]
             */
            @Nonnull
            fun build(
                alunaProperties: AlunaProperties,
                missingTranslations: HashMap<DiscordLocale, ArrayList<String>>
            ): AlunaResourceBundleLocalizationFunction {
                return AlunaResourceBundleLocalizationFunction(bundles, alunaProperties, missingTranslations)
            }
        }

        private class Bundle(val targetLocale: DiscordLocale, val resourceBundle: ResourceBundle) {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is Bundle) return false
                return if (targetLocale != other.targetLocale) false else resourceBundle == other.resourceBundle
            }

            override fun hashCode(): Int {
                return Objects.hash(targetLocale, resourceBundle)
            }
        }

        companion object {
            /**
             * Creates an empty [ResourceBundleLocalizationFunction] builder and adds the provided bundle and locale.
             * <br></br>This is the same as using `ResourceBundleLocalizationFunction.empty().addBundle(resourceBundle, locale)`
             *
             *
             * **Example usage:**
             * <br></br>This creates a LocalizationFunction from a French ResourceBundle (MyCommands_fr.properties)
             *
             * <pre>`
             * final LocalizationFunction localizationFunction = ResourceBundleLocalizationFunction
             * .fromBundle(ResourceBundle.getBundle("MyCommands", Locale.FRENCH), DiscordLocale.FRENCH)
             * .build();
            `</pre> *
             *
             * @param  resourceBundle
             * The resource bundle to get the localized strings from
             *
             * @param  locale
             * The locale of the resources
             *
             * @throws IllegalArgumentException
             *
             *  * If the resource bundle is null
             *  * If the locale is null
             *  * If the locale is [DiscordLocale.UNKNOWN]
             *
             *
             * @return The new builder
             */
            @Nonnull
            fun fromBundle(@Nonnull resourceBundle: ResourceBundle, @Nonnull locale: DiscordLocale): Builder {
                return Builder()
                    .addBundle(resourceBundle, locale)
            }

            /**
             * Creates a [ResourceBundleLocalizationFunction] builder with the provided bundles.
             * <br></br>This will insert the resource bundles with the specified name, with each specified locale.
             * <br></br>This is the same as using `ResourceBundleLocalizationFunction.empty().addBundles(baseName, locales)`
             *
             *
             * **Example usage:**
             * <br></br>This creates a LocalizationFunction from 2 resource bundles, one in Spanish (MyCommands_es_ES.properties) and one in French (MyCommands_fr.properties)
             *
             * <pre>`
             * final LocalizationFunction localizationFunction = ResourceBundleLocalizationFunction
             * .fromBundles("MyCommands", DiscordLocale.SPANISH, DiscordLocale.FRENCH)
             * .build();
            `</pre> *
             *
             * @param  baseName
             * The base name of the resource bundle, for example, the base name of `"MyBundle_fr_FR.properties"` would be `"MyBundle"`
             *
             * @param  locales
             * The locales to get from the resource bundle
             *
             * @throws IllegalArgumentException
             *
             *  * If the base name is null
             *  * If the locales or one of the locale is null
             *  * If one of the locale is [DiscordLocale.UNKNOWN]
             *
             *
             * @return The new builder
             */
            @Nonnull
            fun fromBundles(@Nonnull baseName: String?, @Nonnull vararg locales: DiscordLocale): Builder {
                return Builder().addBundles(baseName, *locales)
            }

            /**
             * Creates an empty [ResourceBundleLocalizationFunction] builder.
             *
             * @return The empty builder
             */
            @Nonnull
            fun empty(): Builder {
                return Builder()
            }
        }
    }

}