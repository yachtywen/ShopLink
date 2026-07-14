# 黑马点评前端 Nginx 学习指南

本项目的 Nginx 容器不是替代 Spring Boot，而是放在浏览器与后端之间：

```text
浏览器 http://localhost:8080
  ├─ /、/assets/*  → Nginx 返回 Vue 构建产物
  ├─ /api/*       → Nginx 反向代理 → Spring Boot :8081
  └─ /imgs/*      → Nginx 从宿主机挂载的上传目录读取图片
```

开发时使用 Vite 的代理，便于热更新；执行 `docker compose up --build` 后，Nginx 负责生产式访问。两者都让前端只请求 `/api`，因此业务代码无需因环境变化而修改。

## 配置解读

配置文件位于 `frontend/nginx/default.conf`：

| 配置 | 作用 |
| --- | --- |
| `root /usr/share/nginx/html` | Dockerfile 将 Vite `dist` 复制到此目录，作为站点根目录。 |
| `location / { try_files ... /index.html; }` | 找不到真实文件时返回 Vue 入口，支持 History 路由刷新。 |
| `location /api/ { proxy_pass http://host.docker.internal:8081/; }` | 把 `/api/user/login` 转为宿主机的 `/user/login`。尾部 `/` 会移除 `/api/` 前缀。 |
| `proxy_set_header` | 将原始 Host、客户端 IP 和协议传给后端，日志和安全策略需要这些信息。 |
| `location /imgs/ { alias ...; }` | 将 URL `/imgs/blogs/a.jpg` 映射到挂载目录中的 `blogs/a.jpg`。`alias` 必须以 `/` 结尾。 |
| `location /assets/` | 对带 hash 的构建资源设置一年不可变缓存。 |
| `/health` | 不经过 Vue 或 Spring Boot 的轻量健康检查。 |

`host.docker.internal` 是 Docker Desktop 提供的宿主机别名。它让容器中的 Nginx 能连接在 Windows 宿主机运行的 Spring Boot `8081`。

## 命令手册

在 `frontend/` 目录执行：

```powershell
# 首次运行或源码、Nginx 配置变化后重建
docker compose up --build -d

# 服务、访问与错误日志
docker compose ps
docker compose logs -f frontend

# 查看容器实际加载的完整配置；改完配置后的语法检查与热加载
docker compose exec frontend nginx -T
docker compose exec frontend nginx -t
docker compose exec frontend nginx -s reload

# 核验三个请求类型
Invoke-WebRequest http://localhost:8080/health
Invoke-WebRequest http://localhost:8080/api/shop-type/list
Invoke-WebRequest http://localhost:8080/assets/<构建后的文件名>
```

上传图片目录通过 Compose 的 `HM_DP_IMAGE_DIR` 映射。它必须对应 Java 常量 `SystemConstants.IMAGE_UPLOAD_DIR`。若默认目录不存在，请先用 `New-Item -ItemType Directory -Force 'D:\lesson\nginx-1.18.0\html\hmdp\imgs'` 创建；若你的本机目录不同，先设置：

```powershell
$env:HM_DP_IMAGE_DIR='你的图片目录'
docker compose up --build -d
```

## 三个动手练习

### 1. 理解 SPA 刷新为什么需要 `try_files`

1. 启动容器，在浏览器打开 `http://localhost:8080/shops`，然后刷新。
2. 暂时将 `location /` 中的 `try_files $uri $uri/ /index.html;` 改成 `try_files $uri =404;`。
3. 重建容器并刷新 `/shops`，将收到 Nginx 404。
4. 恢复 `index.html` 回退规则。Nginx 不知道 Vue 路由；它只能读取磁盘文件，所以必须把未知页面交给 Vue。

### 2. 观察错误代理为何返回 502

1. 将 `proxy_pass` 临时改为 `http://host.docker.internal:9999/`。
2. 重建后访问 `http://localhost:8080/api/shop-type/list`，浏览器会得到 502。
3. 执行 `docker compose logs frontend`，可看到 Nginx 连接上游失败。
4. 恢复为 `8081`。这说明 502 是 Nginx 连接不到上游服务，而不是 Vue 的业务错误。

### 3. 验证静态缓存

1. 打开浏览器开发者工具的 Network 面板，选择任意 `/assets/*.js` 请求。
2. 查看 `Cache-Control: public, max-age=31536000, immutable`。
3. 将 `location /assets/` 的 `max-age` 改成 `60`，重建后重新查看响应头。
4. 不要对 `index.html` 使用长期不可变缓存；它需要尽快指向新版 hash 资源。

## 常见故障

| 现象 | 排查方向 |
| --- | --- |
| `502 Bad Gateway` | 确认 Spring Boot 是否监听 `8081`，以及 `host.docker.internal` 可用。 |
| `/imgs/...` 返回 404 | 检查 `HM_DP_IMAGE_DIR` 是否存在、是否与 Java 上传目录一致、容器是否重建。 |
| 刷新 `/blogs/1` 为 404 | 确认 `try_files` 未被删掉，使用 `nginx -T` 查看有效配置。 |
| API 仍访问 5173 或 8081 | 生产页面应从 `8080` 打开；检查浏览器缓存并重新执行 Docker 构建。 |
