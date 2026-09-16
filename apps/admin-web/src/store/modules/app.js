const STORAGE_KEY = 'admin-web-sidebar-collapsed'

const state = {
  sidebarCollapsed: localStorage.getItem(STORAGE_KEY) === 'true'
}

const mutations = {
  TOGGLE_SIDEBAR(state) {
    state.sidebarCollapsed = !state.sidebarCollapsed
    localStorage.setItem(STORAGE_KEY, String(state.sidebarCollapsed))
  },
  SET_SIDEBAR_COLLAPSED(state, collapsed) {
    state.sidebarCollapsed = Boolean(collapsed)
    localStorage.setItem(STORAGE_KEY, String(state.sidebarCollapsed))
  }
}

export default {
  namespaced: true,
  state,
  mutations
}
