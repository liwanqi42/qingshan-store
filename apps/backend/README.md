# 青山商城

青山商城是一个面向微信小程序的电商系统，包含管理端后台和用户端小程序。项目覆盖商品、分类、套装、购物车、订单、退款、浏览历史等核心电商流程。

## 技术栈

- 后端：Spring Boot 2.7、MyBatis、PageHelper、Druid
- 数据库：MySQL
- 缓存：Redis
- 认证：JWT
- 文件存储：阿里云 OSS
- 支付：微信支付 V3（开发阶段使用模拟支付）
- 前端：微信小程序原生开发

## 项目结构

```text
qingshan-common     公共常量、异常、工具和配置属性
qingshan-pojo       DTO、Entity、VO
qingshan-server     控制器、服务、Mapper、配置和定时任务
wx-miniapp          微信小程序端
resources           数据库脚本和图片资源
```

## 功能模块

- 管理端：员工管理、商品分类、商品与 SKU、套装管理、订单管理、退款处理、数据统计
- 用户端：微信/手机号登录、商品浏览、商品详情、购物车、下单、支付、订单查询、退款售后、浏览历史
- 基础设施：JWT 鉴权、Redis 缓存、OSS 图片上传、定时任务、统一异常处理

## 本地运行

1. 安装 JDK 8+、Maven、MySQL、Redis。
2. 执行 `resources/qingshan-store-init.sql` 初始化数据库。
3. 在 `qingshan-server/src/main/resources/application-dev.yml` 中配置数据库、Redis、OSS 和微信参数。
4. 启动后端：

```bash
mvn -q -DskipTests compile
mvn -pl qingshan-server spring-boot:run
```

5. 使用微信开发者工具打开 `wx-miniapp`，修改 `config.js` 中的 `baseURL` 后运行小程序。

## 开发计划

| 阶段 | 时间 | 目标 |
| --- | --- | --- |
| 第一阶段 | 2026-07 ~ 2026-08 | 搭建基础工程，完成员工、分类、商品、套装和 OSS 上传 |
| 第二阶段 | 2026-08 ~ 2026-09 | 完成用户登录、商品展示、商品详情、购物车和 SKU 库存 |
| 第三阶段 | 2026-09 ~ 2026-10 | 完成下单、支付、退款、小程序订单流程和上线前加固 |

## 说明

项目当前处于开发阶段，支付相关配置、域名、证书和密钥需要在部署环境中单独配置。
