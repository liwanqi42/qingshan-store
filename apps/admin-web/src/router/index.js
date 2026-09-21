import Vue from 'vue'
import VueRouter from 'vue-router'
import { constantRoutes } from './routes'

Vue.use(VueRouter)

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
