import { get } from '../utils/request'

class Banner {

  // 首页轮播：取上架商品作为推荐位
  async getHomeBanner (page, size) {
    const res = await get('/user/product/page', { page, pageSize: size })
    if (!res || !Array.isArray(res.records)) {
      return res
    }
    return res.records.map(product => ({
      id: product.id,
      businessId: product.id,
      picture: product.image
    }))
  }
}

export default new Banner()
