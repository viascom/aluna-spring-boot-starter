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
