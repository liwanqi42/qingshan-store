import request from '@/utils/request'

export function getTurnoverStatistics(params) {
  return request({
    url: '/admin/report/turnoverStatistics',
    method: 'get',
    params
  })
}

export function getUserStatistics(params) {
  return request({
    url: '/admin/report/userStatistics',
    method: 'get',
    params
  })
}

export function getOrderStatistics(params) {
  return request({
    url: '/admin/report/ordersStatistics',
    method: 'get',
    params
  })
}

export function getTop10(params) {
  return request({
    url: '/admin/report/top10',
    method: 'get',
    params
  })
}

export function exportReport() {
  return request({
    url: '/admin/report/export',
    method: 'get',
    responseType: 'blob'
  })
}
