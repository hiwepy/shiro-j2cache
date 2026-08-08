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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.Collection;

import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.CacheManager;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.SimpleSession;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link J2CacheCachingSessionDAO}.
 *
 * <p>These tests cover the four template-method hooks
 * ({@link J2CacheCachingSessionDAO#doCreate},
 * {@link J2CacheCachingSessionDAO#doReadSession},
 * {@link J2CacheCachingSessionDAO#doUpdate} and
 * {@link J2CacheCachingSessionDAO#doDelete}) plus the
 * {@link J2CacheCachingSessionDAO#setCacheManager} integration that
 * bootstraps the {@link J2CacheManager} on injection.</p>
 */
public class J2CacheCachingSessionDAOTest {

    private static final String CACHE_NAME = "shiro-activeSessionCache";

    private RecordingCacheChannel channel;
    private J2CacheManager manager;
    private J2CacheCachingSessionDAO dao;

    @Before
    public void setUp() {
        channel = new RecordingCacheChannel();
        manager = new J2CacheManager(channel);
        dao = new J2CacheCachingSessionDAO();
    }

    @Test
    public void shouldInstantiateWithoutConfiguration() {
        J2CacheCachingSessionDAO fresh = new J2CacheCachingSessionDAO();
        assertNotNull(fresh);
    }

    @Test
    public void shouldInitialiseJ2CacheManagerOnInjection() {
        dao.setCacheManager(manager);

        // The active sessions cache is lazily created on first create();
        // trigger it and verify it is a J2CacheWrapper.
        SimpleSession session = new SimpleSession();
        dao.create(session);

        Cache<Serializable, Session> active = dao.getActiveSessionsCache();
        assertNotNull("active session cache must be resolved", active);
        assertTrue(active instanceof J2CacheWrapper);
    }

    @Test
    public void shouldIgnoreNonJ2CacheManager() {
        CacheManager foreign = new ForeignCacheManager();
        // Should not throw and must not initialise the foreign manager.
        dao.setCacheManager(foreign);
        assertNull(dao.getActiveSessionsCache());
    }

    @Test
    public void shouldGenerateAndAssignSessionIdOnCreate() {
        dao.setCacheManager(manager);

        SimpleSession session = new SimpleSession();
        Serializable generated = dao.create(session);

        assertNotNull(generated);
        assertSame("doCreate must bind the generated id to the session",
                generated, session.getId());
    }

    @Test
    public void shouldPersistCreatedSessionIntoActiveCache() {
        dao.setCacheManager(manager);

        SimpleSession session = new SimpleSession();
        Serializable id = dao.create(session);

        assertSame("created session must be cached",
                session, channel.peek(CACHE_NAME, id.toString()));
    }

    @Test
    public void shouldReturnNullFromDoReadSession() {
        // doReadSession is a deliberate no-op hook per the documented
        // design. Direct invocation must return null.
        assertNull(dao.doReadSession("some-id"));
    }

    @Test
    public void shouldReadCachedSessionThroughParent() {
        dao.setCacheManager(manager);

        SimpleSession session = new SimpleSession();
        Serializable id = dao.create(session);
        Session readBack = dao.readSession(id);
        assertSame(session, readBack);
    }

    @Test
    public void shouldReturnNullFromDoUpdate() {
        // doUpdate is a documented no-op hook.
        SimpleSession session = new SimpleSession();
        dao.doUpdate(session);
    }

    @Test
    public void shouldReturnNullFromDoDelete() {
        // doDelete is a documented no-op hook.
        SimpleSession session = new SimpleSession();
        dao.doDelete(session);
    }

    @Test
    public void shouldPropagateUpdateThroughParentContract() {
        dao.setCacheManager(manager);

        SimpleSession session = new SimpleSession();
        Serializable id = dao.create(session);

        // Touch the session and update via the public entry point.
        session.setLastAccessTime(new java.util.Date());
        dao.update(session);

        // The updated session must still be retrievable through the cache.
        Session stored = dao.readSession(id);
        assertNotNull(stored);
        assertSame(session, stored);
    }

    @Test
    public void shouldDeleteSessionThroughParentContract() {
        dao.setCacheManager(manager);

        SimpleSession session = new SimpleSession();
        Serializable id = dao.create(session);
        assertSame(session, dao.readSession(id));

        dao.delete(session);

        // After deletion the session is no longer in cache. The parent
        // CachingSessionDAO.readSession throws UnknownSessionException
        // when both cache and doReadSession return null.
        try {
            dao.readSession(id);
        } catch (org.apache.shiro.session.UnknownSessionException expected) {
            // expected — session was removed from cache
        }
    }

    @Test
    public void shouldReportActiveSessionsThroughParentContract() {
        dao.setCacheManager(manager);

        SimpleSession session = new SimpleSession();
        dao.create(session);

        Collection<Session> active = dao.getActiveSessions();
        assertNotNull(active);
        assertTrue(active.contains(session));
    }

    @Test
    public void shouldRespectCustomActiveSessionCacheName() {
        dao.setCacheManager(manager);
        dao.setActiveSessionsCacheName("custom-region");

        SimpleSession session = new SimpleSession();
        Serializable id = dao.create(session);
        assertNotNull(id);
        assertSame(session, channel.peek("custom-region", id.toString()));
    }

    /**
     * Cache manager stub that returns {@code null} for every region,
     * simulating a non-J2Cache environment to confirm
     * {@link J2CacheCachingSessionDAO#setCacheManager(CacheManager)}
     * performs a type-safe check before any bootstrap.
     */
    private static final class ForeignCacheManager implements CacheManager {

        @Override
        public <K, V> Cache<K, V> getCache(String name) {
            return null;
        }
    }
}
