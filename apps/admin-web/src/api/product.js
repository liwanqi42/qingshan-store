import request from '@/utils/request'

export function getProductPage(params) {
  return request({
    url: '/admin/product/page',
    method: 'get',
    params
  })
}

export function addProduct(data) {
  return request({
    url: '/admin/product',
    method: 'post',
    data
  })
}

export function updateProduct(data) {
  return request({
    url: '/admin/product',
    method: 'put',
    data
  })
}

export function deleteProducts(ids) {
  return request({
    url: '/admin/product',
    method: 'delete',
    params: { ids: ids.join(',') }
  })
}

export function getProductById(id) {
  return request({
    url: `/admin/product/${id}`,
    method: 'get'
  })
}

export function enableOrDisableProduct(status, id) {
  return request({
    url: `/admin/product/status/${status}`,
    method: 'post',
    params: { id }
  })
}

export function getProductList(categoryId) {
  return request({
    url: '/admin/product/list',
    method: 'get',
    params: { categoryId }
  })
}
