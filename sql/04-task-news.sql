USE `icinfo_task_management`;

CREATE TABLE IF NOT EXISTS `task_news` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `task_id` BIGINT NOT NULL,
    `news_id` BIGINT NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task_news_task_id_news_id` (`task_id`, `news_id`),
    KEY `idx_task_news_task_id` (`task_id`),
    KEY `idx_task_news_news_id` (`news_id`),
    CONSTRAINT `fk_task_news_task_id` FOREIGN KEY (`task_id`) REFERENCES `tasks` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_task_news_news_id` FOREIGN KEY (`news_id`) REFERENCES `news_items` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
