const STORAGE_KEY = 'admin-web-menu-settings'

// 菜单默认标题调整后，清理旧的本地覆盖，避免侧边栏继续显示旧名称
const LEGACY_TITLES = {
  SystemRoleView: ['角色管理']
}

function loadSettings() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    const parsed = raw ? JSON.parse(raw) : {}
    const settings = parsed && typeof parsed === 'object' ? parsed : {}
    let changed = false

    Object.keys(LEGACY_TITLES).forEach(key => {
      const config = settings[key]
      if (config && LEGACY_TITLES[key].includes(config.title)) {
        delete config.title
        changed = true
        if (Object.keys(config).length === 0) {
          delete settings[key]
        }
      }
    })

    if (changed) {
      persistSettings(settings)
    }
    return settings
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
