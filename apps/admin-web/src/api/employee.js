import request from '@/utils/request'

export function login(data) {
  return request({
    url: '/admin/employee/login',
    method: 'post',
    data
  })
}
