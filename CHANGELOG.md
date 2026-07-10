# Changelog

## [1.1.0] - Unreleased

### ⚠️ 破坏性变更

- **`DefaultBeanRowMapper.of()` 不再抛出 `SQLException`**：工厂方法在反射异常时改为抛出非受检异常 `IllegalStateException`。调用方如果 `catch (SQLException e)` 包裹 `of()` 调用，该捕获将失效，需移除相关 `catch` 块或改为捕获 `IllegalStateException`。
- **`ThrowingConsumer` 与 `ThrowingPredicate` 迁移至 `xyz.zhouxy.jdbc.function` 子包**：需更新 import 路径。
- **`queryList` / `queryFirst` 默认 Map 映射器由 `RowMapper.HASH_MAP_MAPPER` 改为 `RowMapper.LINKED_HASH_MAP_MAPPER`**
  - 无显式 `RowMapper` 参数的 `queryList(sql, params)` / `queryFirst(sql, params)` 等方法，返回的 `Map` 实现由 `HashMap` 切换为 `LinkedHashMap`，以保留列的查询顺序。
  - 依赖 `HashMap` 迭代顺序，或使用 `getClass() == HashMap.class` 等精确类型判断的现有代码需要调整；因为 `LinkedHashMap` 继承自 `HashMap`，所以 `instanceof HashMap` 判断不会受影响。如需恢复无序实现，可显式传入 `RowMapper.HASH_MAP_MAPPER`。

### 新增

- 命名参数 JDBC 操作支持（`#{paramName}` 风格）：
  - `NamedParamJdbcOperations` 接口：提供查询、更新、批量操作的命名参数重载
  - `NamedParamSql`：SQL 模板解析（`#{param}` → `?`），支持参数名自省
  - `PreparedSql`：预构建的命名参数 SQL，提供链式 Builder
  - `SimpleJdbcTemplate` 同时实现 `JdbcOperations` 与 `NamedParamJdbcOperations`
  - `TransactionTemplate` 新增命名参数事务方法：`executeNamed` / `commitIfTrueNamed`（纯命名参数回调），以及双参数回调重载（混用位置与命名参数）
  - 补充命名参数查询、更新、批量操作的完整单元测试
- `ParamBuilder.handleItem` 方法由 `private` 提升为 `public`

### 修复

- **Map 映射器重复列名取值修正**：重复列名场景下，Map 映射器现在按列索引依次取值，最终 Map 中保留的是最后一列的值。旧实现按列名取值时，重复标签始终返回第一列的值；显式使用 `HASH_MAP_MAPPER` 并依赖旧行为的代码需要调整。

### 重构

- **优化事务自动提交恢复逻辑**：`TransactionTemplate` 在 `finally` 块恢复 `autoCommit` 时，若原始事务已发生异常，则将恢复过程中的 `SQLException` 通过 `addSuppressed` 附加到原始异常上，避免异常信息丢失
- **空值注解迁移：JSR-305 → JSpecify**
  - 依赖由 `com.google.code.findbugs:jsr305` 替换为 `org.jspecify:jspecify:1.0.0`
  - `AssertTools.checkCondition` 异常边界约束收紧为 `<T extends @NonNull Exception>`
  - 测试代码同步更新：`UserRowMapper.mapRow` 参数加 `@NonNull`，`ParamBuilderTest` 加 `@SuppressWarnings("null")`
- **单列查询方法语义优化**：消除 Class 参数重载歧义，补全方法族
  - `queryList(sql, params, Class<T>)` → 已过时，请使用 `queryValues(sql, params, Class<T>)`（多行单列 → 值列表）
  - `queryFirst(sql, params, Class<T>)` → 已过时，请使用 `queryValue(sql, params, Class<T>)`（单行单列 → 单值 Optional）
  - 无参数重载同步废弃：`queryList(sql, Class<T>)` → `queryValues(sql, Class<T>)`；`queryFirst(sql, Class<T>)` → `queryValue(sql, Class<T>)`
  - 新增 `queryValueOrDefault(sql, params, Class<T>, T defaultValue)` 及无参数重载（单行单列 → 带默认值的非 Optional 返回值）
  - 旧方法将在后续版本中移除

### 文档

- 优化 `DefaultBeanRowMapper` 类注释，明确性能限制和使用建议
- 更新 README，补充命名参数使用说明和示例
- 更新 NOTICE，声明 MyBatis 代码引用及许可

---

## [1.0.0] - 2026-06-17

### 重构
- 移除 `plusone-commons` 及 Guava 依赖，内化 `AssertTools`、`NamingTools`、`ThrowingConsumer`、`ThrowingPredicate` 工具类
- 将 `batchUpdate` 中断时对 `BatchUpdateResult` 的变更逻辑内化到 `BatchUpdateResult` 中

### 测试
- 添加 `ParamBuilderTest#buildParamsTemporal` 测试方法验证时间类型参数构建
- 添加 `TransactionTest` 事务异常测试用例
- 补充测试数据库初始化脚本注释

### 文档
- 新增 `CHANGELOG.md` 文件记录各版本更新内容
- 优化 README 文档结构与内容
- 补充 `ParamBuilder` 和 `SimpleJdbcTemplate` 的文档注释
- 更新 `DefaultBeanRowMapper` 类文档注释，强调使用场景

---

## [1.0.0-RC3] - 2026-06-05

### 新增
- `ResultHandler` 新增静态工厂方法 `mapToList(RowMapper)`，消除各处 `ResultSet` 遍历重复代码

### 重构
- `JdbcOperationSupport` 中无参数 SQL 统一改用 `Statement` 执行，避免创建空参数 `PreparedStatement`
- `ParamBuilder#buildParams` 为常用数据类型（`CharSequence`/`Number`/`Boolean`/`Temporal`）添加短路处理，优化参数构建性能

### 测试
- 重构 `UpdateTest`，补全 `Statement` 路径覆盖

### 文档
- 更新 README.md 使用说明和示例
- 更新 `DefaultBeanRowMapper` 类文档以避免歧义
- 更新项目简介

---

## [1.0.0-RC2] - 2026-05-31

### 新增
- `TransactionTemplate` 从 `SimpleJdbcTemplate` 中分离为独立类，封装事务生命周期管理（开启/提交/回滚/恢复自动提交）
- `SimpleJdbcTemplate.transaction()` 暴露 `TransactionTemplate` 实例
- 新增 `ParamBuilderTest` 单元测试
- 新增 `Instant` 类型参数端到端测试

### 重构
- 简化 `ParamBuilder` 参数构建逻辑
- 优化批量更新批次内索引计算逻辑

### 文档
- 完善 `SimpleJdbcTemplate` 类文档注释
- 更新批量更新相关类（`BatchUpdateResult`/`BatchUpdateStatus`/`BatchUpdateErrorInfo`）的 JavaDoc
- 更新 README 使用说明和示例

### 其他
- 版权年份由固定范围更新为包含当前时间的表述

---

## [1.0.0-RC1] - 2026-05-29

项目首个正式发布的候选版本（2023-07 从 plusone-commons 独立开发）。

### 新增

**核心框架**
- `SimpleJdbcTemplate` 核心模板类，封装 JDBC 连接管理、异常处理和资源释放
- `JdbcOperations` 接口规范，定义统一的数据库操作 API
- `JdbcOperationSupport` 静态工具类，封装底层 `PreparedStatement` / `Statement` 操作

**查询**
- `query` + `ResultHandler`：自定义 `ResultSet` 处理
- `queryList`：返回列表（支持 `RowMapper`、`Class`、`Map<String, Object>` 三种变体）
- `queryFirst`：返回 `Optional<T>`（支持 `RowMapper`、`Class`、`Map<String, Object>` 三种变体）
- `queryBoolean`：返回 `boolean`，结果集为空时返回 `false`
- 全部查询方法提供无参数重载

**更新**
- `update`：执行 INSERT / UPDATE / DELETE，返回影响行数
- `updateAndReturnKeys`：执行 DML 并返回自动生成的主键，支持 `RowMapper` 映射
- `batchUpdate`：分批执行 DML，支持非静默模式（遇错中断）和静默模式（遇错继续）

**批量更新结果**
- `BatchUpdateResult`：封装批次级粒度的执行结果（总数据量、批次计数、成功/失败/剩余批次）
- `BatchUpdateStatus` 枚举（SUCCESS / COMPLETED_WITH_ERRORS / INTERRUPTED）
- `BatchUpdateErrorInfo`：封装错误批次的异常详情

**参数构建**
- `ParamBuilder.buildParams`：构建 `Object[]` 参数数组，支持 `Optional` / `OptionalInt` / `OptionalLong` / `OptionalDouble` 自动拆箱
- `ParamBuilder.buildBatchParams`：批量构建 `List<Object[]>` 参数列表

**映射策略**
- `RowMapper` 函数式接口：`ResultSet` 单行映射
- `RowMapper.HASH_MAP_MAPPER`：将行数据映射为 `Map<String, Object>`
- `DefaultBeanRowMapper`：默认 Bean 映射实现，自动匹配 属性名（小驼峰）↔ 列名（小写蛇形），通过反射调用 setter
- `RowMapper.beanRowMapper(Class)` / `beanRowMapper(Class, Map)` 静态工厂方法

**事务**
- `TransactionException`：包装事务执行中的原始异常
- `commitIfTrue` 方法：根据业务逻辑返回值（true 提交 / false 回滚）控制事务

**测试**
- 基于 H2 内存数据库的集成测试体系
- 覆盖查询 / 更新 / 批量 / 事务 / RowMapper 全场景

**许可**
- Apache License 2.0
