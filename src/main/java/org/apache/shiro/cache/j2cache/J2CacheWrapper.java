/*
 * Copyright (c) 2018, Loong Wan (https://github.com/loong10k).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.shiro.cache.j2cache;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.CacheException;

import net.oschina.j2cache.CacheChannel;
import net.oschina.j2cache.CacheObject;

/**
 * A Shiro {@link Cache} adapter that translates the Shiro cache contract
 * onto a single J2Cache region.
 *
 * <p>Each wrapper is bound to a single J2Cache region (identified by
 * {@link #region}) and re-uses the supplied {@link CacheChannel} for all
 * read, write and eviction operations. Multiple wrappers can share the same
 * channel concurrently because the underlying J2Cache layer is
 * thread-safe.</p>
 *
 * <p>The wrapper deliberately follows the Shiro {@code Cache} contract which
 * uses {@code String} keys and value-type parameters; the value type is
 * exposed via the generic parameter {@code V}. The unchecked casts inside
 * the implementation are intentional because J2Cache stores {@code Object}
 * values.</p>
 *
 * @param <V> the value type stored in the underlying J2Cache region
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 * @see Cache
 * @see CacheChannel
 * @see J2CacheManager
 */
@SuppressWarnings("unchecked")
public class J2CacheWrapper<V> implements Cache<String, V> {

    /**
     * The J2Cache region name this wrapper is bound to. Set during
     * construction and never mutated afterwards.
     */
    protected String region;

    /**
     * The J2Cache channel used to satisfy every cache operation. Set during
     * construction and never mutated afterwards.
     */
    protected CacheChannel channel;

    /**
     * Create a wrapper bound to the supplied region and channel.
     *
     * @param region  the J2Cache region name; must not be {@code null}.
     * @param channel the J2Cache channel used for the actual cache calls;
     *                must not be {@code null}.
     */
    public J2CacheWrapper(String region, CacheChannel channel) {
        this.region = region;
        this.channel = channel;
    }

    /**
     * Read a value from the underlying J2Cache region.
     *
     * @param key the cache key; never {@code null} in typical Shiro usage.
     * @return the value previously stored against {@code key}, or
     *         {@code null} if the key is absent.
     * @throws CacheException propagated from the underlying J2Cache call if
     *                         the channel has been closed or any other
     *                         transport-level failure occurs.
     */
    @Override
    public V get(String  key) throws CacheException {
        CacheObject val = this.channel.get(region, key);
        if (val == null)
            return null;
        return (V) val.getValue();
    }

    /**
     * Store a value in the underlying J2Cache region.
     *
     * @param key   the cache key; never {@code null} in typical Shiro usage.
     * @param value the value to associate with {@code key}; may be
     *              {@code null} in which case J2Cache decides whether to
     *              store a null sentinel based on its global configuration.
     * @return always {@code null}; the Shiro contract does not use the
     *         previous-value return for {@code put}.
     * @throws CacheException propagated from the underlying J2Cache call if
     *                         the channel has been closed or any other
     *                         transport-level failure occurs.
     */
    @Override
    public V put(String key, V value) throws CacheException {
        this.channel.set(region, key, value);
        return null;
    }

    /**
     * Remove a single entry from the underlying J2Cache region.
     *
     * @param key the cache key to evict; never {@code null} in typical
     *            Shiro usage.
     * @return always {@code null}; the Shiro contract does not use the
     *         previous-value return for {@code remove}.
     * @throws CacheException propagated from the underlying J2Cache call if
     *                         the channel has been closed or any other
     *                         transport-level failure occurs.
     */
    @Override
    public V remove(String key) throws CacheException {
        this.channel.evict(region, key);
        return null;
    }

    /**
     * Remove every entry from the underlying J2Cache region. Other regions
     * served by the same channel are not affected.
     *
     * @throws CacheException propagated from the underlying J2Cache call if
     *                         the channel has been closed or any other
     *                         transport-level failure occurs.
     */
    @Override
    public void clear() throws CacheException {
        this.channel.clear(region);
    }

    /**
     * Return the number of keys currently held in the underlying J2Cache
     * region.
     *
     * <p>For some J2Cache providers the underlying call may be expensive
     * (notably {@code ehcache3} which does not support key enumeration); in
     * those deployments callers should avoid relying on the size.</p>
     *
     * @return the number of keys in the region; never negative.
     */
    @Override
    public int size() {
        return this.channel.keys(region).size();
    }

    /**
     * Return every key currently held in the underlying J2Cache region.
     *
     * @return a fresh {@link HashSet} snapshot of the region's keys; the set
     *         is detached from the underlying cache and may be safely
     *         iterated by the caller.
     */
    @Override
    public Set<String> keys() {
        return new HashSet<String>(this.channel.keys(region));
    }

    /**
     * Return every value currently held in the underlying J2Cache region.
     *
     * <p>The result is materialised eagerly by iterating over the key set
     * and calling {@link #get(String)} for each entry; this means a missing
     * entry discovered between the key snapshot and the lookup will appear
     * as a {@code null} in the returned list.</p>
     *
     * @return a {@link List} containing the values currently visible in the
     *         region. The list is freshly allocated and may be modified by
     *         the caller.
     */
    @Override
    public Collection<V> values() {
        List<V> list = new ArrayList<V>();
        for (String k : keys()) {
            list.add(get(k));
        }
        return list;
    }

}
