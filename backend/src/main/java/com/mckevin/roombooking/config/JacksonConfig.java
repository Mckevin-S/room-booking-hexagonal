package com.mckevin.roombooking.config;

import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import java.time.format.DateTimeFormatter;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Les heures circulent au format « HH:mm » : la granularité des réservations est le quart d'heure. */
@Configuration(proxyBeanMethods = false)
public class JacksonConfig {

    private static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("HH:mm");

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer hourFormat() {
        return builder -> builder.serializers(new LocalTimeSerializer(HOUR)).deserializers(new LocalTimeDeserializer(HOUR));
    }
}
