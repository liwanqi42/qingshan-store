import { post, get } from '../utils/request'

class Cart {

  // 购物车-添加商品，productStyle 为已选属性 JSON，例如 {"颜色":"黑色","尺码":"L"}
  async addCart(productId, skuId, number, productStyle) {
    return post('/user/shoppingcart/add', {
      productId,
      skuId: skuId > 0 ? skuId : null,
      number,
      productStyle
    })
  }

  // 按订单商品加购，支持套装（bundleId）与 SKU
  async addCartItem(item) {
    return post('/user/shoppingcart/add', {
      productId: item.productId || null,
      bundleId: item.bundleId || null,
      skuId: item.skuId > 0 ? item.skuId : null,
      number: item.num,
      productStyle: item.productStyle
    })
  }

  // 编辑购物车，number 为修改后的数量，小于等于0时后端会删除该记录
  async editCart(id, number) {
    return post('/user/shoppingcart/edit', {
      id, number
    })
  }

  // 批量删除购物车商品，ids 为购物车记录id数组
  async deleteCart(ids) {
    return post('/user/shoppingcart/delete', {
      ids
    })
  }

  // 购物车列表，后端返回数组，这里包装成页面使用的 { list }
  async getCartList(page, size) {
    const res = await get('/user/shoppingcart/list')
    if (Array.isArray(res)) {
      return { list: res }
    }
    return res
  }

  async getCartGoodsNum() {
    return get('/user/shoppingcart/count')
  }
}

export default new Cart()
