# shiro-j2cache

[English](./README.md) | [简体中文](./README.zh-CN.md)

Shiro extension with J2Cache — a `CacheManager` and a caching session DAO on top of [J2Cache](https://gitee.com/ld/J2Cache) (the two-level Java caching framework), so that Shiro's cache and session caching can share the J2Cache channel.

## Table of Contents

- [1. Project Overview](#1-project-overview)
- [2. Features & Status](#2-features--status)
- [3. Requirements & Compatibility](#3-requirements--compatibility)
- [4. Architecture & Modules](#4-architecture--modules)
- [5. Installation](#5-installation)
- [6. Quick Start](#6-quick-start)
- [7. Configuration](#7-configuration)
- [8. Core Usage / API](#8-core-usage--api)
- [9. Testing & Build](#9-testing--build)
- [10. Versioning & Branches](#10-versioning--branches)
- [11. Contributing & License](#11-contributing--license)

## 1. Project Overview

**What it is**

`shiro-j2cache` bridges Apache Shiro and J2Cache:

- `J2CacheManager` — a Shiro `CacheManager` (`AbstractCacheManager` + `Initializable` + `Destroyable`) backed by the J2Cache `CacheChannel` (obtained from `J2Cache.getChannel()`).
- `J2CacheWrapper<V>` — a Shiro `Cache<String, V>` implementation over the J2Cache channel (region-based `get` / `put` / `remove` / `clear` / `size` / `keys` / `values`).
- `J2CacheCachingSessionDAO` — a `CachingSessionDAO` implementation that persists Shiro sessions through the J2Cache-backed cache manager (`doCreate` / `doReadSession` / `doUpdate` / `doDelete`).

**What it is not**

- It is not a J2Cache configuration library — J2Cache's own configuration (`j2cache.properties`) and channel lifecycle remain the responsibility of your application.
- It is not a Spring Boot starter; no auto-configuration is shipped.

**Typical scenarios**

| Scenario | Description |
| :--- | :--- |
| Distributed Shiro cache | Set `securityManager.cacheManager = new J2CacheManager()` so authentication/authorization caches use the J2Cache channel (e.g. Redis-backed level-2). |
| Session caching | Use `J2CacheCachingSessionDAO` as the `sessionDAO` of a `DefaultWebSessionManager` to cache sessions in J2Cache. |
| Two-level cache reuse | Keep one `CacheChannel` shared between application caches and Shiro. |

## 2. Features & Status

| Capability | Status | Notes |
| :--- | :--- | :--- |
| `J2CacheManager` | Available | `init()` acquires the channel via `J2Cache.getChannel()` if none was injected; `destroy()` closes it. |
| `J2CacheWrapper<V>` | Available | Implements Shiro `Cache<String, V>` with region + key semantics. |
| `J2CacheCachingSessionDAO` | Available | `CachingSessionDAO` CRUD over the J2Cache-backed cache manager. |

> Status is reported as of `1.0.x.20260630-SNAPSHOT` on the `feature/1.0.x` branch.

## 3. Requirements & Compatibility

| Item | Version |
| :--- | :--- |
| JDK | 8+ |
| Maven | 3.0+ (Maven Wrapper 3.5.0 bundled) |
| Apache Shiro | 1.13.0 (`shiro-core`) |
| J2Cache | 2.8.5-release (`net.oschina.j2cache:j2cache-core`) |

**Version lines**

| Branch | JDK baseline | Version pattern |
| :--- | :--- | :--- |
| `feature/1.0.x` | JDK 8 | `1.0.x.*` |
| `feature/2.0.x` | JDK 17 | `2.0.x.*` |
| `feature/3.0.x` | JDK 21 | `3.0.x.*` |

## 4. Architecture & Modules

```text
 Shiro SecurityManager
   |
   +-- cacheManager: J2CacheManager
   |      |
   |      v
   |  J2CacheWrapper<V> (region + key)
   |      |
   |      v
   |  CacheChannel (J2Cache two-level cache)
   |
   +-- sessionManager.sessionDAO: J2CacheCachingSessionDAO
          |
          v
      (cached via the J2Cache-backed cache manager)
```

This is a **single-module** project (packaging `jar`), three classes under `org.apache.shiro.cache.j2cache`:

| Class | Role |
| :--- | :--- |
| `J2CacheManager` | Shiro `CacheManager` over the J2Cache `CacheChannel`. |
| `J2CacheWrapper<V>` | Shiro `Cache<String, V>` wrapper. |
| `J2CacheCachingSessionDAO` | `CachingSessionDAO` persisting sessions through J2Cache. |

## 5. Installation

The artifact is not yet published to Maven Central. Resolve it from the project's configured artifact repository (Aliyun Packages) or install it locally from source; the snapshot version currently used on the `feature/1.0.x` branch is `1.0.x.20260630-SNAPSHOT`.

**Maven**

```xml
<dependency>
    <groupId>io.github.easy4j</groupId>
    <artifactId>shiro-j2cache</artifactId>
    <version>1.0.x.20260630-SNAPSHOT</version>
</dependency>
```

**Gradle**

```groovy
implementation 'io.github.easy4j:shiro-j2cache:1.0.x.20260630-SNAPSHOT'
```

## 6. Quick Start

Use J2Cache as the Shiro cache manager:

```java
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.cache.j2cache.J2CacheManager;

DefaultSecurityManager securityManager = new DefaultSecurityManager();

// Channel is auto-acquired from J2Cache.getChannel() in init()
J2CacheManager cacheManager = new J2CacheManager();
cacheManager.init();
securityManager.setCacheManager(cacheManager);
```

**Expected result:** Shiro authentication/authorization caches are created as J2Cache regions; cache contents are shared across the JVM cluster through the J2Cache channel's level-2 cache (e.g. Redis).

Session caching variant:

```java
import org.apache.shiro.cache.j2cache.J2CacheCachingSessionDAO;

J2CacheCachingSessionDAO sessionDAO = new J2CacheCachingSessionDAO();
sessionDAO.setCacheManager(cacheManager);
// sessionManager.setSessionDAO(sessionDAO);
```

## 7. Configuration

This library has no configuration properties or prefix. The J2Cache channel behavior is controlled by J2Cache's own configuration files (`j2cache.properties` etc.) in the consuming application. `J2CacheManager` accepts an optional `CacheChannel` via constructor; without it, `init()` falls back to `J2Cache.getChannel()`.

## 8. Core Usage / API

| Class | Key API |
| :--- | :--- |
| `J2CacheManager` | `init()`, `destroy()`, `createCache(String name)`; constructors `J2CacheManager()` and `J2CacheManager(CacheChannel)`. |
| `J2CacheWrapper<V>` | `get(String key)`, `put(String key, V value)`, `remove(String key)`, `clear()`, `size()`, `keys()`, `values()`; fields `region`, `channel`. |
| `J2CacheCachingSessionDAO` | `setCacheManager(CacheManager)`, `doCreate`, `doReadSession`, `doUpdate`, `doDelete` (Shiro session CRUD). |

## 9. Testing & Build

```bash
# Full build with JaCoCo coverage report/check
./mvnw clean verify

# Install into the local repository
./mvnw install
```

Test & gate facts (as configured in the pom):

- No unit tests exist in this module yet (the pom's surefire configuration defaults to skipping tests unless enabled).
- JaCoCo is bound to `prepare-agent` / `report` / `check`; the `check` rule requires a **90% line coverage ratio** (configured with `haltOnFailure=false`).

## 10. Versioning & Branches

| Branch | JDK baseline | Version pattern | Status |
| :--- | :--- | :--- | :--- |
| `feature/1.0.x` | JDK 8 | `1.0.x.*` | Active; current snapshot `1.0.x.20260630-SNAPSHOT` |
| `feature/2.0.x` | JDK 17 | `2.0.x.*` | Maintained |
| `feature/3.0.x` | JDK 21 | `3.0.x.*` | Maintained |

Maintenance strategy: the 1.0.x line keeps JDK 8 compatibility for legacy deployments; the 2.0.x and 3.0.x lines are the modern JDK baselines. Release artifacts are published to the project's configured artifact repository (Aliyun Packages) and GitHub Releases; the project has not yet published to Maven Central.

## 11. Contributing & License

Contributions are welcome — please open an issue or a pull request on the [GitHub repository](https://github.com/easy-4-java/shiro-j2cache).

This project is licensed under the **Apache License 2.0**. See [LICENSE](LICENSE) for details.
