import Layout from '@/layout/index.vue'
import Login from '@/views/login/index.vue'
import Dashboard from '@/views/dashboard/index.vue'
import SystemUser from '@/views/system/user/index.vue'
import SystemMenu from '@/views/system/menu/index.vue'
import SystemRole from '@/views/system/role/index.vue'
import NotFound from '@/views/error/404.vue'
import Unauthorized from '@/views/error/401.vue'
import Redirect from '@/views/redirect/index.vue'

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
          title: '角色管理',
          icon: 'el-icon-s-check',
          roles: ['admin']
        }
      }
    ]
  },
  {
    path: '*',
    redirect: '/404',
    meta: { hidden: true }
  }
]
