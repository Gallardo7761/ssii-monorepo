package net.miarma.integridos.common.db;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public class DBConnector {
    private static DBConnector instance;
    private EntityManagerFactory emf;

    private DBConnector() {
        this.emf = Persistence.createEntityManagerFactory("ssii-pai1");
    }

    public static DBConnector getInstance() {
        if (instance == null) {
            instance = new DBConnector();
        }
        return instance;
    }

    public EntityManager createEntityManager() {
        return emf.createEntityManager();
    }

    public void close() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }
}
