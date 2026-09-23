const STORAGE_KEY = 'admin-web-roles'

function getDefaultRoles() {
  return [
    {
      id: 1,
      name: '超级管理员',
      code: 'admin',
      status: 1,
      menuPaths: [
        'DashboardView',
        'StatisticsView',
        'CategoryView',
        'ProductView',
        'BundleView',
        'SystemUserView',
        'SystemMenuView',
        'SystemRoleView',
        'SystemDictView'
      ]
    },
    {
      id: 2,
      name: '普通员工',
      code: 'employee',
      status: 1,
      menuPaths: ['DashboardView', 'SystemUserView']
    }
  ]
}

function loadRoles() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    const parsed = raw ? JSON.parse(raw) : null
    return Array.isArray(parsed) ? parsed : getDefaultRoles()
  } catch (e) {
    return getDefaultRoles()
  }
}

function persistRoles(roles) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(roles))
}

const state = {
  roles: loadRoles()
}

const mutations = {
  SET_ROLES(state, roles) {
    state.roles = roles
    persistRoles(roles)
  },
  ADD_ROLE(state, role) {
    state.roles.push(role)
    persistRoles(state.roles)
  },
  UPDATE_ROLE(state, role) {
    const index = state.roles.findIndex(item => item.id === role.id)
    if (index > -1) {
      state.roles.splice(index, 1, role)
      persistRoles(state.roles)
    }
  },
  DELETE_ROLE(state, id) {
    state.roles = state.roles.filter(item => item.id !== id)
    persistRoles(state.roles)
  },
  RESET_ROLES(state) {
    state.roles = getDefaultRoles()
    persistRoles(state.roles)
  }
}

const actions = {
  saveRole({ commit, state }, role) {
    if (role.id) {
      commit('UPDATE_ROLE', { ...role })
      return
    }
    const nextId =
      state.roles.reduce((max, item) => Math.max(max, item.id), 0) + 1
    commit('ADD_ROLE', { ...role, id: nextId })
  },
  deleteRole({ commit }, id) {
    commit('DELETE_ROLE', id)
  },
  saveRoleMenus({ commit, state }, { id, menuPaths }) {
    const role = state.roles.find(item => item.id === id)
    if (role) {
      commit('UPDATE_ROLE', { ...role, menuPaths })
    }
  },
  resetRoles({ commit }) {
    commit('RESET_ROLES')
  }
}

export default {
  namespaced: true,
  state,
  mutations,
  actions
}
