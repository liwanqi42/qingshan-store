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
      const next = {
        ...route,
        meta: {
          ...meta,
          title: config.title || meta.title,
          icon: config.icon || meta.icon
        }
      }

      if (route.children) {
        next.children = applyMenuSettings(route.children, settings)
      }

      return {
        route: next,
        hidden: config.hidden === true,
        sort: Number(config.sort) || 0
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

const state = {}

const getters = {
  sidebarRoutes: (state, getters, rootState) => {
    const roles = rootState.user ? rootState.user.roles : []
    const permissions = rootState.user ? rootState.user.permissions : []
    const menuPaths = rootState.user ? rootState.user.menuPaths : []
    const settings = rootState.menu ? rootState.menu.settings : {}
    const routes = filterRoutes(constantRoutes, roles, permissions, menuPaths)
    const rootRoute = routes.find(route => route.path === '/')
    const children = rootRoute && rootRoute.children ? rootRoute.children : []

    return applyMenuSettings(children, settings)
  }
}

export default {
  namespaced: true,
  state,
  getters
}
