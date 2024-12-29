package xyz.zhouxy.jdbc;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import javax.annotation.Nullable;

/**
 * JdbcOperations
 *
 * <p>
 * 定义 JdbcTemplate 的 API
 * </p>
 *
 * @author <a href="http://zhouxy.xyz:3000/ZhouXY108">ZhouXY</a>
 * @since 1.0.0
 */
interface JdbcOperations {

    // #region - query

    /**
     * 执行查询，并按照自定义处理逻辑对结果进行处理，将结果转换为指定类型并返回
     *
     * @param sql           SQL
     * @param params        参数
     * @param resultHandler 结果处理器，用于处理 {@link ResultSet}
     */
    <T> T query(String sql, Object[] params, ResultHandler<T> resultHandler)
            throws SQLException;

    /**
     * 执行查询，并按照自定义处理逻辑对结果进行处理，将结果转换为指定类型并返回
     *
     * @param sql           SQL
     * @param resultHandler 结果处理器，用于处理 {@link ResultSet}
     */
    <T> T query(String sql, ResultHandler<T> resultHandler)
            throws SQLException;

    // #endregion

    // #region - queryList

    /**
     * 执行查询，将查询结果的每一行数据按照指定逻辑进行处理，返回结果列表
     *
     * @param sql       SQL
     * @param params    参数
     * @param rowMapper {@link ResultSet} 中每一行的数据的处理逻辑
     */
    <T> List<T> queryList(String sql, Object[] params, RowMapper<T> rowMapper)
            throws SQLException;

    /**
     * 执行查询，返回结果映射为指定的类型。当结果为单列时使用
     *
     * @param sql    SQL
     * @param params 参数
     * @param clazz  将结果映射为指定的类型
     */
    <T> List<T> queryList(String sql, Object[] params, Class<T> clazz)
            throws SQLException;

    /**
     * 执行查询，每一行数据映射为 {@code Map<String, Object>}，返回结果列表
     *
     * @param sql    SQL
     * @param params 参数列表
     */
    List<Map<String, Object>> queryList(String sql, Object[] params)
            throws SQLException;

    /**
     * 执行查询，将查询结果的每一行数据按照指定逻辑进行处理，返回结果列表
     *
     * @param sql       SQL
     * @param rowMapper {@link ResultSet} 中每一行的数据的处理逻辑
     */
    <T> List<T> queryList(String sql, RowMapper<T> rowMapper)
            throws SQLException;

    /**
     * 执行查询，返回结果映射为指定的类型。当结果为单列时使用
     *
     * @param sql   SQL
     * @param clazz 将结果映射为指定的类型
     */
    <T> List<T> queryList(String sql, Class<T> clazz)
            throws SQLException;

    /**
     * 执行查询，每一行数据映射为 {@code Map<String, Object>}，返回结果列表
     *
     * @param sql SQL
     */
    List<Map<String, Object>> queryList(String sql)
            throws SQLException;

    // #endregion

    // #region - queryFirst

    /**
     * 执行查询，将查询结果的第一行数据按照指定逻辑进行处理，返回 {@link Optional}
     *
     * @param sql       SQL
     * @param params    参数
     * @param rowMapper {@link ResultSet} 中每一行的数据的处理逻辑
     */
    <T> Optional<T> queryFirst(String sql, Object[] params, RowMapper<T> rowMapper)
            throws SQLException;

    /**
     * 查询第一行第一列，并转换为指定类型
     *
     * @param <T>    目标类型
     * @param sql    SQL
     * @param params 参数
     * @param clazz  目标类型
     */
    <T> Optional<T> queryFirst(String sql, Object[] params, Class<T> clazz)
            throws SQLException;

    /**
     * 执行查询，将第一行数据转为 Map<String, Object>
     *
     * @param sql    SQL
     * @param params 参数
     */
    Optional<Map<String, Object>> queryFirst(String sql, Object[] params)
            throws SQLException;

    /**
     * 查询第一行第一列，并转换为字符串
     *
     * @param sql    SQL
     * @param params 参数
     */
    Optional<String> queryFirstString(String sql, Object[] params)
            throws SQLException;

    /**
     * 查询第一行第一列，并转换为整数值
     *
     * @param sql    SQL
     * @param params 参数
     */
    OptionalInt queryFirstInt(String sql, Object[] params)
            throws SQLException;

    /**
     * 查询第一行第一列，并转换为长整型
     *
     * @param sql    SQL
     * @param params 参数
     */
    OptionalLong queryFirstLong(String sql, Object[] params)
            throws SQLException;

    /**
     * 查询第一行第一列，并转换为双精度浮点型
     *
     * @param sql    SQL
     * @param params 参数
     */
    OptionalDouble queryFirstDouble(String sql, Object[] params)
            throws SQLException;

    /**
     * 查询第一行第一列，并转换为 {@link BigDecimal}
     *
     * @param sql    SQL
     * @param params 参数
     */
    Optional<BigDecimal> queryFirstBigDecimal(String sql, Object[] params)
            throws SQLException;

    /**
     * 执行查询，将查询结果的第一行数据按照指定逻辑进行处理，返回 {@link Optional}
     *
     * @param sql       SQL
     * @param rowMapper {@link ResultSet} 中每一行的数据的处理逻辑
     */
    <T> Optional<T> queryFirst(String sql, RowMapper<T> rowMapper)
            throws SQLException;

    /**
     * 查询第一行第一列，并转换为指定类型
     *
     * @param <T>   目标类型
     * @param sql   SQL
     * @param clazz 目标类型
     */
    <T> Optional<T> queryFirst(String sql, Class<T> clazz)
            throws SQLException;

    /**
     * 执行查询，将第一行数据转为 Map<String, Object>
     *
     * @param sql SQL
     */
    Optional<Map<String, Object>> queryFirst(String sql)
            throws SQLException;

    /**
     * 查询第一行第一列，并转换为字符串
     *
     * @param sql SQL
     */
    Optional<String> queryFirstString(String sql)
            throws SQLException;

    /**
     * 查询第一行第一列，并转换为整数值
     *
     * @param sql SQL
     */
    OptionalInt queryFirstInt(String sql)
            throws SQLException;

    /**
     * 查询第一行第一列，并转换为长整型
     *
     * @param sql SQL
     */
    OptionalLong queryFirstLong(String sql)
            throws SQLException;

    /**
     * 查询第一行第一列，并转换为双精度浮点型
     *
     * @param sql SQL
     */
    OptionalDouble queryFirstDouble(String sql)
            throws SQLException;

    /**
     * 查询第一行第一列，并转换为 {@link BigDecimal}
     *
     * @param sql SQL
     */
    Optional<BigDecimal> queryFirstBigDecimal(String sql)
            throws SQLException;

    /**
     * 查询结果，并转换为 boolean
     *
     * @param sql SQL
     */
    boolean queryAsBoolean(String sql)
            throws SQLException;

    /**
     * 查询结果，并转换为 boolean
     *
     * @param sql SQL
     */
    boolean queryAsBoolean(String sql, Object[] params)
            throws SQLException;

    // #endregion

    // #region - update & batchUpdate

    /**
     * 执行更新操作
     *
     * @param sql    要执行的 SQL
     * @param params 参数
     * @return 更新记录数
     */
    int update(String sql, Object[] params)
            throws SQLException;

    /**
     * 执行更新操作
     *
     * @param sql 要执行的 SQL
     * @return 更新记录数
     */
    int update(String sql)
            throws SQLException;

    /**
     * 执行 SQL 并返回生成的 keys
     *
     * @param sql       要执行的 SQL
     * @param params    参数
     * @param rowMapper 行数据映射逻辑
     *
     * @return generated keys
     * @throws SQLException 执行 SQL 遇到异常情况将抛出
     */
    <T> List<T> update(String sql, Object[] params, RowMapper<T> rowMapper)
            throws SQLException;

    /**
     * 执行 SQL 并返回生成的 keys
     *
     * @param sql       要执行的 SQL
     * @param rowMapper 行数据映射逻辑
     *
     * @return generated keys
     * @throws SQLException 执行 SQL 遇到异常情况将抛出
     */
    <T> List<T> update(String sql, RowMapper<T> rowMapper)
            throws SQLException;

    /**
     * 执行批量更新，批量更新数据，返回每条记录更新的行数
     *
     * @param sql       SQL 语句
     * @param params    参数列表
     * @param batchSize 每次批量更新的数据量
     */
    List<int[]> batchUpdate(String sql, @Nullable Collection<Object[]> params, int batchSize)
            throws SQLException;

    /**
     * 批量更新，返回更新成功的记录行数。发生异常时不中断操作，将异常存入 {@code exceptions} 中
     *
     * @param sql        sql语句
     * @param params     参数列表
     * @param batchSize  每次批量更新的数据量
     * @param exceptions 异常列表，用于记录异常信息
     */
    List<int[]> batchUpdateAndIgnoreException(String sql, @Nullable Collection<Object[]> params,
            int batchSize, List<Exception> exceptions)
            throws SQLException;

    // #endregion
}
