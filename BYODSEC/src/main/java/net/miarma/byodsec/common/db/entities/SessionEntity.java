package net.miarma.byodsec.common.db.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "sessions")
public class SessionEntity {
    @Id
    @Column(name = "sessionId", nullable = false, updatable = false)
    private String sessionId;

    @Column(name = "userId", nullable = false)
    private String userId;

    @Column(name = "createdAt", nullable = false, updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "expiresAt", nullable = false, updatable = false, insertable = false)
    private LocalDateTime expiresAt;

    public SessionEntity() {}

    public SessionEntity(String sessionId, String userId, LocalDateTime createdAt, LocalDateTime expiresAt) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}