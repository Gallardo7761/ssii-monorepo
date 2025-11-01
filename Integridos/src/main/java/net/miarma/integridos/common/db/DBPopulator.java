package net.miarma.integridos.common.db;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import net.miarma.integridos.common.security.PasswordHasher;
import net.miarma.integridos.common.db.entities.UserEntity;

import java.util.UUID;

public class DBPopulator {
    private final DBConnector conn = DBConnector.getInstance();
    private EntityManager em;
    private static DBPopulator instance;

    private DBPopulator() {
        em = conn.createEntityManager();
    }

    public static DBPopulator getInstance() {
        if (instance == null) {
            instance = new DBPopulator();
        }
        return instance;
    }

    public boolean isFirstRun() {
        TypedQuery<Long> q = em.createQuery("SELECT COUNT(u) FROM UserEntity u", Long.class);
        Long count = q.getSingleResult();
        return count == 0;
    }

    public void populate() {
        UserEntity u1 = new UserEntity(UUID.randomUUID().toString(),
                "ricolinad", PasswordHasher.hash("ricardo2025"));
        UserEntity u2 = new UserEntity(UUID.randomUUID().toString(),
                "alvgulveg", PasswordHasher.hash("alcalde_dimision"));
        UserEntity u3 = new UserEntity(UUID.randomUUID().toString(),
                "smt4497", PasswordHasher.hash("quemenlaus"));
        try {
            em.getTransaction().begin();
            em.persist(u1);
            em.persist(u2);
            em.persist(u3);
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
        } finally {
            em.close();
        }
    }
}
