package xyz.zhouxy.jdbc.test;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jspecify.annotations.NonNull;

import xyz.zhouxy.jdbc.RowMapper;

/**
 * 自定义 User RowMapper，直接操作 ResultSet 进行映射。
 *
 * <p>相较于 SimpleBeanRowMapper，自定义 RowMapper 避免了反射开销，性能更优。</p>
 */
public class UserRowMapper implements RowMapper<User> {

    @Override
    public User mapRow(@NonNull ResultSet rs, int rowNumber) throws SQLException {
        User user = new User();
        user.setId(rs.getObject("id", Long.class));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setAge(rs.getObject("age", Integer.class));
        user.setBalance(rs.getObject("balance", Long.class));
        user.setActive(rs.getObject("active", Boolean.class));
        user.setCreatedAt(rs.getObject("created_at", java.time.LocalDateTime.class));
        user.setBirthDate(rs.getObject("birth_date", java.time.LocalDate.class));
        user.setWorkStartTime(rs.getObject("work_start_time", java.time.LocalTime.class));
        return user;
    }
}
