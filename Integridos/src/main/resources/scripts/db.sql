USE pai1;

-- Cleanup
DROP TABLE IF EXISTS sessions;
DROP TABLE IF EXISTS transactions;
DROP TABLE IF EXISTS users;
-- END Cleanup

-- DDL
CREATE TABLE users (
    userId UUID NOT NULL,
    userName VARCHAR(64) NOT NULL UNIQUE,
    password VARCHAR(256) NOT NULL,
    balance DECIMAL(12,2) NOT NULL DEFAULT 0,
    PRIMARY KEY (userId)
);

CREATE TABLE transactions (
    transactionId UUID NOT NULL,
    fromUser UUID NOT NULL,
    toUser UUID NOT NULL,
    transactionAmount DECIMAL(12,2) NOT NULL,
    nonce TEXT NOT NULL,
    PRIMARY KEY (transactionId),
    FOREIGN KEY (fromUser) REFERENCES users (userId) ON DELETE CASCADE,
    FOREIGN KEY (toUser) REFERENCES users (userId) ON DELETE CASCADE
);

CREATE TABLE sessions (
    sessionId UUID NOT NULL,
    userId UUID NOT NULL,
    createdAt TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expiresAt TIMESTAMP AS (createdAt + INTERVAL 15 MINUTE) STORED,
    PRIMARY KEY (sessionId),
    FOREIGN KEY (userId) REFERENCES users(userId) ON DELETE CASCADE
);

CREATE EVENT IF NOT EXISTS clear_expired_sessions
ON SCHEDULE EVERY 1 MINUTE
DO
DELETE FROM sessions WHERE expiresAt <= NOW();

CREATE OR REPLACE TRIGGER tr_subtract_amount
AFTER INSERT ON transactions
FOR EACH ROW
BEGIN
UPDATE users
SET balance = balance - NEW.transactionAmount
WHERE userId = NEW.fromUser;
END;
-- END DDL

-- DML
INSERT INTO users (userId, userName, password, balance)
VALUES
    (UUID(), 'pepe', 'pass123', 100.50),
    (UUID(), 'lola', 'secreto', 250.00);

INSERT INTO transactions (transactionId, fromUser, toUser, transactionAmount)
VALUES (
           UUID(),
           (SELECT userId FROM users WHERE userName = 'pepe'),
           (SELECT userId FROM users WHERE userName = 'lola'),
           25.75
       );

INSERT INTO sessions (sessionId, userId)
VALUES (
           UUID(),
           (SELECT userId FROM users WHERE userName = 'pepe')
       );
-- END DML

-- DQL
SELECT * FROM sessions;

-- END DQL