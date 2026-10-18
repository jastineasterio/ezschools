package com.edumanage.config.cache;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class CacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    public CacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public <T> void put(String key, T value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key, Class<T> clazz) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of((T) value);
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }

    public void deleteByPattern(String pattern) {
        var keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    public boolean exists(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
