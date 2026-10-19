package com.gridpulse.core.platform.config;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.Locale;
import java.util.Optional;

import org.springframework.context.annotation.Configuration;
import org.springframework.format.Formatter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// reads a time from the url (?startTime=06:00:00) as LocalTime
// some proxies encode the query string twice ("06%3A00%3A00"), so a value with "%" is decoded once more
@Configuration
public class TimeParamConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addFormatterForFieldType(LocalTime.class, new Formatter<LocalTime>() {

            @Override
            public LocalTime parse(String text, Locale locale) {
                String value = text.trim();
                // a "%" still in it = the proxy encoded it twice
                String time = Optional.of(value)
                        .filter(encoded -> encoded.contains("%"))
                        .map(encoded -> URLDecoder.decode(encoded, StandardCharsets.UTF_8))
                        .orElse(value);
                return LocalTime.parse(time);
            }

            @Override
            public String print(LocalTime time, Locale locale) {
                return time.toString();
            }
        });
    }
}
