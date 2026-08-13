import { get, _delete, post, put } from '../utils/request'

class Address {

  async getAddressList() {
    return get('/user/addressBook/list')
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
