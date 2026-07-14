# 黑马点评前端联调台

这是与 Spring Boot 后端配套的 Vue 3 测试前端。它不是完整的商业客户端，而是用真实页面覆盖店铺、笔记、关注、优惠券与秒杀接口。

## 本地开发

```powershell
cd frontend
npm install
npm run dev
```

Vite 服务运行在 `http://localhost:5173`，会把 `/api/*` 转发到 `http://localhost:8081/*`。先启动 MySQL、Redis、RocketMQ 和 Spring Boot；登录验证码输出在后端 debug 日志中。

```powershell
npm run typecheck
npm run lint
npm run test
npm run build
npm run e2e
```

浏览器测试使用接口 fixture，不依赖后端。真实登录、上传和秒杀请按下方 Nginx 联调流程手工验证。

## 使用 Nginx 部署与联调

1. 确保 Spring Boot 已运行在宿主机 `8081` 端口。
2. 确认 `HM_DP_IMAGE_DIR` 与后端 `SystemConstants.IMAGE_UPLOAD_DIR` 一致。默认目录是 `D:/lesson/nginx-1.18.0/html/hmdp/imgs`；当前机器该目录尚不存在时，先创建它：

```powershell
New-Item -ItemType Directory -Force 'D:\lesson\nginx-1.18.0\html\hmdp\imgs'
```
3. 在 `frontend/` 运行：

```powershell
$env:HM_DP_IMAGE_DIR='D:/lesson/nginx-1.18.0/html/hmdp/imgs'
docker compose up --build -d
```

4. 访问 `http://localhost:8080`，用下面命令验证：

```powershell
Invoke-WebRequest http://localhost:8080/health
Invoke-WebRequest http://localhost:8080/api/shop-type/list
docker compose logs -f frontend
docker compose exec frontend nginx -T
```

停止容器：`docker compose down`。更多配置解释和练习见 [Nginx 学习指南](../docs/NGINX_FRONTEND_GUIDE.md)。

## 已知后端边界

- 退出登录、评论、真实支付、退款、订单列表未实现。
- 后端新增优惠券、上传和图片删除等接口缺少生产级权限控制，因此界面标注为开发测试。
- 秒杀涉及 Redis 和 RocketMQ；未启动时可能停留在“排队中”或返回业务失败。
