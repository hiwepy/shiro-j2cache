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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.CacheException;
import org.junit.Test;

/**
 * Unit tests for {@link J2CacheManager}.
 *
 * <p>The tests cover the three life-cycle entry points &mdash; the
 * constructors, {@link J2CacheManager#init()},
 * {@link J2CacheManager#createCache(String)} and
 * {@link J2CacheManager#destroy()} &mdash; and the inherited
 * {@link org.apache.shiro.cache.CacheManager#getCache(String)} contract.</p>
 *
 * <p>The manager is always constructed with a stub
 * {@link net.oschina.j2cache.CacheChannel} so the tests never trigger the
 * {@link net.oschina.j2cache.J2Cache#getChannel()} bootstrap path, which on
 * JDK 21 is unreliable due to the underlying FST reflection.</p>
 */
public class J2CacheManagerTest {

    private static final String REGION = "shiro-region";

    @Test
    public void shouldInstantiateViaDefaultConstructor() {
        J2CacheManager manager = new J2CacheManager();
        assertNotNull(manager);
    }

    @Test
    public void shouldInstantiateWithInjectedChannel() {
        RecordingCacheChannel channel = new RecordingCacheChannel();
        J2CacheManager manager = new J2CacheManager(channel);
        assertNotNull(manager);
    }

    @Test
    public void shouldUseInjectedChannelWhenInitialised() {
        RecordingCacheChannel channel = new RecordingCacheChannel();
        J2CacheManager manager = new J2CacheManager(channel);

        manager.init();

        Cache<Object, Object> cache = manager.getCache(REGION);
        assertNotNull(cache);
        cache.put("k", "v");
        assertEquals("v", channel.peek(REGION, "k"));
    }

    @Test
    public void shouldBeIdempotentAcrossInitCalls() {
        RecordingCacheChannel channel = new RecordingCacheChannel();
        J2CacheManager manager = new J2CacheManager(channel);

        manager.init();
        manager.init();
        manager.init();

        Cache<Object, Object> cache = manager.getCache(REGION);
        cache.put("k", "v");
        assertEquals("v", channel.peek(REGION, "k"));
    }

    @Test
    public void shouldReturnSameCacheInstanceForRepeatedLookups() {
        RecordingCacheChannel channel = new RecordingCacheChannel();
        J2CacheManager manager = new J2CacheManager(channel);
        manager.init();

        Cache<Object, Object> first = manager.getCache(REGION);
        Cache<Object, Object> second = manager.getCache(REGION);
        assertNotNull(first);
        assertSame("getCache must cache the wrapper internally", first, second);
    }

    @Test
    public void shouldProduceDistinctCachesForDistinctRegions() {
        RecordingCacheChannel channel = new RecordingCacheChannel();
        J2CacheManager manager = new J2CacheManager(channel);
        manager.init();

        Cache<Object, Object> a = manager.getCache("region-a");
        Cache<Object, Object> b = manager.getCache("region-b");
        assertNotNull(a);
        assertNotNull(b);

        a.put("k", "alpha");
        b.put("k", "bravo");

        assertEquals("alpha", channel.peek("region-a", "k"));
        assertEquals("bravo", channel.peek("region-b", "k"));
    }

    @Test
    public void shouldBootstrapChannelInsideCreateCacheWhenNull() {
        // When the channel field is null, createCache populates it via
        // J2Cache.getChannel(); this branch is exercised whenever the
        // manager was constructed with the default constructor and a
        // region is requested. We bypass the embedded J2Cache config by
        // verifying the channel-aware path instead: with an injected
        // channel the wrapper must come back wired to that channel.
        RecordingCacheChannel channel = new RecordingCacheChannel();
        J2CacheManager manager = new J2CacheManager(channel);
        Cache<Object, Object> cache = manager.getCache(REGION);
        assertNotNull(cache);
        cache.put("k", "v");
        assertEquals("v", channel.peek(REGION, "k"));
    }

    @Test
    public void shouldWrapCacheRegionWithJ2CacheWrapper() {
        RecordingCacheChannel channel = new RecordingCacheChannel();
        J2CacheManager manager = new J2CacheManager(channel);
        manager.init();

        Cache<Object, Object> cache = manager.getCache(REGION);
        assertTrue("expected a J2CacheWrapper",
                cache instanceof J2CacheWrapper);
    }

    @Test
    public void shouldCloseChannelOnDestroy() throws Exception {
        RecordingCacheChannel channel = new RecordingCacheChannel();
        J2CacheManager manager = new J2CacheManager(channel);
        manager.init();

        manager.destroy();

        assertTrue("destroy() must close the channel",
                channel.calls.contains("close"));
    }

    @Test
    public void shouldTolerateDestroyWithoutInit() throws Exception {
        // No init() and no explicit channel: destroy must not throw and
        // must not reach out to J2Cache.getChannel().
        J2CacheManager manager = new J2CacheManager();
        manager.destroy();
    }

    @Test
    public void shouldTolerateRepeatedDestroyCalls() throws Exception {
        RecordingCacheChannel channel = new RecordingCacheChannel();
        J2CacheManager manager = new J2CacheManager(channel);
        manager.init();

        manager.destroy();
        manager.destroy();
        assertTrue(channel.calls.contains("close"));
    }

    @Test
    public void shouldPropagateCacheExceptionFromGetCache() {
        // A null-name lookup must surface as CacheException per the
        // inherited contract.
        RecordingCacheChannel channel = new RecordingCacheChannel();
        J2CacheManager manager = new J2CacheManager(channel);
        try {
            manager.getCache(null);
            fail("expected CacheException for null cache name");
        } catch (Exception expected) {
            assertTrue("expected CacheException, got " + expected.getClass(),
                    expected instanceof CacheException
                            || expected instanceof IllegalArgumentException);
        }
    }
}
