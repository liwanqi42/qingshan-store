import request from '@/utils/request'

export function getBundlePage(params) {
  return request({
    url: '/admin/bundle/page',
    method: 'get',
    params
  })
}

export function addBundle(data) {
  return request({
    url: '/admin/bundle',
    method: 'post',
    data
  })
}

export function updateBundle(data) {
  return request({
    url: '/admin/bundle',
    method: 'put',
    data
  })
}

export function deleteBundles(ids) {
  return request({
    url: '/admin/bundle',
    method: 'delete',
    params: { ids: ids.join(',') }
  })
}

export function getBundleById(id) {
  return request({
    url: `/admin/bundle/${id}`,
    method: 'get'
  })
}

export function enableOrDisableBundle(status, id) {
  return request({
    url: `/admin/bundle/status/${status}`,
    method: 'post',
    params: { id }
  })
}
