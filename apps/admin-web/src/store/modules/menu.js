import {
  addMenu,
  deleteMenu,
  getMenuList,
  resetMenus as resetMenusApi,
  updateMenu
} from '@/api/menu'

function toSettings(list) {
  const settings = {}
  const menus = list || []
  menus.forEach(menu => {
    if (!menu || !menu.name) {
      return
    }
    settings[menu.name] = {
      id: menu.id,
      name: menu.name,
      path: menu.path,
      title: menu.title,
      icon: menu.icon,
      sort: Number(menu.sort) || 0,
      hidden: Number(menu.hidden) === 1,
      more: Number(menu.more) === 1
    }
  })
  return settings
}

const state = {
  settings: {},
  loaded: false
}

const mutations = {
  SET_SETTINGS(state, settings) {
    state.settings = settings || {}
  },
  SET_LOADED(state, loaded) {
    state.loaded = Boolean(loaded)
  }
}

const actions = {
  loadMenus({ state, commit }, options = {}) {
    if (state.loaded && !options.force) {
      return Promise.resolve()
    }
    return getMenuList()
      .then(res => {
        commit('SET_SETTINGS', toSettings(res.data || []))
      })
      .catch(() => {
        // 接口异常时回退到路由 meta 中的默认菜单配置
        commit('SET_SETTINGS', {})
      })
      .then(() => {
        commit('SET_LOADED', true)
      })
  },
  saveMenu({ dispatch }, payload) {
    return updateMenu(payload).then(() => dispatch('loadMenus', { force: true }))
  },
  createMenu({ dispatch }, payload) {
    return addMenu(payload).then(() => dispatch('loadMenus', { force: true }))
  },
  deleteMenu({ dispatch }, id) {
    return deleteMenu(id).then(() => dispatch('loadMenus', { force: true }))
  },
  resetMenus({ dispatch }) {
    return resetMenusApi().then(() => dispatch('loadMenus', { force: true }))
  }
}

export default {
  namespaced: true,
  state,
  mutations,
  actions
}
