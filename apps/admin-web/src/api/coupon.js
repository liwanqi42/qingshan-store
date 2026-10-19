import request from '@/utils/request'

export function getCouponPage(params) {
  return request({
    url: '/admin/coupon/page',
    method: 'get',
    params
  })
}

export function addCoupon(data) {
  return request({
    url: '/admin/coupon',
    method: 'post',
    data
  })
}

export function updateCoupon(data) {
  return request({
    url: '/admin/coupon',
    method: 'put',
    data
  })
}

export function deleteCoupon(id) {
  return request({
    url: '/admin/coupon',
    method: 'delete',
    params: { id }
  })
}

export function updateCouponStatus(status, id) {
  return request({
    url: `/admin/coupon/status/${status}`,
    method: 'post',
    params: { id }
  })
}
