/*
 * Copyright (c) 2018-present, easy-4-java (https://github.com/easy-4-java).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.shiro.cache.j2cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.oschina.j2cache.CacheChannel;
import net.oschina.j2cache.CacheObject;
import net.oschina.j2cache.CacheProviderHolder;
import net.oschina.j2cache.J2CacheConfig;

/**
 * In-memory {@link CacheChannel} stub used exclusively by unit tests.
 *
 * <p>The stub overrides every inherited method used by
 * {@link J2CacheWrapper} and {@link J2CacheManager} so it never touches the
 * parent's private {@code holder} or {@code config} fields. Operations are
 * recorded against a per-region {@link LinkedHashMap}, and a {@link List} of
 * invocations is retained for behavioural assertions.</p>
 */
class RecordingCacheChannel extends CacheChannel {

    /** Region-keyed backing store for the stub. */
    final Map<String, Map<String, Object>> regions = new LinkedHashMap<>();

    /** Per-call invocation log used by tests to assert call ordering. */
    final List<String> calls = new ArrayList<>();

    /**
     * Construct a stub channel. The parent constructor is invoked with a
     * default {@link J2CacheConfig} so the field initialisation runs without
     * a {@link NullPointerException}; the stub never accesses the inherited
     * {@code config} or {@code holder} fields.
     */
    RecordingCacheChannel() {
        super(new J2CacheConfig(), (CacheProviderHolder) null);
    }

    private Map<String, Object> regionMap(String region) {
        return regions.computeIfAbsent(region, r -> new LinkedHashMap<>());
    }

    @Override
    public CacheObject get(String region, String key, boolean... cacheNullObject) {
        calls.add("get:" + region + ":" + key);
        Object value = regionMap(region).get(key);
        return new CacheObject(region, key, CacheObject.LEVEL_1, value);
    }

    @Override
    public void set(String region, String key, Object value) {
        calls.add("set:" + region + ":" + key + "=" + value);
        regionMap(region).put(key, value);
    }

    @Override
    public void evict(String region, String... keys) {
        for (String key : keys) {
            calls.add("evict:" + region + ":" + key);
            regionMap(region).remove(key);
        }
    }

    @Override
    public void clear(String region) {
        calls.add("clear:" + region);
        Map<String, Object> map = regions.get(region);
        if (map != null) {
            map.clear();
        }
    }

    @Override
    public Collection<String> keys(String region) {
        calls.add("keys:" + region);
        Map<String, Object> map = regions.get(region);
        if (map == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(map.keySet());
    }

    @Override
    public void close() {
        calls.add("close");
    }

    @Override
    protected void sendClearCmd(String region) {
        // no-op for the stub
    }

    @Override
    protected void sendEvictCmd(String region, String... keys) {
        // no-op for the stub
    }

    /**
     * Helper for tests that want to look up a recorded value without going
     * through the wrapper.
     *
     * @param region the region name
     * @param key the cache key
     * @return the stored value, or {@code null} if absent
     */
    Object peek(String region, String key) {
        Map<String, Object> map = regions.get(region);
        return map == null ? null : map.get(key);
    }

    /**
     * Helper for tests that need to assert the union of all keys in the
     * region.
     *
     * @param region the region name
     * @return the underlying key set (never {@code null})
     */
    Set<String> regionKeys(String region) {
        Map<String, Object> map = regions.get(region);
        return map == null ? java.util.Collections.emptySet() : map.keySet();
    }
}
