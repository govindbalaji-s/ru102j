package com.redislabs.university.RU102J.dao;

import com.redislabs.university.RU102J.api.MeterReading;
import redis.clients.jedis.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FeedDaoRedisImpl implements FeedDao {

    private final JedisPool jedisPool;
    private static final long GLOBAL_MAX_FEED_LENGTH = 10000;
    private static final long SITE_MAX_FEED_LENGTH = 2440;

    public FeedDaoRedisImpl(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    // Challenge #6
    @Override
    public void insert(MeterReading meterReading) {
        // START Challenge #6
        Map<String, String> meterReadingMap = meterReading.toMap();
        Long siteId = meterReading.getSiteId();
        try (Jedis jedis = jedisPool.getResource();
             Pipeline pipeline = jedis.pipelined()) {
            pipeline.xadd(RedisSchema.getGlobalFeedKey(), StreamEntryID.NEW_ENTRY, meterReadingMap, GLOBAL_MAX_FEED_LENGTH,
                    true);
            pipeline.xadd(RedisSchema.getFeedKey(siteId), StreamEntryID.NEW_ENTRY, meterReadingMap, SITE_MAX_FEED_LENGTH,
                    true);
        }
        // END Challenge #6
    }

    @Override
    public List<MeterReading> getRecentGlobal(int limit) {
        return getRecent(RedisSchema.getGlobalFeedKey(), limit);
    }

    @Override
    public List<MeterReading> getRecentForSite(long siteId, int limit) {
        return getRecent(RedisSchema.getFeedKey(siteId), limit);
    }

    public List<MeterReading> getRecent(String key, int limit) {
        List<MeterReading> readings = new ArrayList<>(limit);
        try (Jedis jedis = jedisPool.getResource()) {
            List<StreamEntry> entries = jedis.xrevrange(key, null,
                    null, limit);
            for (StreamEntry entry : entries) {
                readings.add(new MeterReading(entry.getFields()));
            }
            return readings;
        }
    }
}
