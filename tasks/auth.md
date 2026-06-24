# 用户认证

## 模块目标

实现导师和实习生可用的登录、Mock 登录、简化注册、JWT 鉴权和当前用户识别能力，并在前端完成 token 保存与路由守卫。

## 输入依据

- `plan.md`：用户认证模块要求支持登录/注册，MVP 阶段可使用 Mock 用户。
- `plan.md`：登录页需要支持导师、实习生 Mock 账号快速登录，登录成功后保存 token 并跳转任务列表页。
- `detailed-design.md`：认证模块包含登录、Mock 登录、简化注册、签发/解析 JWT、获取当前用户。
- `detailed-design.md`：认证 API 包含 `/api/auth/login`、`/api/auth/mock-login`、`/api/auth/register`、`/api/auth/me`。

## 最小任务 checklist

- [x] 添加用户角色枚举 `MENTOR`、`INTERN`，保证后端和前端类型一致。
- [x] 设计用户实体或表结构字段：`id`、`username`、`password`、`displayName`、`role`、`createdAt`。
- [x] 添加登录请求 DTO，包含 `username` 和 `password`。
- [x] 添加注册请求 DTO，包含 `username`、`password`、`displayName`、`role`。
- [x] 添加认证响应 DTO，返回 token 和当前用户信息，响应中不返回密码。
- [x] 实现用户名唯一性校验。
- [x] 实现简化注册接口 `POST /api/auth/register`。
- [x] 实现用户名密码登录接口 `POST /api/auth/login`。
- [x] 实现 Mock 登录接口 `POST /api/auth/mock-login`，支持按角色或用户名快速登录。
- [x] 实现 JWT 签发逻辑，token 中至少包含用户 id、用户名和角色。
- [x] 实现 JWT 解析逻辑，能从请求头还原当前用户身份。
- [x] 实现认证拦截器或过滤器，拦截需要登录的 API。
- [x] 实现当前用户接口 `GET /api/auth/me`。
- [x] 配置 CORS，允许前端开发环境携带认证请求。
- [x] 初始化导师和实习生 Mock 账号。
- [x] 前端封装认证 API：登录、Mock 登录、注册、当前用户。
- [x] 前端保存 token 和用户信息到统一用户状态中。
- [x] 前端 Axios 请求自动附加 `Authorization` header。
- [x] 前端路由守卫拦截未登录访问。
- [x] 前端登录页提供用户名密码表单。
- [x] 前端登录页提供导师 Mock 登录按钮。
- [x] 前端登录页提供实习生 Mock 登录按钮。
- [x] 登录成功后跳转任务列表页。
- [x] token 失效或接口返回未登录时清理本地登录状态并返回登录页。

## 验收 checklist

- [x] 可以使用导师 Mock 账号登录并获得 token。
- [x] 可以使用实习生 Mock 账号登录并获得 token。
- [x] 可以通过 `GET /api/auth/me` 获取当前登录用户。
- [x] 未携带 token 访问受保护接口时返回未登录错误。
- [x] 注册重复用户名时返回参数错误或业务错误。
- [x] 前端刷新页面后仍能根据 token 恢复登录态。
- [x] 前端未登录访问任务页或仪表盘时跳转登录页。
