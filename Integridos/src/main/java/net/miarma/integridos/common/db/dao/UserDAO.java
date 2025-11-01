package net.miarma.integridos.common.db.dao;

import jakarta.persistence.EntityManager;
import net.miarma.integridos.common.db.entities.UserEntity;

public class UserDAO {
    public UserEntity getByUserName(EntityManager em, String userName) {
        try {
            return em.createQuery("SELECT u FROM UserEntity u WHERE u.userName = :userName", UserEntity.class)
                    .setParameter("userName", userName)
                    .getSingleResult();
        } catch (jakarta.persistence.NoResultException e) {
            return null; // No se encontró, devolvemos null
        }
    }
}
