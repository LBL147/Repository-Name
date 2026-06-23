USE `icinfo_task_management`;

CREATE TABLE IF NOT EXISTS `news_items` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `title` VARCHAR(512) NOT NULL,
    `url` TEXT NOT NULL,
    `url_hash` CHAR(64) NOT NULL,
    `source` VARCHAR(128) NOT NULL,
    `keyword` VARCHAR(128) NOT NULL,
    `published_at` DATETIME NULL,
    `fetched_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_news_items_url_hash` (`url_hash`),
    KEY `idx_news_items_keyword` (`keyword`),
    KEY `idx_news_items_published_at` (`published_at`),
    KEY `idx_news_items_fetched_at` (`fetched_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
