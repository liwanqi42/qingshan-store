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

export function getEmployeePage(params) {
  return request({
    url: '/admin/employee/page',
    method: 'get',
    params
  })
}

export function addEmployee(data) {
  return request({
    url: '/admin/employee',
    method: 'post',
    data
  })
}

export function updateEmployee(data) {
  return request({
    url: '/admin/employee',
    method: 'put',
    data
  })
}

export function getEmployeeById(id) {
  return request({
    url: `/admin/employee/${id}`,
    method: 'get'
  })
}

export function enableOrDisableEmployee(status, id) {
  return request({
    url: `/admin/employee/status/${status}`,
    method: 'post',
    params: { id }
  })
}

export function resetEmployeePassword(id) {
  return request({
    url: `/admin/employee/${id}/reset-password`,
    method: 'put'
  })
}
