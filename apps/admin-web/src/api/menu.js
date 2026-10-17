import request from '@/utils/request'

export function getMenuList() {
  return request({
    url: '/admin/menu/list',
    method: 'get'
  })
}

export function addMenu(data) {
  return request({
    url: '/admin/menu',
    method: 'post',
    data
  })
}

export function updateMenu(data) {
  return request({
    url: '/admin/menu',
    method: 'put',
    data
  })
}

export function deleteMenu(id) {
  return request({
    url: `/admin/menu/${id}`,
    method: 'delete'
  })
}

export function resetMenus() {
  return request({
    url: '/admin/menu/reset',
    method: 'post'
  })
}
