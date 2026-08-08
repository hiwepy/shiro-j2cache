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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link J2CacheWrapper}.
 *
 * <p>These tests verify that the Shiro {@code Cache} contract is correctly
 * forwarded to the underlying J2Cache {@link net.oschina.j2cache.CacheChannel}
 * and that the value type parameter is honoured on retrieval.</p>
 */
public class J2CacheWrapperTest {

    private static final String REGION = "shiro-test-region";

    private RecordingCacheChannel channel;
    private J2CacheWrapper<String> wrapper;

    @Before
    public void setUp() {
        channel = new RecordingCacheChannel();
        wrapper = new J2CacheWrapper<String>(REGION, channel);
    }

    @Test
    public void shouldExposeConstructorArguments() {
        assertNotNull(wrapper);
    }

    @Test
    public void shouldReturnNullWhenKeyAbsent() {
        assertNull(wrapper.get("missing"));
        assertTrue(channel.calls.contains("get:shiro-test-region:missing"));
    }

    @Test
    public void shouldReturnStoredValueOnGet() {
        wrapper.put("alpha", "first");
        assertEquals("first", wrapper.get("alpha"));
    }

    @Test
    public void shouldDelegatePutToChannel() {
        wrapper.put("alpha", "first");

        assertEquals("first", channel.peek(REGION, "alpha"));
        assertTrue(channel.calls.contains("set:shiro-test-region:alpha=first"));
    }

    @Test
    public void shouldReturnNullFromPut() {
        assertNull(wrapper.put("alpha", "first"));
    }

    @Test
    public void shouldDelegateRemoveToChannel() {
        wrapper.put("alpha", "first");
        assertNull(wrapper.remove("alpha"));
        assertNull(channel.peek(REGION, "alpha"));
        assertTrue(channel.calls.contains("evict:shiro-test-region:alpha"));
    }

    @Test
    public void shouldDelegateClearToChannel() {
        wrapper.put("a", "1");
        wrapper.put("b", "2");
        wrapper.clear();

        assertTrue(channel.regionKeys(REGION).isEmpty());
        assertTrue(channel.calls.contains("clear:shiro-test-region"));
    }

    @Test
    public void shouldReportSizeBasedOnChannelKeys() {
        assertEquals(0, wrapper.size());

        wrapper.put("a", "1");
        wrapper.put("b", "2");
        wrapper.put("c", "3");
        assertEquals(3, wrapper.size());
    }

    @Test
    public void shouldReturnSnapshotOfKeys() {
        wrapper.put("a", "1");
        wrapper.put("b", "2");

        Set<String> keys = wrapper.keys();
        assertNotNull(keys);
        assertTrue(keys.contains("a"));
        assertTrue(keys.contains("b"));
        assertEquals(2, keys.size());
    }

    @Test
    public void shouldReturnValuesMatchingKeys() {
        wrapper.put("a", "1");
        wrapper.put("b", "2");

        Collection<String> values = wrapper.values();
        assertNotNull(values);
        assertEquals(2, values.size());
        assertTrue(values.contains("1"));
        assertTrue(values.contains("2"));
    }

    @Test
    public void shouldReturnEmptyValuesWhenRegionIsEmpty() {
        Collection<String> values = wrapper.values();
        assertNotNull(values);
        assertTrue(values.isEmpty());
    }

    @Test
    public void shouldReturnEmptyKeysWhenRegionIsEmpty() {
        Set<String> keys = wrapper.keys();
        assertNotNull(keys);
        assertTrue(keys.isEmpty());
    }

    @Test
    public void shouldSupportNullValuePut() {
        // The wrapper does not forbid null values; the channel decides
        // whether to persist them.
        wrapper.put("nullable", null);
        assertNull(channel.peek(REGION, "nullable"));
    }

    @Test
    public void shouldReturnNullAfterRemove() {
        wrapper.put("alpha", "first");
        assertNull(wrapper.remove("alpha"));
        assertNull(wrapper.get("alpha"));
    }

    @Test
    public void shouldReturnFreshKeySetInstance() {
        wrapper.put("a", "1");
        Set<String> first = wrapper.keys();
        Set<String> second = wrapper.keys();
        assertFalse("each call should return a fresh set",
                first == second);
    }
}
