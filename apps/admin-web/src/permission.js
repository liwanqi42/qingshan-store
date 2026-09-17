import router from './router'
import store from './store'
import { checkRouteAccess } from '@/utils/permission'
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
    const roles = store.getters['user/roles']
    const permissions = store.getters['user/permissions']
    const allowed = to.matched.every(route =>
      checkRouteAccess(route, roles, permissions)
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
