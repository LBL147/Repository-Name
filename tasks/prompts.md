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

## 9. 前端登录与鉴权

前端阶段通用上下文：每个新会话先阅读 `frontend/DESIGN.md`、`frontend/README.md` 和 `design-system/icinfo-task-management/MASTER.md`，并遵守“不要批量删除文件或目录”的项目约束。

```text
请在 frontend/ 工程中实现前端登录与鉴权模块。

前置条件：
- 后端认证接口已经完成。
- frontend/ 已有 Vue3 + Vite + TypeScript + Element Plus + Pinia + Vue Router 骨架。
- 先阅读 design-system/icinfo-task-management/MASTER.md、frontend/README.md、tasks/auth.md。

实现要求：
1. 完善 src/api/auth.ts 与 src/stores/auth.ts，确保 login、mock-login、register、me 可用。
2. 登录页支持用户名密码登录。
3. 登录页支持导师 Mock 登录和实习生 Mock 登录。
4. 登录成功后保存 token 和用户信息，并跳转 /tasks。
5. Axios 请求自动附加 Authorization: Bearer <token>。
6. 路由守卫拦截未登录访问 /tasks、/dashboard、/news。
7. 页面刷新后能根据 token 恢复登录态。
8. token 失效或接口返回未登录时清理本地登录状态并返回登录页。
9. 表单、按钮、错误提示使用 Element Plus；图标使用 @element-plus/icons-vue，不使用 emoji 图标。
10. 保持响应式布局，至少检查 375px、768px、1024px、1440px。
11. 完成后运行：
    cd frontend
    npm install
    npm run build
12. 如后端可用，启动后端和前端，手动验证导师/实习生登录。
13. 完成后勾选 tasks/auth.md 中所有前端相关未完成项。

注意：
- 不要批量删除文件或目录。
- 不要实现任务 CRUD、资讯、仪表盘、导出等后续模块。
- 不要修改后端，除非发现接口和文档明显不一致，并先说明原因。
```

## 10. 前端任务列表与 CRUD

```text
请在 frontend/ 工程中实现任务列表与 CRUD 前端。

前置条件：
- 前端登录与鉴权已经完成。
- 后端任务 CRUD 接口已经完成。
- 先阅读 design-system/icinfo-task-management/MASTER.md、frontend/README.md、tasks/task-management.md、detailed-design.md 中任务相关 API。

实现要求：
1. 完善 src/types/task.ts，使字段与后端 TaskResponse、TaskListItemResponse 一致。
2. 完善 src/api/tasks.ts，封装列表、详情、新增、编辑、删除接口。
3. 实现 /tasks 页面基础数据加载。
4. 实现任务表格视图，展示标题、状态、优先级、负责人、截止日期。
5. 实现任务卡片视图，展示同等核心字段。
6. 实现表格/卡片视图切换。
7. 实现新增任务入口。
8. 实现任务详情/编辑弹窗。
9. 任务表单支持 title、description、assigneeId、priority、dueDate、status。
10. 实现编辑保存和删除操作。
11. 根据当前用户角色隐藏或禁用无权限操作：实习生不能删除任务。
12. 新增、编辑、删除后刷新当前列表。
13. 加载中、空状态、失败提示都要有明确 UI。
14. 图标使用 @element-plus/icons-vue，不使用 emoji 图标。
15. 完成后运行 npm run build，并手动验证导师和实习生两种角色。
16. 完成后勾选 tasks/task-management.md 中所有前端相关未完成项。

注意：
- 不要批量删除文件或目录。
- 不要实现筛选搜索、状态流转、资讯、仪表盘、导出等后续模块，除非 CRUD 页面必须保留最小入口。
```

## 11. 前端筛选搜索与状态流转

```text
请在 frontend/ 工程中实现任务筛选搜索与状态流转前端。

前置条件：
- 前端登录与鉴权已经完成。
- 前端任务列表与 CRUD 已经完成。
- 后端筛选搜索和 PATCH /api/tasks/{id}/status 已经完成。
- 先阅读 tasks/filter-search.md、tasks/status-flow.md、frontend/README.md、design-system/icinfo-task-management/MASTER.md。

实现要求：
1. 任务列表页添加状态筛选控件。
2. 添加负责人筛选控件；如后端没有用户列表接口，可先使用已有任务中的负责人 id 或保留数字输入，并在代码中标注后续替换点。
3. 添加截止日期范围筛选控件。
4. 添加关键词输入框。
5. 搜索按钮按当前筛选条件请求 GET /api/tasks。
6. 重置按钮清空筛选条件并刷新列表。
7. 实现分页控件，分页变化时保留当前筛选条件。
8. 新增、编辑、删除、状态更新后按当前筛选条件刷新。
9. 前端定义 TaskStatus 类型并与后端 TODO、IN_PROGRESS、DONE 保持一致。
10. 任务列表展示状态标签。
11. 任务详情弹窗展示状态选择器。
12. TODO 任务提供切换到 IN_PROGRESS 的按钮。
13. IN_PROGRESS 任务提供切换到 DONE 的按钮。
14. DONE 任务展示已完成状态，不展示继续流转按钮。
15. 根据权限隐藏或禁用无权更新的状态按钮。
16. 状态更新失败时展示友好错误提示。
17. 完成后运行 npm run build，并手动验证筛选、分页、状态按钮和权限边界。
18. 完成后勾选 tasks/filter-search.md 与 tasks/status-flow.md 中所有前端相关未完成项。

注意：
- 不要批量删除文件或目录。
- 不要实现拖拽状态切换。
- 不要实现资讯、仪表盘、导出等后续模块。
```

## 12. 前端资讯与任务关联资讯

```text
请在 frontend/ 工程中实现实时资讯与任务关联资讯前端。

前置条件：
- 前端任务列表、详情/编辑弹窗已经完成。
- 后端 GET /api/news、POST /api/news/refresh、GET /api/tasks/{id}/news、POST /api/tasks/{id}/news/refresh 已经完成。
- 先阅读 tasks/news.md、tasks/task-news.md、frontend/README.md、design-system/icinfo-task-management/MASTER.md。

实现要求：
1. 完善 src/types/news.ts，与后端 NewsItemResponse、RefreshNewsResponse、TaskNewsResponse 对齐。
2. 完善 src/api/news.ts，封装资讯列表、资讯刷新、任务关联资讯查询、任务关联资讯刷新。
3. 实现 /news 页面，支持关键词搜索、刷新和分页列表。
4. 资讯列表展示标题、来源、发布时间、关键词和跳转链接。
5. 外部链接使用新窗口打开，并加 rel="noopener noreferrer"。
6. 刷新失败时展示友好提示，并保留已有列表。
7. 在任务详情/编辑弹窗中增加关联资讯区域。
8. 打开任务详情时加载关联资讯。
9. 支持按任务标题刷新关联资讯。
10. 支持手动关键词刷新关联资讯。
11. 刷新成功后更新关联资讯列表。
12. 刷新失败时只提示资讯错误，不关闭任务弹窗，不影响任务编辑。
13. 加载中、空状态、失败态都要稳定，不发生布局跳动。
14. 完成后运行 npm run build，并手动验证独立资讯页和任务弹窗里的关联资讯。
15. 完成后勾选 tasks/news.md 与 tasks/task-news.md 中所有前端相关未完成项。

注意：
- 不要批量删除文件或目录。
- 不要实现仪表盘、导出等后续模块。
```

## 13. 前端仪表盘

```text
请在 frontend/ 工程中实现仪表盘前端。

前置条件：
- 前端登录与鉴权已经完成。
- 后端 /api/dashboard/summary 与 /api/dashboard/status-chart 已经完成。
- 先阅读 tasks/dashboard.md、frontend/README.md、design-system/icinfo-task-management/MASTER.md。

实现要求：
1. 完善 src/types/dashboard.ts，与后端 DashboardSummaryResponse、DashboardStatusChartResponse 对齐。
2. 完善 src/api/dashboard.ts。
3. 实现 /dashboard 页面数据加载。
4. 展示待办、进行中、已完成、总数、完成率统计卡片。
5. 集成 ECharts。
6. 实现状态分布图或完成率图。
7. 实现临期任务提示。
8. 实现逾期任务提示；逾期规则为截止日期早于当前日期且状态不是 DONE。
9. 如后端没有独立临期/逾期接口，复用任务筛选接口并在前端计算展示。
10. 加载失败时展示友好错误或空状态。
11. 根据当前登录用户展示其权限范围内的统计结果。
12. 确保 ECharts 容器有稳定高度，移动端不溢出。
13. 完成后运行 npm run build，并手动验证导师和实习生统计差异。
14. 完成后勾选 tasks/dashboard.md 中所有前端相关未完成项。

注意：
- 不要批量删除文件或目录。
- 不要实现 Excel 导出模块。
```

## 14. 前端 Excel 导出

```text
请在 frontend/ 工程中实现任务 Excel 导出前端。

前置条件：
- 前端任务列表和筛选搜索已经完成。
- 后端 GET /api/tasks/export 已经完成，返回 .xlsx 文件。
- 先阅读 tasks/excel-export.md、frontend/README.md、design-system/icinfo-task-management/MASTER.md。

实现要求：
1. 完善 src/api/tasks.ts 中 exportTasks 方法，使用 responseType: 'blob'。
2. 在任务列表页添加导出按钮。
3. 导出时携带当前筛选条件：status、assigneeId、dueDateStart、dueDateEnd、keyword。
4. 从 Content-Disposition 中解析文件名；解析不到时使用默认文件名 tasks.xlsx。
5. 处理二进制响应并触发浏览器下载。
6. 导出过程中按钮展示 loading 状态。
7. 导出失败时展示友好错误提示。
8. 根据权限隐藏或禁用不允许导出的入口；如后端允许实习生导出本人任务，则前端保留入口。
9. 完成后运行 npm run build，并手动验证下载文件可以打开且筛选条件生效。
10. 完成后勾选 tasks/excel-export.md 中所有前端相关未完成项。

注意：
- 不要批量删除文件或目录。
- 不要改动无关业务模块。
```

## 15. 前端 README、截图与最终验收

```text
请完成项目最终交付文档、截图和验收检查。

前置条件：
- 后端所有模块已经完成。
- 前端登录、任务列表/CRUD、筛选/状态流转、资讯关联、仪表盘、Excel 导出已经完成。
- 先阅读 plan.md 的验收清单、detailed-design.md、frontend/README.md、tasks/progress.md。

实现要求：
1. 新增或完善根目录 README.md。
2. README 包含后端启动方式、前端启动方式、数据库 SQL 执行顺序。
3. README 包含导师和实习生测试账号说明。
4. README 包含主要功能说明：登录、任务 CRUD、筛选搜索、状态流转、资讯、仪表盘、Excel 导出。
5. README 说明 AI 工具使用方式、验证过程和遇到的问题。
6. 启动后端和前端，完成一次导师账号主流程验收。
7. 完成一次实习生账号权限边界验收。
8. 使用浏览器截图保存关键页面：登录页、任务列表、任务弹窗含资讯、仪表盘、导出入口。
9. 截图放入 docs/screenshots/；如果目录不存在则创建。
10. 更新 plan.md 验收清单中已完成项。
11. 更新 tasks/progress.md，明确前端模块完成状态。
12. 运行后端 mvn test。
13. 运行前端 npm run build。
14. 最终回复中列出验证命令、截图路径和仍存在的风险。

注意：
- 不要批量删除文件或目录。
- 如需要删除错误截图，只能一次删除一个明确路径的文件。
- 不要使用 Remove-Item -Recurse、rm -rf、del /s、rd /s、rmdir /s。
```
