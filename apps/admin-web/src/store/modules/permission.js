import { constantRoutes } from '@/router/routes'
import { checkRouteAccess } from '@/utils/permission'

export function filterRoutes(routes, roles, permissions, menuPaths) {
  return routes
    .filter(route => checkRouteAccess(route, roles, permissions, menuPaths))
    .map(route => {
      if (!route.children) {
        return { ...route }
      }
      return {
        ...route,
        children: filterRoutes(route.children, roles, permissions, menuPaths)
      }
    })
    .filter(route => !route.children || route.children.length > 0)
}

function applyMenuSettings(routes, settings) {
  return routes
    .map(route => {
      const key = route.name || route.path
      const config = settings[key] || {}
      const meta = route.meta || {}
      const hasMoreConfig = config.more !== undefined && config.more !== null
      const more = hasMoreConfig ? config.more === true : meta.more === true
      const next = {
        ...route,
        meta: {
          ...meta,
          title: config.title || meta.title,
          icon: config.icon || meta.icon,
          more
        }
      }

      if (route.children) {
        next.children = applyMenuSettings(route.children, settings)
      }

      //菜单排序优先使用本地配置，未配置时使用路由 meta 上的默认排序
      const hasCustomSort =
        config.sort !== undefined && config.sort !== null && config.sort !== ''

      return {
        route: next,
        hidden: config.hidden === true,
        sort: hasCustomSort ? Number(config.sort) || 0 : Number(meta.sort) || 0
      }
    })
    .filter(item => {
      if (item.hidden) {
        return false
      }
      return !item.route.children || item.route.children.length > 0
    })
    .sort((a, b) => a.sort - b.sort)
    .map(item => item.route)
}

//“更多管理”抽屉内的固定展示顺序
const MORE_MANAGE_NAMES = [
  'SystemRoleView',
  'SystemDictView',
  'SystemMenuView',
  'ShopView'
]

function getAccessibleChildren(rootState) {
  const roles = rootState.user ? rootState.user.roles : []
  const permissions = rootState.user ? rootState.user.permissions : []
  const menuPaths = rootState.user ? rootState.user.menuPaths : []
  const routes = filterRoutes(constantRoutes, roles, permissions, menuPaths)
  const rootRoute = routes.find(route => route.path === '/')
  return rootRoute && rootRoute.children ? rootRoute.children : []
}

const state = {}

const getters = {
  sidebarRoutes: (state, getters, rootState) => {
    const settings = rootState.menu ? rootState.menu.settings : {}
    const children = getAccessibleChildren(rootState)

    //归入“更多管理”的路由不在侧边栏展示
    return applyMenuSettings(children, settings).filter(
      route => !(route.meta && route.meta.more)
    )
  },
  moreManageRoutes: (state, getters, rootState) => {
    const settings = rootState.menu ? rootState.menu.settings : {}
    const children = getAccessibleChildren(rootState)
    const moreRoutes = applyMenuSettings(children, settings).filter(
      route => route.meta && route.meta.more
    )

    //默认四项保持固定顺序，其余手动移入的菜单按侧边栏排序追加
    const fixedRoutes = MORE_MANAGE_NAMES.map(name =>
      moreRoutes.find(route => route.name === name)
    ).filter(Boolean)
    const extraRoutes = moreRoutes.filter(
      route => !MORE_MANAGE_NAMES.includes(route.name)
    )

    return [...fixedRoutes, ...extraRoutes]
  }
}

export default {
  namespaced: true,
  state,
  getters
}
