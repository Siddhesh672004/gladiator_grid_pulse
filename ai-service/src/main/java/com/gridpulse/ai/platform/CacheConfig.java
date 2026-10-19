package com.gridpulse.ai.platform;

import java.time.Duration;
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

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

// redis cache (keys gp:<cache>::<key>), like the core's: a json serializer for the exact java type per cache
// redis down -> logged, gemini answers instead. CACHE_TYPE=none = no cache, no redis needed
@Slf4j
@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    public static final String EMBEDDINGS = "embeddings";

    @Bean
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = true)
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        Map<String, RedisCacheConfiguration> caches = Map.of(
                EMBEDDINGS, cache(objectMapper, objectMapper.getTypeFactory().constructType(float[].class),
                        Duration.ofHours(24)));
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
        log.warn("Cache '{}' could not {} ({}) - gemini answers instead", cache.getName(), action,
                e.getClass().getSimpleName());
    }
}
