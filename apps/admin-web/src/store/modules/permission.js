import { constantRoutes } from '@/router'
import { checkPermission, checkRole } from '@/utils/permission'

function hasRouteAccess(route, roles, permissions) {
  const meta = route.meta || {}

  if (meta.roles && !checkRole(roles, meta.roles)) {
    return false
  }
  if (
    meta.permissions &&
    !checkRole(roles, 'admin') &&
    !checkPermission(permissions, meta.permissions)
  ) {
    return false
  }
  return true
}

export function filterRoutes(routes, roles, permissions) {
  return routes
    .filter(route => hasRouteAccess(route, roles, permissions))
    .map(route => {
      if (!route.children) {
        return { ...route }
      }
      return {
        ...route,
        children: filterRoutes(route.children, roles, permissions)
      }
    })
    .filter(route => !route.children || route.children.length > 0)
}

const state = {}

const getters = {
  sidebarRoutes: (state, getters, rootState) => {
    const roles = rootState.user ? rootState.user.roles : []
    const permissions = rootState.user ? rootState.user.permissions : []
    const routes = filterRoutes(constantRoutes, roles, permissions)
    const rootRoute = routes.find(route => route.path === '/')
    return rootRoute && rootRoute.children ? rootRoute.children : []
  }
}

export default {
  namespaced: true,
  state,
  getters
}
