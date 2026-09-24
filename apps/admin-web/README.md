# 青山商店管理后台（admin-web）

青山商店管理端，基于 Vue 2 + Element UI 构建，覆盖商品、套餐、订单、报表、员工和系统配置等后台功能。

## 技术栈

- Vue 2.6 / Vue Router 3 / Vuex 3
- Element UI 2.15
- Axios、NProgress、ECharts
- Sass

## 主要功能

- 登录、退出登录、修改密码、登录失效统一处理
- 路由级权限、按钮级权限、按角色生成侧边栏
- 工作台今日经营数据和订单/商品/套餐总览
- 数据统计图表、日期筛选和运营报表导出
- 分类管理、商品管理（图片上传、SKU 价格库存）、套餐管理（商品选择）
- 订单查询、详情、接单、拒单、取消、派送、完成和售后审核
- 员工管理、菜单管理、角色管理、字典管理
- 店铺营业状态设置

## 目录结构

```text
src/
├── api/                 # 接口封装
├── components/          # 通用组件
│   ├── Charts/          # ECharts 容器
│   ├── DictTag/         # 字典标签
│   └── ImageUpload/     # 图片上传
├── directive/           # v-permission / v-role
├── layout/              # 后台布局、侧边栏、导航、标签页
├── router/              # 路由入口和路由表
├── store/               # Vuex 模块
├── styles/              # 全局样式和登录页样式
├── utils/               # 请求、鉴权、校验、格式化、下载
└── views/               # 页面
```

## 本地开发

```bash
npm install
npm run serve
```

- 开发服务器默认端口：`8081`
- `/api` 代理到：`http://localhost:8080`
- 如果端口被占用，Vue CLI 会自动递增端口

## 构建与校验

```bash
npm run lint
npm run build
```

## 环境变量

`.env.development` / `.env.production`：

```env
VUE_APP_TITLE=青山商店管理后台
VUE_APP_BASE_API=/api
```

生产环境可由 Nginx 将 `/api` 反向代理到后端服务。

## 实现说明

- 菜单、角色、字典目前为前端本地配置，使用 `localStorage` 持久化，已预留 API 层替换位置。
- 角色配置暂未绑定真实登录用户，登录用户的角色仍由登录态默认值提供。
- 路由页面已改为懒加载，ECharts 按需引入折线图、柱状图、Grid、Tooltip、Legend 和 Canvas 渲染器。
- 构建时仍有 Vue 2.7 的 `style0` export 警告，不影响运行；可后续通过固定 Vue 版本或调整 vue-loader 清理。
- Element UI 仍为全量引入，后续可改为按需引入进一步减小 vendor 体积。
