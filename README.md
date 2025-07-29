# SimpleJDBC

对 JDBC 的简单封装。

之前遇到的一个老项目，没有引入任何 ORM 框架，对数据库的操作几乎都在写原生 JDBC。故自己写了几个工具类，对 JDBC 进行简单封装。

## 查询

### 查询方法

- `query`：**最基础的查询方法**。可使用 `ResultHandler` 将查询结果映射为 Java 对象。
- `queryList`：**查询列表**。可使用 `RowMapper` 将结果的每一行数据映射为 Java 对象，返回列表。
- `queryFirst`：**查询，并获取第一行数据**。一般可以结合 `LIMIT 1` 使用。可使用 `RowMapper` 将结果的第一行数据映射为 Java 对象，返回 `Optional`。
- `queryFirstXXX`：**查询，并获取第一行数据的第一个字段**，并转换为对应类型，返回对应的类型的 `Optional`。
- `queryAsBoolean`：**查询，并获取第一行数据的第一个字段**，并转换为布尔类型。如果结果为空，则返回 `false`。

### 结果映射

- `ResultHandler` 用于处理查询结果，自定义逻辑将完整的 `ResultSet` 映射为 Java 对象。 *结果可以是任意类型（包括集合）。*
- `RowMapper` 用于将 `ResultSet` 中的一行数据映射为 Java 对象。
  - `RowMapper#HASH_MAP_MAPPER`：将 `ResultSet` 中的一行数据映射为 `HashMap`。
  - `RowMapper#beanRowMapper`：返回将 `ResultSet` 转换为 Java Bean 的默认实现。

## 更新

- `int update`：**执行 DML**，包括 `INSERT`、`UPDATE`、`DELETE` 等。返回受影响行数。
- `<T> List<T> update`：**执行 DML**，自动生成的字段将使用 `rowMapper` 进行映射，并返回列表。
- `List<int[]> batchUpdate`：**分批次执行 DML**，返回每个批次的每条 SQL 语句影响的行数。
- `List<int[]> batchUpdateAndIgnoreException`：**分批次执行 DML，如果某个批次出现异常，继续执行下一个批次**。返回每个批次的每条 SQL 语句影响的行数。

## 事务

- `executeTransaction`：**执行事务**。传入一个 `ThrowingConsumer` 函数，入参是一个 `JdbcExecutor` 对象，在 `ThrowingConsumer` 内使用该入参执行 jdbc 操作。如果 `ThrowingConsumer` 内部有异常抛出，这些操作将被回滚。

- `commitIfTrue`：**执行事务**。传入一个 `ThrowingPredicate` 函数，入参是一个 `JdbcExecutor` 对象，在 `ThrowingPredicate` 内使用该入参执行 jdbc 操作。如果`ThrowingPredicate` 返回 `true`，则提交事务；如果返回 `false` 或有异常抛出，则回滚这些操作。

## 参数构建

此项目中的所有查询和更新的方法，都**不使用可变长入参**，避免强行将 SQL 语句的参数列表放在最后，也避免和数组发生歧义。

### 构建参数列表

可使用 `ParamBuilder#buildParams` 构建 `Object[]` 数组作为 SQL 的参数列表。该方法会自动将 `Optional` 中的值“拆”出来。

### 批量构建参数列表

使用 `ParamBuilder#buildBatchParams`，将使用传入的函数，将集合中的每一个元素转为 `Object[]`，并返回一个 `List<Object[]>`。

## 示例

创建 SimpleJdbcTemplate 对象
```java
SimpleJdbcTemplate jdbcTemplate = new SimpleJdbcTemplate(dataSource);
```

查询
```java
// 查询
List<Account> list = jdbcTemplate.query(
    "SELECT * FROM account WHERE deleted = 0 AND username LIKE ? AND org_no = ?",
    buildParams("admin%", "0000"),
    rs -> {
        List<T> result = new ArrayList<>();
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

// 查询列表
List<Account> list = jdbcTemplate.queryList(
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
    )
)

// 查询一行数据，并获取第一个字段
OptionalInt age = jdbcTemplate.queryFirstInt(
    "SELECT age FROM view_account WHERE deleted = 0 AND id = ?",
    buildParams(10000L)
);

// 查询 boolean
boolean exists = jdbcTemplate.queryAsBoolean(
    "SELECT EXISTS(SELECT 1 FROM account WHERE deleted = 0 AND id = ? LIMIT 1)",
    buildParams(10000L)
);
```

更新
```java
// 执行 DML
int affectedRows = jdbcTemplate.update(
    "UPDATE account SET deleted = 1 WHERE id = ?",
    buildParams(10000L)
);

// 执行 DML，并获取生成的主键
List<Pair<Long, LocalDateTime>> keys = jdbcTemplate.update(
    "INSERT INTO account (username, password, org_no) VALUES (?, ?, ?)",
    buildParams("admin", "123456", "0000"),
    (rs, rowNum) -> Pair.of(
      rs.getLong("id"),
      rs.getObject("create_time", LocalDateTime.class)
    )
);
```

批量更新
```java
jdbcTemplate.batchUpdate(
    "INSERT INTO account (username, password, org_no) VALUES (?, ?, ?)",
    buildBatchParams(accountList, account -> buildParams(
        account.getUsername(),
        account.getPassword(),
        account.getOrgNo()
    )),
    100 // 每100条数据一个批次
);
```

事务
```java
jdbcTemplate.executeTransaction(jdbc -> {
    ...
    jdbc.update(...);
    ...
    jdbc.update(...);
    ...
});

jdbcTemplate.commitIfTrue(jdbc -> {
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

>**！！！本项目不比成熟的工具，如若使用请自行承担风险。建议仅作为 JDBC 的学习参考。**
