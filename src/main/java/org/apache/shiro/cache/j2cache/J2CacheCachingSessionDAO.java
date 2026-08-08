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

import java.io.Serializable;

import org.apache.shiro.cache.CacheManager;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.eis.CachingSessionDAO;

/**
 * Shiro {@link CachingSessionDAO} that piggy-backs on
 * {@link J2CacheManager} so all session persistence flows through the same
 * J2Cache cluster as ordinary application caches.
 *
 * <p>This DAO is deliberately minimal: every concrete persistence operation
 * ({@link #doCreate(Session)}, {@link #doReadSession(Serializable)},
 * {@link #doUpdate(Session)} and {@link #doDelete(Session)}) is either a
 * no-op or returns {@code null} because the parent
 * {@link CachingSessionDAO} class already orchestrates reads, writes and
 * evictions against the configured cache. As a result this class exists
 * mostly to:</p>
 * <ul>
 *   <li>force the {@link J2CacheManager#init()} bootstrap when Shiro wires the
 *       DAO into its session manager, ensuring the J2Cache channel is
 *       available before the first session lookup; and</li>
 *   <li>declare a stable integration point that downstream applications can
 *       subclass without touching the parent class contract.</li>
 * </ul>
 *
 * <p>Whether sessions are kept memory-only or replicated across the cluster
 * is entirely a property of the J2Cache configuration bound to the manager
 * &mdash; this DAO makes no assumptions about storage semantics.</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 * @see CachingSessionDAO
 * @see J2CacheManager
 */
public class J2CacheCachingSessionDAO extends CachingSessionDAO {

    /**
     * Replace the cache manager and trigger the J2Cache bootstrap whenever the
     * injected manager is a {@link J2CacheManager}.
     *
     * <p>The base {@link CachingSessionDAO#setCacheManager(CacheManager)}
     * implementation stores the manager reference; this override additionally
     * invokes {@link J2CacheManager#init()} so the underlying
     * {@link net.oschina.j2cache.CacheChannel} is created eagerly. This
     * matters because Shiro may attempt to read or write sessions before any
     * other component has had the chance to initialise J2Cache.</p>
     *
     * @param cacheManager the cache manager supplied by the surrounding
     *                     Shiro environment. When it is a
     *                     {@link J2CacheManager} its {@link J2CacheManager#init()}
     *                     hook is invoked as part of this call.
     */
    @Override
    public void setCacheManager(CacheManager cacheManager) {
        super.setCacheManager(cacheManager);
        if (cacheManager instanceof J2CacheManager) {
            ((J2CacheManager) cacheManager).init();
        }
    }

    /**
     * Generate a new session identifier and bind it to the supplied session.
     *
     * <p>The parent {@link CachingSessionDAO} drives the cache write via its
     * {@link CachingSessionDAO#create(Session)} entry point, which calls this
     * method to obtain a session id before storing the session under that
     * id in the cache. The default {@link org.apache.shiro.session.mgt.eis.SessionIdGenerator}
     * is used to produce the identifier.</p>
     *
     * @param session the session that needs a freshly generated identifier.
     *                The id is assigned back onto the session via
     *                {@link org.apache.shiro.session.mgt.eis.AbstractSessionDAO#assignSessionId(Session, Serializable)}.
     * @return the newly generated session identifier; never {@code null}.
     */
    @Override
    protected Serializable doCreate(Session session) {
        Serializable sessionId = generateSessionId(session);
        assignSessionId(session, sessionId);
        return sessionId;
    }

    /**
     * Read a session from the underlying cache.
     *
     * <p>This method deliberately returns {@code null}: it should never be
     * invoked directly because the parent {@link CachingSessionDAO} resolves
     * every session read against its configured cache (which is backed by
     * J2Cache). The implementation policy of whether the cache is
     * memory-only, replicated, or disk-persistent lives entirely in the
     * J2Cache configuration; this DAO neither knows nor cares.</p>
     *
     * @param sessionId the id of the session being requested.
     * @return always {@code null} &mdash; the cache implementation is the
     *         sole source of truth.
     */
    @Override
    protected Session doReadSession(Serializable sessionId) {
        return null;
        // should never execute because this implementation relies on parent class to access cache, which
        // is where all sessions reside - it is the cache implementation that determines
        // if the cache is memory only or disk-persistent, etc.
    }

    /**
     * Update the cache copy of a session.
     *
     * <p>This is a no-op because the parent {@link CachingSessionDAO} already
     * pushes mutations into the cache before delegating to this hook.</p>
     *
     * @param session the session that has just been mutated. Not inspected
     *                by this implementation.
     */
    @Override
    protected void doUpdate(Session session) {
        // does nothing - parent class persists to cache.
    }

    /**
     * Delete the cache copy of a session.
     *
     * <p>This is a no-op because the parent {@link CachingSessionDAO} already
     * removes the entry from the cache before delegating to this hook.</p>
     *
     * @param session the session that has just been invalidated or
     *                stopped. Not inspected by this implementation.
     */
    @Override
    protected void doDelete(Session session) {
        // does nothing - parent class removes from cache.
    }

}
