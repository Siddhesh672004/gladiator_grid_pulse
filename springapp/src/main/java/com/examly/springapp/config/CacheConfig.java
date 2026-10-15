package com.examly.springapp.config;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

import com.examly.springapp.dto.response.CompetitionResponse;
import com.examly.springapp.dto.response.TrackResponse;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

import lombok.extern.slf4j.Slf4j;

// redis cache (keys gp:<cache>::<key>). every cache gets a json serializer for its exact java type:
// the records are final, so no class names go into the json. redis down -> logged, the database answers instead
@Slf4j
@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    public static final String TRACKS = "tracks";
    public static final String TRACK = "track";
    public static final String COMPETITIONS = "competitions";

    // CACHE_TYPE=none (tests, a server without redis) -> spring's no-op cache, nothing is stored
    @Bean
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = true)
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        TypeFactory types = objectMapper.getTypeFactory();
        Map<String, RedisCacheConfiguration> caches = Map.of(
                TRACKS, cache(objectMapper, types.constructCollectionType(List.class, TrackResponse.class),
                        Duration.ofSeconds(30)),
                TRACK, cache(objectMapper, types.constructType(TrackResponse.class), Duration.ofSeconds(30)),
                COMPETITIONS, cache(objectMapper, types.constructCollectionType(List.class, CompetitionResponse.class),
                        Duration.ofSeconds(60)));
        return RedisCacheManager.builder(connectionFactory)
                .withInitialCacheConfigurations(caches)
                // a cache that is not listed above is a typo, not something to store blindly
                .disableCreateOnMissingCache()
                .build();
    }

    private RedisCacheConfiguration cache(ObjectMapper objectMapper, JavaType type, Duration ttl) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .disableCachingNullValues()
                .prefixCacheNameWith("gp:")
                .serializeValuesWith(SerializationPair.fromSerializer(new Jackson2JsonRedisSerializer<>(objectMapper, type)));
    }

    // redis down or a broken entry: one warning, the method runs as if there was no cache
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {

            @Override
            public void handleCacheGetError(RuntimeException e, Cache cache, Object key) {
                warn("read", cache, e);
            }

            @Override
            public void handleCachePutError(RuntimeException e, Cache cache, Object key, Object value) {
                warn("write", cache, e);
            }

            @Override
            public void handleCacheEvictError(RuntimeException e, Cache cache, Object key) {
                warn("clear", cache, e);
            }

            @Override
            public void handleCacheClearError(RuntimeException e, Cache cache) {
                warn("clear", cache, e);
            }
        };
    }

    private void warn(String action, Cache cache, RuntimeException e) {
        log.warn("Cache '{}' could not {} ({}) - the database answers instead", cache.getName(), action,
                e.getClass().getSimpleName());
    }
}
