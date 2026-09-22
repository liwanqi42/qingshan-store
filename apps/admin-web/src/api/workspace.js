import request from '@/utils/request'

export function getBusinessData() {
  return request({
    url: '/admin/workspace/businessData',
    method: 'get'
  })
}

export function getOrderOverview() {
  return request({
    url: '/admin/workspace/overviewOrders',
    method: 'get'
  })
}

export function getProductOverview() {
  return request({
    url: '/admin/workspace/overviewProducts',
    method: 'get'
  })
}

export function getBundleOverview() {
  return request({
    url: '/admin/workspace/overviewBundles',
    method: 'get'
  })
}
