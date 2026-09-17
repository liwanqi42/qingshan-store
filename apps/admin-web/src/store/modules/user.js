import {
  getToken,
  setToken,
  removeToken,
  getUserInfo,
  setUserInfo,
  removeUserInfo
} from '@/utils/auth'

const token = getToken()
const cachedUserInfo = token ? getUserInfo() : null

const state = {
  token: token || '',
  name: cachedUserInfo
    ? cachedUserInfo.name || cachedUserInfo.userName || ''
    : '',
  userInfo: cachedUserInfo,
  roles: token ? ['admin'] : [],
  permissions: token ? ['*'] : []
}

const getters = {
  isLoggedIn: state => Boolean(state.token),
  name: state => state.name,
  roles: state => state.roles,
  permissions: state => state.permissions
}

const mutations = {
  SET_TOKEN(state, token) {
    state.token = token || ''
  },
  SET_USER_INFO(state, userInfo) {
    state.userInfo = userInfo || null
    state.name = userInfo ? userInfo.name || userInfo.userName || '' : ''
  },
  SET_ROLES(state, roles) {
    state.roles = Array.isArray(roles) ? roles : []
  },
  SET_PERMISSIONS(state, permissions) {
    state.permissions = Array.isArray(permissions) ? permissions : []
  },
  RESET_STATE(state) {
    state.token = ''
    state.name = ''
    state.userInfo = null
    state.roles = []
    state.permissions = []
  }
}

const actions = {
  setLoginState({ commit }, payload = {}) {
    const { token, userInfo, roles, permissions } = payload

    if (token) {
      setToken(token)
      commit('SET_TOKEN', token)
    }
    if (userInfo) {
      setUserInfo(userInfo)
      commit('SET_USER_INFO', userInfo)
    }

    commit('SET_ROLES', Array.isArray(roles) ? roles : ['admin'])
    commit(
      'SET_PERMISSIONS',
      Array.isArray(permissions) ? permissions : ['*']
    )
  },
  clearLoginState({ commit }) {
    removeToken()
    removeUserInfo()
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
