# 模块实现提示词

按下面顺序为每个模块开启新会话。每次只复制一个模块的提示词，等该模块完成并通过测试后，再进入下一个模块。

## 1. 用户认证

```text
请根据 tasks/auth.md 实现用户认证模块。

当前项目基础：
- 后端框架已经搭好，包名为 com.icinfo.taskmanagement。
- Spring Boot 3.3.6 + Java 17 + MyBatis-Plus + MySQL。
- 数据库为 icinfo_task_management，默认账号 root/root。
- 已有统一响应 ApiResponse、ErrorCode、BusinessException、GlobalExceptionHandler。
- 已有 JWT 工具骨架、CurrentUserContext、AuthenticationInterceptor、CORS、/api/health。

实现要求：
1. 追加 users 表建表 SQL 和导师/实习生初始化账号 SQL，放在 sql/ 目录下。
2. 实现用户实体、Mapper、DTO、Service、Controller。
3. 完成 POST /api/auth/login。
4. 完成 POST /api/auth/mock-login。
5. 完成 POST /api/auth/register。
6. 完成 GET /api/auth/me。
7. 登录和 Mock 登录返回 token 和用户信息，响应中不返回密码。
8. JWT 中至少包含用户 id、username、role。
9. /api/auth/** 和 /api/health 放行，其他 /api/** 需要 token。
10. 补充必要测试，至少覆盖健康检查、数据库连接、登录、Mock 登录、当前用户接口。
11. 运行测试时使用 JDK 17：
   $env:JAVA_HOME='C:\Program Files\Java\jdk-17'
   $env:PATH="$env:JAVA_HOME\bin;$env:PATH"
   mvn test
12. 完成后勾选 tasks/auth.md 中已完成项，并同步更新 tasks/progress.md 中用户认证模块。

注意：
- 不要批量删除文件或目录。
- 不要实现任务管理、资讯、仪表盘、导出等后续模块。
- 如果需要新增 SQL，请说明执行顺序。
```

## 2. 任务管理

```text
请根据 tasks/task-management.md 实现任务管理模块。

前置条件：
- 用户认证模块已经完成。
- 当前用户可通过 CurrentUserContext 获取。
- users 表和导师/实习生账号已经存在。

实现要求：
1. 追加 tasks 表建表 SQL，放在 sql/ 目录下，并说明执行顺序。
2. 实现任务实体、Mapper、DTO、Service、Controller。
3. 支持任务字段：title、description、status、priority、assigneeId、creatorId、dueDate、createdAt、updatedAt。
4. 实现 POST /api/tasks 创建任务。
5. 实现 GET /api/tasks/{id} 查询任务详情。
6. 实现 PUT /api/tasks/{id} 编辑任务。
7. 实现 DELETE /api/tasks/{id} 删除任务。
8. 导师可以查看、创建、编辑、删除所有任务。
9. 实习生只能查看和维护分配给自己的任务，不能删除任务。
10. 保持统一响应格式和统一异常处理。
11. 补充必要测试，覆盖导师全量权限、实习生本人权限、越权访问、CRUD 主流程。
12. 使用 JDK 17 运行 mvn test。
13. 完成后勾选 tasks/task-management.md 中已完成项，并同步更新 tasks/progress.md 中任务管理模块。

注意：
- 不要批量删除文件或目录。
- 不要实现筛选搜索、状态流转、资讯、仪表盘、导出等后续模块，除非任务管理 CRUD 必须使用最小状态/优先级枚举。
```

## 3. 筛选搜索

```text
请根据 tasks/filter-search.md 实现筛选搜索模块。

前置条件：
- 用户认证模块已经完成。
- 任务管理模块已经完成。
- /api/tasks 基础列表或任务查询能力已经存在。

实现要求：
1. 为 GET /api/tasks 增加筛选参数：status、assigneeId、dueDateStart、dueDateEnd、keyword、page、size。
2. 查询逻辑必须复用当前用户权限范围。
3. 导师可在全量可见任务中筛选。
4. 实习生只能在分配给自己的任务中筛选。
5. 支持空条件查询。
6. 支持状态精确匹配。
7. 支持负责人精确匹配。
8. 支持截止日期范围过滤。
9. 支持关键词对标题和描述模糊匹配。
10. 支持多条件组合查询和分页返回。
11. 保持已有任务 CRUD 行为不回退。
12. 补充必要测试，覆盖空条件、单条件、多条件、分页、实习生越权边界。
13. 使用 JDK 17 运行 mvn test。
14. 完成后勾选 tasks/filter-search.md 中已完成项，并同步更新 tasks/progress.md 中筛选搜索模块。

注意：
- 不要批量删除文件或目录。
- 不要实现状态流转 PATCH、资讯、仪表盘、导出等后续模块。
```

## 4. 状态流转

```text
请根据 tasks/status-flow.md 实现状态流转模块。

前置条件：
- 用户认证模块已经完成。
- 任务管理模块已经完成。
- 任务状态枚举包含 TODO、IN_PROGRESS、DONE。

实现要求：
1. 实现 PATCH /api/tasks/{id}/status。
2. 请求体包含 status。
3. 校验状态值只能是 TODO、IN_PROGRESS、DONE。
4. 导师可以更新任意任务状态。
5. 实习生只能更新分配给自己的任务状态。
6. 状态更新后刷新 updatedAt。
7. 任务不存在、无权限、非法状态都要返回统一错误响应。
8. 保留编辑任务接口中修正状态的能力，不破坏任务管理模块。
9. 补充必要测试，覆盖 TODO 到 IN_PROGRESS、IN_PROGRESS 到 DONE、非法状态、实习生越权。
10. 使用 JDK 17 运行 mvn test。
11. 完成后勾选 tasks/status-flow.md 中已完成项，并同步更新 tasks/progress.md 中状态流转模块。

注意：
- 不要批量删除文件或目录。
- 不要实现拖拽状态切换；这里只做后端状态接口和规则。
- 不要实现资讯、仪表盘、导出等后续模块。
```

## 5. 实时资讯

```text
请根据 tasks/news.md 实现实时资讯模块。

前置条件：
- 后端基础框架已经完成。
- 用户认证可用。
- 当前模块只处理资讯自身的搜索、刷新和缓存，不处理任务关联。

实现要求：
1. 追加 news_items 表建表 SQL，放在 sql/ 目录下，并说明执行顺序。
2. 实现资讯实体、Mapper、DTO、Service、Controller。
3. 实现 GET /api/news，支持 keyword、page、size。
4. 实现 POST /api/news/refresh，按 keyword 刷新资讯。
5. 优先使用免 Key 公开 API；失败时降级 RSS。
6. 外部资讯字段至少映射为 title、url、source、keyword、publishedAt、fetchedAt。
7. url 去重，避免重复缓存。
8. 外部源失败时返回缓存数据或友好错误，不影响其他模块。
9. 补充必要测试；外部 API/RSS 建议用 Mock，不依赖真实网络作为单元测试前提。
10. 使用 JDK 17 运行 mvn test。
11. 完成后勾选 tasks/news.md 中已完成项，并同步更新 tasks/progress.md 中实时资讯模块。

注意：
- 不要批量删除文件或目录。
- 不要实现任务与资讯关联接口，那是 tasks/task-news.md。
- 不要实现仪表盘、导出等后续模块。
```

## 6. 资讯关联

```text
请根据 tasks/task-news.md 实现资讯关联模块。

前置条件：
- 用户认证模块已经完成。
- 任务管理模块已经完成。
- 实时资讯模块已经完成。
- tasks 表和 news_items 表已经存在。

实现要求：
1. 追加 task_news 表建表 SQL，放在 sql/ 目录下，并说明执行顺序。
2. 实现任务资讯关联实体、Mapper、DTO、Service、Controller。
3. 实现 GET /api/tasks/{id}/news。
4. 实现 POST /api/tasks/{id}/news/refresh。
5. 查询或刷新任务关联资讯前，必须校验当前用户可见该任务。
6. 刷新时如果请求没有 keyword，默认使用任务标题作为关键词。
7. 如果请求提供 keyword，手动关键词优先。
8. 刷新逻辑复用实时资讯模块能力。
9. task_id + news_id 去重，避免重复关联。
10. 资讯刷新失败不影响任务详情基础信息。
11. 补充必要测试，覆盖关联查询、按任务标题刷新、手动关键词刷新、重复关联去重、无权限访问。
12. 使用 JDK 17 运行 mvn test。
13. 完成后勾选 tasks/task-news.md 中已完成项，并同步更新 tasks/progress.md 中资讯关联模块。

注意：
- 不要批量删除文件或目录。
- 不要实现仪表盘、导出等后续模块。
```

## 7. 仪表盘

```text
请根据 tasks/dashboard.md 实现仪表盘模块。

前置条件：
- 用户认证模块已经完成。
- 任务管理模块已经完成。
- 任务状态和权限范围规则已经稳定。

实现要求：
1. 实现 GET /api/dashboard/summary。
2. 实现 GET /api/dashboard/status-chart。
3. 统计逻辑必须复用当前用户权限范围。
4. 导师统计全量任务。
5. 实习生只统计分配给自己的任务。
6. summary 返回待办数、进行中数、已完成数、总数、完成率。
7. 无任务时完成率为 0。
8. status-chart 返回适合前端 ECharts 使用的状态分布数据。
9. 实现临期/逾期任务查询能力，逾期规则为截止日期早于当前日期且状态不是 DONE。
10. 如新增接口用于临期/逾期提示，请保持 /api/dashboard/** 路径。
11. 补充必要测试，覆盖导师统计、实习生统计、完成率、空数据、逾期规则。
12. 使用 JDK 17 运行 mvn test。
13. 完成后勾选 tasks/dashboard.md 中已完成项，并同步更新 tasks/progress.md 中仪表盘模块。

注意：
- 不要批量删除文件或目录。
- 不要实现 Excel 导出模块。
```

## 8. Excel 导出

```text
请根据 tasks/excel-export.md 实现 Excel 导出模块。

前置条件：
- 用户认证模块已经完成。
- 任务管理模块已经完成。
- 筛选搜索模块已经完成。
- 当前任务列表查询已经支持权限范围和筛选条件。

实现要求：
1. 添加 EasyExcel 依赖。
2. 实现 GET /api/tasks/export。
3. 导出接口支持与任务列表相同的筛选参数：status、assigneeId、dueDateStart、dueDateEnd、keyword。
4. 导出逻辑必须复用任务列表权限范围。
5. 导师可导出全量可见任务。
6. 实习生只能导出分配给自己的任务。
7. 导出字段包含标题、描述、状态、优先级、负责人、创建人、截止日期、创建时间、更新时间。
8. 设置正确的 Content-Type 和 Content-Disposition。
9. 返回 .xlsx 文件。
10. 补充必要测试，覆盖响应头、筛选条件、导师权限、实习生权限、越权数据不导出。
11. 使用 JDK 17 运行 mvn test。
12. 完成后勾选 tasks/excel-export.md 中已完成项，并同步更新 tasks/progress.md 中 Excel 导出模块。

注意：
- 不要批量删除文件或目录。
- 不要改动无关业务模块。
```
