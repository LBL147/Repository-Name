# Database Scripts

## 初始化数据库

当前后端框架默认连接 MySQL 数据库 `icinfo_task_management`。首次启动前，先用 MySQL 客户端执行：

```sql
SOURCE sql/00-create-database.sql;
SOURCE sql/01-auth-users.sql;
SOURCE sql/02-task-tasks.sql;
SOURCE sql/03-news-items.sql;
SOURCE sql/04-task-news.sql;
```

Execution order: run `00-create-database.sql` first, then `01-auth-users.sql`, then `02-task-tasks.sql`, then `03-news-items.sql`, then `04-task-news.sql`.

也可以直接复制 `00-create-database.sql` 内容到 MySQL 客户端执行。

## 默认连接配置

后端配置在 `src/main/resources/application.yml` 中：

- `DB_URL`：默认 `jdbc:mysql://localhost:3306/icinfo_task_management?...`
- `DB_USERNAME`：默认 `root`
- `DB_PASSWORD`：默认 `root`

如本机账号密码不同，启动前设置环境变量覆盖。

## 当前脚本范围

当前已包含用户认证、任务管理、实时资讯缓存和任务资讯关联所需表结构。仪表盘和 Excel 导出复用现有任务、用户和资讯关联数据，不需要额外建表。
