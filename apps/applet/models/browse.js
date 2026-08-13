import { get, post } from '../utils/request'

class Browse {

  // 记录商品浏览
  async recordBrowse(productId) {
    return post('/user/browse/record', { productId })
  }

  async getBrowseHistory(page, size) {
    return get(`/user/browse/list?page=${page}&size=${size}`)
  }

  async clearBrowseHistory(ids) {
    return post('/user/browse/clear', { ids })
  }
}

export default new Browse()
