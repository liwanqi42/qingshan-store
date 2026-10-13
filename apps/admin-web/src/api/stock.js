import request from '@/utils/request'

export function lookupStockItem(code) {
  return request({
    url: '/admin/stock/item',
    method: 'get',
    params: { code }
  })
}

export function getStockOrderPage(params) {
  return request({
    url: '/admin/stock/orders/page',
    method: 'get',
    params
  })
}

export function getStockOrderDetail(id) {
  return request({
    url: `/admin/stock/orders/${id}`,
    method: 'get'
  })
}

export function addStockOrder(data) {
  return request({
    url: '/admin/stock/orders',
    method: 'post',
    data
  })
}

export function updateStockOrder(data) {
  return request({
    url: '/admin/stock/orders',
    method: 'put',
    data
  })
}

export function deleteStockOrder(id) {
  return request({
    url: `/admin/stock/orders/${id}`,
    method: 'delete'
  })
}

export function auditStockOrder(id) {
  return request({
    url: `/admin/stock/orders/${id}/audit`,
    method: 'post'
  })
}

export function exportStockOrders(params) {
  return request({
    url: '/admin/stock/orders/export',
    method: 'get',
    params,
    responseType: 'blob',
    timeout: 60000
  })
}

export function getStockLedgerPage(params) {
  return request({
    url: '/admin/stock/ledger/page',
    method: 'get',
    params
  })
}

export function exportStockLedger(params) {
  return request({
    url: '/admin/stock/ledger/export',
    method: 'get',
    params,
    responseType: 'blob',
    timeout: 60000
  })
}
