package com.ecommerce.productservice.config;

import com.ecommerce.productservice.dto.ProductResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Duration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

@Configuration
@EnableCaching
public class RedisConfig {

  @Bean
  public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
    ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofMinutes(10));

    // GenericJackson2JsonRedisSerializer relies on an embedded "@class" property to know
    // the target type on read; ProductResponse is a record (implicitly final), so Jackson's
    // default-typing modes never emit that property for it and reads come back as a raw
    // LinkedHashMap instead. Binding a type-specific serializer to this cache avoids that,
    // without turning on class-based polymorphic typing just for one DTO.
    RedisCacheConfiguration productsConfig = defaultConfig.serializeValuesWith(
        RedisSerializationContext.SerializationPair.fromSerializer(
            new Jackson2JsonRedisSerializer<>(objectMapper, ProductResponse.class)));

    return RedisCacheManager.builder(connectionFactory)
        .cacheDefaults(defaultConfig)
        .withCacheConfiguration("products", productsConfig)
        .build();
  }
}
