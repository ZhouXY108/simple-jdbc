package xyz.zhouxy.jdbc.test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.stream.Collectors;

import org.h2.jdbcx.JdbcConnectionPool;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.zhouxy.jdbc.SimpleJdbcTemplate;

/**
 * 测试基类，提供 H2 内存数据库连接池和模板实例。
 *
 * <p>使用 H2 内置 JdbcConnectionPool 作为连接池，符合项目"基本仅考虑数据库连接池"的设计。</p>
 */
public abstract class BaseH2Test {

    protected static final Logger logger = LoggerFactory.getLogger(BaseH2Test.class);

    protected static JdbcConnectionPool dataSource;

    protected SimpleJdbcTemplate createTemplate() {
        return new SimpleJdbcTemplate(dataSource);
    }

    @BeforeAll
    static void initDatabase() throws Exception {
        dataSource = JdbcConnectionPool.create(
                "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DATABASE_TO_UPPER=FALSE",
                "sa", "");
        dataSource.setMaxConnections(10);

        // 加载并执行 SQL 初始化文件
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            String sqlContent = loadSqlFile("init_tables.sql");
            // 按分号拆分并逐条执行
            for (String singleSql : sqlContent.split(";")) {
                String trimmed = singleSql.trim();
                if (!trimmed.isEmpty()) {
                    stmt.execute(trimmed);
                }
            }
        }

        logger.info("H2 数据库初始化完成，连接池已就绪");
    }

    @AfterAll
    static void closeDatabase() {
        if (dataSource != null) {
            dataSource.dispose();
            logger.info("H2 数据库连接池已关闭");
        }
    }

    /**
     * 重新执行初始化脚本，恢复数据到初始状态。
     */
    protected static void resetDatabase() throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            String sqlContent = loadSqlFile("init_tables.sql");
            for (String singleSql : sqlContent.split(";")) {
                String trimmed = singleSql.trim();
                if (!trimmed.isEmpty()) {
                    stmt.execute(trimmed);
                }
            }
        }

        logger.info("数据库已重置为初始状态");
    }

    private static String loadSqlFile(String fileName) throws Exception {
        try (InputStream is = BaseH2Test.class.getClassLoader().getResourceAsStream(fileName);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }
}
