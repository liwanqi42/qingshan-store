import { get } from '../utils/request'

// 图片地址去空白，兼容历史数据中带换行符的地址
function normalizeImage (url) {
  return typeof url === 'string' ? url.trim() : ''
}

// 后端商品字段转换为小程序页面使用的字段
function toGoodsItem (product) {
  return {
    ...product,
    picture: normalizeImage(product.image),
    title: product.name,
    humanNum: product.sales || 0
  }
}

class Goods {

  // 对应后端 /user/product/page：page、pageSize、name、categoryId
  // sort: 排序：0-综合 1-新品 2-销量 3-价格（后端暂不支持，保留参数）
  async getGoodsList(keyword, sort, categoryId, page, size) {
    const params = {
      page,
      pageSize: size
    }
    if (keyword) {
      params.name = keyword
    }
    if (categoryId && parseInt(categoryId, 10) !== 0) {
      params.categoryId = categoryId
    }
    const res = await get('/user/product/page', params)
    if (!res || !Array.isArray(res.records)) {
      return res
    }
    return {
      total: res.total,
      list: res.records.map(toGoodsItem)
    }
  }

  // 对应后端 /user/product/{id}，返回商品的属性列表 attrs
  async getGoodsDetails(id) {
    const res = await get(`/user/product/${id}`)
    if (!res || res.error_code !== undefined || res.id === undefined) {
      return res
    }
    // 用户端不展示已下架商品
    if (res.status !== 1) {
      return {
        error_code: 0,
        msg: '商品已下架'
      }
    }
    // 商品图片列表，封面图优先，去空去重
    const images = []
    const pushImage = (url) => {
      const image = normalizeImage(url)
      if (image && images.indexOf(image) === -1) {
        images.push(image)
      }
    }
    pushImage(res.image)
    if (Array.isArray(res.images)) {
      res.images.forEach(pushImage)
    }
    // 详情长图，与轮播样式图分开
    const detailImages = []
    const pushDetailImage = (url) => {
      const image = normalizeImage(url)
      if (image && detailImages.indexOf(image) === -1) {
        detailImages.push(image)
      }
    }
    if (Array.isArray(res.detailImages)) {
      res.detailImages.forEach(pushDetailImage)
    }
    // 后端商品规格 attrs -> 页面规格选择结构
    const specList = (Array.isArray(res.attrs) ? res.attrs : []).map((attr, index) => {
      let values = []
      if (Array.isArray(attr.attrValues)) {
        values = attr.attrValues
      } else if (typeof attr.attrValues === 'string') {
        try {
          values = JSON.parse(attr.attrValues)
        } catch (e) {
          values = []
        }
      }
      return {
        specId: attr.id || index + 1,
        name: attr.name,
        attrList: (Array.isArray(values) ? values : []).map((value, valueIndex) => ({
          attrId: valueIndex + 1,
          value
        }))
      }
    }).filter(spec => spec.attrList.length > 0)

    // 规格名/值 -> 页面属性id，用于把SKU的attrs映射成页面选择结构
    const attrIdMap = {}
    specList.forEach(spec => {
      const valueMap = {}
      spec.attrList.forEach(attr => {
        valueMap[attr.value] = attr.attrId
      })
      attrIdMap[spec.name] = valueMap
    })
    // 后端SKU（每个规格组合独立库存） -> 页面SKU结构
    const rawSkuList = Array.isArray(res.skuList) ? res.skuList : []
    const skuList = rawSkuList
      .filter(sku => sku && (sku.status === undefined || sku.status === null || sku.status === 1))
      .map(sku => {
        let attrs = sku.attrs
        if (typeof attrs === 'string') {
          try {
            attrs = JSON.parse(attrs)
          } catch (e) {
            attrs = {}
          }
        }
        const specs = []
        if (attrs && typeof attrs === 'object' && !Array.isArray(attrs)) {
          Object.keys(attrs).forEach(name => {
            const spec = specList.find(item => item.name === name)
            const valueMap = attrIdMap[name]
            const attrId = valueMap ? valueMap[attrs[name]] : undefined
            if (spec && attrId !== undefined) {
              specs.push({ keyId: spec.specId, valueId: attrId })
            }
          })
        }
        return {
          id: sku.id,
          price: sku.price,
          stock: sku.stock || 0,
          picture: normalizeImage(sku.image) || images[0] || '',
          specs: JSON.stringify(specs)
        }
      })
      // 有规格的商品，规格对不上的SKU无法被选中，直接忽略
      .filter(sku => specList.length === 0 || sku.specs !== '[]')
    // 完全没有SKU数据时兼容旧商品：按商品整体库存生成一个无规格选项
    if (skuList.length === 0 && rawSkuList.length === 0 && res.stock > 0) {
      skuList.push({
        id: 0,
        price: res.price,
        stock: res.stock,
        picture: images[0] || '',
        specs: '[]'
      })
    }
    return {
      ...res,
      title: res.name,
      picture: images[0] || '',
      // 详情页轮播图使用的图片列表
      bannerPicture: images,
      // 页面会对该字段做 JSON.parse，用于商品详情长图展示
      detailPicture: JSON.stringify(detailImages),
      specList,
      skuList
    }
  }
}

export default new Goods()
