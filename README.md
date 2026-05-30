# SimpleJDBC

SimpleJDBC 是一个轻量级 JDBC 工具库，提供简洁的 API 用于执行 SQL 查询、更新、批量操作及事务管理，适用于未引入 ORM 框架、直接使用原生 JDBC 的项目。

## 1. 快速开始

**要求 JDK 8+。**

Maven 依赖：

```xml
<dependency>
    <groupId>xyz.zhouxy.jdbc</groupId>
    <artifactId>simple-jdbc</artifactId>
    <version>${simple-jdbc.version}</version>
</dependency>
```

> 本项目基于 **Apache License 2.0** 开源。

## 2. 查询

### 2.1 查询方法

所有查询方法均使用 `Object[]` 作为参数，并提供了无参便捷重载（适用于不含占位符的 SQL）。

| 方法 | 说明 |
|---|---|
| `query(sql, params, resultHandler)` | 最基础的查询，通过 `ResultHandler` 自定义映射逻辑 |
| `queryList(sql, params, rowMapper)` | 查询列表，通过 `RowMapper` 逐行映射 |
| `queryList(sql, params, Class)` | 单列查询列表，每行取第一列转为指定类型 |
| `queryList(sql, params)` | 查询列表，每行转为 `Map<String, Object>` |
| `queryFirst(sql, params, rowMapper)` | 查询第一行，通过 `RowMapper` 映射，返回 `Optional` |
| `queryFirst(sql, params, Class)` | 查询第一行第一列，返回 `Optional<T>` |
| `queryFirst(sql, params)` | 查询第一行，返回 `Optional<Map<String, Object>>` |
| `queryBoolean(sql, params)` | 查询第一行第一列并转为 boolean，结果为空返回 `false` |

> 以上方法均有不含 `params` 的便捷重载，例如 `queryList(sql, rowMapper)`、`queryFirst(sql, Class)` 等，适用于无参数 SQL。

### 2.2 结果映射

- **`ResultHandler`**：处理完整的 `ResultSet`，自定义逻辑将结果映射为任意类型（包括集合）。
- **`RowMapper`**：将 `ResultSet` 中的一行数据映射为 Java 对象。
  - `RowMapper.HASH_MAP_MAPPER`：每行映射为 `HashMap<String, Object>`。
  - `RowMapper.beanRowMapper(Class)`：默认的 Bean 映射，属性名（小驼峰） ↔ 列名（小写蛇形）。
  - `RowMapper.beanRowMapper(Class, Map<String, String>)`：自定义属性名与列名映射的 Bean 映射。

## 3. 更新

所有更新方法同样提供了无参便捷重载。

| 方法 | 说明 |
|---|---|
| `update(sql, params)` | 执行 DML（INSERT / UPDATE / DELETE），返回受影响行数 |
| `updateAndReturnKeys(sql, params, rowMapper)` | 执行 DML 并返回自动生成的键，通过 `RowMapper` 映射 |
| `batchUpdate(sql, params, batchSize)` | 分批执行 DML，遇错即中断 |
| `batchUpdate(sql, params, batchSize, quietly)` | 分批执行 DML；`quietly=true` 遇错不中断，全部执行完毕 |

### BatchUpdateResult

`batchUpdate` 返回 `BatchUpdateResult`，包含：

- `getStatus()`：批次状态（`SUCCESS` / `COMPLETED_WITH_ERRORS` / `INTERRUPTED`）
- `getTotal()`：总数据量
- `getBatchCount()`：总批次数
- `getSuccessBatchCount()` / `getErrorBatchCount()`：成功/失败批次数
- `getBatchUpdateErrorInfo(batchIndex)`：获取指定批次的错误详情

## 4. 事务

通过 `TransactionTemplate` 管理事务，可直接创建或通过 `SimpleJdbcTemplate.transaction()` 获取。

- **`execute(consumer)`**：执行事务。传入 `ThrowingConsumer<JdbcOperations>`，若内部无异常则提交，有异常则回滚。
- **`commitIfTrue(predicate)`**：执行事务。传入 `ThrowingPredicate<JdbcOperations>`，返回 `true` 提交，返回 `false` 或抛异常则回滚。

## 5. 参数构建

此项目中所有方法都**不使用可变长参数**，避免强制将参数列表放在 SQL 语句末尾，也避免与数组产生歧义。

### 5.1 构建参数列表

使用 `ParamBuilder.buildParams(...)` 构建 `Object[]` 作为 SQL 参数。该方法会自动将 `Optional` 值拆箱。

```java
import static xyz.zhouxy.jdbc.ParamBuilder.buildParams;

buildParams("admin%", "0000");           // → Object[]{"admin%", "0000"}
buildParams(Optional.of("hello"));        // → Object[]{"hello"}
buildParams(Optional.empty());            // → Object[]{null}
```

### 5.2 批量构建参数列表

使用 `ParamBuilder.buildBatchParams(collection, func)` 将集合中每个元素转为 `Object[]`，返回 `List<Object[]>`。

```java
import static xyz.zhouxy.jdbc.ParamBuilder.buildBatchParams;
import static xyz.zhouxy.jdbc.ParamBuilder.buildParams;

buildBatchParams(accountList, account -> buildParams(
    account.getUsername(),
    account.getPassword(),
    account.getOrgNo()
));
```

## 6. 示例

创建 `SimpleJdbcTemplate` 对象：

```java
SimpleJdbcTemplate jdbcTemplate = new SimpleJdbcTemplate(dataSource);
```

### 6.1 查询

```java
// 查询（使用 ResultHandler 处理全部结果）
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

// 查询列表（使用 DefaultBeanRowMapper 进行映射）
List<Account> accounts = jdbcTemplate.queryList(
    "SELECT * FROM account WHERE deleted = 0 AND username LIKE ? AND org_no = ?",
    buildParams("admin%", "0000"),
    RowMapper.beanRowMapper(Account.class)
);

// 查询列表（使用自定义 RowMapper 进行映射）
List<Account> accounts = jdbcTemplate.queryList(
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

// 查询一行数据
Optional<Account> account = jdbcTemplate.queryFirst(
    "SELECT * FROM account WHERE deleted = 0 AND id = ?",
    buildParams(10000L),
    (rs, rowNum) -> new Account(
        rs.getLong("id"),
        rs.getString("username"),
        rs.getString("password"),
        rs.getString("org_no"),
        rs.getTimestamp("create_time"),
        rs.getTimestamp("update_time")
    )
);

// 查询 boolean
boolean exists = jdbcTemplate.queryBoolean(
    "SELECT EXISTS(SELECT 1 FROM account WHERE deleted = 0 AND id = ?)",
    buildParams(10000L)
);

// 无参数 SQL 可直接省略 params
List<Account> allAccounts = jdbcTemplate.queryList(
    "SELECT * FROM account WHERE deleted = 0",
    RowMapper.beanRowMapper(Account.class)
);
```

### 6.2 更新

```java
// 执行 DML
int affectedRows = jdbcTemplate.update(
    "UPDATE account SET deleted = 1 WHERE id = ?",
    buildParams(10000L)
);

// 执行 DML，并获取生成的主键
List<Pair<Long, LocalDateTime>> keys = jdbcTemplate.updateAndReturnKeys(
    "INSERT INTO account (username, password, org_no) VALUES (?, ?, ?)",
    buildParams("admin", "123456", "0000"),
    (rs, rowNum) -> Pair.of(
      rs.getLong("id"),
      rs.getObject("create_time", LocalDateTime.class)
    )
);
```

### 6.3 批量更新

```java
// 默认：遇错即中断
BatchUpdateResult result = jdbcTemplate.batchUpdate(
    "INSERT INTO account (username, password, org_no) VALUES (?, ?, ?)",
    buildBatchParams(accountList, account -> buildParams(
        account.getUsername(),
        account.getPassword(),
        account.getOrgNo()
    )),
    100 // 每 100 条数据一个批次
);

// 静默模式：遇错不中断，全部执行完毕后统一检查结果
BatchUpdateResult result = jdbcTemplate.batchUpdate(
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
if (result.getStatus() == BatchUpdateStatus.COMPLETED_WITH_ERRORS) {
    for (int idx : result.getErrorBatchIndexes()) {
        BatchUpdateErrorInfo err = result.getBatchUpdateErrorInfo(idx);
        System.err.println("批次 " + idx + " 失败: " + err.getCause().getMessage());
    }
}
```

### 6.4 事务

```java
jdbcTemplate.transaction().execute(jdbc -> {
    ...
    jdbc.update(...);
    ...
    jdbc.update(...);
    ...
    // 无异常则自动提交
});

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

> **！！！本项目不比成熟的工具，如若使用请自行承担风险。**
>
> - **线程安全**：`SimpleJdbcTemplate` 无内部状态，线程安全。但所依赖的 `DataSource` 需自行保证线程安全。
> - **连接管理**：每次操作自动从 `DataSource` 获取连接并在操作完成后关闭，无需手动管理。
> - **适用场景**：不适合高并发或大数据量场景，建议仅用于学习参考或小型项目。
