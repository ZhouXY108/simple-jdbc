package xyz.zhouxy.jdbc.test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

/**
 * 测试用的 Java Bean，用于验证 RowMapper 映射。
 *
 * <p>注意：属性全部使用引用类型，以匹配 SimpleBeanRowMapper 的设计约束。</p>
 */
public class User {

    private Long id;
    private String username;
    private String email;
    private Integer age;
    private Long balance;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDate birthDate;
    private LocalTime workStartTime;

    public User() {
    }

    public User(String username, String email, Integer age, Long balance, Boolean active) {
        this.username = username;
        this.email = email;
        this.age = age;
        this.balance = balance;
        this.active = active;
    }

    public User(String username, String email, Integer age, Long balance, Boolean active,
            LocalDateTime createdAt, LocalDate birthDate, LocalTime workStartTime) {
        this.username = username;
        this.email = email;
        this.age = age;
        this.balance = balance;
        this.active = active;
        this.createdAt = createdAt;
        this.birthDate = birthDate;
        this.workStartTime = workStartTime;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public Long getBalance() {
        return balance;
    }

    public void setBalance(Long balance) {
        this.balance = balance;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public LocalTime getWorkStartTime() {
        return workStartTime;
    }

    public void setWorkStartTime(LocalTime workStartTime) {
        this.workStartTime = workStartTime;
    }

    @Override
    public String toString() {
        return "User{id=" + id + ", username='" + username + "', email='" + email
                + "', age=" + age + ", balance=" + balance + ", active=" + active + '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username, email, age, balance, active, createdAt, birthDate, workStartTime);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof User))
            return false;
        User other = (User) obj;
        return Objects.equals(id, other.id) && Objects.equals(username, other.username)
                && Objects.equals(email, other.email) && Objects.equals(age, other.age)
                && Objects.equals(balance, other.balance) && Objects.equals(active, other.active)
                && Objects.equals(createdAt, other.createdAt) && Objects.equals(birthDate, other.birthDate)
                && Objects.equals(workStartTime, other.workStartTime);
    }
}
