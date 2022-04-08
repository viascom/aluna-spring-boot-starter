package io.viascom.discord.bot.starter.configuration

import com.google.gson.Gson
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.converter.ByteArrayHttpMessageConverter
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.http.converter.json.GsonHttpMessageConverter
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
@ConditionalOnWebApplication
@ConditionalOnMissingBean(WebMvcConfigurer::class)
@Suppress("SpringJavaInjectionPointsAutowiringInspection")
open class WebAppConfig(
    private val gson: Gson,
) : WebMvcConfigurer {

    override fun configureMessageConverters(converters: MutableList<HttpMessageConverter<*>>) {
        val gsonConverter = GsonHttpMessageConverter()
        gsonConverter.gson = gson
        converters.add(StringHttpMessageConverter())
        converters.add(gsonConverter)
        converters.add(ByteArrayHttpMessageConverter())
        super.configureMessageConverters(converters);
    }

    override fun configureContentNegotiation(configurer: ContentNegotiationConfigurer) {
        configurer.defaultContentType(MediaType.APPLICATION_JSON)
    }

    @Bean
    open fun corsConfigurer(): WebMvcConfigurer? {
        return object : WebMvcConfigurer {
            override fun addCorsMappings(registry: CorsRegistry) {
                registry.addMapping("/**").allowedOrigins("*").allowedMethods(
                    *arrayListOf(
                        HttpMethod.GET.name,
                        HttpMethod.HEAD.name,
                        HttpMethod.POST.name,
                        HttpMethod.PUT.name,
                        HttpMethod.DELETE.name
                    ).toTypedArray()
                )
            }
        }
    }

}
