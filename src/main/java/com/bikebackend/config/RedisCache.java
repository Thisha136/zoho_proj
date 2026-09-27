package com.bikebackend.config;

import com.bikebackend.model.Bike;
import com.bikebackend.model.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import redis.clients.jedis.RedisClient;

import java.util.List;

/** Fail-open Redis cache-aside helper. Database remains the source of truth. */
public final class RedisCache {
    private static final long TTL_SECONDS = 300;
    private static final RedisClient REDIS = RedisConnection.getRedis();
    private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();
    public static final TypeReference<List<User>> USERS = new TypeReference<>() {};
    public static final TypeReference<List<Bike>> BIKES = new TypeReference<>() {};

    private RedisCache() {}

    public static <T> T get(String key, Class<T> type) {
        try {
            String value = REDIS.get(key);
            return value == null ? null : MAPPER.readValue(value, type);
        } catch (Exception ignored) { return null; }
    }

    public static <T> T get(String key, TypeReference<T> type) {
        try {
            String value = REDIS.get(key);
            return value == null ? null : MAPPER.readValue(value, type);
        } catch (Exception ignored) { return null; }
    }

    public static void put(String key, Object value) {
        try {
            REDIS.set(key, MAPPER.writeValueAsString(value));
            REDIS.expire(key, TTL_SECONDS);
        } catch (Exception ignored) {}
    }

    public static void evict(String key) { try { REDIS.del(key); } catch (Exception ignored) {} }

    public static long version(String key) {
        try {
            String value = REDIS.get(key);
            if (value == null) {
                REDIS.set(key, "1");
                REDIS.expire(key, 86400);
                return 1;
            }
            return Long.parseLong(value);
        } catch (Exception ignored) { return 0; }
    }

    public static void bumpVersion(String key) {
        try { REDIS.incr(key); REDIS.expire(key, 86400); } catch (Exception ignored) {}
    }

    public static String userKey(long uid) { return "vrm:user:" + uid; }
    public static String bikeKey(long bikeId) { return "vrm:bike:" + bikeId; }
    public static String usersVersionKey() { return "vrm:users:version"; }
    public static String bikesVersionKey() { return "vrm:bikes:version"; }
    public static String userListKey(long version, String search, String status, String sort, String order) {
        return "vrm:users:list:" + version + ":" + part(search) + ":" + part(status) + ":" + part(sort) + ":" + part(order);
    }
    public static String bikesByUserKey(long version, long uid) { return "vrm:bikes:user:" + version + ":" + uid; }
    public static String allBikesKey(long version, Long uid, Long variantId) { return "vrm:bikes:all:" + version + ":" + uid + ":" + variantId; }
    private static String part(Object value) { return value == null ? "null" : String.valueOf(value).replace(":", "%3A"); }
}
