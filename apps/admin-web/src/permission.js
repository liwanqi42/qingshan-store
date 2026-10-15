import router from './router'
import store from './store'
import {checkRouteAccess} from '@/utils/permission'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'

NProgress.configure({ showSpinner: false })

const APP_TITLE = process.env.VUE_APP_TITLE || '青山商店管理后台'

router.beforeEach((to, from, next) => {
  NProgress.start()

  const hasToken = store.getters['user/isLoggedIn']

  if (hasToken) {
    if (to.path === '/login') {
      const redirect = to.query.redirect
      const canRedirect =
        typeof redirect === 'string' &&
        redirect.startsWith('/') &&
        !redirect.startsWith('//') &&
        redirect !== '/login'
      next(canRedirect ? redirect : '/dashboard')
      return
    }
    // 首次登录或管理员重置密码后，必须先修改密码
    const mustChangePassword = store.getters['user/mustChangePassword']
    if (mustChangePassword && to.path !== '/force-password') {
      next('/force-password')
      return
    }
    if (!mustChangePassword && to.path === '/force-password') {
      next('/dashboard')
      return
    }

    const roles = store.getters['user/roles']
    const permissions = store.getters['user/permissions']
    const menuPaths = store.getters['user/menuPaths']
    const allowed = to.matched.every(route =>
      checkRouteAccess(route, roles, permissions, menuPaths)
    )

    if (!allowed) {
      next('/401')
      return
    }

    next()
    return
  }

  if (to.meta && to.meta.notNeedAuth) {
    next()
    return
  }

  next({
    path: '/login',
    query: { redirect: to.fullPath }
  })
})

router.afterEach(to => {
  NProgress.done()
  const title = to.meta && to.meta.title
  document.title = title ? `${title} - ${APP_TITLE}` : APP_TITLE
})

router.onError(() => {
  NProgress.done()
})
