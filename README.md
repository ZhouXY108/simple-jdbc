# Simple JDBC

`Simple JDBC` 提供了一套轻量级的 JDBC 封装工具类，是作者在对传统遗留项目进行改造时设计。该项目未引入 ORM 框架，原本的数据库交互高度依赖原生 JDBC API，导致存在大量冗余的样板代码（Boilerplate Code）。本项目通过抽象底层数据库操作，简化了连接管理、SQL 执行与结果集处理流程，提升数据访问层的开发效率与代码可维护性。

> 注：本项目基于 [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0) 开源协议发布。

---

## 1. ✨ 核心特性

- **轻量无依赖**：基于原生 JDBC 封装，无第三方重量级依赖。
- **API 简洁**：提供丰富的快捷方法，大幅减少样板代码。
- **灵活的映射**：支持自定义 `ResultHandler` 与 `RowMapper`，内置默认 Bean 映射策略。
- **事务与批处理**：提供声明式的事务模板与完善的批量更新错误处理机制。
- **线程安全**：核心模板类无状态设计，天然支持多线程环境。

---

## 2. 📦 快速开始

### 2.1 环境要求

- **JDK 8** 或更高版本

### 2.2 添加 Maven 依赖

将以下配置添加到您的 `pom.xml` 中：

```xml
<dependency>
    <groupId>xyz.zhouxy.jdbc</groupId>
    <artifactId>simple-jdbc</artifactId>
    <version>${simple-jdbc.version}</version>
</dependency>
```

### 2.3 初始化

```java
SimpleJdbcTemplate jdbcTemplate = new SimpleJdbcTemplate(dataSource);
```

> 💡 关于与数据库连接池（如 HikariCP、Druid、DBCP 2 等）的集成方式，请参见「[连接池集成](#7-连接池集成)」章节。

### 2.4 查询操作

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
List<String> usernames = jdbcTemplate.queryList(
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
```

> 📖 完整的方法列表与映射策略说明请参见「[3. 数据查询](#3-🔍-数据查询-query)」章节。

### 2.5 更新操作

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
```

> 📖 完整的方法列表与批量更新结果说明请参见「[4. 数据更新](#4-✏️-数据更新-update)」章节。

### 2.6 批量更新操作

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

> 📖 批量更新的详细结果说明请参见「[4.2 批量更新结果](#42-批量更新结果-batchupdateresult)」章节。

### 2.7 事务管理

```java
// 自动提交/回滚事务
jdbcTemplate.transaction().execute(jdbc -> {
    ...
    jdbc.update(...);
    ...
    jdbc.update(...);
    ...
    // 内部无异常抛出则自动提交，抛出异常则自动回滚
});

// 根据返回值控制事务
jdbcTemplate.transaction().commitIfTrue(jdbc -> {
    ...
    jdbc.update(...);
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
    jdbc.update(...);
    ...
    // 提交事务
    return true;
});
```

> 📖 事务方法的详细说明请参见「[5. 事务管理](#5-🔄-事务管理-transaction)」章节。

---

## 3. 🔍 数据查询 (Query)

### 3.1 查询方法列表

| 方法签名 | 说明 |
| :--- | :--- |
| `query(sql, params, resultHandler)` | 最基础的查询，通过 `ResultHandler` 自定义完整的映射逻辑。 |
| `queryList(sql, params, rowMapper)` | 查询列表，通过 `RowMapper` 逐行映射。 |
| `queryList(sql, params, Class)` | 单列查询列表，每行提取第一列并转换为指定类型。 |
| `queryList(sql, params)` | 查询列表，每行自动转换为 `Map<String, Object>`。 |
| `queryFirst(sql, params, rowMapper)` | 查询第一行，通过 `RowMapper` 映射，返回 `Optional<T>`。 |
| `queryFirst(sql, params, Class)` | 查询第一行第一列，返回 `Optional<T>`。 |
| `queryFirst(sql, params)` | 查询第一行，返回 `Optional<Map<String, Object>>`。 |
| `queryBoolean(sql, params)` | 查询第一行第一列并转换为 `boolean`，若结果为空则返回 `false`。 |

*💡 提示：以上方法均有省略 `params` 的重载（如 `queryList(sql, rowMapper)`），适用于不含占位符的 SQL 语句。*

### 3.2 结果映射策略

- **`ResultHandler`**：处理完整的 `ResultSet`，允许自定义逻辑将结果集映射为任意类型（包括集合）。
- **`RowMapper`**：将 `ResultSet` 中的单行数据映射为 Java 对象。内置以下默认实现：
  - `RowMapper.HASH_MAP_MAPPER`：将每行数据映射为 `HashMap<String, Object>`。
  - `DefaultBeanRowMapper`：将 `ResultSet` 中的一行数据映射为 Java Bean 的默认实现。使用反射获取类型信息、调用无参构造器和 `setter` 方法。**（注：实际生产中更建议针对目标类型自定义 `RowMapper` 以提升性能）**
    - `RowMapper.beanRowMapper(Class)`：自动匹配 **属性名（小驼峰） ↔ 列名（小写蛇形）**。
    - `RowMapper.beanRowMapper(Class, Map<String, String>)`：通过 `Map` 自定义属性名与列名映射关系。

---

## 4. ✏️ 数据更新 (Update)

所有更新方法同样提供了无参重载。

### 4.1 更新方法列表

| 方法签名 | 说明 |
| :--- | :--- |
| `update(sql, params)` | 执行 DML（INSERT / UPDATE / DELETE），返回受影响的行数。 |
| `updateAndReturnKeys(sql, params, rowMapper)` | 执行 DML 并返回自动生成的键（如自增 ID），通过 `RowMapper` 进行映射。 |
| `batchUpdate(sql, params, batchSize)` | 分批执行 DML，遇到错误立即中断。 |
| `batchUpdate(sql, params, batchSize, quietly)` | 分批执行 DML；若 `quietly=true`，则遇到错误不中断，直至全部执行完毕。 |

### 4.2 批量更新结果 (BatchUpdateResult)

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

## 5. 🔄 事务管理 (Transaction)

通过 `TransactionTemplate` 管理事务，可直接实例化或通过 `SimpleJdbcTemplate.transaction()` 获取。

事务模板的核心机制如下：

- **共享连接**：事务内的所有 JDBC 操作共享同一个数据库连接，确保操作在同一事务上下文中执行。
- **自动提交管理**：进入事务时关闭连接的自动提交模式，事务结束后恢复原始状态。
- **异常处理**：操作中抛出异常时自动执行回滚，并将原始异常包装为 `TransactionException` 抛出。

### 5.1 获取事务模板

```java
// 方式一：通过 SimpleJdbcTemplate 获取
TransactionTemplate tx = jdbcTemplate.transaction();

// 方式二：直接实例化
TransactionTemplate tx = new TransactionTemplate(dataSource);
```

### 5.2 事务方法

- **`execute(consumer)`**：执行事务。传入 `ThrowingConsumer<JdbcOperations>`，若内部代码无异常抛出则自动提交，发生异常则回滚。
- **`commitIfTrue(predicate)`**：执行事务。传入 `ThrowingPredicate<JdbcOperations>`，根据返回值决定事务走向：返回 `true` 提交，返回 `false` 或抛出异常则回滚。

---

## 6. 🛠️ 参数构建

为避免与数组产生歧义并规范 API 设计，`JdbcOperations` 中的所有方法均不使用可变长参数（Varargs），而是统一使用 `Object[]` 作为参数传递。您可以使用内置的 `ParamBuilder` 快速构建参数。

### 6.1 构建单条参数列表

使用 `ParamBuilder.buildParams(...)` 构建 `Object[]`。该方法会自动将 `Optional` 值进行拆箱处理。

```java
import static xyz.zhouxy.jdbc.ParamBuilder.buildParams;

buildParams("admin%", "0000");           // 返回 Object[]{"admin%", "0000"}
buildParams(Optional.of("hello"));       // 返回 Object[]{"hello"}
buildParams(Optional.empty());           // 返回 Object[]{null}
```

### 6.2 批量构建参数列表

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

## 7. 📌 连接池集成

`SimpleJdbcTemplate` 仅依赖标准的 `javax.sql.DataSource` 接口，可与任何主流数据库连接池集成。以下为常用连接池的配置示例。

### 7.1 使用 HikariCP

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

### 7.2 使用 Druid

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

### 7.3 使用 DBCP 2

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

## 8. ⚠️ 注意事项与适用场景

1. **风险提示**：本项目定位为轻量级工具，相较于成熟的 ORM 框架（如 MyBatis、Hibernate），其功能覆盖面和生态相对有限。**在生产环境使用前，请务必进行充分的测试，使用风险自行承担。**
2. **线程安全**：`SimpleJdbcTemplate` 本身无内部状态，是**线程安全**的。但请确保其底层依赖的 `DataSource`（如 HikariCP、Druid 等连接池）已正确配置并保证线程安全。
3. **连接管理**：每次数据库操作均会自动从 `DataSource` 获取连接，并在操作完成（或发生异常）后自动关闭，开发者无需手动管理连接的释放。
4. **适用场景**：中小型项目、内部工具、快速原型开发、未引入 ORM 框架的遗留系统改造、学习 JDBC 原理。
