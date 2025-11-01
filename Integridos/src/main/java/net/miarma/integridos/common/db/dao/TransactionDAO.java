package net.miarma.integridos.common.db.dao;

import jakarta.persistence.EntityManager;

public class TransactionDAO {
    public boolean isNonceUsed(EntityManager em, String nonce) {
        Long count = em.createQuery(
                "SELECT COUNT(t) FROM TransactionEntity t WHERE t.nonce = :nonce", Long.class)
                .setParameter("nonce", nonce)
                .getSingleResult();
        return count > 0;
    }
}
