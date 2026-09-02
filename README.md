# E-Commerce-Platform

基于 Spring Boot 3 的电商后端项目，覆盖用户体系、商品、购物车、订单、支付模拟、AI 对话等完整业务闭环，并针对实习/校招项目场景做了安全、分层、测试、缓存和 CI 方面的工程化改进。

## 技术栈

- Java 17 + Spring Boot 3.3.6
- Spring Security + JWT（jjwt 0.12.6）+ BCrypt 密码加密
- MyBatis-Plus 3.5.15 + MySQL 8
- Spring Data Redis + Spring Cache（商品/分类缓存）
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

## 内置账号（密码均为 BCrypt 存储）

| 用户名 | 密码 | 角色 |
| --- | --- | --- |
| `admin` | `admin` | ADMIN |
| `buyer01` / `buyer02` | `123456` | BUYER |
| `seller01` / `seller02` | `123456` | SELLER |

## 数据库

`sql/schema.sql` 和 `sql/seed.sql` 提供完整建表与种子数据，目标库为 `E-Commerce_Platform`，包含：

- `user_admin` / `user_buyer` / `user_seller`
- `product_category` / `product`
- `cart`
- `order_info` / `order_item`（订单快照明细）

核心业务约定：

- 下单时从购物车生成订单，写 `order_info` + `order_item` 商品快照，扣减库存，清空购物车，整体事务包裹
- 扣库存使用 SQL 级条件更新 `UPDATE product SET stock = stock - ? WHERE id = ? AND stock >= ?`，避免并发超卖
- 用户表、角色表启用 MyBatis-Plus 逻辑删除（`deleted` 字段）

## 主要接口

| 模块 | 路径 | 权限 |
| --- | --- | --- |
| 认证 | `POST /api/auth/login`、`POST /api/auth/register` | 公开 |
| 商品 | `GET /api/products`、`GET /api/products/{id}` | 公开 |
| 商品管理 | `POST/PUT/DELETE /api/products/**` | SELLER（仅本人商品） |
| 分类 | `GET /api/categories/**` | 公开 |
| 分类管理 | `POST/PUT/DELETE /api/categories/**` | ADMIN |
| 购物车 | `/api/carts/**` | BUYER |
| 订单 | `/api/orders/**` | BUYER |
| 用户管理 | `/api/admin/users/**` | ADMIN |
| AI | `/api/ai/**` | 公开 |
| 上传 | `POST /api/upload/image` | 已登录 |

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

## CI

`.github/workflows/maven.yml` 在 push / PR 时自动执行 JDK 17 + `mvn test`。
