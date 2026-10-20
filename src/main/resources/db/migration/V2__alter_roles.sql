-- ============================================================
-- MarketTrust Marketplace - V2 Migration: Alter roles column
-- Allows both prefixed ('ROLE_ADMIN') and non-prefixed ('ADMIN') values
-- ============================================================

ALTER TABLE roles MODIFY COLUMN name VARCHAR(50) NOT NULL;

INSERT IGNORE INTO roles (name, created_at) VALUES
    ('BUYER', NOW()),
    ('SELLER', NOW()),
    ('ADMIN', NOW()),
    ('ROLE_BUYER', NOW()),
    ('ROLE_SELLER', NOW()),
    ('ROLE_ADMIN', NOW());
