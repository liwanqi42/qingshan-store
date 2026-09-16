const STORAGE_KEY = 'admin-web-visited-views'

function readVisitedViews() {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY)
    const views = raw ? JSON.parse(raw) : []
    return Array.isArray(views) ? views : []
  } catch (e) {
    return []
  }
}

function persistVisitedViews(views) {
  try {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(views))
  } catch (e) {
    return
  }
}

const state = {
  visitedViews: readVisitedViews(),
  cachedViews: []
}

const mutations = {
  ADD_VISITED_VIEW(state, view) {
    if (state.visitedViews.some(item => item.path === view.path)) {
      return
    }
    state.visitedViews.push({
      ...view,
      title: view.title || (view.meta && view.meta.title) || '未命名'
    })
    persistVisitedViews(state.visitedViews)
  },
  ADD_CACHED_VIEW(state, view) {
    if (!view.name || state.cachedViews.includes(view.name)) {
      return
    }
    if (view.meta && view.meta.noCache === true) {
      return
    }
    state.cachedViews.push(view.name)
  },
  DEL_VIEW(state, view) {
    state.visitedViews = state.visitedViews.filter(
      item => item.path !== view.path
    )
    persistVisitedViews(state.visitedViews)
  },
  DEL_CACHED_VIEW(state, view) {
    if (!view.name) {
      return
    }
    state.cachedViews = state.cachedViews.filter(name => name !== view.name)
  },
  DEL_OTHERS_VIEWS(state, view) {
    state.visitedViews = state.visitedViews.filter(
      item => (item.meta && item.meta.affix) || item.path === view.path
    )
    const cachedNames = state.visitedViews
      .filter(item => item.name)
      .map(item => item.name)
    state.cachedViews = state.cachedViews.filter(name =>
      cachedNames.includes(name)
    )
    persistVisitedViews(state.visitedViews)
  },
  DEL_ALL_VIEWS(state) {
    state.visitedViews = state.visitedViews.filter(
      item => item.meta && item.meta.affix
    )
    const cachedNames = state.visitedViews
      .filter(item => item.name)
      .map(item => item.name)
    state.cachedViews = state.cachedViews.filter(name =>
      cachedNames.includes(name)
    )
    persistVisitedViews(state.visitedViews)
  }
}

const actions = {
  addView({ commit }, view) {
    commit('ADD_VISITED_VIEW', view)
    commit('ADD_CACHED_VIEW', view)
  },
  delView({ commit }, view) {
    commit('DEL_VIEW', view)
    commit('DEL_CACHED_VIEW', view)
  },
  delCachedView({ commit }, view) {
    commit('DEL_CACHED_VIEW', view)
  },
  delOthersViews({ commit }, view) {
    commit('DEL_OTHERS_VIEWS', view)
  },
  delAllViews({ commit }) {
    commit('DEL_ALL_VIEWS')
  }
}

export default {
  namespaced: true,
  state,
  mutations,
  actions
}
