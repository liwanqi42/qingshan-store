import { get } from '../utils/request'

class Shop {

  // 获取店铺营业状态：1-营业中 0-打烊中
  async getShopStatus() {
    return get('/user/shop/status')
  }
}

export default new Shop()
