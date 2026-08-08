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


import org.apache.shiro.ShiroException;
import org.apache.shiro.cache.AbstractCacheManager;
import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.CacheException;
import org.apache.shiro.util.Destroyable;
import org.apache.shiro.util.Initializable;

import net.oschina.j2cache.CacheChannel;
import net.oschina.j2cache.J2Cache;

/**
 * Shiro {@link org.apache.shiro.cache.CacheManager} backed by a J2Cache
 * {@link CacheChannel}.
 *
 * <p>The manager lazily resolves a J2Cache {@link CacheChannel} on first use
 * (either via {@link #init()} or via the first call to
 * {@link #createCache(String)}) and exposes a per-region Shiro {@link Cache}
 * through {@link #getCache(String)} which is implemented in the abstract base
 * class.</p>
 *
 * <p>The class implements {@link Initializable} so the surrounding
 * {@code SecurityManager} can trigger J2Cache bootstrap during application
 * start-up, and {@link Destroyable} so that the underlying
 * {@link CacheChannel} is properly released when the security infrastructure
 * is shut down (typically during container stop).</p>
 *
 * <p>Typical usage is to register this manager as a Shiro bean and pair it
 * with {@link J2CacheCachingSessionDAO} so that session data also flows
 * through the same J2Cache cluster.</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 3.0.0
 * @see Cache
 * @see AbstractCacheManager
 * @see Initializable
 * @see Destroyable
 * @see J2CacheWrapper
 * @see J2CacheCachingSessionDAO
 */
public class J2CacheManager extends AbstractCacheManager implements Initializable, Destroyable {

    /**
     * The J2Cache channel used to publish cache reads, writes and evictions.
     * May be {@code null} until {@link #init()} or {@link #createCache(String)}
     * has been invoked for the first time, in which case it is populated via
     * {@link J2Cache#getChannel()}.
     */
    protected CacheChannel channel;

    /**
     * Default no-argument constructor; suitable for reflective instantiation
     * by Shiro's bean container. The channel will be created on demand via
     * {@link #init()} or the first call to {@link #createCache(String)}.
     */
    public J2CacheManager() {
    }

    /**
     * Construct a manager that re-uses an externally managed J2Cache channel.
     *
     * @param channel the J2Cache {@link CacheChannel} to delegate cache
     *                operations to; may be {@code null} in which case the
     *                channel will be resolved from {@link J2Cache#getChannel()}
     *                during {@link #init()}.
     */
    public J2CacheManager(CacheChannel channel) {
        this.channel = channel;
    }

    /**
     * Initialise the manager by ensuring a non-null {@link CacheChannel} is
     * available. If no channel has been injected via the constructor, the
     * singleton returned by {@link J2Cache#getChannel()} is used.
     *
     * <p>Calling this method more than once is safe: a previously assigned
     * channel is preserved.</p>
     *
     * @throws ShiroException if J2Cache bootstrap fails. The exception is
     *                        wrapped in Shiro's runtime exception type so
     *                        callers do not need to declare a checked
     *                        exception.
     */
    @Override
    public void init() throws ShiroException {
        //do nothing
        if (channel == null) {
            channel = J2Cache.getChannel();
        }
    }

    /**
     * Release the underlying J2Cache {@link CacheChannel}.
     *
     * <p>Called by Shiro's {@code LifecycleUtils} when the security manager
     * is being shut down. The method is tolerant of a {@code null} channel,
     * which means {@link #init()} was never invoked.</p>
     *
     * @throws Exception propagated from {@link CacheChannel#close()} if the
     *                     underlying J2Cache channel fails to shut down.
     */
    @Override
    public void destroy() throws Exception {
        if (channel != null) {
            channel.close();
        }
    }

    /**
     * Build a Shiro {@link Cache} that delegates to the J2Cache channel for
     * the supplied region.
     *
     * <p>If no channel has been initialised yet the manager will silently
     * bootstrap one via {@link J2Cache#getChannel()}; the same bootstrap
     * path used by {@link #init()}.</p>
     *
     * @param name the cache region name. Each call produces a wrapper scoped
     *             to this region; the same channel instance is shared across
     *             wrappers.
     * @return a non-null {@link J2CacheWrapper} that exposes the J2Cache
     *         region to Shiro's caching API.
     * @throws CacheException never thrown directly by this implementation,
     *                        but declared for binary compatibility with the
     *                        parent {@link AbstractCacheManager} contract.
     */
    @Override
    protected J2CacheWrapper<Object> createCache(String name) throws CacheException {
        if (channel == null) {
            channel = J2Cache.getChannel();
        }
        return new J2CacheWrapper<Object>(name, channel);
    }

}
