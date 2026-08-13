import { get } from '../utils/request'

class Category {

  // 对应后端 /user/category/list?type=，type：1-商品分类 2-套装分类
  async getCategoryList(type = 1) {
    const res = await get('/user/category/list', { type })
    if (!Array.isArray(res)) {
      return res
    }
    // 用户端只展示启用的分类
    return res.filter(category => category.status === 1)
  }
}

export default new Category()
