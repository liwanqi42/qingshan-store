import router from './router'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'

NProgress.configure({ showSpinner: false })

const APP_TITLE = process.env.VUE_APP_TITLE || '青山商店管理后台'

router.beforeEach((to, from, next) => {
  NProgress.start()
  next()
})

router.afterEach(to => {
  NProgress.done()
  const title = to.meta && to.meta.title
  document.title = title ? `${title} - ${APP_TITLE}` : APP_TITLE
})

router.onError(() => {
  NProgress.done()
})
