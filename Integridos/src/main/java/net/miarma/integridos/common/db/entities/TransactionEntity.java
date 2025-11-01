package net.miarma.integridos.common.db.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "transactions")
public class TransactionEntity {
    @Id
    @Column(name = "transactionId", nullable = false, updatable = false, length = 36)
    private String transactionId;

    @ManyToOne
    @JoinColumn(name = "fromUser", referencedColumnName = "userId", nullable = false, updatable = false)
    private UserEntity fromUser;

    @ManyToOne
    @JoinColumn(name = "toUser", referencedColumnName = "userId", nullable = false, updatable = false)
    private UserEntity toUser;

    @Column(name = "transactionAmount", nullable = false, updatable = false)
    private Double transactionAmount;

    @Column(name = "nonce", nullable = false, updatable = false)
    private String nonce;

    public TransactionEntity() {}

    public TransactionEntity(String transactionId, UserEntity fromUser, UserEntity toUser, Double transactionAmount) {
        this.transactionId = transactionId;
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.transactionAmount = transactionAmount;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public UserEntity getFromUser() {
        return fromUser;
    }

    public void setFromUser(UserEntity fromUser) {
        this.fromUser = fromUser;
    }

    public UserEntity getToUser() {
        return toUser;
    }

    public void setToUser(UserEntity toUser) {
        this.toUser = toUser;
    }

    public Double getTransactionAmount() {
        return transactionAmount;
    }

    public void setTransactionAmount(Double transactionAmount) {
        this.transactionAmount = transactionAmount;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    @Override
    public String toString() {
        return "TransactionEntity{" +
                "transactionId='" + transactionId + '\'' +
                ", fromUser=" + fromUser +
                ", toUser=" + toUser +
                ", transactionAmount=" + transactionAmount +
                ", nonce=" + nonce +
                '}';
    }
}