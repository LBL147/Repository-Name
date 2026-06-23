USE `icinfo_task_management`;

CREATE TABLE IF NOT EXISTS `users` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `username` VARCHAR(64) NOT NULL,
    `password` VARCHAR(128) NOT NULL,
    `display_name` VARCHAR(64) NOT NULL,
    `role` VARCHAR(16) NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_users_username` (`username`),
    KEY `idx_users_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `users` (`username`, `password`, `display_name`, `role`)
VALUES
    ('mentor_mock', '236977126d6375b9fa5f7ec7d7d7055cf36741c990d9c788f68a8427b08cdf08', 'Mock Mentor', 'MENTOR'),
    ('intern_mock', '534d9b45e4168ad5e7ab39ddde0387982ec6a2a18b992f62738b23fcde72f7e7', 'Mock Intern', 'INTERN')
ON DUPLICATE KEY UPDATE
    `display_name` = VALUES(`display_name`),
    `role` = VALUES(`role`);
