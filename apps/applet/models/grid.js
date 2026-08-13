import { get } from '../utils/request'

class Grid {

  // 首页金刚区：使用商品分类
  async getGridList(page, size) {
    const res = await get('/user/category/list', { type: 1 })
    if (!Array.isArray(res)) {
      return res
    }
    const list = size ? res.slice(0, size) : res
    return list.map(category => ({
      id: category.id,
      categoryId: category.id,
      name: category.name,
      picture: category.image || ''
    }))
  }
}

export default new Grid()
