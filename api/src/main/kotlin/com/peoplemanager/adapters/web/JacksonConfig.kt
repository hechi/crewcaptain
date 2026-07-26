package com.peoplemanager.adapters.web

import tools.jackson.databind.cfg.EnumFeature
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JacksonConfig {

    @Bean
    fun jacksonCustomizer(): JsonMapperBuilderCustomizer {
        return JsonMapperBuilderCustomizer { builder ->
            builder.enable(EnumFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
        }
    }
}
