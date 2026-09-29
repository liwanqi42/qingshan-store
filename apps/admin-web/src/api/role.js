import request from '@/utils/request'

export function getRoleList(params) {
  return request({
    url: '/admin/role/list',
    method: 'get',
    params
  })
}

export function addRole(data) {
  return request({
    url: '/admin/role',
    method: 'post',
    data
  })
}

export function updateRole(data) {
  return request({
    url: '/admin/role',
    method: 'put',
    data
  })
}

export function deleteRole(id) {
  return request({
    url: `/admin/role/${id}`,
    method: 'delete'
  })
}

export function enableOrDisableRole(status, id) {
  return request({
    url: `/admin/role/status/${status}`,
    method: 'post',
    params: { id }
  })
}

export function resetRoles() {
  return request({
    url: '/admin/role/reset',
    method: 'post'
  })
}
