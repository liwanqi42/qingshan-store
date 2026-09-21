const STORAGE_KEY = 'admin-web-menu-settings'

function loadSettings() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    const parsed = raw ? JSON.parse(raw) : {}
    return parsed && typeof parsed === 'object' ? parsed : {}
  } catch (e) {
    return {}
  }
}

function persistSettings(settings) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(settings))
}

const state = {
  settings: loadSettings()
}

const mutations = {
  SET_SETTINGS(state, settings) {
    state.settings = settings || {}
    persistSettings(state.settings)
  },
  RESET_SETTINGS(state) {
    state.settings = {}
    persistSettings(state.settings)
  }
}

const actions = {
  saveSettings({ commit }, settings) {
    commit('SET_SETTINGS', settings)
  },
  resetSettings({ commit }) {
    commit('RESET_SETTINGS')
  }
}

export default {
  namespaced: true,
  state,
  mutations,
  actions
}
