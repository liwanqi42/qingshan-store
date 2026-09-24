import request from '@/utils/request'

export function getShopStatus() {
  return request({
    url: '/admin/shop/status',
    method: 'get'
  })
}

export function setShopStatus(status) {
  return request({
    url: `/admin/shop/${status}`,
    method: 'put'
  })
}
