# 青山商店（Qingshan Store）

青山商店是一套面向小型零售 / 门店电商的全栈项目，包含 **微信小程序商城**、**Vue 管理后台** 和 **Spring Boot 后端服务**。系统覆盖商品与 SKU、套餐、购物车、下单支付、订单履约、售后退款、库存出入库、经营报表、员工与角色权限等完整业务链路。

> 当前项目处于开发 / 演示阶段：支付默认使用**模拟支付**，管理端的菜单配置和字典配置暂存在浏览器本地。上线前请务必阅读本文的[安全与上线检查](#安全与上线检查)和 `apps/backend/SECURITY_TODO.md`（如本地存在）。

- 管理后台使用教程：[docs/后台管理系统使用教程.md](docs/后台管理系统使用教程.md)
- 管理端接口导出：[docs/青山商店-管理端接口.json](docs/青山商店-管理端接口.json)
- 用户端接口导出：[docs/青山商店-用户端接口.json](docs/青山商店-用户端接口.json)

---

## 目录

- [功能概览](#功能概览)
- [技术栈](#技术栈)
- [仓库结构](#仓库结构)
- [系统架构](#系统架构)
- [快速开始](#快速开始)
- [默认账号与端口](#默认账号与端口)
- [核心业务说明](#核心业务说明)
- [配置说明](#配置说明)
- [接口约定与接口文档](#接口约定与接口文档)
- [构建与部署](#构建与部署)
- [常见问题](#常见问题)
- [安全与上线检查](#安全与上线检查)
- [开发约定](#开发约定)

---

## 功能概览

### 管理后台（`apps/admin-web`）

| 模块 | 能力 |
| --- | --- |
| 登录与账号 | 员工登录、退出、修改密码、记住账号、登录失效统一跳转 |
| 工作台 | 今日营业额、有效订单、完成率、客单价、新增用户；订单 / 商品 / 套餐总览，每分钟自动刷新 |
| 数据统计 | 营业额、用户、订单趋势图，销量 Top10；按日期筛选，导出近 30 天运营报表 |
| 订单管理 | 订单查询与详情、接单、拒单、取消、派送、完成；售后退款审核、拒绝、确认收货退款 |
| 入库 / 出库 | 待审核单据、扫码/编号快速录入、多规格选择、编辑删除、审核、打印、Excel 导出 |
| 库存台账 | 手工出入库产生的库存流水，按类型 / 单据 / 商品 / 日期查询与导出 |
| 分类管理 | 商品分类与套餐分类，排序、启停、删除保护 |
| 商品管理 | 商品编号、分类、价格、封面 / 轮播 / 详情图、多规格 SKU、价格库存、上下架、批量删除 |
| 套餐管理 | 套餐分类、价格、图片、描述、包含商品与份数、上下架 |
| 员工管理 | 员工列表、新增、编辑、启停、详情；管理员专属写操作 |
| 权限管理 | 角色增删改、启停、菜单权限分配、内置角色恢复 |
| 菜单管理 | 菜单名称、图标、排序、显示 / 隐藏、移入「更多管理」（本地配置） |
| 字典管理 | 字典类型 / 字典项、标签样式、排序、恢复默认（本地配置） |
| 店铺管理 | 营业中 / 打烊中切换，用户端首页展示营业状态（当前版本下单接口未强制校验） |
| 实时提醒 | WebSocket 来单提醒、登录后待接单提醒、用户催发货提醒 |

### 微信小程序（`apps/applet`）

- 首页轮播 / 分类入口 / 商品推荐，支持搜索、商品列表和分类筛选
- 商品详情：轮播图、详情长图、规格选择、SKU 价格与库存、加入购物车、立即购买
- 购物车：数量修改、删除、结算
- 下单结算：收货地址、订单备注、金额计算、提交订单
- 支付：开发环境模拟支付；已预留微信支付 V3 接口
- 订单：待付款 / 待发货 / 待收货 / 已完成 / 退款售后、取消、确认收货、催发货、再来一单
- 售后：按发货状态申请仅退款 / 退货退款 / 换货 / 补寄，填写退货物流，查看售后进度
- 我的：登录 / 注册、订单角标、优惠券页面（后端接口尚未实现）、收货地址、浏览历史、客服电话
- 登录：手机号密码登录、注册；后端保留微信登录接口，前端当前以密码登录为主

### 后端服务（`apps/backend`）

- 管理端 REST API 与用户端 REST API 分离，JWT 分开校验
- 商品 / SKU / 分类 / 套餐 / 购物车 / 订单 / 售后 / 地址簿 / 浏览历史
- 订单超时自动取消、库存扣减与回滚、支付回调幂等、退款处理
- 手工入库 / 出库单据与库存台账
- 工作台与经营报表、Excel 导出
- Redis 缓存、店铺状态、登录失败限流、定时任务分布式锁
- WebSocket 消息推送、阿里云 OSS 图片上传、统一异常处理

---

## 技术栈

| 层次 | 技术 |
| --- | --- |
| 管理后台 | Vue 2.7、Vue Router 3、Vuex 3、Element UI 2.15、Axios、ECharts 5、Sass、Vue CLI 5 |
| 小程序 | 微信原生小程序、WeUI、自定义登录授权组件 |
| 后端 | Spring Boot 2.7.3、MyBatis 2.2、PageHelper 1.3、Druid、JWT（jjwt）、Knife4j 3 |
| 数据库 | MySQL 8（兼容 5.7+），utf8mb4 |
| 缓存 / 中间件 | Redis、Spring Cache、WebSocket |
| 文件存储 | 阿里云 OSS |
| 支付 | 微信支付 V3（当前开发环境使用模拟支付） |
| 构建 | Maven 多模块、npm / Vue CLI |

---

## 仓库结构

```text
qingshan/
├── apps/
│   ├── admin-web/                 # Vue 2 管理后台
│   │   └── src/
│   │       ├── api/               # 接口封装
│   │       ├── components/        # 图表、字典标签、图片上传
│   │       ├── directive/         # v-permission 等指令
│   │       ├── layout/            # 侧边栏、导航、标签页、修改密码
│   │       ├── router/            # 路由表与路由守卫
│   │       ├── store/             # Vuex：用户、菜单、权限、标签页、字典
│   │       ├── utils/             # 请求、鉴权、校验、WebSocket、打印、下载
│   │       └── views/             # 业务页面
│   ├── applet/                    # 微信小程序（原生）
│   │   ├── pages/                 # 页面
│   │   ├── models/                # 接口模型
│   │   ├── components/            # 登录授权、数量输入
│   │   ├── utils/                 # 请求、token、图片、登录校验
│   │   └── config.js              # 后端地址、模拟支付、店铺信息
│   └── backend/                   # Spring Boot 多模块后端
│       ├── qingshan-common/       # 常量、异常、工具、配置属性
│       ├── qingshan-pojo/         # DTO、Entity、VO
│       └── qingshan-server/       # Controller、Service、Mapper、配置、定时任务
├── docs/                          # 管理端 / 用户端接口导出、使用教程
├── images/                        # 项目展示图片
├── mysql/
│   ├── schema.sql                 # 建库建表（所有环境，可重复执行）
│   ├── seed-base.sql              # 基础数据：分类 / 角色 / 管理员
│   ├── seed-demo.sql              # 演示商品（仅开发 / 演示环境）
│   └── drop-dev.sql               # 开发环境重置（删除全部业务表）
└── scripts/                       # 预留脚本目录
```

> 子项目内还有各自的 `README.md`。数据库初始化脚本现已随仓库提供；若子 README 与本文件描述不一致，以本文件为准。

---

## 系统架构

```text
┌──────────────────────┐        HTTP /api         ┌──────────────────────────┐
│  admin-web (Vue 2)   │ ───────────────────────▶ │                          │
│  浏览器管理后台       │        WebSocket /ws     │  qingshan-server         │
└──────────────────────┘ ◀─────────────────────── │  Spring Boot 2.7         │
                                                   │                          │
┌──────────────────────┐        HTTP               │  Controller / Service    │
│  applet (微信小程序)  │ ───────────────────────▶ │  MyBatis / 定时任务        │
└──────────────────────┘                          └───────────┬──────────────┘
                                                              │
                         ┌────────────────────────────────────┼────────────────────┐
                         ▼                                    ▼                    ▼
                    ┌─────────┐                          ┌─────────┐        ┌─────────────┐
                    │  MySQL  │                          │  Redis  │        │  阿里云 OSS  │
                    │  业务库  │                          │ 缓存/状态 │        │  商品图片    │
                    └─────────┘                          └─────────┘        └─────────────┘
```

- 管理端通过 `token` 请求头访问 `/admin/**`；小程序通过 `authentication` 请求头访问 `/user/**`。
- WebSocket 地址为 `/ws/{sid}`，管理端登录后连接，接收来单、待接单和催发货消息。
- 订单定时任务每分钟扫描一次，超时未支付订单默认 15 分钟后自动取消并回滚库存。
- Redis 同时用于商品缓存、店铺营业状态、登录失败限流和订单定时任务分布式锁。

---

## 快速开始

### 1. 环境要求

| 软件 | 建议版本 | 说明 |
| --- | --- | --- |
| JDK | 8 / 11 / 17 | Spring Boot 2.7，建议使用 LTS 版本 |
| Maven | 3.6+ | 后端多模块构建 |
| MySQL | 8.0（兼容 5.7+） | 数据库名 `qingshan-store` |
| Redis | 5.0+ | 缓存、店铺状态、限流、任务锁 |
| Node.js | 16+ / 18 LTS | 管理后台构建 |
| 微信开发者工具 | 最新稳定版 | 运行小程序 |
| 阿里云 OSS | — | 商品图片上传；不配置时商品图片上传会失败 |

### 2. 初始化数据库

数据库脚本按用途拆分，**生产环境只能执行 `schema.sql` + `seed-base.sql`**：

| 文件 | 用途 | 适用环境 |
| --- | --- | --- |
| `mysql/schema.sql` | 建库建表，不含 `DROP TABLE` 和种子数据 | 所有环境 |
| `mysql/seed-base.sql` | 基础数据：分类、角色、管理员账号 | 所有环境 |
| `mysql/seed-demo.sql` | 演示商品、SKU、商品图片 | 仅开发 / 演示 |
| `mysql/drop-dev.sql` | 删除全部业务表，用于重置开发库 | 仅开发 |

开发 / 演示环境（在仓库根目录执行）：

```bash
# 需要彻底重置时先执行 drop-dev.sql
mysql -u root -p --default-character-set=utf8mb4 < mysql/schema.sql
mysql -u root -p --default-character-set=utf8mb4 < mysql/seed-base.sql
mysql -u root -p --default-character-set=utf8mb4 < mysql/seed-demo.sql
```

生产环境只执行：

```bash
mysql -u root -p --default-character-set=utf8mb4 < mysql/schema.sql
mysql -u root -p --default-character-set=utf8mb4 < mysql/seed-base.sql
```

> ⚠ 默认管理员仍是 `admin / 123456`，上线前必须在管理端修改密码。
> ⚠ `seed-demo.sql` 和 `drop-dev.sql` 禁止在生产执行。

`schema.sql` 使用 `CREATE TABLE IF NOT EXISTS`，可以重复执行，但不会升级已有表结构；旧库缺列 / 索引时需手工 `ALTER`。原 `mysql/migrations/` 下的增量脚本已合并进 `schema.sql`，该目录不再使用。

脚本中的数据库名硬编码为 `qingshan-store`；如需修改，请同步替换各脚本中的 `CREATE DATABASE` / `USE`，并调整 `application-dev.yml` 中的 `qingshan.datasource.database`。

### 3. 启动 Redis

```bash
redis-server
# 如设置了密码，记下密码，后续填写到 application-dev.yml
```

店铺营业状态默认存在 Redis：首次启动且没有 `SHOP_STATUS` 时按「打烊中」处理，可以到管理后台「店铺管理」中切换。该状态目前用于小程序首页展示，后端下单接口尚未强制校验「打烊中」。

### 4. 配置并启动后端

后端开发配置 `application-dev.yml` 已被 `.gitignore` 排除，新克隆仓库后需要手动创建：

`apps/backend/qingshan-server/src/main/resources/application-dev.yml`

```yaml
qingshan:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    host: localhost
    port: 3306
    database: qingshan-store
    username: root
    password: your_mysql_password
  redis:
    host: localhost
    port: 6379
    password: your_redis_password
    database: 7
  alioss:
    endpoint: oss-cn-beijing.aliyuncs.com
    access-key-id: your_access_key_id
    access-key-secret: your_access_key_secret
    bucket-name: your_bucket_name
  jwt:
    admin-secret-key: replace-with-a-random-admin-secret
    user-secret-key: replace-with-a-random-user-secret
  order:
    # 开发环境开启模拟支付接口
    mock-pay-enabled: true
```

如需接入微信登录 / 真实微信支付，除通过环境变量注入 `QINGSHAN_WECHAT_*` 外，还需要补充小程序端调用和支付流程，详见[配置说明](#配置说明)。

构建并启动：

```bash
cd apps/backend
mvn -pl qingshan-server -am clean package -DskipTests
java -jar qingshan-server/target/qingshan-server-1.0-SNAPSHOT.jar
```

或开发模式：

```bash
cd apps/backend
mvn -DskipTests install
cd qingshan-server
mvn spring-boot:run
```

默认地址：`http://localhost:8080`。启动后可用公开接口验证，例如访问 `/user/shop/status`。

### 5. 启动管理后台

```bash
cd apps/admin-web
npm install
npm run serve
```

- 访问：`http://localhost:8081`
- 开发代理：`/api` → `http://localhost:8080`，`/ws/` → `ws://localhost:8080`
- 默认账号：`admin` / `123456`

### 6. 运行微信小程序

1. 用微信开发者工具「导入项目」，目录选择 `apps/applet`。
2. 在 `apps/applet/project.config.json` 中替换为自己的小程序 `appid`，或直接使用测试号。
3. 修改 `apps/applet/config.js`：

```js
export default {
  version: '0.1.0',
  // 真机调试必须使用电脑局域网 IP，不能用 localhost
  baseURL: 'http://192.168.1.8:8080',
  // 开发环境使用弹窗模拟支付
  mockPay: true,
  shopName: '青山商城',
  shareProfile: '青山商城',
  phoneNumber: '188700000000'
}
```

4. 开发者工具「详情 → 本地设置」勾选「不校验合法域名」，或为正式域名配置 HTTPS 后关闭该选项。
5. 编译运行。小程序当前使用手机号注册 / 密码登录，可直接完成浏览、下单和模拟支付流程；微信一键登录 UI 尚未接入。

> 仓库当前仍包含 `project.private.config.json`，正式项目建议移除并加入 `.gitignore`。

---

## 默认账号与端口

| 项目 | 默认值 | 说明 |
| --- | --- | --- |
| 后端端口 | `8080` | `server.port` |
| 管理后台开发端口 | `8081` | 被占用时 Vue CLI 会自动递增 |
| MySQL | `localhost:3306` | 数据库 `qingshan-store` |
| Redis | `localhost:6379` | 开发配置默认 DB 7 |
| 管理员账号 | `admin` / `123456` | 初始化脚本内置，上线前必须修改 |
| 新增员工初始密码 | `123456` | 由后端统一设置，员工首次登录后应修改 |
| 接口文档 | `http://localhost:8080/doc.html` | 仅非生产环境开启 |

---

## 核心业务说明

### 订单状态

| 数据库状态 | 管理端显示 | 用户端显示 | 说明 |
| --- | --- | --- | --- |
| `1` | 待付款 | 待付款 | 下单即占用库存，15 分钟未支付自动取消 |
| `2` | 待接单 | 待发货 | 支付成功，等待商家接单 / 拒单 |
| `3` | 待发货 | 待发货 | 商家已接单，等待派送 |
| `4` | 配送中 | 待收货 | 商家已派送 / 发货 |
| `5` | 已完成 | 已完成 | 商家点击完成或用户确认收货 |
| `6` | 已取消 | 交易关闭 | 用户取消、商家拒单、超时取消 |
| `7` | 退款 | 退款 / 售后 | 售后退款完成 |

支付状态：`0` 未支付、`1` 已支付、`2` 已退款。

### 售后状态

| 状态值 | 含义 |
| --- | --- |
| `0` | 无售后 |
| `1` | 售后申请中，商家可同意 / 拒绝 |
| `2` | 待买家退货（已发货的仅退款 / 退货退款） |
| `3` | 买家已退货，待商家收货 |
| `4` | 售后完成 |
| `5` | 售后申请被拒绝 |
| `6` | 售后关闭 |

售后类型：`1` 仅退款、`2` 退货退款、`3` 换货、`4` 补寄。

- 未发货订单只能申请仅退款；已发货订单可申请退货退款 / 换货 / 补寄。
- 未发货的仅退款：商家同意后直接退款并回滚库存。
- 已发货的仅退款 / 退货退款：商家同意后进入「待买家退货」，买家填写退货物流，商家确认收货后立即退款。
- 换货 / 补寄：商家同意后只完成售后，不发起退款。

### 库存与销量

- 下单时按 SKU 原子扣减库存，订单超时 / 取消 / 拒单 / 退款时回滚库存。
- 手工入库审核通过后增加库存，手工出库审核通过后扣减库存；任一行扣减失败则整单回滚。
- 出库审核通过后累加商品销量；已支付订单在支付成功时累加销量，退款 / 取消时扣减。
- **库存台账只记录手工出入库审核产生的流水**，销售下单、退款、商品编辑直接改库存不会写入台账。
- 商品编辑页可以直接修改 SKU 库存，但没有台账记录。需要可追溯的库存变动，请使用「入库管理 / 出库管理」。

### 实时提醒

管理端登录后自动连接 WebSocket：

| 消息类型 | 场景 | 标题 |
| --- | --- | --- |
| `1` | 用户支付成功产生新订单 | 来单提醒 |
| `2` | 管理端建立连接时存在待接单订单 | 待接单提醒 |
| `3` | 用户点击「催发货」 | 催发货提醒 |

前端每 30 秒发送心跳，断线 5 秒后自动重连；退出登录时主动断开。

---

## 配置说明

### 管理后台环境变量

`apps/admin-web/.env.development` / `.env.production`：

```env
VUE_APP_TITLE=青山商店管理后台
VUE_APP_BASE_API=/api
```

生产环境由 Nginx 将 `/api` 反向代理到后端，并转发 `/ws/` WebSocket。

### 后端开发配置

见[快速开始](#4-配置并启动后端)。开发配置 `application-dev.yml` 不会被提交，请勿把真实密钥写入仓库。

### 后端生产配置

生产启动使用 `prod` profile：

```bash
SPRING_PROFILES_ACTIVE=prod java -jar qingshan-server/target/qingshan-server-1.0-SNAPSHOT.jar
```

主要环境变量：

| 变量 | 说明 |
| --- | --- |
| `QINGSHAN_DB_HOST` / `QINGSHAN_DB_PORT` / `QINGSHAN_DB_NAME` | MySQL 连接 |
| `QINGSHAN_DB_USERNAME` / `QINGSHAN_DB_PASSWORD` | MySQL 账号密码 |
| `QINGSHAN_REDIS_HOST` / `QINGSHAN_REDIS_PORT` / `QINGSHAN_REDIS_PASSWORD` | Redis 连接 |
| `QINGSHAN_REDIS_DATABASE` | Redis 库号，默认 0 |
| `QINGSHAN_ALIOSS_ENDPOINT` / `QINGSHAN_ALIOSS_ACCESS_KEY_ID` / `QINGSHAN_ALIOSS_ACCESS_KEY_SECRET` / `QINGSHAN_ALIOSS_BUCKET_NAME` | OSS 配置 |
| `QINGSHAN_JWT_ADMIN_SECRET_KEY` / `QINGSHAN_JWT_USER_SECRET_KEY` | JWT 密钥，必须使用随机强密钥 |
| `QINGSHAN_JWT_ADMIN_TTL` / `QINGSHAN_JWT_USER_TTL` | Token 有效期（毫秒） |
| `QINGSHAN_SNOWFLAKE_WORKER_ID` | 雪花 ID 工作节点，多实例必须不同 |
| `QINGSHAN_WECHAT_APPID` / `QINGSHAN_WECHAT_SECRET` | 小程序登录 |
| `QINGSHAN_WECHAT_MCHID` / `QINGSHAN_WECHAT_MCH_SERIAL_NO` / `QINGSHAN_WECHAT_PRIVATE_KEY_FILE_PATH` / `QINGSHAN_WECHAT_API_V3_KEY` / `QINGSHAN_WECHAT_PAY_CERT_FILE_PATH` / `QINGSHAN_WECHAT_NOTIFY_URL` / `QINGSHAN_WECHAT_REFUND_NOTIFY_URL` | 真实微信支付 |
| `QINGSHAN_ORDER_TIMEOUT_MINUTES` | 未支付订单保留分钟数，默认 15 |
| `QINGSHAN_LOGIN_MAX_FAILURES` / `QINGSHAN_LOGIN_IP_MAX_FAILURES` / `QINGSHAN_LOGIN_LOCK_MINUTES` | 登录失败限流 |
| `SERVER_PORT` | 后端端口，默认 8080 |

生产 profile 已关闭模拟支付、接口文档页面和 SQL 参数日志。打包生产包时建议排除开发配置：

```bash
cd apps/backend
mvn -pl qingshan-server -am clean package -Pprod-package -DskipTests
```

---

## 接口约定与接口文档

### 统一响应

```json
{
  "code": 1,
  "msg": null,
  "data": {}
}
```

- `code = 1` 表示成功，其他值表示业务失败。
- 分页数据格式：`{ "total": 100, "records": [] }`。
- 管理端请求头：`token: <JWT>`。
- 用户端请求头：`authentication: <JWT>`。
- 未登录 / Token 失效：HTTP `401`。

### 接口文档

开发环境启动后端后访问：

- Knife4j 文档：`http://localhost:8080/doc.html`
- 管理端分组：`/admin/**`
- 用户端分组：`/user/**`

`docs/` 下的两个 JSON 是接口平台的导出文件，适合导入 Apifox / YApi 等工具查看，不会随代码自动更新。

---

## 构建与部署

### 后端

```bash
cd apps/backend
mvn -pl qingshan-server -am clean package -Pprod-package -DskipTests
```

产物：`apps/backend/qingshan-server/target/qingshan-server-1.0-SNAPSHOT.jar`

启动时固定时区并注入环境变量：

```bash
SPRING_PROFILES_ACTIVE=prod \
QINGSHAN_DB_HOST=127.0.0.1 \
QINGSHAN_DB_USERNAME=qingshan \
QINGSHAN_DB_PASSWORD='***' \
QINGSHAN_REDIS_HOST=127.0.0.1 \
QINGSHAN_JWT_ADMIN_SECRET_KEY='***' \
QINGSHAN_JWT_USER_SECRET_KEY='***' \
java -Duser.timezone=Asia/Shanghai -jar qingshan-server-1.0-SNAPSHOT.jar
```

### 管理后台

```bash
cd apps/admin-web
npm ci
npm run build
```

产物：`apps/admin-web/dist/`。

Nginx 参考配置：

```nginx
server {
    listen 80;
    server_name admin.example.com;

    root /var/www/qingshan/admin;
    index index.html;

    # 单页应用回退（当前使用 hash 路由，保留 history 模式兼容）
    location / {
        try_files $uri $uri/ /index.html;
    }

    # 后端 API
    location /api/ {
        proxy_pass http://127.0.0.1:8080/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        client_max_body_size 5m;
    }

    # WebSocket 实时提醒
    location /ws/ {
        proxy_pass http://127.0.0.1:8080/ws/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_read_timeout 3600s;
    }
}
```

### 小程序

- 在微信公众平台配置 `request` 合法域名（必须 HTTPS）。
- `apps/applet/config.js` 的 `baseURL` 改为正式 HTTPS 域名。
- `project.config.json` 的 `urlCheck` 改为 `true`（或删除该字段）。
- 正式环境关闭模拟支付：`config.js` 的 `mockPay` 改为 `false`，后端禁止 `mock-pay-enabled`。
- 上传体验版并完成登录、下单、支付、退款、地址、订单全流程回归。

---

## 常见问题

### 后端启动失败：数据库 / Redis 连接不上

- 检查 `application-dev.yml` 是否存在、账号密码和端口是否正确。
- 确认 MySQL 已创建 `qingshan-store` 数据库，且 JDBC URL 的时区参数正常。
- Redis 是必需依赖：店铺状态、缓存、限流、定时任务锁都依赖它。
- 如本机 MySQL 端口不是 3306，请同步修改配置。

### 管理后台登录失败 / 一直跳登录页

- 确认后端已启动，`http://localhost:8080` 可访问。
- 确认 `application-dev.yml` 中 `qingshan.jwt.admin-secret-key` 与当前登录时一致，修改密钥后旧 Token 全部失效。
- 同一账号连续失败 5 次或同一 IP 失败 20 次会锁定 15 分钟，请等待后重试或由运维清理限流数据。
- `401` 表示 Token 失效，重新登录即可。

### 图片上传失败

- 商品图片通过后端上传到阿里云 OSS，检查 `qingshan.alioss` 四项配置。
- 确认 Bucket 允许公开读取，否则管理端和小程序都无法展示图片。
- 确认 AccessKey 有对应 Bucket 的写入权限，且文件不超过 5MB。

### 小程序请求失败

- `config.js` 的 `baseURL` 必须能被开发机或手机访问；真机不能用 `localhost`。
- 开发者工具中开启「不校验合法域名」，或配置 HTTPS 合法域名。
- 检查电脑防火墙是否放行 8080 端口，手机与电脑是否在同一局域网。
- 登录成功后 token 存在本地；切换账号或后端修改 JWT 密钥后需要重新登录。

### 管理端菜单缺失

- 普通员工只能看到角色 `menu_paths` 中配置的路由名称，调整后需要**重新登录**。
- 超级管理员默认拥有全部菜单；如菜单被「菜单管理」隐藏，可在该页「恢复默认」。
- 菜单配置保存在当前浏览器 `localStorage`，不同浏览器 / 电脑之间不会同步。

### 实时提醒不弹出

- 确认后端已启动并允许 WebSocket，Nginx / 开发代理需正确转发 `/ws/`。
- 浏览器需允许桌面通知；消息以右下角 Element UI 通知展示，不依赖系统通知权限。
- 检查管理端是否已登录（未登录不会建立连接）。

### 模拟支付接口 404

- 生产 profile 已禁用模拟支付，`/user/order/mock-pay` 返回 404 属正常现象。
- 开发环境需同时满足：`application-dev.yml` 的 `mock-pay-enabled: true`，小程序 `config.js` 的 `mockPay: true`。

---

## 安全与上线检查

项目当前是开发 / 演示版本，上线前至少完成以下事项：

1. **修改默认密码**：`mysql/seed-base.sql` 仍会创建 `admin / 123456`，新增员工初始密码也是 `123456`，两者都必须修改；生产建议使用随机初始密码并强制首次登录修改。
2. **更换全部密钥**：数据库、Redis、OSS AccessKey、微信 secret、JWT 密钥全部轮换，通过环境变量注入，禁止提交到仓库。
3. **使用 prod profile**：关闭模拟支付、Knife4j 文档、SQL 参数日志；使用 `-Pprod-package` 打包，避免把 `application-dev.yml` 打进生产包。
4. **初始化脚本区分环境**：生产只执行 `mysql/schema.sql` + `mysql/seed-base.sql`；`seed-demo.sql`（演示商品）和 `drop-dev.sql`（`DROP TABLE`）禁止在生产执行。
5. **数据库最小权限**：应用账号不要使用 root，只授予业务库必要的 DML 权限；MySQL / Redis 不要暴露到公网。
6. **全站 HTTPS**：小程序正式环境必须使用备案 HTTPS 域名；Nginx 终止 TLS 并配置 HSTS。
7. **真实支付接入**：按 `SECURITY_TODO.md` 中的微信支付清单删除模拟支付代码；支付结果以微信回调为准，校验签名、金额和幂等。
8. **登录与鉴权加固**：JWT 有效期、登录失败锁定、密码哈希（当前已使用 BCrypt）、越权校验、接口分页上限。管理端角色 / 菜单目前主要控制前端可见性，员工和角色写操作、库存审核已在服务端校验管理员；商品、订单等写接口上线前应补齐后端权限校验。
9. **日志脱敏**：手机号、地址、身份证号、支付回调明文不要落盘；清理开发期历史日志。
10. **多实例部署**：雪花 `worker-id` 每个实例必须不同；定时任务已使用 Redis 分布式锁；生产建议补充监控、备份和回滚方案。

更完整的检查项请参考 `apps/backend/SECURITY_TODO.md`（该文件在开发环境本地存在，可能未纳入版本库）。

---

## 开发约定

- 管理端接口统一放在 `apps/admin-web/src/api/`，页面只通过 API 层访问后端。
- 后端分模块：`qingshan-common`（公共能力）、`qingshan-pojo`（数据模型）、`qingshan-server`（业务实现）。
- 管理端路由使用懒加载；新增页面需同步 `router/routes.js` 的 `name`，角色菜单权限按路由 `name` 匹配。
- 订单、库存等涉及并发和金额的操作放在 Service 事务中，并通过条件更新保证幂等。
- 提交前建议执行：

```bash
# 管理端
cd apps/admin-web
npm run lint
npm run build

# 后端
cd apps/backend
mvn -pl qingshan-server -am clean package -DskipTests
```

---

## License

`apps/applet` 目录附有 Apache License 2.0。若需将整个仓库对外开源，请补充根目录 `LICENSE` 并确认各子项目的授权范围。
