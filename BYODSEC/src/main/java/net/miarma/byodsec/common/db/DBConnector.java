package net.miarma.byodsec.common.db;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public class DBConnector {
    private static DBConnector instance;
    private EntityManagerFactory emf;
    private final ThreadLocal<EntityManager> threadLocal = new ThreadLocal<>();

    private DBConnector() {
        this.emf = Persistence.createEntityManagerFactory("ssii-pai2");
    }

    public static DBConnector getInstance() {
        if (instance == null) {
            instance = new DBConnector();
        }
        return instance;
    }

    public EntityManager getEntityManager() {
        EntityManager em = threadLocal.get();
        if (em == null || !em.isOpen()) {
            em = emf.createEntityManager();
            threadLocal.set(em);
        }
        return em;
    }

    public void close() {
        EntityManager em = threadLocal.get();
        if (em != null && em.isOpen()) {
            em.close();
        }
        threadLocal.remove();
    }
}
