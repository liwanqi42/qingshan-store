# 青山商城后端服务

青山商城后端服务，为管理端后台和微信小程序端提供 REST API。项目覆盖商品、分类、套装、购物车、订单、退款、浏览历史等核心电商流程。

管理后台与微信小程序同属本仓库，分别位于 `apps/admin-web` 和 `apps/applet`。完整项目说明见[根目录 README](../../README.md)，管理端使用教程见[后台管理系统使用教程](../../docs/后台管理系统使用教程.md)。

## 技术栈

- 后端：Spring Boot 2.7.18、MyBatis、PageHelper 5.3.3、Druid 1.2.28、fastjson2
- 数据库：MySQL 8（驱动 8.4.0）
- 缓存：Redis
- 认证：JWT（jjwt 0.11.5，密钥至少 32 字节）
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

1. 安装 JDK 17+、Maven、MySQL、Redis。
2. 在仓库根目录依次导入数据库脚本（开发环境）：

```bash
mysql -u root -p --default-character-set=utf8mb4 < mysql/schema.sql
mysql -u root -p --default-character-set=utf8mb4 < mysql/seed-base.sql
mysql -u root -p --default-character-set=utf8mb4 < mysql/seed-demo.sql
```

生产环境只执行 `schema.sql` 和 `seed-base.sql`，不要导入演示数据。

已有数据的库不要重复执行 `schema.sql`（它不会修改已存在的表），缺列 / 索引时需手工执行 `ALTER`。

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

运行测试：

```bash
mvn test
```

测试不依赖 MySQL / Redis；回归范围与 1 万订单量级性能冒烟见：
- [测试与回归清单](../../docs/测试与回归清单.md)
- [性能基线测试](../../docs/性能基线测试.md)

