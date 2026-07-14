# 黑马点评（hm-dianping）前端接口文档

> 文档版本：v1.0  
> 代码基线：当前 `main` 工作区（Spring Boot 2.3，默认端口 `8081`）  
> 生成时间：2026-07-10  
> 说明：本文以当前 Controller、Service、拦截器的**实际实现**为准，而不是按产品设想补全接口。未实现或仅用于演示的能力已明确标注。

## 1. 接入约定

### 1.1 服务地址与请求格式

- 开发环境基地址：`http://localhost:8081`
- 除文件上传外，提交 JSON 时使用 `Content-Type: application/json`。
- 文件上传使用 `multipart/form-data`。
- 项目当前没有 CORS 配置。前端本地开发时应通过 Vite/Webpack 开发服务器代理转发至 `8081`，或由后端补充 CORS 配置；浏览器跨域直连会被拦截。
- 时间字段由 Jackson 序列化，前端按 ISO-8601 本地日期时间字符串处理，例如 `"2026-07-10T14:30:00"`。
- 金额、经纬度和 `id` 均直接来自后端实体。Java `Long` 在 JSON 中以数字输出；若前端需要兼容 JavaScript 大整数精度，请在请求库层将大整数转换为字符串处理。

### 1.2 统一响应体

所有**正常进入 Controller**的接口均使用如下包装：

```json
{
  "success": true,
  "errorMsg": null,
  "data": {},
  "total": null
}
```

| 字段 | 类型 | 含义 |
| --- | --- | --- |
| `success` | boolean | 业务是否成功 |
| `errorMsg` | string \| null | 失败原因；成功时通常为 `null` |
| `data` | any \| null | 业务数据 |
| `total` | number \| null | 保留字段；当前已实现接口均未设置分页总数 |

业务失败示例（多数业务失败的 HTTP 状态仍为 `200`，必须判断 `success`）：

```json
{
  "success": false,
  "errorMsg": "库存不足",
  "data": null,
  "total": null
}
```

### 1.3 认证与错误处理

1. 先调用“发送验证码”与“登录”。登录成功后，`data` 是纯 token 字符串。
2. 登录态接口在每次请求中携带：`authorization: <token>`。不要自动添加 `Bearer ` 前缀，后端读取的是完整请求头值。
3. token 存于 Redis，服务端每次收到有效请求会续期；配置时长为 36,000 分钟。
4. 访问需登录接口而未登录/令牌无效时，拦截器直接返回 HTTP `401`，通常没有统一 JSON 响应体。
5. 未捕获的 `RuntimeException` 会被统一转为 `{ "success": false, "errorMsg": "服务器异常" }`。参数绑定、404 等框架错误不保证符合该包装。

### 1.4 权限标识

| 标识 | 含义 |
| --- | --- |
| `公开` | 未登录可调用；如果携带有效 token，部分返回字段会附带当前用户状态 |
| `登录` | 必须携带 `authorization` |
| `当前实现公开` | 代码未做鉴权，通常属于管理或测试能力，生产前应增加权限控制 |

### 1.5 分页与图片约定

- `current` 从 `1` 开始。
- 店铺按分类列表每页 `5` 条；店铺名称搜索、热门笔记、用户笔记列表每页 `10` 条。
- 返回的是数组，不包含 `hasNext`、`pages`、`total`；前端以“返回数量少于页大小”判断是否结束。
- `Shop.images` 和 `Blog.images` 是逗号分隔的图片路径字符串，前端需自行 `split(',')` 并过滤空字符串。
- 上传接口返回的路径形式为 `/blogs/{d1}/{d2}/{uuid}.{ext}`。该项目只负责写入本地目录，未提供图片读取接口；页面展示时需要由已配置的 Nginx/静态资源服务器拼接图片域名。

## 2. 数据模型

以下字段为接口实际返回或提交时使用的字段。`createTime`、`updateTime` 多数为数据库自动维护字段，创建/更新请求通常无需提交。

### 2.1 UserDTO（公开用户摘要）

```json
{ "id": 1, "nickName": "user_abcd1234", "icon": "/imgs/icons/1.png" }
```

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | number | 用户 ID |
| `nickName` | string | 昵称 |
| `icon` | string | 头像路径，可能为空字符串 |

### 2.2 Shop（店铺）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | number | 店铺 ID |
| `name` | string | 店铺名称 |
| `typeId` | number | 店铺分类 ID |
| `images` | string | 多图逗号分隔路径 |
| `area` | string | 商圈 |
| `address` | string | 地址 |
| `x` / `y` | number | 经度 / 纬度 |
| `avgPrice` | number | 人均价格（整数） |
| `sold` | number | 销量 |
| `comments` | number | 评论数 |
| `score` | number | 评分，数据库以 10 倍整数保存；展示时通常除以 10 |
| `openHours` | string | 营业时间，如 `10:00-22:00` |
| `distance` | number | 仅在携带 `x`、`y` 的分类附近搜索中出现，单位为米 |
| `createTime` / `updateTime` | string | 创建 / 修改时间 |

### 2.3 Blog（探店笔记）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | number | 笔记 ID |
| `shopId` | number | 关联店铺 ID |
| `userId` | number | 作者用户 ID；创建时后端覆盖为当前用户 |
| `title` | string | 标题 |
| `images` | string | 最多 9 张图片的逗号分隔路径 |
| `content` | string | 正文 |
| `liked` / `comments` | number | 点赞数 / 评论数 |
| `name` / `icon` | string | 作者昵称 / 头像，仅热榜、详情、关注流中由后端补充 |
| `isLike` | boolean | 当前登录用户是否点赞；未登录查询热榜时该字段可能缺失或为 `null` |
| `createTime` / `updateTime` | string | 创建 / 修改时间 |

### 2.4 Voucher（优惠券）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | number | 优惠券 ID |
| `shopId` | number | 店铺 ID |
| `title` / `subTitle` | string | 标题 / 副标题 |
| `rules` | string | 使用规则 |
| `payValue` / `actualValue` | number | 支付金额 / 抵扣金额，整数金额单位以当前数据为准 |
| `type` / `status` | number | 券类型 / 券状态；当前代码未定义枚举，请按后端数据展示 |
| `stock` | number | 秒杀券库存；普通券通常无此字段或为 `null` |
| `beginTime` / `endTime` | string | 秒杀起止时间；普通券通常无此字段或为 `null` |
| `createTime` / `updateTime` | string | 创建 / 修改时间 |

### 2.5 ScrollResult（关注流游标分页）

```json
{
  "list": ["Blog 数组"],
  "minTime": 1720584000000,
  "offset": 1
}
```

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `list` | Blog[] | 本次返回的笔记，最多 2 条 |
| `minTime` | number | 本页最小时间戳，下次作为 `lastId` 传入 |
| `offset` | number | 与 `minTime` 相同的记录数，下次作为 `offset` 传入 |

### 2.6 秒杀订单状态

提交秒杀成功时 `data` 为：

```json
{ "orderId": 123456789, "statusCode": 10, "statusText": "排队中" }
```

查询订单状态时 `data` 为：

```json
{
  "orderId": 123456789,
  "statusCode": 12,
  "statusText": "待支付",
  "payTime": null,
  "createTime": "2026-07-10T14:30:00"
}
```

| `statusCode` | `statusText` | 前端建议 |
| --- | --- | --- |
| `10` | 排队中 | 继续轮询 |
| `11` | 处理中 | 继续轮询 |
| `12` | 待支付 | 进入支付页或展示模拟支付按钮 |
| `13` | 支付成功 | 结束轮询，展示成功 |
| `14` | 已取消 | 结束轮询，提示超时取消 |
| `15` | 处理失败 | 结束轮询，提示下单失败 |

## 3. 接口总览

| 模块 | 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- | --- |
| 用户 | POST | `/user/code` | 公开 | 发送短信验证码（验证码仅打印到后端日志） |
| 用户 | POST | `/user/login` | 公开 | 手机号 + 验证码登录/自动注册 |
| 用户 | GET | `/user/me` | 登录 | 当前用户摘要 |
| 用户 | GET | `/user/{id}` | 登录 | 用户摘要 |
| 用户 | GET | `/user/info/{id}` | 登录 | 用户扩展资料 |
| 用户 | POST | `/user/sign` | 登录 | 当日签到 |
| 用户 | GET | `/user/sign/count` | 登录 | 连续签到天数 |
| 用户 | POST | `/user/logout` | 登录 | 未实现，固定失败 |
| 店铺 | GET | `/shop/{id}` | 公开 | 店铺详情 |
| 店铺 | GET | `/shop/of/type` | 公开 | 分类/附近店铺列表 |
| 店铺 | GET | `/shop/of/name` | 公开 | 名称搜索 |
| 店铺 | POST | `/shop` | 登录 | 新增店铺 |
| 店铺 | PUT | `/shop` | 登录 | 更新店铺 |
| 分类 | GET | `/shop-type/list` | 公开 | 店铺分类列表 |
| 笔记 | POST | `/blog` | 登录 | 发布笔记 |
| 笔记 | PUT | `/blog/like/{id}` | 登录 | 点赞/取消点赞切换 |
| 笔记 | GET | `/blog/hot` | 公开 | 热门笔记 |
| 笔记 | GET | `/blog/{id}` | 登录 | 笔记详情 |
| 笔记 | GET | `/blog/likes/{id}` | 登录 | 点赞前 5 位用户 |
| 笔记 | GET | `/blog/of/me` | 登录 | 我的笔记 |
| 笔记 | GET | `/blog/of/user` | 登录 | 指定用户笔记 |
| 笔记 | GET | `/blog/of/follow` | 登录 | 关注流游标分页 |
| 关注 | PUT | `/follow/{id}/{isFollow}` | 登录 | 关注或取关 |
| 关注 | GET | `/follow/or/not/{id}` | 登录 | 是否已关注 |
| 关注 | GET | `/follow/common/{id}` | 登录 | 共同关注 |
| 上传 | POST | `/upload/blog` | 公开 | 上传笔记图片 |
| 上传 | GET | `/upload/blog/delete` | 公开 | 删除笔记图片 |
| 优惠券 | POST | `/voucher` | 当前实现公开 | 新增普通券 |
| 优惠券 | POST | `/voucher/seckill` | 当前实现公开 | 新增秒杀券 |
| 优惠券 | GET | `/voucher/list/{shopId}` | 当前实现公开 | 店铺优惠券列表 |
| 秒杀订单 | POST | `/voucher-order/seckill/{id}` | 登录 | 提交秒杀请求 |
| 秒杀订单 | GET | `/voucher-order/status/{id}` | 登录 | 查询异步订单状态 |
| 秒杀订单 | POST | `/voucher-order/mock-pay/{id}/{success}` | 登录 | 演示用模拟支付 |

## 4. 用户与登录

### 4.1 发送验证码

`POST /user/code` · 公开

查询参数：

| 参数 | 必填 | 类型 | 说明 |
| --- | --- | --- | --- |
| `phone` | 是 | string | 中国大陆手机号 |

示例：`POST /user/code?phone=13800138000`

成功时 `data` 为 `null`。验证码不通过接口返回，也不接入真实短信服务，而是输出在后端 debug 日志；有效期为 2 分钟。

### 4.2 登录/自动注册

`POST /user/login` · 公开

```json
{ "phone": "13800138000", "code": "123456" }
```

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `phone` | 是 | 手机号 |
| `code` | 是 | 验证码 |
| `password` | 否 | DTO 中虽有此字段，但当前服务端不使用密码登录 |

成功响应：`data` 为 token 字符串。该手机号不存在时会自动创建昵称为 `user_...` 的用户。

### 4.3 获取当前用户

`GET /user/me` · 登录

响应 `data`：[`UserDTO`](#21-userdto公开用户摘要)。

### 4.4 获取用户信息

`GET /user/{id}` · 登录

响应 `data`：[`UserDTO`](#21-userdto公开用户摘要)；用户不存在时返回成功且 `data: null`。

`GET /user/info/{id}` · 登录

响应 `data` 为扩展资料：

```json
{
  "userId": 1,
  "city": "上海",
  "introduce": "热爱探店",
  "fans": 10,
  "followee": 3,
  "gender": true,
  "birthday": "2000-01-01",
  "credits": 0,
  "level": false
}
```

资料不存在时同样返回成功且 `data: null`。`gender` 中 `true` 为男、`false` 为女；`level` 在实体中为布尔值，当前代码没有进一步的会员等级枚举。

### 4.5 签到

`POST /user/sign` · 登录

标记当前自然日已签到，成功时 `data: null`。重复调用是幂等的。

`GET /user/sign/count` · 登录

返回 `data: number`，表示截至今天的连续签到天数；如果今天未签到，返回 `0`。

### 4.6 登出（未实现）

`POST /user/logout` · 登录

当前固定返回 `success: false`，服务端不会销毁 token。前端暂时只能清除本地 token；如需真正退出，需要后端补充 Redis token 删除逻辑。

## 5. 店铺与分类

### 5.1 获取分类列表

`GET /shop-type/list` · 公开

响应 `data` 为按 `sort` 升序的数组：

```json
[{ "id": 1, "name": "美食", "icon": "/types/ms.png", "sort": 1 }]
```

### 5.2 获取店铺详情

`GET /shop/{id}` · 公开

响应 `data`：[`Shop`](#22-shop店铺)。店铺不存在时业务失败：`errorMsg: "店铺不存在！"`。

### 5.3 分类/附近店铺查询

`GET /shop/of/type` · 公开

| 参数 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `typeId` | 是 | - | 店铺分类 ID |
| `current` | 否 | `1` | 页码，从 1 开始 |
| `x` | 否 | - | 经度；须与 `y` 同时传入 |
| `y` | 否 | - | 纬度；须与 `x` 同时传入 |

- 未同时传入坐标：按分类查询，每页 5 条，未指定排序。
- 同时传入坐标：在该分类的 Redis 地理索引中查询 5,000 米内店铺，按距离升序，每页 5 条，返回项含 `distance`（米）。
- 查询结果为空时 `data: []`。

示例：`GET /shop/of/type?typeId=1&current=1&x=121.481&y=31.236`

### 5.4 按名称搜索店铺

`GET /shop/of/name` · 公开

| 参数 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `name` | 否 | - | 店铺名模糊匹配关键字；不传时查询全部 |
| `current` | 否 | `1` | 页码，从 1 开始 |

响应 `data` 为 `Shop[]`，每页 10 条。

### 5.5 新增店铺

`POST /shop` · 登录

请求体为 `Shop`，最小示例：

```json
{
  "name": "示例餐厅",
  "typeId": 1,
  "images": "/blogs/a.jpg,/blogs/b.jpg",
  "area": "陆家嘴",
  "address": "示例路 1 号",
  "x": 121.5,
  "y": 31.2,
  "avgPrice": 88,
  "openHours": "10:00-22:00"
}
```

成功时 `data` 为新建店铺 ID。当前仅验证登录态，未实现商家/管理员角色限制；前端管理页须自行限制入口，但不能将其视为安全边界。

### 5.6 更新店铺

`PUT /shop` · 登录

请求体为要更新的 `Shop` 字段，`id` 必填；可提交部分字段。

```json
{ "id": 1, "name": "新店名", "openHours": "09:00-23:00" }
```

成功时 `data: null`。未提供 `id` 时返回业务失败：`店铺id不能为空`。

## 6. 笔记与关注

### 6.1 发布笔记

`POST /blog` · 登录

```json
{
  "shopId": 1,
  "title": "周末探店",
  "images": "/blogs/1/2/a.jpg,/blogs/1/2/b.jpg",
  "content": "环境很好，推荐。"
}
```

成功时 `data` 为笔记 ID。`userId` 由当前登录用户强制写入，前端不应传递或信任该字段。发布成功后会向该作者的粉丝写入关注流。

### 6.2 热门笔记

`GET /blog/hot` · 公开

| 参数 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `current` | 否 | `1` | 页码，从 1 开始 |

响应 `data`：`Blog[]`，按 `liked` 降序，每页 10 条。携带有效 token 调用时，每项会带当前用户的 `isLike` 状态；匿名调用该字段可能缺失。

### 6.3 笔记详情与用户笔记

`GET /blog/{id}` · 登录

返回单个 `Blog`，附带作者 `name`、`icon` 及当前用户 `isLike`。笔记不存在时业务失败：`笔记不存在！`。

`GET /blog/of/me?current=1` · 登录

返回当前用户的 `Blog[]`，每页 10 条。该接口只直接查询表数据，因此返回项不会补充 `name`、`icon`、`isLike`。

`GET /blog/of/user?id={userId}&current=1` · 登录

返回指定用户的 `Blog[]`，每页 10 条；同样不补充作者信息和点赞状态。

### 6.4 点赞与点赞用户

`PUT /blog/like/{id}` · 登录

无请求体。此接口是**切换**操作：未点赞则点赞，已点赞则取消点赞；响应成功时 `data: null`。前端应以请求后重新拉取详情/热榜的 `isLike` 和 `liked` 为准，避免因重复点击导致状态相反。

`GET /blog/likes/{id}` · 登录

返回 `data: UserDTO[]`，为最早点赞的最多 5 位用户（Redis ZSet 正序），并非点赞数最高的用户。

### 6.5 关注流（滚动分页）

`GET /blog/of/follow?lastId={timestamp}&offset={offset}` · 登录

| 参数 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `lastId` | 是 | - | 首次加载传当前时间戳（毫秒），后续传上一响应的 `data.minTime` |
| `offset` | 否 | `0` | 首次传 `0`，后续传上一响应的 `data.offset` |

响应 `data`：[`ScrollResult`](#25-scrollresult关注流游标分页)。若没有更多数据，当前实现返回 `data: null`（不是空 `ScrollResult`），前端需要兼容。

### 6.6 关注关系

`PUT /follow/{id}/{isFollow}` · 登录

| 路径参数 | 类型 | 说明 |
| --- | --- | --- |
| `id` | number | 被关注用户 ID |
| `isFollow` | boolean | `true` 关注，`false` 取关 |

成功时 `data: null`。当前没有阻止关注自己，也没有保证重复关注的幂等性，前端应避免重复提交。

`GET /follow/or/not/{id}` · 登录

返回 `data: boolean`，表示当前用户是否关注该用户。

`GET /follow/common/{id}` · 登录

返回 `data: UserDTO[]`，表示当前用户与路径中用户的共同关注列表；无共同关注时返回 `[]`。

## 7. 图片上传

### 7.1 上传笔记图片

`POST /upload/blog` · 公开

请求为 `multipart/form-data`：

| 字段 | 必填 | 类型 | 说明 |
| --- | --- | --- | --- |
| `file` | 是 | file | 图片文件 |

成功时 `data` 为相对图片路径，例如：

```json
{ "success": true, "errorMsg": null, "data": "/blogs/2/13/uuid.jpg", "total": null }
```

当前实现未校验 MIME、扩展名、文件大小，也没有上传鉴权；生产环境应在后端补齐校验、鉴权和对象存储策略。

### 7.2 删除笔记图片

`GET /upload/blog/delete?name=/blogs/2/13/uuid.jpg` · 公开

成功时 `data: null`。该接口使用 GET 执行删除且没有鉴权，属于当前实现的安全风险；前端仅在编辑取消图片时使用，生产发布前建议改为受保护的 `DELETE` 接口。

## 8. 优惠券与秒杀订单

### 8.1 新增普通券

`POST /voucher` · 当前实现公开

请求体为 `Voucher`，示例：

```json
{
  "shopId": 1,
  "title": "50 元代金券",
  "subTitle": "满 100 可用",
  "rules": "仅限工作日使用",
  "payValue": 50,
  "actualValue": 100,
  "type": 0,
  "status": 1
}
```

成功时 `data` 为优惠券 ID。

### 8.2 新增秒杀券

`POST /voucher/seckill` · 当前实现公开

请求体同时包含 `Voucher` 字段和秒杀扩展字段：

```json
{
  "shopId": 1,
  "title": "秒杀代金券",
  "payValue": 10,
  "actualValue": 100,
  "type": 1,
  "stock": 100,
  "beginTime": "2026-07-11T10:00:00",
  "endTime": "2026-07-11T12:00:00"
}
```

后端会创建基础券、秒杀券库存记录，并初始化 Redis 库存。成功时 `data` 为优惠券 ID。

### 8.3 查询店铺优惠券

`GET /voucher/list/{shopId}` · 当前实现公开

返回 `data: Voucher[]`。秒杀券会包含 `stock`、`beginTime`、`endTime`；普通券这些字段为 `null` 或不出现。

### 8.4 提交秒杀

`POST /voucher-order/seckill/{id}` · 登录

无请求体，`id` 是秒杀优惠券 ID。成功并不代表订单已落库，而是代表 Redis 预扣库存和消息投递成功：

```json
{
  "success": true,
  "data": { "orderId": 123456789, "statusCode": 10, "statusText": "排队中" }
}
```

前端拿到 `orderId` 后应轮询下一接口，直到状态为 12–15。库存不足或重复下单时 `success: false`，`errorMsg` 分别为“库存不足”或“不能重复下单”。

### 8.5 查询秒杀订单状态

`GET /voucher-order/status/{id}` · 登录

`id` 是上一步返回的 `orderId`。响应 `data` 为[秒杀订单状态](#26-秒杀订单状态)。建议每 1–2 秒轮询，收到终态（13、14、15）即停止。

当前实现仅校验“已登录”，**没有校验订单归属**；前端必须只查询自己保存的订单号，后端上线前应补充归属校验。

### 8.6 模拟支付（仅开发演示）

`POST /voucher-order/mock-pay/{id}/{success}` · 登录

| 路径参数 | 类型 | 说明 |
| --- | --- | --- |
| `id` | number | 订单 ID |
| `success` | boolean | `true` 将待支付订单改为已支付；`false` 仅返回“支付失败”，不修改订单 |

示例：`POST /voucher-order/mock-pay/123456789/true`

成功时 `data` 为“支付成功”或“支付失败”字符串。它不是实际支付通道，且未校验订单归属；只应用于开发联调。

## 9. 当前未提供的页面接口

以下实体或页面需求在项目中尚没有对应 Controller 方法，前端应先隐藏入口、使用静态数据，或与后端约定新增接口：

- 笔记评论：`BlogCommentsController` 为空，没有评论列表、发布、回复、删除、点赞等 API。
- 用户资料编辑、头像修改、密码登录与真正登出：均未实现。
- 店铺删除、优惠券编辑/删除、订单列表和订单详情：未实现。
- 真实支付、支付回调、退款、券核销：未实现；只有模拟支付和超时取消流程。
- 角色/商家/管理员授权：未实现。当前新增店铺、优惠券管理、上传和图片删除等高风险接口缺少相应权限控制。

## 10. 前端联调建议

1. 在请求拦截器中统一附加 `authorization`；收到 `401` 时清除 token 并跳转登录页。
2. 在响应拦截器中先判断 HTTP 状态，再判断 `body.success`。不要把 HTTP `200` 当作成功。
3. 对笔记点赞使用“请求中禁用按钮 + 请求后刷新数据”的方式，因为接口是 toggle，而不是显式的 `like/unlike`。
4. 秒杀页保存 `orderId` 并按状态码轮询；页面离开或得到终态时取消定时器。
5. 图片选择后先调用上传接口，将返回路径组成逗号分隔字符串，再提交笔记；在开发环境配置静态资源域名，例如 `VITE_IMAGE_BASE_URL`。
6. 管理功能即使当前能直接调用，也应在前端按角色/环境隐藏，并将接口权限缺口列入后端上线前改造项。

