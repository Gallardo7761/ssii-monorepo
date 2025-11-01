package net.miarma.integridos.common.db.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class UserEntity {
    @Id
    @Column(name = "userId", nullable = false, updatable = false, length = 36)
    private String userId;

    @Column(name = "userName", unique = true,  nullable = false, length = 64)
    private String userName;

    @Column(name = "password", nullable = false, length = 256)
    private String password;

    @Column(name = "balance", nullable = false)
    private Double balance = 0.00;

    public UserEntity() {}

    public UserEntity(String userId, String userName, String password) {
        this.userId = userId;
        this.userName = userName;
        this.password = password;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Double getBalance() {
        return balance;
    }

    public void setBalance(Double balance) {
        this.balance = balance;
    }
}