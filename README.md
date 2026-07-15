# Simple JDBC

`Simple JDBC` 提供了一套轻量级的 JDBC 封装工具类，是作者在对传统遗留项目进行改造时设计。该项目未引入 ORM 框架，原本的数据库交互高度依赖原生 JDBC API，导致存在大量冗余的样板代码（Boilerplate Code）。本项目通过抽象底层数据库操作，简化了连接管理、SQL 执行与结果集处理流程，提升数据访问层的开发效率与代码可维护性。

> 注：本项目基于 [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0) 开源协议发布。

---

## 1. 核心特性

- **轻量无依赖**：基于原生 JDBC 封装，无第三方重量级依赖。
- **API 简洁**：提供丰富的快捷方法，大幅减少样板代码。
- **灵活的映射**：支持自定义 `ResultHandler` 与 `RowMapper` 灵活处理结果集，内置 `Map` 类型支持（`LinkedHashMap` / `HashMap`）。
- **事务与批处理**：提供声明式的事务模板与完善的批量更新错误处理机制。
- **命名参数支持**：同时支持传统位置参数（`?`）与命名参数（`#{paramName}`）两种 SQL 风格，可在同一模板实例中无缝切换或混用，提升 SQL 可读性。
- **线程安全**：核心模板类无状态设计，天然支持多线程环境。

---

## 2. 设计考量与取舍

`Simple JDBC` 不追求大而全，在功能设计上保持克制。以下是本项目的一些设计考量与取舍：

- 明确**不支持存储过程**：专注于基础 CRUD。
- **不提供分页 API**：不同数据库的分页方言差异巨大，且实际业务中的分页查询往往不是简单的 `LIMIT OFFSET`（存在如游标分页、延迟关联等深度优化空间）。为了保持轻量与灵活，分页 SQL 交由开发者根据具体数据库与业务场景自行编写。
- **不提供缓存支持**：数据缓存应当被视为一个独立的关注点，通常交由更高层的抽象模块来处理。
- **有限但灵活的结果映射**：为应对多样化的数据处理需求，Simple JDBC 提供了 `ResultHandler` 与 `RowMapper` 两层抽象机制。前者负责整体结果集的统筹处理，后者专注于单行数据的解析与映射（详见 [4.2 结果映射策略](#42-结果映射策略)）。两者均设计为函数式接口，支持通过 Lambda 表达式快速定制映射逻辑。

---

## 3. 快速开始

### 3.1 环境要求

| 场景 | JDK 版本 | 说明 |
|---|---|---|
| **运行** | JDK 8+ | 构建产物 target 为 JDK 8，兼容 JDK 8 及以上版本 |
| **构建/测试** | JDK 17+ | jspecify 的 `@NullMarked` 注解使用了 `ElementType.MODULE`（Java 9 引入），JDK 8 的 javac 无法解析该注解，因此当前项目选择使用 JDK 17 运行 Maven |

### 3.2 添加 Maven 依赖

将以下配置添加到您的 `pom.xml` 中：

```xml
<dependency>
    <groupId>xyz.zhouxy.jdbc</groupId>
    <artifactId>simple-jdbc</artifactId>
    <version>${simple-jdbc.version}</version>
</dependency>
```

### 3.3 基本用法

```java
// ---- SimpleJdbcTemplate：内部管理连接，最常用 ----
// 初始化
SimpleJdbcTemplate jdbcTemplate = new SimpleJdbcTemplate(dataSource);
// 查询列表
List<Account> accounts = jdbcTemplate.queryList("SELECT * FROM account WHERE org_no = ?",
    buildParams("0000"), RowMapper.beanRowMapper(Account.class));
// 更新
int updatedRows = jdbcTemplate.update("UPDATE account SET deleted = 1 WHERE id = ?",
    buildParams(10000L));
// 批量更新
BatchUpdateResult batchResult = jdbcTemplate.batchUpdate("INSERT INTO account (username, password, org_no) VALUES (?, ?, ?)",
    batchParams, 100);

// ---- TransactionTemplate：事务管理 ----
// 初始化
TransactionTemplate tx = jdbcTemplate.transaction();
// 自动提交/回滚
tx.execute(ops -> {
    // 查询列表
    List<Account> txAccounts = ops.queryList("SELECT * FROM account WHERE org_no = ?",
        buildParams("0000"), RowMapper.beanRowMapper(Account.class));
    // 更新
    int txAffected = ops.update("UPDATE account SET deleted = 1 WHERE id = ?",
        buildParams(10000L));
    // 批量更新
    BatchUpdateResult txBatchResult = ops.batchUpdate("INSERT INTO account (username, password, org_no) VALUES (?, ?, ?)",
        batchParams, 100);
});
// 根据返回值控制提交/回滚
tx.commitIfTrue(ops -> {
    int rows = ops.update("UPDATE account SET balance = ? WHERE id = ?",
        buildParams(100, 1));
    return rows > 0; // true → 提交；false 或抛异常 → 回滚
});

// ---- JdbcExecutor：外部连接执行器，适用于 JTA / 容器托管等场景 ----
// 初始化（所有方法首参需传入 Connection）
JdbcExecutor executor = new JdbcExecutor();
// 查询列表
List<Account> extAccounts = executor.queryList(conn, "SELECT * FROM account WHERE org_no = ?",
    buildParams("0000"), RowMapper.beanRowMapper(Account.class));
// 更新
int extAffected = executor.update(conn, "UPDATE account SET deleted = 1 WHERE id = ?",
    buildParams(10000L));
// 批量更新
BatchUpdateResult extBatchResult = executor.batchUpdate(conn, "INSERT INTO account (username, password, org_no) VALUES (?, ?, ?)",
    batchParams, 100);

// ---- NamedParamJdbcExecutor：命名参数外部连接执行器 ----
// 初始化
NamedParamJdbcExecutor namedExecutor = new NamedParamJdbcExecutor();
// 查询列表
List<Account> namedAccounts = namedExecutor.queryList(conn, "SELECT * FROM account WHERE org_no = #{orgNo}",
    Map.of("orgNo", "0000"), RowMapper.beanRowMapper(Account.class));
// 更新
int namedAffected = namedExecutor.update(conn, "UPDATE account SET deleted = 1 WHERE id = #{id}",
    Map.of("id", 10000L));
// 批量更新
BatchUpdateResult namedBatchResult = namedExecutor.batchUpdate(conn, "INSERT INTO account (username, password, org_no) VALUES(#{un}, #{pw}, #{org})",
    namedBatchParams, 100);
```

> 💡 关于与数据库连接池（如 HikariCP、Druid、DBCP 2 等）的集成方式，请参见「[8. 连接池集成](#8-连接池集成)」章节。
>
> 📖 查询详见「[4. 数据查询](#4-数据查询-query)」，更新与批量详见「[5. 数据更新](#5-数据更新-update)」，事务详见「[6. 事务管理](#6-事务管理-transaction)」。

---

## 4. 数据查询 (Query)

### 4.1 查询方法列表

| 方法签名                                           | 说明                                                                  |
| :------------------------------------------------- | :-------------------------------------------------------------------- |
| `query(sql, params, resultHandler)`                | 最基础的查询，通过 `ResultHandler` 自定义完整的映射逻辑。             |
| `queryList(sql, params, rowMapper)`                | 查询列表，通过 `RowMapper` 逐行映射。                                 |
| `queryList(sql, params)`                           | 查询列表，每行自动转换为 `Map<String, Object>`。                      |
| `queryFirst(sql, params, rowMapper)`               | 查询第一行，通过 `RowMapper` 映射，返回 `Optional<T>`。               |
| `queryFirst(sql, params)`                          | 查询第一行，返回 `Optional<Map<String, Object>>`。                    |
| `queryValues(sql, params, Class)`                  | 单列查询列表，每行提取第一列并转换为指定类型。                        |
| `queryValue(sql, params, Class)`                   | 查询第一行第一列，返回 `Optional<T>`。                                |
| `queryValueOrDefault(sql, params, Class, default)` | 查询第一行第一列，结果为空时返回默认值。适用于 COUNT/SUM 等聚合查询。 |
| `queryBoolean(sql, params)`                        | 查询第一行第一列并转换为 `boolean`，若结果为空则返回 `false`。        |

*💡 提示：以上方法均有省略 `params` 的重载（如 `queryList(sql, rowMapper)`），适用于不含占位符的 SQL 语句。`queryValues`、`queryValue`、`queryValueOrDefault` 同理。*

*💡 命名参数：以上所有方法在 `SimpleJdbcTemplate` 上均有三种参数模式的重载：① 直接传参 — 直接传入 SQL 字符串 + `Map`（每次解析）；② 模板传参 — 先解析为 `NamedParamSql`，后续每次调用传入模板 + `Map`（解析一次、绑参多次）；③ 预构建传参 — 传入 `PreparedSql`（参数已绑死）。详见「[5.2.2 命名参数构建](#522-命名参数构建)」。*

### 4.2 结果映射策略

- **`ResultHandler`**：处理完整的 `ResultSet`，允许自定义逻辑将结果集映射为任意类型（包括集合）。
- **`RowMapper`**：将 `ResultSet` 中的单行数据映射为 Java 对象。内置以下实现：
  - `RowMapper.HASH_MAP_MAPPER`：将每行数据映射为 `HashMap<String, Object>`，不保证键的迭代顺序。
  - `RowMapper.LINKED_HASH_MAP_MAPPER`：将每行数据映射为 `LinkedHashMap<String, Object>`，保持列的查询顺序。`queryList(sql, params)` 和 `queryFirst(sql, params)` 默认使用此映射器。
  - `SimpleBeanRowMapper`：将 `ResultSet` 中的一行数据映射为 Java Bean 的简易实现，运行时通过 `MethodHandle` 调用构造器和 setter。

> **定位说明**：`SimpleBeanRowMapper` 是一个优先级很低的存在，项目提供它只是为了"开箱即用"的便捷性，**并非推荐的映射方式**。原因如下：
> - 由于运行时需要进行类型发现和 MethodHandle 调用，它仍存在可观的运行时开销；
> - 因此它的定位只是"有这么个简单实现"，更鼓励用户针对自己的类型编写自定义 `RowMapper`。
>
> 如果你仍然想使用，可通过以下工厂方法：
> - `RowMapper.beanRowMapper(Class)`：自动匹配 **属性名（小驼峰）↔ 列名（小写蛇形）**。
> - `RowMapper.beanRowMapper(Class, Map<String, String>)`：通过 `Map` 自定义属性名与列名映射关系。

### 4.3 查询操作

```java
// 基础查询（使用 ResultHandler 处理全部结果）
List<Account> accounts = jdbcTemplate.query(
    "SELECT * FROM account WHERE deleted = 0 AND username LIKE ? AND org_no = ?",
    buildParams("admin%", "0000"),
    rs -> {
        List<Account> result = new ArrayList<>();
        while (rs.next()) {
            result.add(new Account(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("password"),
                rs.getString("org_no"),
                rs.getTimestamp("create_time"),
                rs.getTimestamp("update_time")
            ));
        }
        return result;
    }
);

// 查询列表（单列）
List<String> usernames = jdbcTemplate.queryValues(
    "SELECT username FROM account WHERE deleted = 0 AND username LIKE ? AND org_no = ?",
    buildParams("admin%", "0000"),
    String.class
);

// 查询列表（使用内置 Bean 映射）
List<Account> mappedAccounts = jdbcTemplate.queryList(
    "SELECT * FROM account WHERE deleted = 0 AND username LIKE ? AND org_no = ?",
    buildParams("admin%", "0000"),
    RowMapper.beanRowMapper(Account.class)
);

// RowMapper.beanRowMapper(...) 内部是基于 MethodHandle 的 `SimpleBeanRowMapper`，仅适合对性能不敏感的场景。
// 实际生产环境中，建议为具体类型自定义 RowMapper。

// 查询列表（使用自定义 RowMapper 映射）
List<Account> customMappedAccounts = jdbcTemplate.queryList(
    "SELECT * FROM account WHERE deleted = 0 AND username LIKE ? AND org_no = ?",
    buildParams("admin%", "0000"),
    (rs, rowNum) -> new Account(
        rs.getLong("id"),
        rs.getString("username"),
        rs.getString("password"),
        rs.getString("org_no"),
        rs.getTimestamp("create_time"),
        rs.getTimestamp("update_time")
    )
);

// 查询单行数据
Optional<Account> account = jdbcTemplate.queryFirst(
    "SELECT * FROM account WHERE deleted = 0 AND id = ?",
    buildParams(10000L),
    (rs, rowNum) -> new Account(
        rs.getLong("id"),
        rs.getString("username")
        // ... 省略其他字段
    )
);

// 查询单个值（所有 ResultSet.getObject 支持的类型）
Long count = jdbcTemplate.queryValue(
    "SELECT COUNT(*) FROM account WHERE deleted = 0 AND username LIKE ? AND org_no = ?",
    buildParams("admin%", "0000"),
    Long.class
).orElse(0L);
// 或者
Long count = jdbcTemplate.queryValueOrDefault(
    "SELECT COUNT(*) FROM account WHERE deleted = 0 AND username LIKE ? AND org_no = ?",
    buildParams("admin%", "0000"),
    Long.class,
    0L
);

// 查询 Boolean 值
boolean exists = jdbcTemplate.queryBoolean(
    "SELECT EXISTS(SELECT 1 FROM account WHERE deleted = 0 AND id = ?)",
    buildParams(10000L)
);

// 无参数 SQL 可直接省略 params 参数
List<Account> allAccounts = jdbcTemplate.queryList(
    "SELECT * FROM account WHERE deleted = 0",
    RowMapper.beanRowMapper(Account.class)
);

// --- 命名参数风格（使用 #{paramName} 代替 ?）---
// 命名参数格式 #{paramName}，参数使用 Map 传递，提升 SQL 可读性

// 查询列表（命名参数 + RowMapper）
List<Account> mappedByName = jdbcTemplate.queryList(
    "SELECT * FROM account WHERE username LIKE #{keyword} AND org_no = #{orgNo}",
    Map.of("keyword", "admin%", "orgNo", "0000"),
    RowMapper.beanRowMapper(Account.class)
);

// 单列查询（命名参数）
List<String> names = jdbcTemplate.queryValues(
    "SELECT username FROM account WHERE org_no = #{orgNo}",
    Map.of("orgNo", "0000"),
    String.class
);

// 单值查询（命名参数）
Long count = jdbcTemplate.queryValueOrDefault(
    "SELECT COUNT(*) FROM account WHERE org_no = #{orgNo}",
    Map.of("orgNo", "0000"),
    Long.class,
    0L
);

// 布尔查询（命名参数）
boolean exists = jdbcTemplate.queryBoolean(
    "SELECT EXISTS(SELECT 1 FROM account WHERE id = #{id})",
    Map.of("id", 10000L)
);

// 使用 PreparedSql 预构建对象
PreparedSql prebuilt = NamedParamSql.of(
        "SELECT * FROM account WHERE username = #{name} AND org_no = #{org}")
    .prepare()
    .param("name", "admin")
    .param("org", "0000")
    .build();
List<Account> result = jdbcTemplate.queryList(prebuilt, RowMapper.beanRowMapper(Account.class));

// 模板传参：解析一次，多次换参（避免每次重复解析 SQL）
NamedParamSql queryTmpl = NamedParamSql.of(
    "SELECT * FROM account WHERE username = #{name} AND org_no = #{org}");
List<Account> r1 = jdbcTemplate.queryList(queryTmpl,
    Map.of("name", "admin", "org", "0000"), RowMapper.beanRowMapper(Account.class));
List<Account> r2 = jdbcTemplate.queryList(queryTmpl,
    Map.of("name", "user", "org", "0001"), RowMapper.beanRowMapper(Account.class));
```

---

## 5. 数据更新 (Update)

所有更新方法同样提供了无参重载。

### 5.1 更新操作

| 方法签名                                       | 说明                                                                  |
| :--------------------------------------------- | :-------------------------------------------------------------------- |
| `update(sql, params)`                          | 执行 DML（INSERT / UPDATE / DELETE），返回受影响的行数。              |
| `updateAndReturnKeys(sql, params, rowMapper)`  | 执行 DML 并返回自动生成的键（如自增 ID），通过 `RowMapper` 进行映射。 |

*💡 命名参数：`update` 与 `updateAndReturnKeys` 有三种参数模式：① 直接传参（SQL + `Map`）② 模板传参（`NamedParamSql` + `Map`）③ 预构建传参（`PreparedSql`）。*

```java
// 执行常规 DML
int affectedRows = jdbcTemplate.update(
    "UPDATE account SET deleted = 1 WHERE id = ?",
    buildParams(10000L)
);

// 执行 DML 并获取生成的主键
// 注：按 JDBC 规范可获取自增 ID，能否获取其他值取决于具体数据库及其 JDBC Driver 实现。
List<Pair<Long, LocalDateTime>> keys = jdbcTemplate.updateAndReturnKeys(
    "INSERT INTO account (username, password, org_no) VALUES (?, ?, ?)",
    buildParams("admin", "123456", "0000"),
    (rs, rowNum) -> Pair.of(
        rs.getLong("id"),
        rs.getObject("create_time", LocalDateTime.class)
    )
);

// --- 命名参数风格 ---

// 常规 DML（命名参数）
int affected = jdbcTemplate.update(
    "UPDATE account SET deleted = 1 WHERE id = #{id}",
    Map.of("id", 10000L)
);

// 插入并获取生成的主键（命名参数）
List<Pair<Long, LocalDateTime>> keys2 = jdbcTemplate.updateAndReturnKeys(
    "INSERT INTO account (username, password, org_no) VALUES(#{un}, #{pw}, #{org})",
    Map.of("un", "admin", "pw", "123456", "org", "0000"),
    (rs, rowNum) -> Pair.of(
        rs.getLong("id"),
        rs.getObject("create_time", LocalDateTime.class)
    )
);
// 使用 PreparedSql 预构建
PreparedSql insertSql = NamedParamSql.of(
        "INSERT INTO account (username, password, org_no) VALUES(#{un}, #{pw}, #{org})")
    .prepare()
    .param("un", "admin")
    .param("pw", "123456")
    .param("org", "0000")
    .build();
int rows = jdbcTemplate.update(insertSql);

// 模板传参：解析一次，多次换参
NamedParamSql updateTmpl = NamedParamSql.of(
    "UPDATE account SET deleted = 1 WHERE id = #{id}");
jdbcTemplate.update(updateTmpl, Map.of("id", 1L));
jdbcTemplate.update(updateTmpl, Map.of("id", 2L));
```

### 5.2 批量更新操作

#### 5.2.1 批量更新方法

| 方法签名                                       | 说明                                                                  |
| :--------------------------------------------- | :-------------------------------------------------------------------- |
| `batchUpdate(sql, params, batchSize)`          | 分批执行 DML，遇到错误立即中断。                                      |
| `batchUpdate(sql, params, batchSize, quietly)` | 分批执行 DML；若 `quietly=true`，则遇到错误不中断，直至全部执行完毕。 |

*💡 命名参数：`batchUpdate` 有两种参数模式：① 直接传参（SQL + `List<Map>`）② 模板传参（`NamedParamSql` + `List<Map>`），无 `PreparedSql` 重载（不适用）。*

#### 5.2.2 命名参数构建

命名参数 SQL 使用 `#{paramName}` 格式。`SimpleJdbcTemplate` 同时实现了 `JdbcOperations` 和 `NamedParamJdbcOperations`，因此可直接在同一实例上使用命名参数方法。

| 场景                                     | 推荐方式             | 说明                                           |
| :--------------------------------------- | :------------------- | :--------------------------------------------- |
| 一次性查询/更新                          | String + Map         | 最简单，每次调用自动解析 SQL。                 |
| 同一 SQL 多次执行、仅参数变化            | NamedParamSql + Map  | 模板解析一次，后续每次调用只绑参数值。         |
| 参数需固化在对象中、跨方法传递           | PreparedSql          | SQL 与参数数组整体封装。                       |
| 需要自省参数名或获取 JDBC `Object[]`     | NamedParamSql + toArgs | 底层灵活控制，直接使用位置参数 API。         |

**方式一：直接传参（最简，每次调用解析 SQL）**

```java
// 使用 Map.of() 快速构建参数
jdbcTemplate.queryList(
    "SELECT * FROM account WHERE username = #{name} AND org_no = #{org}",
    Map.of("name", "admin", "org", "0000"),
    RowMapper.beanRowMapper(Account.class)
);
```

**方式二：NamedParamSql 模板传参（适合同一 SQL 多次执行、仅参数变化的场景）**

```java
// 解析一次模板，后续每次调用传入模板 + Map，避免重复解析 SQL
NamedParamSql accountTmpl = NamedParamSql.of(
    "SELECT * FROM account WHERE username = #{name} AND org_no = #{org}");
List<Account> r1 = jdbcTemplate.queryList(accountTmpl,
    Map.of("name", "admin", "org", "0000"), RowMapper.beanRowMapper(Account.class));
List<Account> r2 = jdbcTemplate.queryList(accountTmpl,
    Map.of("name", "user",  "org", "0001"), RowMapper.beanRowMapper(Account.class));
```

**方式三：PreparedSql 预构建传参（适合需要将参数整体封装传递的场景）**

```java
import xyz.zhouxy.jdbc.namedparam.NamedParamSql;
import xyz.zhouxy.jdbc.namedparam.PreparedSql;

// 从 SQL 字符串直接启动
PreparedSql ps = PreparedSql
    .sql("SELECT * FROM account WHERE username = #{name} AND org_no = #{org}")
    .param("name", "admin")
    .param("org", "0000")
    .build();
List<Account> result = jdbcTemplate.queryList(ps, RowMapper.beanRowMapper(Account.class));

// 或从已有 NamedParamSql 模板启动（复用解析结果）
NamedParamSql tmpl = NamedParamSql.of(
    "SELECT * FROM account WHERE username = #{name} AND org_no = #{org}");
PreparedSql ps2 = PreparedSql
    .sql(tmpl)
    .param("name", "admin")
    .param("org", "0000")
    .build();

// 更简写法：模板的 .prepare() 直接进入 PreparedSql.Builder
PreparedSql ps3 = tmpl.prepare()
    .param("name", "admin")
    .param("org", "0000")
    .build();
```

**方式四：NamedParamSql 纯模板 + toArgs（适合底层灵活控制）**

```java
NamedParamSql tmpl = NamedParamSql.of(
    "SELECT * FROM account WHERE id = #{id}");

// 解析后的 JDBC SQL
String jdbcSql = tmpl.getSql();              // SELECT * FROM account WHERE id = ?
// 自省参数名
List<String> names = tmpl.getParamNames();   // [id]
// 延迟绑定参数值
Object[] args = tmpl.toArgs(Map.of("id", 10000L));

// 委托给位置参数 API
List<Account> result = jdbcTemplate.queryList(jdbcSql, args,
    RowMapper.beanRowMapper(Account.class));
```

> 💡 **工作原理**：`NamedParamSql` 仅解析 SQL（`#{paramName}` → `?`）并记录参数名顺序，不绑定值。参数值通过 `toArgs(Map)` 或 `PreparedSql` 的 Builder 延迟绑定，值会经过 `ParamBuilder.handleItem` 处理（Optional 拆箱等）。两者均构建后不可变，线程安全。

#### 5.2.3 批量命名参数

使用 `NamedParamSql.toBatchArgs()` 将 `List<Map>` 转换为 `List<Object[]>`，配合位置参数 `batchUpdate`；或直接使用 `NamedParamJdbcOperations` 上的命名参数批量方法，其中包括模板传参重载。

```java
// 方式一：直接传参（每次解析 SQL）
List<Map<String, Object>> batchParams = new ArrayList<>();
batchParams.add(Map.of("name", "Alice", "age", 25));
batchParams.add(Map.of("name", "Bob",   "age", 30));

BatchUpdateResult result = jdbcTemplate.batchUpdate(
    "INSERT INTO users(name, age) VALUES(#{name}, #{age})",
    batchParams, 100);

// 方式二：NamedParamSql 模板传参（解析一次，多次批量执行）
NamedParamSql tmpl = NamedParamSql.of(
    "INSERT INTO users(name, age) VALUES(#{name}, #{age})");
result = jdbcTemplate.batchUpdate(tmpl, batchParams, 100);

// 方式三：NamedParamSql 模板 + toBatchArgs（灵活控制）
List<Object[]> batchArgs = tmpl.toBatchArgs(batchParams);
result = jdbcTemplate.batchUpdate(tmpl.getSql(), batchArgs, 100);
```

#### 5.2.4 多态视图

`SimpleJdbcTemplate` 同时实现了两个接口，可以通过多态获取不同视图：

```java
SimpleJdbcTemplate tmpl = new SimpleJdbcTemplate(dataSource);

// 位置参数视图
JdbcOperations ops = tmpl;
ops.update("UPDATE t SET x = ?", buildParams(1));

// 命名参数视图
NamedParamJdbcOperations nops = tmpl;
nops.update("UPDATE t SET x = #{val}", Map.of("val", 1));

// 直接使用完整模板（两种风格均可）
tmpl.update("UPDATE t SET x = ?", buildParams(1));          // 位置参数
tmpl.update("UPDATE t SET x = #{val}", Map.of("val", 1));  // 命名参数
```

#### 5.2.5 批量更新操作示例

**位置参数风格：**

```java
// 默认模式：遇错即中断
BatchUpdateResult result = jdbcTemplate.batchUpdate(
    "INSERT INTO account (username, password, org_no) VALUES (?, ?, ?)",
    buildBatchParams(accountList, account -> buildParams(
        account.getUsername(),
        account.getPassword(),
        account.getOrgNo()
    )),
    100 // 每 100 条数据为一个批次
);

// 静默模式：遇错不中断，全部执行完毕后统一检查
BatchUpdateResult quietResult = jdbcTemplate.batchUpdate(
    "INSERT INTO account (username, password, org_no) VALUES (?, ?, ?)",
    buildBatchParams(accountList, account -> buildParams(
        account.getUsername(),
        account.getPassword(),
        account.getOrgNo()
    )),
    100,
    true // quietly = true，遇错不中断
);

// 检查批量更新结果
if (quietResult.getStatus() == BatchUpdateStatus.COMPLETED_WITH_ERRORS) {
    for (int idx : quietResult.getErrorBatchIndexes()) {
        BatchUpdateErrorInfo err = quietResult.getBatchUpdateErrorInfo(idx);
        System.err.println("批次 " + idx + " 失败: " + err.getCause().getMessage());
    }
}
```

**命名参数风格：**

```java
import xyz.zhouxy.jdbc.namedparam.NamedParamSql;

// 方式一：直接传参（每次解析 SQL）
List<Map<String, Object>> batchParams = new ArrayList<>();
batchParams.add(Map.of("name", "Alice", "age", 25));
batchParams.add(Map.of("name", "Bob",   "age", 30));

BatchUpdateResult result = jdbcTemplate.batchUpdate(
    "INSERT INTO users(name, age) VALUES(#{name}, #{age})",
    batchParams, 100);

// 静默模式
BatchUpdateResult quietResult = jdbcTemplate.batchUpdate(
    "INSERT INTO users(name, age) VALUES(#{name}, #{age})",
    batchParams, 100, true);

// 方式二：NamedParamSql 模板传参（解析一次，多次批量执行）
NamedParamSql tmpl = NamedParamSql.of(
    "INSERT INTO users(name, age) VALUES(#{name}, #{age})");
result = jdbcTemplate.batchUpdate(tmpl, batchParams, 100);

// 方式三：NamedParamSql 模板 + toBatchArgs（灵活控制）
List<Object[]> batchArgs = tmpl.toBatchArgs(batchParams);
result = jdbcTemplate.batchUpdate(tmpl.getSql(), batchArgs, 100);
```

#### 5.2.6 批量更新结果 (BatchUpdateResult)

`batchUpdate` 方法返回 `BatchUpdateResult` 对象，包含以下信息：

- `getStatus()`：批量更新的总状态（`SUCCESS` / `COMPLETED_WITH_ERRORS` / `INTERRUPTED`）。
- `getTotal()`：总数据量。
- `getBatchSize()`：批次大小
- `getBatchCount()`：总批次数。
- `getCompleteBatchCount()`：已完成的批次数。
- `getSuccessBatchCount()` / `getErrorBatchCount()`：成功 / 失败的批次数。
- `getRemainingBatchCount()`：（中断后）未执行的剩余批次数。
- `getUpdateCounts(batchIndex)`：获取指定批次更新结果。
- `getErrorBatchIndexes()`：获取所有出错的批次号.
- `getBatchUpdateErrorInfo(batchIndex)`：获取指定批次的详细错误信息。
- `getAllErrorsInfo()`：获取所有出错的批次的错误信息。

---

## 6. 事务管理 (Transaction)

通过 `TransactionTemplate` 管理事务，可直接实例化或通过 `SimpleJdbcTemplate.transaction()` 获取。

事务模板的核心机制如下：

- **共享连接**：事务内的所有 JDBC 操作共享同一个数据库连接，确保操作在同一事务上下文中执行。
- **自动提交管理**：进入事务时关闭连接的自动提交模式，事务结束后恢复原始状态。
- **异常处理**：操作中抛出异常时自动执行回滚，并将原始异常包装为 `TransactionException` 抛出。

### 6.1 获取事务模板

```java
// 方式一：通过 SimpleJdbcTemplate 获取
TransactionTemplate tx = jdbcTemplate.transaction();

// 方式二：直接实例化
TransactionTemplate tx = new TransactionTemplate(dataSource);
```

### 6.2 事务方法

下表列出所有事务执行方法，覆盖三种调用模式。

| 调用模式     | execute（消费型）                                                       | commitIfTrue（谓词型）                                                        |
| :----------- | :---------------------------------------------------------------------- | :---------------------------------------------------------------------------- |
| **位置参数** | `execute(ThrowingConsumer<JdbcOperations>)`                             | `commitIfTrue(ThrowingPredicate<JdbcOperations>)`                             |
| **命名参数** | `executeNamed(ThrowingConsumer<NamedParamJdbcOperations>)`              | `commitIfTrueNamed(ThrowingPredicate<NamedParamJdbcOperations>)`              |
| **混用**     | `execute(ThrowingBiConsumer<JdbcOperations, NamedParamJdbcOperations>)` | `commitIfTrue(ThrowingBiPredicate<JdbcOperations, NamedParamJdbcOperations>)` |

**说明：**

- **位置参数**：回调接收 `JdbcOperations`，SQL 中使用 `?` 占位符，通过 `buildParams(...)` 传参。
- **命名参数**：回调接收 `NamedParamJdbcOperations`，SQL 中使用 `#{paramName}` 占位符，通过 `Map.of(...)` 传参。
- **混用**：回调同时接收两个接口，可在同一事务中按需选择参数风格。

**执行语义：**

- `execute` 系列：若回调无异常抛出则自动提交，发生异常则回滚。
- `commitIfTrue` 系列：回调返回 `true` 提交，返回 `false` 或抛出异常则回滚。

### 6.3 事务操作示例

```java
// 自动提交/回滚事务
jdbcTemplate.transaction().execute(ops -> {
    ...
    ops.update(...);
    ...
    ops.update(...);
    ...
    // 内部无异常抛出则自动提交，抛出异常则自动回滚
});

// 根据返回值控制事务
jdbcTemplate.transaction().commitIfTrue(ops -> {
    ...
    ops.update(...);
    ...
    if (...) {
        // 中断操作并回滚
        return false;
    }
    ...
    if (...) {
        // 某些条件下提前结束并提交事务
        return true;
    }
    ...
    ops.update(...);
    ...
    // 提交事务
    return true;
});

// --- 命名参数风格 ---

// 纯命名参数事务（事务内所有 SQL 均使用命名参数）
jdbcTemplate.transaction().executeNamed(nops -> {
    nops.update("INSERT INTO account (username, org_no) VALUES(#{name}, #{org})",
            Map.of("name", "alice", "org", "0000"));
    nops.update("UPDATE account SET deleted = 1 WHERE username = #{name}",
            Map.of("name", "bob"));
});

// 命名参数谓词事务
jdbcTemplate.transaction().commitIfTrueNamed(nops -> {
    nops.update("UPDATE account SET deleted = 1 WHERE id = #{id}",
            Map.of("id", 10000L));
    return nops.queryBoolean(
            "SELECT EXISTS(SELECT 1 FROM account WHERE id = #{id} AND deleted = 1)",
            Map.of("id", 10000L));
});

// --- 混用两种参数风格 ---

// 同一事务内可自由选择位置参数或命名参数
jdbcTemplate.transaction().execute((ops, nops) -> {
    ops.update("UPDATE account SET balance = ? WHERE id = ?",
            buildParams(100, 1));
    nops.update("INSERT INTO log (msg, user_id) VALUES(#{msg}, #{uid})",
            Map.of("msg", "transfer", "uid", 1));
});

// 事务中发生异常会自动回滚（命名参数）
jdbcTemplate.transaction().executeNamed(nops -> {
    nops.update("INSERT INTO account (username) VALUES(#{name})",
            Map.of("name", "rollback_user"));
    // 抛出异常将导致事务回滚
    throw new RuntimeException("业务异常");
});
// 异常被包装为 TransactionException 抛出，事务已回滚
```

---

## 7. 位置参数构建 (ParamBuilder)

使用 `ParamBuilder` 可以快速构建位置参数（`?`）所需的参数数组。

### 7.1 构建单条参数列表（位置参数）

为避免与 Varargs 产生数组歧义，`JdbcOperations` 的方法统一使用 `Object[]` 传参。使用 `ParamBuilder.buildParams(...)` 可以快速构建 `Object[]` 参数数组，该方法会自动将 `Optional` 值进行拆箱处理。

```java
import static xyz.zhouxy.jdbc.ParamBuilder.buildParams;

buildParams("admin%", "0000");           // 返回 Object[]{"admin%", "0000"}
buildParams(Optional.of("hello"));       // 返回 Object[]{"hello"}
buildParams(Optional.empty());           // 返回 Object[]{null}
```

### 7.2 批量构建参数列表

使用 `ParamBuilder.buildBatchParams(collection, func)` 将集合中的每个元素转换为 `Object[]`，最终返回 `List<Object[]>`。

```java
import static xyz.zhouxy.jdbc.ParamBuilder.buildBatchParams;
import static xyz.zhouxy.jdbc.ParamBuilder.buildParams;

List<Object[]> batchParams = buildBatchParams(accountList, account -> buildParams(
    account.getUsername(),
    account.getPassword(),
    account.getOrgNo()
));
```

---

## 8. 连接池集成

`SimpleJdbcTemplate` 仅依赖标准的 `javax.sql.DataSource` 接口，可与任何主流数据库连接池集成。以下为常用连接池的配置示例。

### 8.1 使用 HikariCP

> [HikariCP](https://github.com/brettwooldridge/HikariCP) 是目前性能最优的数据库连接池之一，推荐在新项目中优先使用。

```xml
<!-- Maven 依赖 -->
<dependency>
    <groupId>com.zaxxer</groupId>
    <artifactId>HikariCP</artifactId>
    <version>${hikaricp.version}</version>
</dependency>
```

```java
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

// 配置 HikariCP 连接池
HikariConfig config = new HikariConfig();
config.setJdbcUrl("jdbc:mysql://localhost:3306/your_database");
config.setUsername("your_username");
config.setPassword("your_password");
// 其他连接池参数按需配置

DataSource dataSource = new HikariDataSource(config);
SimpleJdbcTemplate jdbcTemplate = new SimpleJdbcTemplate(dataSource);
```

### 8.2 使用 Druid

> [Druid](https://github.com/alibaba/druid) 是阿里巴巴开源的数据库连接池，提供了内置的监控和扩展能力，在中文社区中广泛采用。

```xml
<!-- Maven 依赖 -->
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>druid</artifactId>
    <version>${druid.version}</version>
</dependency>
```

```java
import com.alibaba.druid.pool.DruidDataSource;

DruidDataSource dataSource = new DruidDataSource();
dataSource.setUrl("jdbc:mysql://localhost:3306/your_database");
dataSource.setUsername("your_username");
dataSource.setPassword("your_password");
// 其他连接池参数按需配置

SimpleJdbcTemplate jdbcTemplate = new SimpleJdbcTemplate(dataSource);
```

### 8.3 使用 DBCP 2

> [DBCP 2](https://commons.apache.org/proper/commons-dbcp/) 是 Apache Commons 提供的数据库连接池，适用于需要依赖轻量的场景。

```xml
<!-- Maven 依赖 -->
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-dbcp2</artifactId>
    <version>${dbcp2.version}</version>
</dependency>
```

```java
import org.apache.commons.dbcp2.BasicDataSource;

BasicDataSource dataSource = new BasicDataSource();
dataSource.setUrl("jdbc:mysql://localhost:3306/your_database");
dataSource.setUsername("your_username");
dataSource.setPassword("your_password");
// 其他连接池参数按需配置

SimpleJdbcTemplate jdbcTemplate = new SimpleJdbcTemplate(dataSource);
```

---

## 9. 注意事项与适用场景

1. **风险提示**：本项目定位为轻量级工具，相较于成熟的 ORM 框架（如 MyBatis、Hibernate），其功能覆盖面和生态相对有限。**在生产环境使用前，请务必进行充分的测试，使用风险自行承担。**
2. **线程安全**：`SimpleJdbcTemplate` 本身无内部状态，是**线程安全**的。但请确保其底层依赖的 `DataSource`（如 HikariCP、Druid 等连接池）已正确配置并保证线程安全。
3. **连接管理**：每次数据库操作均会自动从 `DataSource` 获取连接，并在操作完成（或发生异常）后自动关闭，开发者无需手动管理连接的释放。
4. **适用场景**：中小型项目、内部工具、快速原型开发、未引入 ORM 框架的遗留系统改造、学习 JDBC 原理。

---

## 10. 致谢

- **MyBatis** — 本项目的命名参数解析功能使用了 MyBatis（https://mybatis.org/）中的 `GenericTokenParser` 和 `TokenHandler` 实现（v3.6.0），遵循 Apache License 2.0 许可。这两个工具类属于稳定的底层基础设施，在 MyBatis 各版本间变动极小。项目将持续关注 MyBatis 发布说明（[Releases](https://github.com/mybatis/mybatis-3/releases)），如有相关安全修复或 bug 修复，将及时同步更新。详细声明请参见 [`NOTICE`](NOTICE)。
