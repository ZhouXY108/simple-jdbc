# Changelog

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
