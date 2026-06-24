# 内部任务管理系统前端

Vue3 + Vite + TypeScript + Element Plus 前端工程，默认对接本仓库 Spring Boot 后端。

## 启动

```powershell
cd frontend
npm install
npm run dev
```

默认地址：`http://localhost:5173`

默认后端地址：`http://localhost:8080/api`

如需调整后端地址，复制 `.env.example` 为 `.env.local` 后修改：

```text
VITE_API_BASE_URL=http://localhost:8080/api
```

## 目录

```text
src/api        Axios 和后端 API 封装
src/components 通用 UI 组件
src/layouts    登录后主布局
src/router     Vue Router 与登录守卫
src/stores     Pinia 状态
src/styles     全局样式与设计 token
src/types      后端 DTO 对应类型
src/views      页面
```

## 设计依据

- `design-system/icinfo-task-management/MASTER.md`
- `plan.md`
- `detailed-design.md`
- `tasks/*.md`
