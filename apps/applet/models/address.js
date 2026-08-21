import { get, _delete, post, put } from '../utils/request'

class Address {

  // 地址列表：后端返回数组，页面统一按 { list } 使用
  async getAddressList() {
    const res = await get('/user/addressBook/list')
    if (Array.isArray(res)) {
      return { list: res }
    }
    return res
  }

  async getAddressById(id) {
    return get(`/user/addressBook/${id}`)
  }

  async deleteAddress(id) {
    return _delete('/user/addressBook', { id })
  }

  // 新增或修改地址：有 id 走修改，无 id 走新增
  async saveAddress(address) {
    if (address.id) {
      return put('/user/addressBook', address)
    }
    return post('/user/addressBook', address)
  }

  // 设置默认地址
  async setDefaultAddress(id) {
    return put('/user/addressBook/default', { id })
  }

  async getDefaultAddress() {
    return get('/user/addressBook/default')
  }
}

export default new Address()
