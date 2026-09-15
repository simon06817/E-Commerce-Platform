# E-Commerce-Platform

基于 Spring Boot 3 的电商后端项目，覆盖用户体系、商品、购物车、订单、支付模拟、AI 对话等完整业务闭环，并针对实习/校招项目场景做了安全、分层、测试、缓存和 CI 方面的工程化改进。

## 技术栈

- Java 17 + Spring Boot 3.3.6
- Spring Security + JWT（jjwt 0.12.6）+ BCrypt 密码加密
- MyBatis-Plus 3.5.15 + MySQL 8
- Spring Data Redis + Spring Cache（商品/分类缓存）
- RabbitMQ + 本地消息表（outbox）+ 幂等消费
- Flyway 数据库版本管理
- Knife4j / OpenAPI 3 API 文档
- 阿里云 DashScope（通义千问）AI 接口
- JUnit 5 + Mockito + H2 + MockMvc 测试
- Maven + GitHub Actions CI

## 三角色用户模型

系统支持三种登录身份，每种身份使用独立数据表存储：

| 角色 | 表 | 说明 |
| --- | --- | --- |
| 管理员 ADMIN | `user_admin` | 管理买家/卖家、商品分类 |
| 买家 BUYER | `user_buyer` | 购物车、下单、支付、订单管理 |
| 卖家 SELLER | `user_seller` | 管理自己的商品（`product.seller_id` 归属） |

登录接口必须同时指定角色、用户名和密码，JWT 中携带 `role` 声明，接口通过 `@PreAuthorize` 校验权限。

## 架构与时序

### 系统架构

```mermaid
flowchart LR
    UI["Frontend (Vue/React)"] -->|"REST + JWT"| API["Java Spring Boot API"]
    UI -->|"/chat + JWT"| AGENT["Python Agent<br/>FastAPI + LangGraph"]
    API --> MYSQL[("MySQL<br/>E-Commerce_Platform")]
    API --> REDIS[("Redis<br/>Cache + Token")]
    API --> RABBIT[("RabbitMQ")]
    RABBIT --> CONSUMER["Java order event consumer"]
    CONSUMER --> MYSQL
    AGENT -->|"product/order/review/notification + JWT"| API
    AGENT --> CHROMA[("Chroma<br/>RAG vector store")]
    AGENT --> OLLAMA[("Ollama<br/>qwen3.5:0.8b")]
```

### 下单与 Outbox 时序

```mermaid
sequenceDiagram
    participant B as Buyer
    participant API as Java API
    participant DB as MySQL
    participant OB as order_outbox
    participant MQ as RabbitMQ
    participant C as Consumer
    B->>API: POST /api/orders (idempotencyKey)
    API->>DB: check idempotency key
    API->>DB: insert order + items + stock update (transaction)
    API->>OB: insert ORDER_CREATED in same transaction
    API-->>B: order response
    OB->>MQ: scheduled publisher sends pending event
    MQ->>C: ORDER_CREATED
    C->>DB: insert order_event_record (idempotent)
    C->>DB: create buyer notification
```

### JWT 刷新与退出时序

```mermaid
sequenceDiagram
    participant F as Frontend
    participant A as Java Auth API
    participant R as Redis
    F->>A: POST /api/auth/login
    A->>R: save refresh token (7 days)
    A-->>F: access token (30 min) + refresh token
    F->>A: POST /api/auth/refresh
    A->>R: validate and rotate refresh token
    A-->>F: new access + refresh token
    F->>A: POST /api/auth/logout
    A->>R: delete refresh token + blacklist access jti
    A-->>F: success
```

### Agent 对话时序

```mermaid
sequenceDiagram
    participant U as User/Frontend
    participant P as Python Agent
    participant J as Java API
    participant V as Chroma
    participant O as Ollama
    U->>P: POST /chat (JWT + message)
    P->>J: GET /api/auth/me
    P->>V: RAG knowledge + product description search
    P->>J: realtime product/order/notification facts
    P->>O: LangGraph StateGraph + ReAct tools
    O-->>P: answer or tool proposal
    P-->>U: answer (or confirmation prompt)
```

## 内置账号（密码均为 BCrypt 存储）

| 用户名 | 密码 | 角色 |
| --- | --- | --- |
| `admin` | `admin` | ADMIN |
| `buyer01` / `buyer02` | `123456` | BUYER |
| `seller01` / `seller02` | `123456` | SELLER |

## 数据库

`sql/schema.sql` 和 `sql/seed.sql` 提供完整建表与种子数据，目标库为 `E-Commerce_Platform`，包含：

`sql/demo_bulk_data.sql` 可批量生成 10 个商家（每个 10 个商品）、50 个买家的演示数据，商品在 5 个分类间平均分配。

- `user_admin` / `user_buyer` / `user_seller`
- `product_category` / `product`
- `cart`
- `order_info` / `order_item`（订单快照明细）

核心业务约定：

- 下单时从购物车生成订单，写 `order_info` + `order_item` 商品快照，扣减库存，清空购物车，整体事务包裹
- 下单幂等键 + 唯一索引，订单超时自动关单并恢复库存
- 订单事件（ORDER_CREATED/ORDER_PAID）走 outbox + RabbitMQ，消费侧唯一索引幂等
- 扣库存使用 SQL 级条件更新 `UPDATE product SET stock = stock - ? WHERE id = ? AND stock >= ?`，避免并发超卖
- 用户表、角色表启用 MyBatis-Plus 逻辑删除（`deleted` 字段）

> 订单模型按“整单状态流转”实现，演示场景以单卖家下单为主；多卖家拆单/明细级发货是后续演进方向。

## 主要接口

| 模块 | 路径 | 权限 |
| --- | --- | --- |
| 认证 | `POST /api/auth/login`、`POST /api/auth/register` | 公开 |
| 令牌 | `POST /api/auth/refresh`、`POST /api/auth/logout` | 公开 |
| 当前用户 | `GET /api/auth/me` | 已登录 |
| 商品 | `GET /api/products`、`GET /api/products/{id}` | 公开 |
| 商品管理 | `POST/PUT/DELETE /api/products/**` | SELLER（仅本人商品） |
| 分类 | `GET /api/categories/**` | 公开 |
| 分类管理 | `POST/PUT/DELETE /api/categories/**` | ADMIN |
| 购物车 | `/api/carts/**` | BUYER |
| 订单 | `/api/orders/**` | BUYER |
| 用户管理 | `/api/admin/users/**` | ADMIN |
| AI | `/api/ai/**` | 公开 |
| 上传 | `POST /api/upload/image` | 已登录 |
| 评价 | `POST /api/reviews`、`DELETE /api/reviews/{id}` | BUYER |
| 评价查询 | `GET /api/products/{id}/reviews`、`/summary` | 公开 |
| 个人资料 | `GET/PUT /api/profile`、`PUT /api/profile/password` | BUYER / SELLER |
| 评价回复 | `GET /api/seller/reviews`、`PUT /api/seller/reviews/{id}/reply` | SELLER |
| 管理员订单 | `GET /api/admin/orders`、`PUT /api/admin/orders/{id}/force-cancel` | ADMIN |
| 卖家统计 | `GET /api/seller/stats?range=today/7d/30d/all` | SELLER |
| 退货退款 | `/api/returns/**` | BUYER |
| 卖家退货处理 | `/api/seller/returns/**` | SELLER |

AI Agent（`ai-agent/`）：FastAPI + LangChain/LangGraph + Ollama，提供进程内短期会话记忆（每个用户一条会话，保留最近 20 条消息），回答前会先检索知识库、商品描述、实时库存价格和真实评价。

登录后返回 30 分钟 access token 和 7 天 refresh token；refresh token 每次刷新后轮换，退出登录会把 access token 写入 Redis 黑名单并删除 refresh token。

状态码统一由枚举维护并与数据库注释保持一致：`OrderStatusEnum`、`ProductStatusEnum`、`CategoryStatusEnum`、`ReturnStatusEnum`。

API 文档（本地启动后）：`http://localhost:8080/doc.html`

## 运行环境

敏感配置通过环境变量注入，参考根目录 `.env.example`。启动前先在本地设置以下环境变量：

| 环境变量 | 说明 | 示例 |
| --- | --- | --- |
| `MYSQL_HOST` | MySQL 地址 | `localhost` 或 `192.168.x.x` |
| `MYSQL_PORT` | MySQL 端口 | `3306` |
| `MYSQL_USERNAME` | MySQL 用户 | `root` |
| `MYSQL_PASSWORD` | MySQL 密码 | 你的密码 |
| `REDIS_HOST` | Redis 地址 | `localhost` 或 `192.168.x.x` |
| `REDIS_PORT` | Redis 端口 | `6379` |
| `REDIS_USERNAME` | Redis ACL 用户 | `default` 或你的用户 |
| `REDIS_PASSWORD` | Redis 密码 | 你的密码 |
| `JWT_SECRET` | JWT 签名密钥，至少 32 位随机串 | 长随机字符串 |
| `ALIYUN_AI_API_KEY` | 阿里云 DashScope API Key | 你的 Key |
| `UPLOAD_DIR` | 图片上传目录 | `./uploads` |

IDEA 中在 Run Configuration 的 `Environment variables` 里配置；命令行可先执行：

```bash
export MYSQL_HOST=localhost
export MYSQL_USERNAME=root
export MYSQL_PASSWORD=your-password
export JWT_SECRET=your-long-random-secret
```

启动前需要：

1. 在 VM MySQL 执行 `sql/schema.sql` 和 `sql/seed.sql`
2. 确认 Redis 可访问并已配置 ACL 用户
3. 设置上述环境变量后执行 `mvn spring-boot:run`（或直接运行 `Project01Application`）

## 测试

测试使用 H2（MySQL 兼容模式）+ MockMvc，不依赖外部 MySQL/Redis，可离线运行：

```bash
mvn test
```

覆盖范围：JWT 生成解析、三角色登录、购物车下单全流程、库存扣减、权限拒绝、管理员接口。

`RealMiddlewareIntegrationTest` 使用 Testcontainers 启动真实 MySQL 8.4、Redis 7.4 和
RabbitMQ 3.13，验证 Redis 缓存、MySQL 下单事务、Outbox 发布、消息消费和通知副作用。
本机需要运行 Docker；没有 Docker 时该组测试会自动跳过。

## 可观测性

Actuator 与 Micrometer 提供以下本地端点：

- `GET /actuator/health`
- `GET /actuator/info`
- `GET /actuator/metrics`
- `GET /actuator/prometheus`

每个 HTTP 请求都会生成或复用 `X-Trace-Id` 响应头，并在日志的
`logging.pattern.correlation` 中输出对应 `traceId`，便于串联请求日志。

## CI

`.github/workflows/maven.yml` 在 push / PR 时自动执行 JDK 17 + `mvn test`。
