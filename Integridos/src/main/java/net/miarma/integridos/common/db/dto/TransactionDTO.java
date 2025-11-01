package net.miarma.integridos.common.db.dto;

public class TransactionDTO {
    private String fromUserName;
    private String toUserName;
    private double amount;
    private String hmac;
    private String nonce;

    public TransactionDTO() {}

    public TransactionDTO(String fromUserName, String toUserName, double amount, String nonce) {
        this.fromUserName = fromUserName;
        this.toUserName = toUserName;
        this.amount = amount;
        this.nonce = nonce;
    }

    public String getFromUserName() {
        return fromUserName;
    }

    public void setFromUserName(String fromUserName) {
        this.fromUserName = fromUserName;
    }

    public String getToUserName() {
        return toUserName;
    }

    public void setToUserName(String toUserName) {
        this.toUserName = toUserName;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getHmac() {
        return hmac;
    }

    public void setHmac(String hmac) {
        this.hmac = hmac;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }
}