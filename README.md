# 黑马点评（hm-dianping）

一个面向本地学习与接口联调的点评业务项目，包含 Spring Boot 后端和 Vue 3 测试前端。

## 项目亮点

- Redis + Lua：秒杀库存预扣和一人一单资格校验。
- RocketMQ：异步创建秒杀订单、订单消息重试、延迟消息自动关闭超时未支付订单。
- MySQL：条件扣库存、订单唯一索引和状态条件更新，保证订单幂等与状态一致性。
- Redis ZSet + Lua + AOP：全局、IP、用户三维滑动窗口限流，超限统一返回 HTTP 429。
- Caffeine L1 + Redis L2：商户详情与优惠券静态信息的两级缓存。
- Vue 3 + Vite + Element Plus：用于登录、店铺、笔记、优惠券和秒杀接口的测试前端。

## 技术栈

- 后端：Spring Boot 2.3、MyBatis-Plus、Redis、Redisson、RocketMQ、MySQL 8
- 前端：Vue 3、TypeScript、Vite、Element Plus、Pinia
- 部署与联调：Docker Compose、Nginx

## 本地运行

### 1. 准备依赖服务

需要启动 MySQL、Redis 和 RocketMQ。

RocketMQ 可以使用项目内的 Compose 文件启动：

```powershell
docker compose -f docker-compose.rocketmq.yml up -d
```

MySQL 初始化脚本位于 `src/main/resources/db/`：

- `dianping.sql`：基础表结构和基础数据；
- `dianping_incremental.sql`：秒杀订单、消息表等增量结构；
- `dianping_seed.sql`：可选的本地测试数据。

Redis 初始化脚本位于 `src/main/resources/redis/`。

### 2. 配置数据库连接

项目不会提交真实数据库密码。后端从环境变量读取数据库连接信息；未设置时只使用本地开发默认地址和 `root` 用户名，密码为空。

PowerShell 示例：

```powershell
$env:HMDP_DB_URL = 'jdbc:mysql://127.0.0.1:3307/dianping?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true'
$env:HMDP_DB_USERNAME = 'root'
$env:HMDP_DB_PASSWORD = '你的本地数据库密码'
```

也可以创建 `src/main/resources/application-local.yaml` 保存本机配置；该文件已被 Git 忽略，不能提交真实密码。使用该文件时需要启用 `local` Profile：

```powershell
mvn spring-boot:run "-Dspring-boot.run.profiles=local"
```

### 3. 启动后端

```powershell
mvn spring-boot:run
```

后端默认运行在 `http://localhost:8081`。

### 4. 启动前端

```powershell
cd frontend
npm install
npm run dev
```

前端默认运行在 `http://localhost:5173`，开发环境会将 `/api` 代理到后端 `8081` 端口。

## 文档

- [接口说明](docs/API.md)
- [秒杀与 RocketMQ 设计说明](docs/rocketmq-notes.md)
- [缓存架构说明](docs/CACHE_ARCHITECTURE.md)
- [Nginx 前端联调指南](docs/NGINX_FRONTEND_GUIDE.md)
- [项目总结](docs/conclusion.md)

## 注意事项

- 当前支付功能为 mock-pay 演示，不包含真实支付回调、退款和券核销。
- `application.yaml` 只保留可公开的本地默认配置；密码、Token、第三方密钥必须通过环境变量或被 Git 忽略的本地配置文件提供。
- 提交前建议执行 `git status`，确认未包含 `.env`、`node_modules`、构建产物或真实凭据。
