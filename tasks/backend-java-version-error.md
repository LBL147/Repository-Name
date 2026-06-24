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
