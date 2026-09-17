import Vue from 'vue'
import VueRouter from 'vue-router'
import Layout from '@/layout/index.vue'
import Login from '@/views/login/index.vue'
import Dashboard from '@/views/dashboard/index.vue'
import NotFound from '@/views/error/404.vue'
import Unauthorized from '@/views/error/401.vue'
import Redirect from '@/views/redirect/index.vue'

Vue.use(VueRouter)

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
      }
    ]
  },
  {
    path: '*',
    redirect: '/404',
    meta: { hidden: true }
  }
]

const createRouter = () =>
  new VueRouter({
    scrollBehavior: () => ({ x: 0, y: 0 }),
    routes: constantRoutes
  })

const router = createRouter()

export function resetRouter() {
  const newRouter = createRouter()
  router.matcher = newRouter.matcher
}

export default router
