const STORAGE_KEY = 'admin-web-dicts'

function getDefaultDicts() {
  return {
    employee_sex: {
      name: '员工性别',
      items: [
        { label: '男', value: '1', tagType: '', sort: 1 },
        { label: '女', value: '0', tagType: 'danger', sort: 2 }
      ]
    },
    employee_status: {
      name: '员工状态',
      items: [
        { label: '启用', value: '1', tagType: 'success', sort: 1 },
        { label: '禁用', value: '0', tagType: 'danger', sort: 2 }
      ]
    },
    order_status: {
      name: '订单状态',
      items: [
        { label: '待付款', value: '1', tagType: 'info', sort: 1 },
        { label: '待接单', value: '2', tagType: 'warning', sort: 2 },
        { label: '待发货', value: '3', tagType: 'warning', sort: 3 },
        { label: '配送中', value: '4', tagType: 'success', sort: 4 },
        { label: '已完成', value: '5', tagType: 'info', sort: 5 },
        { label: '已取消', value: '6', tagType: 'danger', sort: 6 }
      ]
    }
  }
}

function loadDicts() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    const parsed = raw ? JSON.parse(raw) : null
    return parsed && typeof parsed === 'object' ? parsed : getDefaultDicts()
  } catch (e) {
    return getDefaultDicts()
  }
}

function persistDicts(dicts) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(dicts))
}

const state = {
  dicts: loadDicts()
}

const getters = {
  dictTypes: state =>
    Object.keys(state.dicts).map(type => ({
      type,
      name: state.dicts[type].name,
      count: (state.dicts[type].items || []).length
    })),
  getDictItem: state => (type, value) => {
    const dict = state.dicts[type]
    if (!dict || !Array.isArray(dict.items)) {
      return null
    }
    return (
      dict.items.find(item => String(item.value) === String(value)) || null
    )
  }
}

const mutations = {
  SET_DICTS(state, dicts) {
    state.dicts = dicts
    persistDicts(dicts)
  }
}

const actions = {
  saveDictType({ state, commit }, { type, name }) {
    const dicts = { ...state.dicts }
    dicts[type] = {
      ...(dicts[type] || { items: [] }),
      name
    }
    commit('SET_DICTS', dicts)
  },
  deleteDictType({ state, commit }, type) {
    const dicts = { ...state.dicts }
    delete dicts[type]
    commit('SET_DICTS', dicts)
  },
  saveDictItem({ state, commit }, { type, item, originalValue }) {
    const dict = state.dicts[type]
    if (!dict) {
      return
    }

    const items = Array.isArray(dict.items) ? [...dict.items] : []
    const index =
      originalValue === undefined
        ? -1
        : items.findIndex(
            oldItem => String(oldItem.value) === String(originalValue)
          )

    if (index > -1) {
      items.splice(index, 1, { ...item })
    } else {
      items.push({ ...item })
    }

    items.sort((a, b) => (Number(a.sort) || 0) - (Number(b.sort) || 0))
    commit('SET_DICTS', {
      ...state.dicts,
      [type]: { ...dict, items }
    })
  },
  deleteDictItem({ state, commit }, { type, value }) {
    const dict = state.dicts[type]
    if (!dict) {
      return
    }
    const items = (dict.items || []).filter(
      item => String(item.value) !== String(value)
    )
    commit('SET_DICTS', {
      ...state.dicts,
      [type]: { ...dict, items }
    })
  },
  resetDicts({ commit }) {
    commit('SET_DICTS', getDefaultDicts())
  }
}

export default {
  namespaced: true,
  state,
  getters,
  mutations,
  actions
}
