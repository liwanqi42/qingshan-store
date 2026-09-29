import {
  getToken,
  setToken,
  getUserInfo,
  setUserInfo,
  getRoles,
  setRoles,
  getPermissions,
  setPermissions,
  getMenuPaths,
  setMenuPaths,
  clearAuth
} from '@/utils/auth'
import {
  login as loginApi,
  logout as logoutApi
} from '@/api/employee'

const token = getToken()
const cachedUserInfo = token ? getUserInfo() : null

// 旧登录态没有缓存角色时，先按升级前的管理员权限处理，
// 员工重新登录后以服务端返回的角色为准
function resolveCachedAuth() {
  if (!token) {
    return { roles: [], permissions: [], menuPaths: [] }
  }
  const roles = getRoles()
  if (!roles.length) {
    return { roles: ['admin'], permissions: ['*'], menuPaths: ['*'] }
  }
  return {
    roles,
    permissions: getPermissions(),
    menuPaths: getMenuPaths()
  }
}

const cachedAuth = resolveCachedAuth()

const state = {
  token: token || '',
  name: cachedUserInfo
    ? cachedUserInfo.name || cachedUserInfo.userName || ''
    : '',
  userInfo: cachedUserInfo,
  roleCode: cachedUserInfo ? cachedUserInfo.roleCode || '' : '',
  roles: cachedAuth.roles,
  permissions: cachedAuth.permissions,
  menuPaths: cachedAuth.menuPaths
}

const getters = {
  isLoggedIn: state => Boolean(state.token),
  name: state => state.name,
  roleCode: state => state.roleCode,
  roles: state => state.roles,
  permissions: state => state.permissions,
  menuPaths: state => state.menuPaths
}

const mutations = {
  SET_TOKEN(state, token) {
    state.token = token || ''
  },
  SET_USER_INFO(state, userInfo) {
    state.userInfo = userInfo || null
    state.name = userInfo ? userInfo.name || userInfo.userName || '' : ''
    state.roleCode = userInfo ? userInfo.roleCode || '' : ''
  },
  SET_ROLES(state, roles) {
    state.roles = Array.isArray(roles) ? roles : []
  },
  SET_PERMISSIONS(state, permissions) {
    state.permissions = Array.isArray(permissions) ? permissions : []
  },
  SET_MENU_PATHS(state, menuPaths) {
    state.menuPaths = Array.isArray(menuPaths) ? menuPaths : []
  },
  RESET_STATE(state) {
    state.token = ''
    state.name = ''
    state.userInfo = null
    state.roleCode = ''
    state.roles = []
    state.permissions = []
    state.menuPaths = []
  }
}

const actions = {
  login({ dispatch }, loginForm) {
    return loginApi(loginForm).then(res => {
      const data = res.data || {}

      if (!data.token) {
        throw new Error('登录响应缺少 token')
      }

      return dispatch('setLoginState', {
        token: data.token,
        userInfo: {
          id: data.id,
          userName: data.userName,
          name: data.name,
          roleCode: data.roleCode
        },
        roles: data.roles,
        permissions: data.permissions,
        menuPaths: data.menuPaths
      }).then(() => data)
    })
  },
  logout({ dispatch }) {
    return logoutApi().finally(() => {
      dispatch('clearLoginState')
      dispatch('tagsView/resetViews', null, { root: true })
    })
  },
  setLoginState({ commit }, payload = {}) {
    const { token, userInfo, roles, permissions, menuPaths } = payload
    const nextRoles = Array.isArray(roles) ? roles : ['admin']
    const nextPermissions = Array.isArray(permissions) ? permissions : ['*']
    const nextMenuPaths = Array.isArray(menuPaths) ? menuPaths : ['*']

    if (token) {
      setToken(token)
      commit('SET_TOKEN', token)
    }
    if (userInfo) {
      setUserInfo(userInfo)
      commit('SET_USER_INFO', userInfo)
    }

    setRoles(nextRoles)
    setPermissions(nextPermissions)
    setMenuPaths(nextMenuPaths)
    commit('SET_ROLES', nextRoles)
    commit('SET_PERMISSIONS', nextPermissions)
    commit('SET_MENU_PATHS', nextMenuPaths)
  },
  clearLoginState({ commit }) {
    clearAuth()
    commit('RESET_STATE')
  }
}

export default {
  namespaced: true,
  state,
  getters,
  mutations,
  actions
}
