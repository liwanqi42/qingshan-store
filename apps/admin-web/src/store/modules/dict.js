const STORAGE_KEY = 'admin-web-dicts'
const STORAGE_FINGERPRINT_KEY = 'admin-web-dicts-fingerprint'

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

function mergeDefaults(dicts) {
  const defaults = getDefaultDicts()
  const merged = { ...dicts }

  Object.keys(defaults).forEach(type => {
    const defaultDict = defaults[type]
    const current = merged[type]

    if (!current || typeof current !== 'object') {
      merged[type] = defaultDict
      return
    }

    const items = Array.isArray(current.items) ? [...current.items] : []
    const existingValues = new Set(items.map(item => String(item.value)))
    defaultDict.items.forEach(item => {
      if (!existingValues.has(String(item.value))) {
        items.push({ ...item })
      }
    })
    items.sort((a, b) => (Number(a.sort) || 0) - (Number(b.sort) || 0))

    merged[type] = {
      ...current,
      name: current.name || defaultDict.name,
      items
    }
  })

  return merged
}

//默认字典数据的指纹，代码中的默认数据变化后用于触发本地配置自动同步
function getDefaultsFingerprint() {
  const raw = JSON.stringify(getDefaultDicts())
  let hash = 5381
  for (let i = 0; i < raw.length; i++) {
    hash = ((hash << 5) + hash + raw.charCodeAt(i)) >>> 0
  }
  return String(hash)
}

function saveFingerprint(fingerprint) {
  localStorage.setItem(STORAGE_FINGERPRINT_KEY, fingerprint)
}

function loadDicts() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    const parsed = raw ? JSON.parse(raw) : null
    const fingerprint = getDefaultsFingerprint()

    if (!parsed || typeof parsed !== 'object') {
      const defaults = getDefaultDicts()
      persistDicts(defaults)
      saveFingerprint(fingerprint)
      return defaults
    }

    //默认数据有变化时自动合并新增的字典类型/字典项，无需手动恢复默认
    if (localStorage.getItem(STORAGE_FINGERPRINT_KEY) !== fingerprint) {
      const merged = mergeDefaults(parsed)
      persistDicts(merged)
      saveFingerprint(fingerprint)
      return merged
    }

    return parsed
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
    saveFingerprint(getDefaultsFingerprint())
  }
}

export default {
  namespaced: true,
  state,
  getters,
  mutations,
  actions
}
