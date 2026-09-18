import request from '@/utils/request'

export function login(data) {
  return request({
    url: '/admin/employee/login',
    method: 'post',
    data,
    skipErrorMessage: true
  })
}

export function logout() {
  return request({
    url: '/admin/employee/logout',
    method: 'post'
  })
}

export function editPassword(data) {
  return request({
    url: '/admin/employee/editPassword',
    method: 'put',
    data
  })
}
