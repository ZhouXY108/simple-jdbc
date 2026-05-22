/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.zhouxy.jdbc.test;

import java.time.LocalDateTime;
import java.util.Objects;

public class AccountPO {
    Long id;
    String username;
    String accountStatus;
    LocalDateTime createTime;
    Long createdBy;
    LocalDateTime updateTime;
    Long updatedBy;
    Long version;

    public AccountPO() {
    }

    public AccountPO(Long id, String username, String accountStatus,
            Long createdBy, Long updatedBy) {
        this.id = id;
        this.username = username;
        this.accountStatus = accountStatus;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public AccountPO(Long id, String username, String accountStatus,
            LocalDateTime createTime, Long createdBy,
            LocalDateTime updateTime, Long updatedBy,
            Long version) {
        this.id = id;
        this.username = username;
        this.accountStatus = accountStatus;
        this.createTime = createTime;
        this.createdBy = createdBy;
        this.updateTime = updateTime;
        this.updatedBy = updatedBy;
        this.version = version;
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

    public String getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(String accountStatus) {
        this.accountStatus = accountStatus;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username, accountStatus, createTime, createdBy, updateTime, updatedBy, version);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AccountPO other = (AccountPO) obj;
        return Objects.equals(id, other.id) && Objects.equals(username, other.username)
                && Objects.equals(accountStatus, other.accountStatus) && Objects.equals(createTime, other.createTime)
                && Objects.equals(createdBy, other.createdBy) && Objects.equals(updateTime, other.updateTime)
                && Objects.equals(updatedBy, other.updatedBy) && Objects.equals(version, other.version);
    }

    @Override
    public String toString() {
        return "AccountPO [id=" + id + ", username=" + username + ", accountStatus=" + accountStatus + ", createTime="
                + createTime + ", createdBy=" + createdBy + ", updateTime=" + updateTime + ", updatedBy=" + updatedBy
                + ", version=" + version + "]";
    }
}
