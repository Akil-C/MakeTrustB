-- ============================================================
-- MarketTrust Marketplace - V1 Initial Schema
-- Flyway Migration: V1__init_schema.sql
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 1. ROLES
-- ============================================================
CREATE TABLE IF NOT EXISTS roles (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       ENUM('ROLE_BUYER','ROLE_SELLER','ROLE_ADMIN') NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 2. USERS
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    name              VARCHAR(150)     NOT NULL,
    email             VARCHAR(255)     NOT NULL UNIQUE,
    phone             VARCHAR(20)      UNIQUE,
    password_hash     VARCHAR(255)     NOT NULL,
    status            ENUM('ACTIVE','PENDING_VERIFICATION','SUSPENDED','BANNED','LOCKED')
                                       NOT NULL DEFAULT 'PENDING_VERIFICATION',
    email_verified    BOOLEAN          NOT NULL DEFAULT FALSE,
    phone_verified    BOOLEAN          NOT NULL DEFAULT FALSE,
    profile_image_url VARCHAR(500),
    created_at        TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_users_email      ON users (email);
CREATE INDEX idx_users_phone      ON users (phone);
CREATE INDEX idx_users_status     ON users (status);
CREATE INDEX idx_users_created_at ON users (created_at);

-- ============================================================
-- 3. USER_ROLES
-- ============================================================
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 4. REFRESH_TOKENS
-- ============================================================
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    token      VARCHAR(512)  NOT NULL UNIQUE,
    user_id    BIGINT        NOT NULL,
    expires_at TIMESTAMP     NOT NULL,
    revoked    BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);

-- ============================================================
-- 5. SELLER_PROFILES
-- ============================================================
CREATE TABLE IF NOT EXISTS seller_profiles (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id             BIGINT         NOT NULL UNIQUE,
    display_name        VARCHAR(150),
    bio                 TEXT,
    city                VARCHAR(100),
    state               VARCHAR(100),
    country             VARCHAR(100),
    latitude            DECIMAL(10,8),
    longitude           DECIMAL(11,8),
    profile_image_url   VARCHAR(500),
    kyc_status          ENUM('NONE','PENDING','UNDER_REVIEW','VERIFIED','REJECTED','REQUIRES_RESUBMISSION')
                                        NOT NULL DEFAULT 'NONE',
    trust_score         DECIMAL(5,2)   NOT NULL DEFAULT 0.00,
    seller_level        ENUM('NEW','BRONZE','SILVER','GOLD','PLATINUM','ELITE')
                                        NOT NULL DEFAULT 'NEW',
    response_rate       DECIMAL(5,2)   NOT NULL DEFAULT 0.00,
    cancellation_rate   DECIMAL(5,2)   NOT NULL DEFAULT 0.00,
    completed_orders    INT            NOT NULL DEFAULT 0,
    total_sales         INT            NOT NULL DEFAULT 0,
    average_rating      DECIMAL(3,2)   NOT NULL DEFAULT 0.00,
    review_count        INT            NOT NULL DEFAULT 0,
    is_verified         BOOLEAN        NOT NULL DEFAULT FALSE,
    seller_since        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at          TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_seller_profiles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 6. SELLER_KYC
-- ============================================================
CREATE TABLE IF NOT EXISTS seller_kyc (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    seller_id           BIGINT         NOT NULL UNIQUE,
    id_type             ENUM('AADHAAR','PAN','PASSPORT','DRIVING_LICENSE','VOTER_ID'),
    id_number           VARCHAR(100),
    id_document_url     VARCHAR(500),
    selfie_url          VARCHAR(500),
    address_line1       VARCHAR(255),
    address_line2       VARCHAR(255),
    city                VARCHAR(100),
    state               VARCHAR(100),
    pincode             VARCHAR(20),
    declaration_accepted BOOLEAN,
    status              ENUM('PENDING','UNDER_REVIEW','VERIFIED','REJECTED','REQUIRES_RESUBMISSION')
                                        NOT NULL DEFAULT 'PENDING',
    rejection_reason    TEXT,
    admin_notes         TEXT,
    reviewed_by         BIGINT,
    reviewed_at         TIMESTAMP,
    submitted_at        TIMESTAMP,
    created_at          TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_seller_kyc_seller     FOREIGN KEY (seller_id)    REFERENCES seller_profiles (id) ON DELETE CASCADE,
    CONSTRAINT fk_seller_kyc_reviewer   FOREIGN KEY (reviewed_by)  REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 7. BUYER_PROFILES
-- ============================================================
CREATE TABLE IF NOT EXISTS buyer_profiles (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT        NOT NULL UNIQUE,
    display_name    VARCHAR(150),
    city            VARCHAR(100),
    state           VARCHAR(100),
    latitude        DECIMAL(10,8),
    longitude       DECIMAL(11,8),
    buyer_level     ENUM('NEW_BUYER','REGULAR_BUYER','TRUSTED_BUYER','TOP_BUYER')
                                   NOT NULL DEFAULT 'NEW_BUYER',
    total_purchases INT            NOT NULL DEFAULT 0,
    total_spent     BIGINT         NOT NULL DEFAULT 0,
    created_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_buyer_profiles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 8. CATEGORIES
-- ============================================================
CREATE TABLE IF NOT EXISTS categories (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(150)  NOT NULL,
    description TEXT,
    icon_url    VARCHAR(500),
    parent_id   BIGINT,
    is_active   BOOLEAN       NOT NULL DEFAULT TRUE,
    sort_order  INT           NOT NULL DEFAULT 0,
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES categories (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_categories_parent_id ON categories (parent_id);

-- ============================================================
-- 9. PRODUCTS
-- ============================================================
CREATE TABLE IF NOT EXISTS products (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    seller_id          BIGINT         NOT NULL,
    category_id        BIGINT,
    title              VARCHAR(300)   NOT NULL,
    description        TEXT,
    price_in_credits   BIGINT         NOT NULL,
    `condition`        ENUM('NEW','LIKE_NEW','GOOD','FAIR','POOR') NOT NULL,
    brand              VARCHAR(150),
    status             ENUM('DRAFT','ACTIVE','RESERVED','SOLD','HIDDEN','REPORTED','REJECTED','EXPIRED')
                                       NOT NULL DEFAULT 'DRAFT',
    quantity           INT            NOT NULL DEFAULT 1,
    city               VARCHAR(100),
    state              VARCHAR(100),
    latitude           DECIMAL(10,8),
    longitude          DECIMAL(11,8),
    location_type      ENUM('EXACT','APPROXIMATE') NOT NULL DEFAULT 'APPROXIMATE',
    views              INT            NOT NULL DEFAULT 0,
    unique_views       INT            NOT NULL DEFAULT 0,
    wishlist_count     INT            NOT NULL DEFAULT 0,
    inquiry_count      INT            NOT NULL DEFAULT 0,
    is_featured        BOOLEAN        NOT NULL DEFAULT FALSE,
    expires_at         TIMESTAMP,
    created_at         TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_products_seller   FOREIGN KEY (seller_id)   REFERENCES users (id)       ON DELETE CASCADE,
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id)  ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_products_seller_id        ON products (seller_id);
CREATE INDEX idx_products_category_id      ON products (category_id);
CREATE INDEX idx_products_status           ON products (status);
CREATE INDEX idx_products_location         ON products (latitude, longitude);
CREATE INDEX idx_products_price_in_credits ON products (price_in_credits);
CREATE INDEX idx_products_created_at       ON products (created_at);

-- ============================================================
-- 10. PRODUCT_IMAGES
-- ============================================================
CREATE TABLE IF NOT EXISTS product_images (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT         NOT NULL,
    url        VARCHAR(500)   NOT NULL,
    public_id  VARCHAR(255),
    is_primary BOOLEAN        NOT NULL DEFAULT FALSE,
    sort_order INT            NOT NULL DEFAULT 0,
    created_at TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_images_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_product_images_product_id ON product_images (product_id);

-- ============================================================
-- 11. PRODUCT_VIEWS
-- ============================================================
CREATE TABLE IF NOT EXISTS product_views (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT        NOT NULL,
    viewer_id  BIGINT,
    ip_address VARCHAR(45),
    viewed_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_views_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
    CONSTRAINT fk_product_views_viewer  FOREIGN KEY (viewer_id)  REFERENCES users (id)    ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_product_views_product_id ON product_views (product_id);
CREATE INDEX idx_product_views_viewer_id  ON product_views (viewer_id);
CREATE INDEX idx_product_views_viewed_at  ON product_views (viewed_at);

-- ============================================================
-- 12. WISHLISTS
-- ============================================================
CREATE TABLE IF NOT EXISTS wishlists (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT    NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_wishlists_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 13. WISHLIST_ITEMS
-- ============================================================
CREATE TABLE IF NOT EXISTS wishlist_items (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    wishlist_id    BIGINT    NOT NULL,
    product_id     BIGINT    NOT NULL,
    price_at_add   BIGINT,
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_wishlist_product (wishlist_id, product_id),
    CONSTRAINT fk_wishlist_items_wishlist FOREIGN KEY (wishlist_id) REFERENCES wishlists (id) ON DELETE CASCADE,
    CONSTRAINT fk_wishlist_items_product  FOREIGN KEY (product_id)  REFERENCES products (id)  ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 14. WALLETS
-- ============================================================
CREATE TABLE IF NOT EXISTS wallets (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id           BIGINT    NOT NULL UNIQUE,
    available_credits BIGINT    NOT NULL DEFAULT 0,
    held_credits      BIGINT    NOT NULL DEFAULT 0,
    total_earned      BIGINT    NOT NULL DEFAULT 0,
    total_spent       BIGINT    NOT NULL DEFAULT 0,
    is_frozen         BOOLEAN   NOT NULL DEFAULT FALSE,
    version           BIGINT    NOT NULL DEFAULT 0,
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_wallets_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 15. WALLET_TRANSACTIONS
-- ============================================================
CREATE TABLE IF NOT EXISTS wallet_transactions (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    wallet_id        BIGINT         NOT NULL,
    type             ENUM('ADMIN_GRANT','PURCHASE_HOLD','PURCHASE_RELEASE','PLATFORM_FEE',
                          'SELLER_EARNING','REFUND','ADMIN_ADJUSTMENT','DISPUTE_REFUND','CREDIT_EXPIRY')
                                     NOT NULL,
    amount           BIGINT         NOT NULL,
    balance_after    BIGINT         NOT NULL,
    reference_id     VARCHAR(100),
    reference_type   VARCHAR(100),
    description      VARCHAR(500),
    created_by       BIGINT,
    idempotency_key  VARCHAR(100)   UNIQUE,
    created_at       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_wallet_tx_wallet     FOREIGN KEY (wallet_id)  REFERENCES wallets (id) ON DELETE CASCADE,
    CONSTRAINT fk_wallet_tx_created_by FOREIGN KEY (created_by) REFERENCES users (id)   ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_wallet_tx_wallet_id       ON wallet_transactions (wallet_id);
CREATE INDEX idx_wallet_tx_type            ON wallet_transactions (type);
CREATE INDEX idx_wallet_tx_created_at      ON wallet_transactions (created_at);
CREATE INDEX idx_wallet_tx_idempotency_key ON wallet_transactions (idempotency_key);

-- ============================================================
-- 16. CREDIT_HOLDS
-- ============================================================
CREATE TABLE IF NOT EXISTS credit_holds (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    wallet_id   BIGINT       NOT NULL,
    order_id    VARCHAR(50),
    amount      BIGINT       NOT NULL,
    status      ENUM('ACTIVE','RELEASED','REFUNDED') NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    released_at TIMESTAMP,
    CONSTRAINT fk_credit_holds_wallet FOREIGN KEY (wallet_id) REFERENCES wallets (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_credit_holds_wallet_id ON credit_holds (wallet_id);
CREATE INDEX idx_credit_holds_status    ON credit_holds (status);

-- ============================================================
-- 17. ORDERS
-- ============================================================
CREATE TABLE IF NOT EXISTS orders (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_number     VARCHAR(50)    NOT NULL UNIQUE,
    buyer_id         BIGINT         NOT NULL,
    seller_id        BIGINT         NOT NULL,
    product_id       BIGINT         NOT NULL,
    quantity         INT            NOT NULL DEFAULT 1,
    unit_price       BIGINT         NOT NULL,
    total_price      BIGINT         NOT NULL,
    platform_fee     BIGINT         NOT NULL DEFAULT 0,
    seller_amount    BIGINT         NOT NULL DEFAULT 0,
    status           ENUM('PENDING','CONFIRMED','PROCESSING','READY_FOR_SHIPPING','SHIPPED',
                          'OUT_FOR_DELIVERY','DELIVERED','BUYER_CONFIRMED','COMPLETED',
                          'CANCELLED','DISPUTED','REFUNDED')
                                     NOT NULL DEFAULT 'PENDING',
    delivery_address TEXT,
    tracking_number  VARCHAR(100),
    notes            TEXT,
    idempotency_key  VARCHAR(100)   UNIQUE,
    credit_hold_id   BIGINT,
    created_at       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_orders_buyer        FOREIGN KEY (buyer_id)       REFERENCES users (id)         ON DELETE RESTRICT,
    CONSTRAINT fk_orders_seller       FOREIGN KEY (seller_id)      REFERENCES users (id)         ON DELETE RESTRICT,
    CONSTRAINT fk_orders_product      FOREIGN KEY (product_id)     REFERENCES products (id)      ON DELETE RESTRICT,
    CONSTRAINT fk_orders_credit_hold  FOREIGN KEY (credit_hold_id) REFERENCES credit_holds (id)  ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_orders_buyer_id   ON orders (buyer_id);
CREATE INDEX idx_orders_seller_id  ON orders (seller_id);
CREATE INDEX idx_orders_product_id ON orders (product_id);
CREATE INDEX idx_orders_status     ON orders (status);
CREATE INDEX idx_orders_created_at ON orders (created_at);

-- ============================================================
-- 18. ORDER_STATUS_HISTORY
-- ============================================================
CREATE TABLE IF NOT EXISTS order_status_history (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id   BIGINT        NOT NULL,
    status     VARCHAR(50)   NOT NULL,
    notes      TEXT,
    changed_by BIGINT,
    created_at TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_history_order      FOREIGN KEY (order_id)   REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_history_changed_by FOREIGN KEY (changed_by) REFERENCES users (id)  ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_order_status_history_order_id ON order_status_history (order_id);

-- ============================================================
-- 19. REVIEWS
-- ============================================================
CREATE TABLE IF NOT EXISTS reviews (
    id                          BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id                    BIGINT   NOT NULL UNIQUE,
    product_id                  BIGINT   NOT NULL,
    buyer_id                    BIGINT   NOT NULL,
    seller_id                   BIGINT   NOT NULL,
    rating                      INT      NOT NULL CHECK (rating BETWEEN 1 AND 5),
    title                       VARCHAR(255),
    comment                     TEXT,
    product_condition_rating    INT      CHECK (product_condition_rating BETWEEN 1 AND 5),
    seller_communication_rating INT      CHECK (seller_communication_rating BETWEEN 1 AND 5),
    accuracy_rating             INT      CHECK (accuracy_rating BETWEEN 1 AND 5),
    is_verified_purchase        BOOLEAN  NOT NULL DEFAULT TRUE,
    status                      ENUM('ACTIVE','HIDDEN','REPORTED') NOT NULL DEFAULT 'ACTIVE',
    seller_reply                TEXT,
    seller_replied_at           TIMESTAMP,
    created_at                  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_reviews_order   FOREIGN KEY (order_id)   REFERENCES orders (id)   ON DELETE CASCADE,
    CONSTRAINT fk_reviews_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_buyer   FOREIGN KEY (buyer_id)   REFERENCES users (id)    ON DELETE CASCADE,
    CONSTRAINT fk_reviews_seller  FOREIGN KEY (seller_id)  REFERENCES users (id)    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_reviews_product_id ON reviews (product_id);
CREATE INDEX idx_reviews_seller_id  ON reviews (seller_id);
CREATE INDEX idx_reviews_buyer_id   ON reviews (buyer_id);

-- ============================================================
-- 20. REVIEW_IMAGES
-- ============================================================
CREATE TABLE IF NOT EXISTS review_images (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    review_id BIGINT       NOT NULL,
    url       VARCHAR(500) NOT NULL,
    public_id VARCHAR(255),
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_review_images_review FOREIGN KEY (review_id) REFERENCES reviews (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_review_images_review_id ON review_images (review_id);

-- ============================================================
-- 21. CHAT_ROOMS
-- ============================================================
CREATE TABLE IF NOT EXISTS chat_rooms (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    buyer_id         BIGINT        NOT NULL,
    seller_id        BIGINT        NOT NULL,
    product_id       BIGINT,
    last_message     TEXT,
    last_message_at  TIMESTAMP,
    buyer_unread     INT           NOT NULL DEFAULT 0,
    seller_unread    INT           NOT NULL DEFAULT 0,
    is_blocked       BOOLEAN       NOT NULL DEFAULT FALSE,
    blocked_by       BIGINT,
    created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_chat_room (buyer_id, seller_id, product_id),
    CONSTRAINT fk_chat_rooms_buyer      FOREIGN KEY (buyer_id)   REFERENCES users (id)    ON DELETE CASCADE,
    CONSTRAINT fk_chat_rooms_seller     FOREIGN KEY (seller_id)  REFERENCES users (id)    ON DELETE CASCADE,
    CONSTRAINT fk_chat_rooms_product    FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE SET NULL,
    CONSTRAINT fk_chat_rooms_blocked_by FOREIGN KEY (blocked_by) REFERENCES users (id)    ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_chat_rooms_buyer_id  ON chat_rooms (buyer_id);
CREATE INDEX idx_chat_rooms_seller_id ON chat_rooms (seller_id);

-- ============================================================
-- 22. CHAT_MESSAGES
-- ============================================================
CREATE TABLE IF NOT EXISTS chat_messages (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id      BIGINT       NOT NULL,
    sender_id    BIGINT       NOT NULL,
    content      TEXT         NOT NULL,
    message_type ENUM('TEXT','IMAGE','SYSTEM') NOT NULL DEFAULT 'TEXT',
    is_read      BOOLEAN      NOT NULL DEFAULT FALSE,
    is_flagged   BOOLEAN      NOT NULL DEFAULT FALSE,
    flag_reason  VARCHAR(255),
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chat_messages_room   FOREIGN KEY (room_id)   REFERENCES chat_rooms (id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_messages_sender FOREIGN KEY (sender_id) REFERENCES users (id)      ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_chat_messages_room_id   ON chat_messages (room_id);
CREATE INDEX idx_chat_messages_sender_id ON chat_messages (sender_id);
CREATE INDEX idx_chat_messages_created_at ON chat_messages (created_at);

-- ============================================================
-- 23. NOTIFICATIONS
-- ============================================================
CREATE TABLE IF NOT EXISTS notifications (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id        BIGINT        NOT NULL,
    type           VARCHAR(50)   NOT NULL,
    title          VARCHAR(255)  NOT NULL,
    message        TEXT,
    reference_id   VARCHAR(100),
    reference_type VARCHAR(100),
    is_read        BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_notifications_user_id  ON notifications (user_id);
CREATE INDEX idx_notifications_is_read  ON notifications (is_read);
CREATE INDEX idx_notifications_created_at ON notifications (created_at);

-- ============================================================
-- 24. REPORTS
-- ============================================================
CREATE TABLE IF NOT EXISTS reports (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    reporter_id          BIGINT   NOT NULL,
    reported_user_id     BIGINT,
    reported_product_id  BIGINT,
    reported_review_id   BIGINT,
    reported_message_id  BIGINT,
    reason               ENUM('SCAM','FAKE_PRODUCT','WRONG_DESCRIPTION','INAPPROPRIATE_CONTENT',
                               'SPAM','HARASSMENT','OFF_PLATFORM_PAYMENT','SUSPICIOUS_BEHAVIOR','OTHER')
                                   NOT NULL,
    description          TEXT,
    status               ENUM('OPEN','UNDER_REVIEW','RESOLVED','REJECTED') NOT NULL DEFAULT 'OPEN',
    priority             ENUM('LOW','MEDIUM','HIGH')                       NOT NULL DEFAULT 'LOW',
    ai_risk_score        INT       NOT NULL DEFAULT 0,
    resolved_by          BIGINT,
    resolution_notes     TEXT,
    resolved_at          TIMESTAMP,
    created_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_reports_reporter         FOREIGN KEY (reporter_id)         REFERENCES users (id)          ON DELETE CASCADE,
    CONSTRAINT fk_reports_reported_user    FOREIGN KEY (reported_user_id)    REFERENCES users (id)          ON DELETE SET NULL,
    CONSTRAINT fk_reports_reported_product FOREIGN KEY (reported_product_id) REFERENCES products (id)       ON DELETE SET NULL,
    CONSTRAINT fk_reports_reported_review  FOREIGN KEY (reported_review_id)  REFERENCES reviews (id)        ON DELETE SET NULL,
    CONSTRAINT fk_reports_reported_message FOREIGN KEY (reported_message_id) REFERENCES chat_messages (id)  ON DELETE SET NULL,
    CONSTRAINT fk_reports_resolved_by      FOREIGN KEY (resolved_by)         REFERENCES users (id)          ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_reports_reporter_id      ON reports (reporter_id);
CREATE INDEX idx_reports_reported_user_id ON reports (reported_user_id);
CREATE INDEX idx_reports_status           ON reports (status);
CREATE INDEX idx_reports_priority         ON reports (priority);

-- ============================================================
-- 25. DISPUTES
-- ============================================================
CREATE TABLE IF NOT EXISTS disputes (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id        BIGINT   NOT NULL UNIQUE,
    buyer_id        BIGINT   NOT NULL,
    seller_id       BIGINT   NOT NULL,
    reason          ENUM('NOT_RECEIVED','WRONG_PRODUCT','DAMAGED','FAKE_PRODUCT',
                          'DESCRIPTION_MISMATCH','OTHER') NOT NULL,
    description     TEXT,
    status          ENUM('OPEN','SELLER_RESPONDED','UNDER_REVIEW','RESOLVED','ESCALATED')
                                NOT NULL DEFAULT 'OPEN',
    outcome         ENUM('BUYER_FAVORED','SELLER_FAVORED','PARTIAL_REFUND'),
    refund_amount   BIGINT   NOT NULL DEFAULT 0,
    buyer_evidence  TEXT,
    seller_evidence TEXT,
    admin_notes     TEXT,
    resolved_by     BIGINT,
    resolved_at     TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_disputes_order       FOREIGN KEY (order_id)    REFERENCES orders (id) ON DELETE RESTRICT,
    CONSTRAINT fk_disputes_buyer       FOREIGN KEY (buyer_id)    REFERENCES users (id)  ON DELETE RESTRICT,
    CONSTRAINT fk_disputes_seller      FOREIGN KEY (seller_id)   REFERENCES users (id)  ON DELETE RESTRICT,
    CONSTRAINT fk_disputes_resolved_by FOREIGN KEY (resolved_by) REFERENCES users (id)  ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_disputes_order_id  ON disputes (order_id);
CREATE INDEX idx_disputes_buyer_id  ON disputes (buyer_id);
CREATE INDEX idx_disputes_seller_id ON disputes (seller_id);
CREATE INDEX idx_disputes_status    ON disputes (status);

-- ============================================================
-- 26. SELLER_TRUST_SCORES
-- ============================================================
CREATE TABLE IF NOT EXISTS seller_trust_scores (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    seller_id            BIGINT         NOT NULL UNIQUE,
    total_score          DECIMAL(5,2)   NOT NULL DEFAULT 0.00,
    kyc_points           INT            NOT NULL DEFAULT 0,
    order_points         INT            NOT NULL DEFAULT 0,
    review_points        INT            NOT NULL DEFAULT 0,
    penalty_points       INT            NOT NULL DEFAULT 0,
    cancellation_penalty INT            NOT NULL DEFAULT 0,
    dispute_penalty      INT            NOT NULL DEFAULT 0,
    report_penalty       INT            NOT NULL DEFAULT 0,
    ai_risk_penalty      INT            NOT NULL DEFAULT 0,
    last_calculated      TIMESTAMP,
    created_at           TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_seller_trust_scores_seller FOREIGN KEY (seller_id) REFERENCES seller_profiles (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 27. SELLER_ANALYTICS
-- ============================================================
CREATE TABLE IF NOT EXISTS seller_analytics (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    seller_id         BIGINT   NOT NULL,
    date              DATE     NOT NULL,
    views             INT      NOT NULL DEFAULT 0,
    unique_views      INT      NOT NULL DEFAULT 0,
    wishlist_adds     INT      NOT NULL DEFAULT 0,
    inquiries         INT      NOT NULL DEFAULT 0,
    orders            INT      NOT NULL DEFAULT 0,
    completed_orders  INT      NOT NULL DEFAULT 0,
    cancelled_orders  INT      NOT NULL DEFAULT 0,
    revenue_credits   BIGINT   NOT NULL DEFAULT 0,
    UNIQUE KEY uq_seller_analytics_seller_date (seller_id, date),
    CONSTRAINT fk_seller_analytics_seller FOREIGN KEY (seller_id) REFERENCES seller_profiles (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_seller_analytics_seller_id ON seller_analytics (seller_id);
CREATE INDEX idx_seller_analytics_date      ON seller_analytics (date);

-- ============================================================
-- 28. ADVERTISEMENTS
-- ============================================================
CREATE TABLE IF NOT EXISTS advertisements (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    title              VARCHAR(255)  NOT NULL,
    description        TEXT,
    image_url          VARCHAR(500),
    target_url         VARCHAR(500),
    target_category_id BIGINT,
    target_city        VARCHAR(100),
    target_state       VARCHAR(100),
    start_date         TIMESTAMP,
    end_date           TIMESTAMP,
    priority           INT           NOT NULL DEFAULT 1,
    status             ENUM('DRAFT','SCHEDULED','ACTIVE','PAUSED','EXPIRED') NOT NULL DEFAULT 'DRAFT',
    placement          ENUM('HOMEPAGE_BANNER','CATEGORY_BANNER','SPONSORED_PRODUCT',
                             'SEARCH_RESULT','LOCATION_SPECIFIC')           NOT NULL DEFAULT 'HOMEPAGE_BANNER',
    impressions        BIGINT        NOT NULL DEFAULT 0,
    clicks             BIGINT        NOT NULL DEFAULT 0,
    created_by         BIGINT,
    created_at         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_ads_category   FOREIGN KEY (target_category_id) REFERENCES categories (id) ON DELETE SET NULL,
    CONSTRAINT fk_ads_created_by FOREIGN KEY (created_by)         REFERENCES users (id)      ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_advertisements_status   ON advertisements (status);
CREATE INDEX idx_advertisements_placement ON advertisements (placement);

-- ============================================================
-- 29. AD_IMPRESSIONS
-- ============================================================
CREATE TABLE IF NOT EXISTS ad_impressions (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    ad_id        BIGINT       NOT NULL,
    user_id      BIGINT,
    ip_address   VARCHAR(45),
    page_context VARCHAR(255),
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ad_impressions_ad   FOREIGN KEY (ad_id)   REFERENCES advertisements (id) ON DELETE CASCADE,
    CONSTRAINT fk_ad_impressions_user FOREIGN KEY (user_id) REFERENCES users (id)          ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_ad_impressions_ad_id ON ad_impressions (ad_id);

-- ============================================================
-- 30. AD_CLICKS
-- ============================================================
CREATE TABLE IF NOT EXISTS ad_clicks (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    ad_id      BIGINT    NOT NULL,
    user_id    BIGINT,
    ip_address VARCHAR(45),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ad_clicks_ad   FOREIGN KEY (ad_id)   REFERENCES advertisements (id) ON DELETE CASCADE,
    CONSTRAINT fk_ad_clicks_user FOREIGN KEY (user_id) REFERENCES users (id)          ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_ad_clicks_ad_id ON ad_clicks (ad_id);

-- ============================================================
-- 31. ADMIN_ACTIONS
-- ============================================================
CREATE TABLE IF NOT EXISTS admin_actions (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    admin_id    BIGINT        NOT NULL,
    action_type VARCHAR(100)  NOT NULL,
    target_type VARCHAR(50),
    target_id   BIGINT,
    reason      TEXT,
    ip_address  VARCHAR(45),
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_admin_actions_admin FOREIGN KEY (admin_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_admin_actions_admin_id    ON admin_actions (admin_id);
CREATE INDEX idx_admin_actions_target_type ON admin_actions (target_type, target_id);
CREATE INDEX idx_admin_actions_created_at  ON admin_actions (created_at);

-- ============================================================
-- 32. AUDIT_LOGS
-- ============================================================
CREATE TABLE IF NOT EXISTS audit_logs (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_id    BIGINT,
    action      VARCHAR(100)  NOT NULL,
    entity_type VARCHAR(50)   NOT NULL,
    entity_id   BIGINT,
    old_value   TEXT,
    new_value   TEXT,
    ip_address  VARCHAR(45),
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_logs_actor FOREIGN KEY (actor_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_audit_logs_actor_id   ON audit_logs (actor_id);
CREATE INDEX idx_audit_logs_entity     ON audit_logs (entity_type, entity_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs (created_at);

-- ============================================================
-- 33. AI_INSIGHTS
-- ============================================================
CREATE TABLE IF NOT EXISTS ai_insights (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    entity_type        VARCHAR(50)   NOT NULL,
    entity_id          BIGINT        NOT NULL,
    insight_type       VARCHAR(50)   NOT NULL,
    summary            TEXT,
    strengths          JSON,
    weaknesses         JSON,
    recommendations    JSON,
    pricing_suggestion TEXT,
    risk_level         ENUM('LOW','MEDIUM','HIGH') NOT NULL DEFAULT 'LOW',
    trend              ENUM('UP','DOWN','STABLE')  NOT NULL DEFAULT 'STABLE',
    raw_response       TEXT,
    generated_at       TIMESTAMP,
    created_at         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_ai_insights_entity      ON ai_insights (entity_type, entity_id);
CREATE INDEX idx_ai_insights_insight_type ON ai_insights (insight_type);
CREATE INDEX idx_ai_insights_generated_at ON ai_insights (generated_at);

-- ============================================================
-- 34. AI_RECOMMENDATIONS
-- ============================================================
CREATE TABLE IF NOT EXISTS ai_recommendations (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    seller_id    BIGINT       NOT NULL,
    product_id   BIGINT,
    type         VARCHAR(50)  NOT NULL,
    title        VARCHAR(255) NOT NULL,
    description  TEXT,
    priority     ENUM('LOW','MEDIUM','HIGH') NOT NULL DEFAULT 'MEDIUM',
    is_dismissed BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ai_recs_seller  FOREIGN KEY (seller_id)  REFERENCES seller_profiles (id) ON DELETE CASCADE,
    CONSTRAINT fk_ai_recs_product FOREIGN KEY (product_id) REFERENCES products (id)        ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_ai_recommendations_seller_id  ON ai_recommendations (seller_id);
CREATE INDEX idx_ai_recommendations_product_id ON ai_recommendations (product_id);

-- ============================================================
-- 35. SEARCH_HISTORY
-- ============================================================
CREATE TABLE IF NOT EXISTS search_history (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT,
    keyword     VARCHAR(255),
    category_id BIGINT,
    city        VARCHAR(100),
    min_price   BIGINT,
    max_price   BIGINT,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_search_history_user     FOREIGN KEY (user_id)     REFERENCES users (id)      ON DELETE SET NULL,
    CONSTRAINT fk_search_history_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_search_history_user_id    ON search_history (user_id);
CREATE INDEX idx_search_history_created_at ON search_history (created_at);
CREATE INDEX idx_search_history_keyword    ON search_history (keyword);

-- ============================================================
-- 36. PLATFORM_SETTINGS
-- ============================================================
CREATE TABLE IF NOT EXISTS platform_settings (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    setting_key   VARCHAR(100)  NOT NULL UNIQUE,
    setting_value TEXT,
    description   VARCHAR(500),
    updated_by    BIGINT,
    updated_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_platform_settings_updated_by FOREIGN KEY (updated_by) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- SEED DATA
-- ============================================================

-- Roles
INSERT INTO roles (name, created_at) VALUES
    ('ROLE_BUYER',  NOW()),
    ('ROLE_SELLER', NOW()),
    ('ROLE_ADMIN',  NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- Categories (top-level)
INSERT INTO categories (name, description, icon_url, parent_id, is_active, sort_order, created_at, updated_at) VALUES
    ('Electronics',           'Phones, laptops, tablets, gadgets and accessories',       NULL, NULL, TRUE,  1,  NOW(), NOW()),
    ('Vehicles',              'Cars, bikes, scooters and other vehicles',                NULL, NULL, TRUE,  2,  NOW(), NOW()),
    ('Furniture',             'Home and office furniture, decor',                        NULL, NULL, TRUE,  3,  NOW(), NOW()),
    ('Fashion',               'Clothing, footwear, accessories and jewellery',           NULL, NULL, TRUE,  4,  NOW(), NOW()),
    ('Books',                 'Books, magazines, comics and educational material',       NULL, NULL, TRUE,  5,  NOW(), NOW()),
    ('Sports',                'Sports equipment, fitness gear and outdoor accessories',  NULL, NULL, TRUE,  6,  NOW(), NOW()),
    ('Home Appliances',       'Refrigerators, washing machines, ACs and more',          NULL, NULL, TRUE,  7,  NOW(), NOW()),
    ('Musical Instruments',   'Guitars, keyboards, drums and studio equipment',         NULL, NULL, TRUE,  8,  NOW(), NOW()),
    ('Collectibles',          'Antiques, coins, stamps, art and rare items',            NULL, NULL, TRUE,  9,  NOW(), NOW()),
    ('Real Estate',           'Properties, land, commercial and rental listings',       NULL, NULL, TRUE,  10, NOW(), NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- Platform Settings
INSERT INTO platform_settings (setting_key, setting_value, description, updated_by, updated_at) VALUES
    ('platform_fee_credits',               '2',    'Platform fee deducted per transaction (in credits)',         NULL, NOW()),
    ('max_product_images',                 '8',    'Maximum number of images allowed per product listing',       NULL, NOW()),
    ('max_listing_days',                   '90',   'Number of days a product listing remains active',           NULL, NOW()),
    ('seller_level_bronze_threshold',      '100',  'Minimum trust score to reach BRONZE seller level',          NULL, NOW()),
    ('seller_level_silver_threshold',      '300',  'Minimum trust score to reach SILVER seller level',          NULL, NOW()),
    ('seller_level_gold_threshold',        '600',  'Minimum trust score to reach GOLD seller level',            NULL, NOW()),
    ('seller_level_platinum_threshold',    '1000', 'Minimum trust score to reach PLATINUM seller level',        NULL, NOW()),
    ('seller_level_elite_threshold',       '2000', 'Minimum trust score to reach ELITE seller level',           NULL, NOW()),
    ('trust_score_kyc_points',             '20',   'Trust score points awarded upon KYC verification',          NULL, NOW()),
    ('trust_score_per_order',              '5',    'Trust score points awarded per completed order',            NULL, NOW()),
    ('trust_score_per_review',             '3',    'Trust score points awarded per positive review received',   NULL, NOW())
ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value), description = VALUES(description);
