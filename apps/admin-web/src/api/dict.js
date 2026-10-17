import request from '@/utils/request'

export function getDictList() {
  return request({
    url: '/admin/dict/list',
    method: 'get'
  })
}

export function addDictType(data) {
  return request({
    url: '/admin/dict/type',
    method: 'post',
    data
  })
}

export function updateDictType(data) {
  return request({
    url: '/admin/dict/type',
    method: 'put',
    data
  })
}

export function deleteDictType(id) {
  return request({
    url: `/admin/dict/type/${id}`,
    method: 'delete'
  })
}

export function addDictItem(data) {
  return request({
    url: '/admin/dict/item',
    method: 'post',
    data
  })
}

export function updateDictItem(data) {
  return request({
    url: '/admin/dict/item',
    method: 'put',
    data
  })
}

export function deleteDictItem(id) {
  return request({
    url: `/admin/dict/item/${id}`,
    method: 'delete'
  })
}

export function resetDicts() {
  return request({
    url: '/admin/dict/reset',
    method: 'post'
  })
}
