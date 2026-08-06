# shiro-j2cache

[English](./README.md) | [简体中文](./README.zh-CN.md)

[![Java](https://img.shields.io/badge/Java-8-orange)](https://github.com/easy-4-java/shiro-j2cache) [![License](https://img.shields.io/badge/license-Apache%202.0-green)](./LICENSE)

Shiro 的 J2Cache 扩展——基于 [J2Cache](https://gitee.com/ld/J2Cache)（两级 Java 缓存框架）实现 Shiro `CacheManager` 与缓存型会话 DAO，使 Shiro 的缓存与会话缓存可以共用 J2Cache 通道。

## 目录

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

**是什么**

`shiro-j2cache` 在 Apache Shiro 与 J2Cache 之间架起桥梁：

- `J2CacheManager` —— 基于 J2Cache `CacheChannel`（通过 `J2Cache.getChannel()` 获取）的 Shiro `CacheManager`（`AbstractCacheManager` + `Initializable` + `Destroyable`）。
- `J2CacheWrapper<V>` —— 基于 J2Cache 通道的 Shiro `Cache<String, V>` 实现（按 region 提供 `get` / `put` / `remove` / `clear` / `size` / `keys` / `values`）。
- `J2CacheCachingSessionDAO` —— `CachingSessionDAO` 实现，通过 J2Cache 支撑的缓存管理器持久化 Shiro 会话（`doCreate` / `doReadSession` / `doUpdate` / `doDelete`）。

**不是什么**

- 它不是 J2Cache 配置库——J2Cache 自身的配置（`j2cache.properties`）与通道生命周期由你的应用负责。
- 它不是 Spring Boot Starter；不随包提供自动配置。

**典型场景**

| 场景 | 说明 |
| :--- | :--- |
| 分布式 Shiro 缓存 | 设置 `securityManager.cacheManager = new J2CacheManager()`，使认证/授权缓存使用 J2Cache 通道（如 Redis 二级缓存）。 |
| 会话缓存 | 将 `J2CacheCachingSessionDAO` 作为 `DefaultWebSessionManager` 的 `sessionDAO`，把会话缓存进 J2Cache。 |
| 二级缓存复用 | 应用缓存与 Shiro 共用同一个 `CacheChannel`。 |

## 2. Features & Status

| 能力 | 状态 | 说明 |
| :--- | :--- | :--- |
| `J2CacheManager` | 可用 | `init()` 在未注入通道时通过 `J2Cache.getChannel()` 获取；`destroy()` 关闭通道。 |
| `J2CacheWrapper<V>` | 可用 | 实现 Shiro `Cache<String, V>`，采用 region + key 语义。 |
| `J2CacheCachingSessionDAO` | 可用 | 基于 J2Cache 缓存管理器的 `CachingSessionDAO` 会话 CRUD。 |

> 状态以 `feature/1.0.x` 分支上的 `1.0.x.20260630-SNAPSHOT` 为准。

## 3. Requirements & Compatibility

| 项目 | 版本 |
| :--- | :--- |
| JDK | 8+ |
| Maven | 3.0+（内置 Maven Wrapper 3.5.0） |
| Apache Shiro | 1.13.0（`shiro-core`） |
| J2Cache | 2.8.5-release（`net.oschina.j2cache:j2cache-core`） |

**版本线**

| 分支 | JDK 基线 | 版本模式 |
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
   |  J2CacheWrapper<V>（region + key）
   |      |
   |      v
   |  CacheChannel（J2Cache 两级缓存）
   |
   +-- sessionManager.sessionDAO: J2CacheCachingSessionDAO
          |
          v
      （通过 J2Cache 支撑的缓存管理器缓存）
```

本项目为**单模块**工程（packaging 为 `jar`），`org.apache.shiro.cache.j2cache` 包下共 3 个类：

| 类 | 职责 |
| :--- | :--- |
| `J2CacheManager` | 基于 J2Cache `CacheChannel` 的 Shiro `CacheManager`。 |
| `J2CacheWrapper<V>` | Shiro `Cache<String, V>` 包装类。 |
| `J2CacheCachingSessionDAO` | 通过 J2Cache 持久化会话的 `CachingSessionDAO`。 |

## 5. Installation

该构件尚未发布到 Maven Central。请从项目配置的制品仓库（阿里云制品仓库）获取，或从源码本地安装；`feature/1.0.x` 分支当前使用的快照版本为 `1.0.x.20260630-SNAPSHOT`。

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

将 J2Cache 作为 Shiro 缓存管理器：

```java
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.cache.j2cache.J2CacheManager;

DefaultSecurityManager securityManager = new DefaultSecurityManager();

// 未注入通道时，init() 会自动从 J2Cache.getChannel() 获取
J2CacheManager cacheManager = new J2CacheManager();
cacheManager.init();
securityManager.setCacheManager(cacheManager);
```

**预期结果：** Shiro 认证/授权缓存以 J2Cache region 形式创建；缓存内容通过 J2Cache 通道的二级缓存（如 Redis）在 JVM 集群间共享。

会话缓存变体：

```java
import org.apache.shiro.cache.j2cache.J2CacheCachingSessionDAO;

J2CacheCachingSessionDAO sessionDAO = new J2CacheCachingSessionDAO();
sessionDAO.setCacheManager(cacheManager);
// sessionManager.setSessionDAO(sessionDAO);
```

## 7. Configuration

本库没有配置属性与前缀。J2Cache 通道行为由消费应用中 J2Cache 自身的配置文件（`j2cache.properties` 等）控制。`J2CacheManager` 支持通过构造器注入 `CacheChannel`；未注入时 `init()` 回退到 `J2Cache.getChannel()`。

## 8. Core Usage / API

| 类 | 关键 API |
| :--- | :--- |
| `J2CacheManager` | `init()`、`destroy()`、`createCache(String name)`；构造器 `J2CacheManager()` 与 `J2CacheManager(CacheChannel)`。 |
| `J2CacheWrapper<V>` | `get(String key)`、`put(String key, V value)`、`remove(String key)`、`clear()`、`size()`、`keys()`、`values()`；字段 `region`、`channel`。 |
| `J2CacheCachingSessionDAO` | `setCacheManager(CacheManager)`、`doCreate`、`doReadSession`、`doUpdate`、`doDelete`（Shiro 会话 CRUD）。 |

## 9. Testing & Build

```bash
# 完整构建（含 JaCoCo 覆盖率报告/检查）
./mvnw clean verify

# 安装到本地仓库
./mvnw install
```

测试与门禁事实（以 pom 配置为准）：

- 本模块暂无单元测试（pom 的 surefire 配置默认跳过测试，除非显式启用）。
- JaCoCo 绑定 `prepare-agent` / `report` / `check`；`check` 规则要求**行覆盖率不低于 90%**（配置了 `haltOnFailure=false`）。

## 10. Versioning & Branches

| 分支 | JDK 基线 | 版本模式 | 状态 |
| :--- | :--- | :--- | :--- |
| `feature/1.0.x` | JDK 8 | `1.0.x.*` | 活跃；当前快照 `1.0.x.20260630-SNAPSHOT` |
| `feature/2.0.x` | JDK 17 | `2.0.x.*` | 维护中 |
| `feature/3.0.x` | JDK 21 | `3.0.x.*` | 维护中 |

维护策略：1.0.x 版本线保持 JDK 8 兼容，服务于存量部署；2.0.x 与 3.0.x 版本线为现代 JDK 基线。发布制品发布到项目配置的制品仓库（阿里云制品仓库）与 GitHub Releases；项目尚未发布到 Maven Central。

## 11. Contributing & License

欢迎参与贡献——请在 [GitHub 仓库](https://github.com/easy-4-java/shiro-j2cache) 提交 Issue 或 Pull Request。

本项目基于 **Apache License 2.0** 开源。详见 [LICENSE](LICENSE)。
