const Layout = () =>
  import(/* webpackChunkName: "layout" */ '@/layout/index.vue')
const Login = () =>
  import(/* webpackChunkName: "login" */ '@/views/login/index.vue')
const Dashboard = () =>
  import(/* webpackChunkName: "dashboard" */ '@/views/dashboard/index.vue')
const Statistics = () =>
  import(/* webpackChunkName: "statistics" */ '@/views/statistics/index.vue')
const CategoryView = () =>
  import(/* webpackChunkName: "category" */ '@/views/category/index.vue')
const ProductView = () =>
  import(/* webpackChunkName: "product" */ '@/views/product/index.vue')
const BundleView = () =>
  import(/* webpackChunkName: "bundle" */ '@/views/bundle/index.vue')
const StockOrderView = () =>
  import(/* webpackChunkName: "stock-order" */ '@/views/stock/index.vue')
const StockLedgerView = () =>
  import(/* webpackChunkName: "stock-ledger" */ '@/views/stock/ledger.vue')
const OrderView = () =>
  import(/* webpackChunkName: "order" */ '@/views/order/index.vue')
const ShopView = () =>
  import(/* webpackChunkName: "shop" */ '@/views/shop/index.vue')
const SystemUser = () =>
  import(/* webpackChunkName: "system-user" */ '@/views/system/user/index.vue')
const SystemMenu = () =>
  import(/* webpackChunkName: "system-menu" */ '@/views/system/menu/index.vue')
const SystemRole = () =>
  import(/* webpackChunkName: "system-role" */ '@/views/system/role/index.vue')
const SystemDict = () =>
  import(/* webpackChunkName: "system-dict" */ '@/views/system/dict/index.vue')
const NotFound = () =>
  import(/* webpackChunkName: "error" */ '@/views/error/404.vue')
const Unauthorized = () =>
  import(/* webpackChunkName: "error" */ '@/views/error/401.vue')
const Redirect = () =>
  import(/* webpackChunkName: "redirect" */ '@/views/redirect/index.vue')

export const constantRoutes = [
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: { title: '登录', hidden: true, notNeedAuth: true }
  },
  {
    path: '/404',
    name: 'NotFound',
    component: NotFound,
    meta: { title: '404', hidden: true, notNeedAuth: true }
  },
  {
    path: '/401',
    name: 'Unauthorized',
    component: Unauthorized,
    meta: { title: '401', hidden: true, notNeedAuth: true }
  },
  {
    path: '/redirect',
    component: Layout,
    hidden: true,
    children: [
      {
        path: ':path(.*)',
        name: 'RedirectPage',
        component: Redirect,
        meta: { hidden: true }
      }
    ]
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'DashboardView',
        component: Dashboard,
        meta: { title: '工作台', icon: 'el-icon-s-home', affix: true }
      },
      {
        path: 'statistics',
        name: 'StatisticsView',
        component: Statistics,
        meta: {
          title: '数据统计',
          icon: 'el-icon-s-data',
          roles: ['admin']
        }
      },
      {
        path: 'order',
        name: 'OrderView',
        component: OrderView,
        meta: { title: '订单管理', icon: 'el-icon-s-order', roles: ['admin'] }
      },
      {
        path: 'stock/in',
        name: 'StockInView',
        component: StockOrderView,
        meta: {
          title: '入库管理',
          icon: 'el-icon-download',
          roles: ['admin'],
          stockType: 1
        }
      },
      {
        path: 'stock/out',
        name: 'StockOutView',
        component: StockOrderView,
        meta: {
          title: '出库管理',
          icon: 'el-icon-upload2',
          roles: ['admin'],
          stockType: 2
        }
      },
      {
        path: 'stock/ledger',
        name: 'StockLedgerView',
        component: StockLedgerView,
        meta: {
          title: '库存台账',
          icon: 'el-icon-notebook-2',
          roles: ['admin']
        }
      },
      {
        path: 'category',
        name: 'CategoryView',
        component: CategoryView,
        meta: { title: '分类管理', icon: 'el-icon-menu', roles: ['admin'] }
      },
      {
        path: 'product',
        name: 'ProductView',
        component: ProductView,
        meta: { title: '商品管理', icon: 'el-icon-s-goods', roles: ['admin'] }
      },
      {
        path: 'bundle',
        name: 'BundleView',
        component: BundleView,
        meta: { title: '套餐管理', icon: 'el-icon-box', roles: ['admin'] }
      },
      {
        path: 'system/user',
        name: 'SystemUserView',
        component: SystemUser,
        meta: { title: '员工管理', icon: 'el-icon-s-custom' }
      },
      {
        path: 'system/menu',
        name: 'SystemMenuView',
        component: SystemMenu,
        meta: {
          title: '菜单管理',
          icon: 'el-icon-s-grid',
          roles: ['admin']
        }
      },
      {
        path: 'system/role',
        name: 'SystemRoleView',
        component: SystemRole,
        meta: {
          title: '权限管理',
          icon: 'el-icon-s-check',
          roles: ['admin']
        }
      },
      {
        path: 'system/dict',
        name: 'SystemDictView',
        component: SystemDict,
        meta: {
          title: '字典管理',
          icon: 'el-icon-collection',
          roles: ['admin']
        }
      },
      {
        path: 'shop',
        name: 'ShopView',
        component: ShopView,
        meta: { title: '店铺设置', icon: 'el-icon-s-shop', roles: ['admin'] }
      }
    ]
  },
  {
    path: '*',
    redirect: '/404',
    meta: { hidden: true }
  }
]
