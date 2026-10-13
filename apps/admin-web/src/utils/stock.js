export function stockTypeLabel(type) {
  return Number(type) === 1 ? '入库' : '出库'
}

export function stockStatusLabel(status) {
  return Number(status) === 1 ? '已审核' : '待审核'
}

export function stockStatusTagType(status) {
  return Number(status) === 1 ? 'success' : 'warning'
}

export function formatSkuAttrs(attrs) {
  if (!attrs) {
    return '-'
  }
  try {
    const parsed = JSON.parse(attrs)
    if (parsed && typeof parsed === 'object') {
      const parts = Object.keys(parsed).map(key => `${key}：${parsed[key]}`)
      if (parts.length) {
        return parts.join(' / ')
      }
    }
  } catch (e) {
    // 兼容历史非 JSON 数据，按原样展示
  }
  return attrs
}

export function formatDateTime(value) {
  if (!value) {
    return '-'
  }
  return String(value).replace('T', ' ').slice(0, 19)
}
