# 实时资讯

## 模块目标

实现外部资讯获取、缓存、搜索和刷新能力，优先调用免 Key 公开 API，失败时降级到 RSS，并保证资讯失败不影响任务主流程。

## 输入依据

- `plan.md`：实时资讯模块要求调用公开 API 或 RSS 获取真实资讯。
- `plan.md`：资讯操作要求支持资讯列表、关键词搜索和手动刷新。
- `plan.md`：资讯刷新失败时不影响任务主流程，应返回友好错误提示或使用缓存数据。
- `detailed-design.md`：实时资讯模块包含免 Key API、RSS 降级、缓存资讯、按关键词检索。
- `detailed-design.md`：资讯 API 包含 `GET /api/news` 和 `POST /api/news/refresh`。

## 最小任务 checklist

- [x] 选择一个免 Key 公开资讯 API 作为优先数据源。
- [x] 选择一个 RSS 源作为降级数据源。
- [x] 设计资讯实体或表结构字段：`id`、`title`、`url`、`source`、`keyword`、`publishedAt`、`fetchedAt`。
- [x] 为 `news_items.url` 设计唯一约束或去重逻辑。
- [x] 添加资讯列表响应 DTO。
- [x] 添加资讯刷新请求 DTO，包含 `keyword`。
- [x] 添加资讯数据访问 Mapper 或 Repository。
- [x] 实现按关键词查询缓存资讯。
- [x] 实现资讯分页查询。
- [x] 实现外部 API 客户端。
- [x] 实现 RSS 客户端。
- [x] 实现外部资讯数据到内部 DTO 的字段映射。
- [x] 实现资讯刷新 Service，优先调用免 Key API。
- [x] 实现免 Key API 失败后的 RSS 降级。
- [x] 实现资讯入库缓存。
- [x] 实现重复 URL 去重。
- [x] 实现外部源失败时返回缓存数据或友好错误。
- [x] 实现 `GET /api/news` 资讯列表接口。
- [x] 实现 `POST /api/news/refresh` 手动刷新接口。
- [x] 前端定义资讯 TypeScript 类型。
- [x] 前端封装资讯列表和刷新 API。
- [x] 前端实现资讯列表组件，展示标题、来源、发布时间和跳转链接。
- [x] 前端实现关键词搜索输入框。
- [x] 前端实现资讯刷新按钮。
- [x] 前端刷新失败时展示友好提示，并保留已有列表。
- [x] 前端外部链接使用新窗口打开。

## 验收 checklist

- [x] 可以按关键词查询资讯列表。
- [x] 可以手动刷新指定关键词资讯。
- [x] 外部 API 失败时会尝试 RSS 降级。
- [x] 重复 URL 不会生成重复资讯数据。
- [x] 资讯刷新失败不影响任务 CRUD。
- [x] 前端资讯列表展示标题、来源、发布时间和链接。
- [x] 前端刷新失败时不会清空已有内容。
