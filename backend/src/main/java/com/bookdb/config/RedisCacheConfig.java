package com.bookdb.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

/**
 * All read caching goes through Spring's {@code @Cacheable}/{@code @CacheEvict}, backed by
 * Redis. The TTL here is a defense-in-depth safety net only - correctness (no stale reads
 * after a write) relies on the explicit {@code @CacheEvict} wiring on every write path in
 * BookService/AuthorService/CategoryService/ReviewService, not on this expiry.
 */
@Configuration
public class RedisCacheConfig {

    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer(ObjectMapper objectMapper) {
        // GenericJackson2JsonRedisSerializer needs default typing active on whatever mapper it's
        // given (its own no-arg constructor does this for you; since we reuse the app's mapper -
        // to keep Instant/etc. serialization consistent with the REST layer - we activate it here).
        // deserialize(byte[]) is called with no type hint (Object.class), so it needs an embedded
        // "@class" to know what to reconstruct - EVERYTHING (not NON_FINAL) is required because
        // every cached value here is a record, and records are implicitly final: NON_FINAL skips
        // adding "@class" for final types (root included), which fails deserialization outright.
        ObjectMapper redisMapper = objectMapper.copy();
        redisMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.EVERYTHING, JsonTypeInfo.As.PROPERTY);
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(redisMapper);
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));
        return builder -> builder.cacheDefaults(defaultConfig);
    }
}
