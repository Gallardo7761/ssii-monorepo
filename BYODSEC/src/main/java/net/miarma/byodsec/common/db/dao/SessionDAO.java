package net.miarma.byodsec.common.db.dao;

import jakarta.persistence.EntityManager;
import net.miarma.byodsec.common.db.entities.SessionEntity;

public class SessionDAO {
    public SessionEntity getByUserId(EntityManager em, String userId) {
        return em.createQuery("SELECT s FROM SessionEntity s WHERE s.userId = :userId", SessionEntity.class)
                .setParameter("userId", userId)
                .getSingleResult();
    }

    public boolean isLoggedIn(EntityManager em, String userId) {
        Long count = em.createQuery(
                        "SELECT COUNT(s) FROM SessionEntity s WHERE s.userId = :userId", Long.class)
                .setParameter("userId", userId)
                .getSingleResult();
        return count > 0;
    }
}
