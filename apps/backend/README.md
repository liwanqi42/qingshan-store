# 青山商城后端服务

青山商城后端服务，为管理端后台和微信小程序端提供 REST API。项目覆盖商品、分类、套装、购物车、订单、退款、浏览历史等核心电商流程。

管理后台与微信小程序同属本仓库，分别位于 `apps/admin-web` 和 `apps/applet`。完整项目说明见[根目录 README](../../README.md)，管理端使用教程见[后台管理系统使用教程](../../docs/后台管理系统使用教程.md)。

## 技术栈

- 后端：Spring Boot 2.7、MyBatis、PageHelper、Druid
- 数据库：MySQL
- 缓存：Redis
- 认证：JWT
- 文件存储：阿里云 OSS
- 支付：微信支付 V3（开发阶段使用模拟支付）

## 项目结构

```text
qingshan-common     公共常量、异常、工具和配置属性
qingshan-pojo       DTO、Entity、VO
qingshan-server     控制器、服务、Mapper、配置和定时任务
```

## 功能模块

- 管理端：员工管理、商品分类、商品与 SKU、套装管理、订单管理、退款处理、数据统计
- 用户端接口：微信/手机号登录、商品浏览、商品详情、购物车、下单、支付、订单查询、退款售后、浏览历史
- 基础设施：JWT 鉴权、Redis 缓存、OSS 图片上传、定时任务、统一异常处理

## 本地运行

1. 安装 JDK 8+、Maven、MySQL、Redis。
2. 在仓库根目录依次导入数据库脚本（开发环境）：

```bash
mysql -u root -p --default-character-set=utf8mb4 < mysql/schema.sql
mysql -u root -p --default-character-set=utf8mb4 < mysql/seed-base.sql
mysql -u root -p --default-character-set=utf8mb4 < mysql/seed-demo.sql
```

生产环境只执行 `schema.sql` 和 `seed-base.sql`，不要导入演示数据。

3. 在 `qingshan-server/src/main/resources/application-dev.yml` 中配置数据库、Redis、OSS 和微信参数（该文件已被 `.gitignore` 排除，需要手动创建）。
4. 启动后端：

```bash
mvn -pl qingshan-server -am clean package -DskipTests
java -jar qingshan-server/target/qingshan-server-1.0-SNAPSHOT.jar
```

或开发模式：

```bash
mvn -DskipTests install
cd qingshan-server
mvn spring-boot:run
```

## 开发计划

| 阶段 | 时间 | 目标 |
| --- | --- | --- |
| 第一阶段 | 2026-07 ~ 2026-08 | 搭建基础工程，完成员工、分类、商品、套装和 OSS 上传 |
| 第二阶段 | 2026-08 ~ 2026-09 | 完成用户登录、商品展示、商品详情、购物车和 SKU 库存 |
| 第三阶段 | 2026-09 ~ 2026-10 | 完成下单、支付、退款、用户端订单流程和上线前加固 |

## 说明

项目当前处于开发阶段，支付相关配置、域名、证书和密钥需要在部署环境中单独配置。

- 数据库结构：`mysql/schema.sql`（建库建表，可重复执行）
- 基础数据：`mysql/seed-base.sql`（分类 / 角色 / 管理员，所有环境）
- 演示数据：`mysql/seed-demo.sql`（含演示商品，仅用于开发）
- 开发重置：`mysql/drop-dev.sql`（删除全部业务表，禁止在生产执行）
- 接口导出文件：`docs/`
- 上线前检查清单：`SECURITY_TODO.md`（本地开发文件，可能未纳入版本库）
