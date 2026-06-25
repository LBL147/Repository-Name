# 后端编译与启动错误记录：TaskNewsMapper text block 与 Java 版本不匹配

## 背景

在实现实时资讯与任务关联资讯前端后，需要启动真实后端进行联调。首次执行后端启动命令时失败：

```powershell
mvn.cmd spring-boot:run
```

失败位置集中在：

```text
src/main/java/com/icinfo/taskmanagement/mapper/TaskNewsMapper.java
```

## 现象

Maven 编译阶段报出大量 Java 语法错误，核心错误包括：

```text
未结束的字符串文字
需要=
非法的类型开始
非法字符: '#'
```

最早的错误指向 `TaskNewsMapper.java` 的 `@Select(""" ... """)`：

```java
@Select("""
        SELECT
            tn.id AS id,
            ni.id AS news_id,
            ...
        """)
```

该写法是 Java text block，需要 Java 15+ 支持，项目 `pom.xml` 声明的目标版本是 Java 17。

## 诊断过程

先查看 Maven 实际使用的 Java 版本：

```powershell
mvn.cmd -version
```

输出显示 Maven 默认使用 Java 8：

```text
Apache Maven 3.8.8
Java version: 1.8.0_451
runtime: C:\Program Files\Java\jdk-1.8\jre
```

因此 Java 8 编译器无法识别 `"""` text block，把它当成非法字符串语法处理，导致 `TaskNewsMapper.java` 出现大量级联语法错误。

临时验证时曾将 `TaskNewsMapper.java` 中的 text block 改成 Java 8 也能解析的普通字符串拼接，SQL 内容保持不变，用于确认原始报错确实来自 Java 版本不匹配。

修复后再次使用默认 Java 8 编译，原始 text block 错误消失，但出现新的 classfile 版本错误：

```text
类文件具有错误的版本 61.0, 应为 52.0
```

这说明项目依赖包含 Java 17 编译产物，后端整体仍必须使用 JDK 17 构建和运行。

## 根因

根因有两层：

1. 本机默认 Maven 环境使用 Java 8。
2. 项目是 Spring Boot 3.x / Java 17 项目，且 `TaskNewsMapper.java` 使用了 Java text block 语法。

Java 8 与项目实际运行要求不匹配，导致后端无法编译启动。

随后系统环境已切换为 JDK 17。由于项目本身就是 Java 17 项目，最终代码恢复为 JDK 17 更清晰的 text block 写法。

## 最终修复内容

最终修复方式是统一 Maven/Java 环境到 JDK 17，并保留 `TaskNewsMapper.java` 中的 text block SQL。

修改文件：

```text
src/main/java/com/icinfo/taskmanagement/mapper/TaskNewsMapper.java
```

最终保留：

```java
@Select("""
        SELECT
            tn.id AS id,
            ni.id AS news_id,
            ni.title AS title,
            ni.url AS url,
            ni.source AS source,
            ni.keyword AS keyword,
            ni.published_at AS published_at,
            ni.fetched_at AS fetched_at,
            tn.created_at AS associated_at
        FROM task_news tn
        INNER JOIN news_items ni ON ni.id = tn.news_id
        WHERE tn.task_id = #{taskId}
        ORDER BY ni.published_at DESC, ni.fetched_at DESC, ni.id DESC
        """)
```

该写法要求 Maven 使用 JDK 17，与项目 `pom.xml` 中的 Java 17 要求一致。

## 正确构建方式

后端必须使用 JDK 17。系统环境已修正后，可直接运行：

```powershell
mvn.cmd test
```

启动后端：

```powershell
mvn.cmd spring-boot:run
```

## 验证结果

后端测试：

```powershell
mvn.cmd test
```

结果：

```text
Tests run: 41, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

前端构建：

```powershell
cd frontend
npm.cmd run build
```

结果：构建成功，仅有 Vite/Rollup 关于大 chunk 和依赖注释的警告。

真实联调：

- 后端成功启动到 `http://localhost:8080`
- 前端成功启动到 `http://localhost:5173`
- Mock 登录成功
- 任务列表成功加载真实后端数据
- `/news` 页面可进入，空态稳定
- 任务详情弹窗可打开，关联资讯区域可展示
- 外部资讯源不可用时，只显示资讯错误，不关闭任务弹窗，不影响任务编辑

## 后续建议

建议统一项目本地开发环境的 Java 版本：

- 将 `JAVA_HOME` 指向 `C:\Program Files\Java\jdk-17`
- 或在项目 README 中明确要求后端使用 JDK 17
- 避免后续使用 Java 8 启动 Maven 时再次出现 classfile 版本不匹配问题

---

# 外部资讯源在国内网络不可用记录

## 背景

当前实时资讯功能已经实现了外部资讯刷新、缓存、搜索和任务关联：

- 独立资讯页：`GET /api/news`、`POST /api/news/refresh`
- 任务详情关联资讯：`GET /api/tasks/{id}/news`、`POST /api/tasks/{id}/news/refresh`
- 前端页面：`frontend/src/views/NewsView.vue` 和 `frontend/src/views/TasksView.vue`
- 后端服务：`NewsService`、`TaskNewsService`、`FallbackNewsFetcher`

当前默认外部来源为：

1. `GDELT DOC API`
2. `Google News RSS`

对应代码：

```text
src/main/java/com/icinfo/taskmanagement/service/news/GdeltNewsSource.java
src/main/java/com/icinfo/taskmanagement/service/news/GoogleNewsRssSource.java
src/main/java/com/icinfo/taskmanagement/service/news/FallbackNewsFetcher.java
```

## 现象

在中国大陆网络环境下，`GDELT DOC API` 和 `Google News RSS` 可能无法稳定访问，导致点击“刷新资讯”或任务详情中的“按标题刷新”时出现外部资讯不可用。

当前后端已有降级保护：

- 如果外部源失败且已有缓存，会返回缓存数据。
- 如果外部源失败且没有缓存，会返回友好错误。
- 任务详情中的资讯刷新失败不会影响任务查看、编辑和保存。

但这仍然会影响“获取最新真实资讯”的演示效果。

## 根因

当前默认资讯源依赖海外服务：

- GDELT 虽是公开 API，但国内网络访问不一定稳定。
- Google News RSS 在国内网络通常不可访问。

因此功能逻辑是完整的，但默认数据源不适合国内网络验收。

## 建议修改方案

保留现有前端、数据库和任务关联逻辑，只替换后端资讯抓取来源。

推荐新增国内 RSS 聚合抓取器，例如：

```text
DomesticRssNewsFetcher
```

可用候选 RSS 源：

```text
https://www.oschina.net/news/rss
https://www.infoq.cn/feed
https://36kr.com/feed
https://www.cnblogs.com/news/rss
```

建议默认使用：

1. 开源中国 RSS：适合开源、编程语言、框架、技术社区新闻。
2. InfoQ 中文 RSS：适合软件工程、架构、AI、研发管理和技术趋势。
3. 36氪 RSS：适合公司、AI 行业、创投和商业动态。

博客园新闻 RSS 可以作为补充源，但需要注意编码和内容质量。

## 推荐实现策略

1. 新增一个国内 RSS 来源聚合类，使用 Java `HttpClient` 拉取多个 RSS。
2. 解析 RSS `item` 的 `title`、`link`、`source`、`pubDate`、`description`。
3. 对标题和摘要做关键词过滤，例如任务标题为 `Java后端` 时，只保留包含关键词的资讯。
4. 多源结果合并后按发布时间倒序。
5. 复用现有 `NewsService.cacheFetchedItems` 的 URL hash 去重和 `news_items` 缓存逻辑。
6. 保留“有缓存返回缓存、无缓存返回友好错误”的失败处理。
7. 可将 GDELT/Google 从默认链路移除，或只作为最后兜底，但国内演示建议默认不依赖它们。

## 影响范围

预计主要修改后端：

```text
src/main/java/com/icinfo/taskmanagement/service/news/
```

可能新增或调整：

```text
DomesticRssNewsSource.java
DomesticRssNewsFetcher.java
RssNewsItemParser.java
FallbackNewsFetcher.java
```

前端一般不需要修改，因为前端只调用现有接口：

```text
GET /api/news
POST /api/news/refresh
GET /api/tasks/{id}/news
POST /api/tasks/{id}/news/refresh
```

数据库也不需要新增表，继续使用：

```text
news_items
task_news
```

## 验证建议

修改后至少验证：

```powershell
mvn.cmd test
```

并人工联调：

1. 登录导师账号。
2. 进入“实时资讯”页面。
3. 输入 `Java`、`Spring Boot`、`AI` 等关键词并点击“刷新资讯”。
4. 确认页面出现国内来源资讯，如 OSCHINA、InfoQ、36氪。
5. 打开任务详情弹窗，点击“按标题刷新”或输入手动关键词刷新。
6. 确认资讯写入 `news_items`，并通过 `task_news` 与任务关联。
7. 断网或单个 RSS 源失败时，确认任务详情不崩溃，已有缓存仍可返回。

## 记录结论

该问题不是前端展示或任务关联逻辑缺失，而是默认外部资讯源不适合国内网络环境。建议新会话优先替换后端资讯源为国内 RSS 聚合方案，保留现有 API、数据库和前端交互不变。

---

# 前端构建 Vite 临时文件 EPERM 记录

## 背景

在修复“实时资讯页面点击搜索无结果、点击刷新资讯才有结果”的前端交互问题后，尝试运行前端完整构建验证：

```powershell
cd frontend
npm.cmd run build
```

## 现象

构建在加载 `vite.config.ts` 阶段失败，核心错误为：

```text
failed to load config from C:\work\icinfo-task-management\frontend\vite.config.ts
error during build:
Error: EPERM: operation not permitted, open 'C:\work\icinfo-task-management\frontend\node_modules\.vite-temp\vite.config.ts.timestamp-1782365612566-7839a849d3af88.mjs'
```

该错误发生在 Vite 尝试向 `frontend/node_modules/.vite-temp/` 写入临时配置文件时。

## 临时验证

由于完整构建失败点发生在 Vite 写临时文件阶段，为了验证本次前端代码改动本身没有 TypeScript 类型问题，改为运行：

```powershell
cd frontend
.\node_modules\.bin\vue-tsc.cmd --noEmit
```

验证结果：

```text
vue-tsc --noEmit 通过
```

## 初步判断

该问题不是业务代码或 TypeScript 编译错误，更像是当前执行环境对 `node_modules/.vite-temp` 写入权限受限，或 Windows 文件权限/占用导致的临时文件写入失败。

当时尝试按沙箱规则使用提权方式重新运行：

```powershell
npm.cmd run build
```

但提权请求被审批器拒绝，因此没有完成完整前端 build 复验。

## 后续建议

1. 在本机正常权限终端中重新运行 `frontend` 目录下的 `npm.cmd run build`。
2. 如果仍然报 `EPERM`，检查 `frontend/node_modules/.vite-temp` 是否被占用或权限异常。
3. 可关闭占用 Node/Vite 的进程后重试。
4. 如果目录权限异常，可手动处理该临时目录；注意不要使用批量删除命令。
5. 当前已通过 `vue-tsc --noEmit` 验证前端类型检查，业务改动点集中在 `frontend/src/views/NewsView.vue`。
