import request from '@/utils/request'

export function getCategoryPage(params) {
  return request({
    url: '/admin/category/page',
    method: 'get',
    params
  })
}

export function addCategory(data) {
  return request({
    url: '/admin/category',
    method: 'post',
    data
  })
}

export function updateCategory(data) {
  return request({
    url: '/admin/category',
    method: 'put',
    data
  })
}

export function deleteCategory(id) {
  return request({
    url: '/admin/category',
    method: 'delete',
    params: { id }
  })
}

export function enableOrDisableCategory(status, id) {
  return request({
    url: `/admin/category/status/${status}`,
    method: 'post',
    params: { id }
  })
}

export function getCategoryList(type) {
  return request({
    url: '/admin/category/list',
    method: 'get',
    params: { type }
  })
}
