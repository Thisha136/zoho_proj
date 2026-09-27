
package com.bikebackend.config;

import redis.clients.jedis.RedisClient;

public class RedisConnection {

    private static final RedisClient redis =
            RedisClient.create("redis://localhost:6379");

    public static RedisClient getRedis() {
        return redis;
    }
}