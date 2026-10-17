import {
  addDictItem,
  addDictType,
  deleteDictItem,
  deleteDictType,
  getDictList,
  resetDicts as resetDictsApi,
  updateDictItem,
  updateDictType
} from '@/api/dict'

const STORAGE_KEY = 'admin-web-dicts'

// 内置默认数据只作为接口异常时的离线兜底，正常使用以服务端字典为准
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
    role_status: {
      name: '角色状态',
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
        { label: '已取消', value: '6', tagType: 'danger', sort: 6 },
        { label: '退款', value: '7', tagType: 'danger', sort: 7 }
      ]
    },
    order_refund_status: {
      name: '售后状态',
      items: [
        { label: '售后申请中', value: '1', tagType: 'warning', sort: 1 },
        { label: '待买家退货', value: '2', tagType: 'info', sort: 2 },
        { label: '待商家收货', value: '3', tagType: 'info', sort: 3 },
        { label: '售后完成', value: '4', tagType: 'success', sort: 4 },
        { label: '售后被拒绝', value: '5', tagType: 'danger', sort: 5 },
        { label: '售后关闭', value: '6', tagType: 'info', sort: 6 }
      ]
    },
    order_refund_type: {
      name: '售后类型',
      items: [
        { label: '仅退款', value: '1', tagType: 'warning', sort: 1 },
        { label: '退货退款', value: '2', tagType: 'warning', sort: 2 },
        { label: '换货', value: '3', tagType: 'info', sort: 3 },
        { label: '补寄', value: '4', tagType: 'info', sort: 4 }
      ]
    },
    pay_method: {
      name: '支付方式',
      items: [
        { label: '微信支付', value: '1', tagType: 'success', sort: 1 },
        { label: '支付宝', value: '2', tagType: 'info', sort: 2 },
        { label: '银行卡', value: '3', tagType: 'info', sort: 3 }
      ]
    },
    category_type: {
      name: '分类类型',
      items: [
        { label: '商品分类', value: '1', tagType: '', sort: 1 },
        { label: '套餐分类', value: '2', tagType: 'warning', sort: 2 }
      ]
    },
    category_status: {
      name: '分类状态',
      items: [
        { label: '启用', value: '1', tagType: 'success', sort: 1 },
        { label: '禁用', value: '0', tagType: 'danger', sort: 2 }
      ]
    },
    product_status: {
      name: '商品状态',
      items: [
        { label: '上架', value: '1', tagType: 'success', sort: 1 },
        { label: '下架', value: '0', tagType: 'info', sort: 2 }
      ]
    },
    bundle_status: {
      name: '套餐状态',
      items: [
        { label: '上架', value: '1', tagType: 'success', sort: 1 },
        { label: '下架', value: '0', tagType: 'info', sort: 2 }
      ]
    },
    stock_order_type: {
      name: '库存单据类型',
      items: [
        { label: '入库', value: '1', tagType: 'success', sort: 1 },
        { label: '出库', value: '2', tagType: 'warning', sort: 2 }
      ]
    },
    stock_order_status: {
      name: '库存单据状态',
      items: [
        { label: '待审核', value: '0', tagType: 'warning', sort: 1 },
        { label: '已审核', value: '1', tagType: 'success', sort: 2 }
      ]
    },
    shop_status: {
      name: '营业状态',
      items: [
        { label: '营业中', value: '1', tagType: 'success', sort: 1 },
        { label: '打烊中', value: '0', tagType: 'info', sort: 2 }
      ]
    }
  }
}

function normalizeItem(item, index) {
  const value = item.value === undefined || item.value === null
    ? ''
    : String(item.value)
  return {
    id: item.id === undefined ? null : item.id,
    label: item.label || '',
    value,
    tagType: item.tagType || '',
    sort: Number(item.sort) || index + 1,
    status: item.status === undefined || item.status === null
      ? 1
      : Number(item.status)
  }
}

function normalizeDictType(dictType, fallbackType) {
  return {
    id: dictType.id === undefined ? null : dictType.id,
    type: dictType.type || fallbackType,
    name: dictType.name || fallbackType,
    status: dictType.status === undefined || dictType.status === null
      ? 1
      : Number(dictType.status),
    sort: Number(dictType.sort) || 0,
    isSystem: Number(dictType.isSystem) || 0,
    items: (dictType.items || []).map(normalizeItem)
  }
}

function toDictMap(list) {
  const dicts = {}
  const dictList = list || []
  dictList.forEach(dictType => {
    if (dictType && dictType.type) {
      dicts[dictType.type] = normalizeDictType(dictType, dictType.type)
    }
  })
  return dicts
}

function getFallbackDicts() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    const parsed = raw ? JSON.parse(raw) : null
    if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) {
      const dicts = {}
      Object.keys(parsed).forEach(type => {
        dicts[type] = normalizeDictType(parsed[type] || {}, type)
      })
      if (Object.keys(dicts).length) {
        return dicts
      }
    }
  } catch (e) {
    // 本地缓存损坏时使用内置默认值
  }

  const defaults = getDefaultDicts()
  const dicts = {}
  Object.keys(defaults).forEach((type, index) => {
    dicts[type] = normalizeDictType({
      ...defaults[type],
      type,
      sort: (index + 1) * 10,
      isSystem: 1
    }, type)
  })
  return dicts
}

function persistDicts(dicts) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(dicts))
  } catch (e) {
    // 存储空间不足等情况不影响页面使用
  }
}

const state = {
  dicts: getFallbackDicts(),
  loaded: false
}

const getters = {
  dictTypes: state =>
    Object.keys(state.dicts)
      .map(type => {
        const dict = state.dicts[type]
        return {
          id: dict.id,
          type,
          name: dict.name,
          status: dict.status,
          sort: dict.sort,
          isSystem: dict.isSystem,
          count: (dict.items || []).length
        }
      })
      .sort((a, b) => (a.sort - b.sort) || a.type.localeCompare(b.type)),
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
    state.dicts = dicts || {}
    persistDicts(state.dicts)
  },
  SET_LOADED(state, loaded) {
    state.loaded = Boolean(loaded)
  }
}

const actions = {
  loadDicts({ state, commit }, options = {}) {
    if (state.loaded && !options.force) {
      return Promise.resolve()
    }
    return getDictList()
      .then(res => {
        commit('SET_DICTS', toDictMap(res.data || []))
      })
      .catch(() => {
        // 接口异常时保留本地缓存或内置默认值，页面不白屏
      })
      .then(() => {
        commit('SET_LOADED', true)
      })
  },
  saveDictType({ dispatch }, payload) {
    const save = payload.id ? updateDictType : addDictType
    return save(payload).then(() => dispatch('loadDicts', { force: true }))
  },
  deleteDictType({ dispatch }, id) {
    return deleteDictType(id).then(() => dispatch('loadDicts', { force: true }))
  },
  saveDictItem({ dispatch }, payload) {
    const save = payload.id ? updateDictItem : addDictItem
    return save(payload).then(() => dispatch('loadDicts', { force: true }))
  },
  deleteDictItem({ dispatch }, id) {
    return deleteDictItem(id).then(() => dispatch('loadDicts', { force: true }))
  },
  resetDicts({ dispatch }) {
    return resetDictsApi().then(() => dispatch('loadDicts', { force: true }))
  }
}

export default {
  namespaced: true,
  state,
  getters,
  mutations,
  actions
}
