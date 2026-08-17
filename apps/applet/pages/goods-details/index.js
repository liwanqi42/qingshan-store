import goods from '../../models/goods'
import cart from '../../models/cart'
import address from '../../models/address'
import browse from '../../models/browse'
import { requireLogin, isAuthorized } from '../../utils/auth'

Page({
  data: {
    cartNum: 0,   // 购物车商品数量
    goodsDetail: {},
    hideShopPopup: true,
    address: {},         // 收货地址
    detailImages: [],    // 商品详情图片
    selectedSpecsText: '默认规格', // 已选规格文案
    selectGoodsSku: {
      picture: '',
      price: '',
      stock: '',
      stockName: ''
    },
    selectSpecs: {},
    selectReady: false, // 规格选择就绪
    operationType: 1,   // 操作类型：1-加购物车 2-立即购买
    buyNum: 1,          // 购买数量
    buyNumMax: 999,     // 可购买上限：库存与999取小
    selectSkuId: 0,
    selectSpecAttr: {}, // 是否可选的状态
    outofStockStatus: false, // 缺货状态
    currentBanner: 0    // 当前轮播图序号
  },
  onLoad(e) {
    wx.showShareMenu({
      withShareTicket: true
    }) 
    this.data.goodsId = e.id
    wx.removeStorageSync("goods")
    this.loadGoodsInfo()
    this.loadDefaultAddress()
  },
  onShow() {
    this.loadCartGoodsNum()
  },
  async loadGoodsInfo() {
    wx.showLoading({
      mask: true,
      title: '加载中'
    })
    const data = await goods.getGoodsDetails(this.data.goodsId)
    const {error_code, msg} = data
    if (error_code !== undefined) {
      console.log(msg)
      this.setData({
        goodsDetail: {}
      })
      wx.hideLoading()
      return
    }
    const price = this.calcGoodsPriceInterval(data.price, data.skuList)
    // 商品详情图片：兼容旧接口返回的 JSON 字符串
    let detailPicture = data.detailPicture
    if (typeof detailPicture === 'string') {
      try {
        detailPicture = JSON.parse(detailPicture)
      } catch (e) {
        detailPicture = []
      }
    }
    if (!Array.isArray(detailPicture)) {
      detailPicture = []
    }
    const detailImages = []
    detailPicture.forEach((url) => {
      const image = typeof url === 'string' ? url.trim() : ''
      if (image && detailImages.indexOf(image) === -1) {
        detailImages.push(image)
      }
    })
    // 滚动的商品图：封面图优先，兼容旧接口返回的 JSON 字符串
    let bannerPicture = data.bannerPicture
    if (typeof bannerPicture === 'string') {
      try {
        bannerPicture = JSON.parse(bannerPicture)
      } catch (e) {
        bannerPicture = []
      }
    }
    if (!Array.isArray(bannerPicture)) {
      bannerPicture = []
    }
    const banners = []
    const pushBanner = (url) => {
      const image = typeof url === 'string' ? url.trim() : ''
      if (image && banners.indexOf(image) === -1) {
        banners.push(image)
      }
    }
    pushBanner(data.picture)
    bannerPicture.forEach(pushBanner)
    data.bannerPicture = banners
    data.price = price
    this.setData({
      goodsDetail: data,
      detailImages: detailImages,
      currentBanner: 0
    })
    this.initSelectGoodsSku(data)
    this.initSpecAttrOptionalStatus(data)
    wx.hideLoading()
    this.recordBrowse(data.id)
  },
  // 记录浏览历史：仅登录后记录，失败不影响商品展示
  async recordBrowse(productId) {
    if (!isAuthorized() || !productId) {
      return
    }
    try {
      await browse.recordBrowse(productId)
    } catch (e) {
      console.log(e)
    }
  },
  // 轮播图切换
  swiperChange(e) {
    this.setData({
      currentBanner: e.detail.current
    })
  },
  // 计算商品价格区间
  calcGoodsPriceInterval (price, skuList) {
    let min = 0
    let max = 0
    for (let i = 0; i < skuList.length; i++) {
      const sku = skuList[i]
      const skuPrice = parseFloat(sku.price, 10)
      if (min === 0 || min > skuPrice) {
        min = skuPrice
      }
      if (max === 0 || max < skuPrice) {
        max = skuPrice
      }
    }
    if (min === max) {
      if (min > 0) {
        price = min
      }
    } else {
      price = min + '-' + max
    }
    return price
  },
  // 初始化预选商品
  initSelectGoodsSku({picture, price, skuList, specList}) {
    if (skuList.length === 0) {
      this.setData({
        selectGoodsSku: {
          picture: picture,
          price: price,
          stock: 0,
          stockName: ''
        },
        buyNumMax: 1,
        outofStockStatus: true
      })
      return
    }
    const stock = this.getTotalSkuStock()
    let selectSpecs = {}
    for (let i = 0; i < specList.length; i++) {
      const spec = specList[i]
      selectSpecs[spec.specId] = 0
    }
    // 没有规格时只有一个默认样式，直接展示它的库存；有规格时等用户选定后再显示对应库存
    const noSpec = specList.length === 0
    this.setData({
      selectSpecs: selectSpecs,
      buyNumMax: noSpec ? Math.max(1, Math.min(stock, 999)) : 999,
      selectGoodsSku: {
        picture: picture,
        price: price,
        stock: noSpec ? stock : 0,
        stockName: ''
      }
    })
    this.calcSelectStockName()
  },
  // 所有SKU库存总和（仅用于无规格商品）
  getTotalSkuStock() {
    const skuList = this.data.goodsDetail.skuList || []
    let stock = 0
    for (let i = 0; i < skuList.length; i++) {
      stock += skuList[i].stock || 0
    }
    return stock
  },
  // 计算-已经选择的规格组合
  calcSelectStockName () {
    const selectSpecs = this.data.selectSpecs
    const specList = this.data.goodsDetail.specList || []
    const selectGoodsSku = this.data.selectGoodsSku
    const missing = []
    const selected = []
    for (let i = 0; i < specList.length; i++) {
      const spec = specList[i]
      const curAttrId = selectSpecs[spec.specId]
      if (curAttrId !== 0) {
        for (let j = 0; j < spec.attrList.length; j++) {
          const attr = spec.attrList[j]
          if (attr.attrId === curAttrId) {
            selected.push(attr.value)
          }
        }
        continue
      }
      missing.push(spec.name)
    }
    let selectReady = false
    let stockName = ''
    if (missing.length > 0) {
      stockName = '请选择：' + missing.join('、')
    } else {
      selectReady = true
      stockName = selected.length > 0 ? '已选：' + selected.join('、') : '默认规格'
    }
    selectGoodsSku.stockName = stockName
    this.setData({
      selectReady: selectReady,
      selectGoodsSku: selectGoodsSku,
      selectedSpecsText: stockName
    })
  },
  // 初始化规格选项状态
  initSpecAttrOptionalStatus({specList}) {
    // spec属性矩阵（K => specId-attrId，V => true）
    const selectSpecAttr = {}
    for (let i = 0; i < specList.length; i++) {
      const spec = specList[i]
      const attrList = spec.attrList
      for (let j = 0; j < attrList.length; j++) {
        const attr = attrList[j]
        const key = spec.specId + '-' + attr.attrId
        selectSpecAttr[key] = true
      }
    }
    this.setData({
      selectSpecAttr: selectSpecAttr
    })
    this.updateOptionalSpecAttr()
  },
  // 选择规格
  doSwitchSpec(e) {
    const specId = e.currentTarget.dataset.specId
    const attrId = e.currentTarget.dataset.attrId
    const selectSpecAttr = this.data.selectSpecAttr
    const selectSpecs = this.data.selectSpecs
    const curAttrId = selectSpecs[specId]
    const key = specId + '-' + attrId
    if (!selectSpecAttr[key]) {
      return
    }
    if (curAttrId === attrId) {
      selectSpecs[specId] = 0
    } else {
      selectSpecs[specId] = attrId
    }
    this.setData({
      selectSpecs: selectSpecs
    })
    this.calcSelectStockName()
    this.updateOptionalSpecAttr()
    this.updateOptionalSpec()
  },
  // 更新所选的规格
  updateOptionalSpec() {
    const goodsDetail = this.data.goodsDetail
    const selectGoodsSku = this.data.selectGoodsSku
    const skuList = this.data.goodsDetail.skuList
    const selectSpecs = this.data.selectSpecs
    const selectReady = this.data.selectReady
    let selectSkuId = 0
    // 选择完整规格后才展示该样式的库存；未选完时显示0
    let stock = 0
    if (selectReady) {
      let matchSku = null
      for (let i = 0; i < skuList.length; i++) {
        const sku = skuList[i]
        const specs = JSON.parse(sku.specs)
        let match = true
        for (let j = 0; j < specs.length; j++) {
          const skuSpec = specs[j]
          if (selectSpecs[skuSpec.keyId] !== skuSpec.valueId) {
            match = false
          }
        }
        if (match) {
          matchSku = sku
        }
      }
      if (matchSku) {
        stock = matchSku.stock || 0
        selectGoodsSku.picture = matchSku.picture
        selectGoodsSku.price = matchSku.price
        selectSkuId = matchSku.id
      }
    } else {
      selectGoodsSku.picture = goodsDetail.picture
      selectGoodsSku.price = goodsDetail.price
    }
    // 库存变化后同步可购买上限，并把已选数量收回到上限内
    selectGoodsSku.stock = stock
    const buyNumMax = selectReady ? Math.max(1, Math.min(stock, 999)) : 999
    const buyNum = Math.max(1, Math.min(this.data.buyNum, buyNumMax))
    this.setData({
      selectGoodsSku: selectGoodsSku,
      selectSkuId: selectSkuId,
      buyNumMax: buyNumMax,
      buyNum: buyNum
    })
  },
  // 无库存的选项置灰
  updateOptionalSpecAttr() {
    const selectSpecs = this.data.selectSpecs
    // 提取匹配的SKU
    const matchSkuList = []
    const skuList = this.data.goodsDetail.skuList
    for (let i = 0; i < skuList.length; i++) {
      const sku = skuList[i]
      const specs = JSON.parse(sku.specs)
      let match = true
      if (sku.stock <= 0) {
        match = false
      }
      for (let j = 0; j < specs.length; j++) {
        const skuSpec = specs[j]
        if (selectSpecs[skuSpec.keyId] === 0) {
          continue
        }
        if (selectSpecs[skuSpec.keyId] !== skuSpec.valueId) {
          match = false
        }
      }
      if (sku.stock <= 0) {
        match = false
      }
      if (match) {
        matchSkuList.push(sku)
      }
    }
    // 计算选项的状态
    const selectSpecAttr = this.data.selectSpecAttr
    for (let i in selectSpecAttr) {
      selectSpecAttr[i] = false
    }
    let unrestricted = false
    for (let i = 0; i < matchSkuList.length; i++) {
      const specs = JSON.parse(matchSkuList[i].specs)
      // 后端暂无 SKU 规格映射，空规格表示所有规格组合都可用
      if (specs.length === 0) {
        unrestricted = true
        continue
      }
      for (let j = 0; j < specs.length; j++) {
        const key = specs[j].keyId + '-' + specs[j].valueId
        selectSpecAttr[key] = true
      }
    }
    if (unrestricted) {
      for (let i in selectSpecAttr) {
        selectSpecAttr[i] = true
      }
    }
    this.setData({
      selectSpecAttr: selectSpecAttr
    })
  },
  // 加载默认收货地址
  async loadDefaultAddress() {
    // 未授权时不展示静默登录账号下的地址
    if (!isAuthorized()) {
      this.setData({
        address: {}
      })
      return
    }
    const res = await address.getDefaultAddress()
    if (!res || res.error_code !== undefined || !res.id) {
      this.setData({
        address: {}
      })
      return
    }
    this.setData({
      address: res
    })
  },
  // 选择收货地址
  async selectAddress() {
    if (!(await this.checkUserLogin())) {
      return
    }
    const addressId = this.data.address.id || 0
    wx.navigateTo({
      url: '/pages/option-address/index?id=' + addressId
    })
  },
  // 打开规格选择弹窗
  openSpecPopup() {
    this.setData({
      hideShopPopup: false,
      operationType: 1
    })
  },
  async loadCartGoodsNum() {
    // 未授权时不展示静默登录账号下的购物车数量
    if (!isAuthorized()) {
      this.setData({
        cartNum: 0
      })
      return
    }
    const res = await cart.getCartGoodsNum()
    const { error_code, msg } = res
    if (error_code !== undefined) {
      console.log(msg)
      return
    }
    this.setData({
      cartNum: res.num
    })
  },
  goHome() {
    wx.reLaunch({
      url: "/pages/index/index"
    });
  },
  goShopCar() {
    wx.reLaunch({
      url: "/pages/shop-cart/index"
    });
  },
  async addShopCar() {
    if (!(await this.checkUserLogin())) {
      return
    }
    this.setData({
      hideShopPopup: false,
      operationType: 1
    })
  },
  // 已选规格 -> {"颜色":"黑色","尺码":"L"}，与 product_sku.attrs 保持一致
  buildProductStyle() {
    const specList = this.data.goodsDetail.specList || []
    const selectSpecs = this.data.selectSpecs || {}
    const style = {}
    for (let i = 0; i < specList.length; i++) {
      const spec = specList[i]
      const curAttrId = selectSpecs[spec.specId]
      if (!curAttrId) {
        continue
      }
      for (let j = 0; j < spec.attrList.length; j++) {
        const attr = spec.attrList[j]
        if (attr.attrId === curAttrId) {
          style[spec.name] = attr.value
          break
        }
      }
    }
    return Object.keys(style).length > 0 ? JSON.stringify(style) : ''
  },
  async toAddShopCar() {
    if (this.checkGoodsSelectAndStock()) {
      const res = await cart.addCart(
        parseInt(this.data.goodsId, 10),
        this.data.selectSkuId,
        this.data.buyNum,
        this.buildProductStyle()
      )
      const { error_code, msg } = res
      if (error_code !== undefined) {
        wx.showToast({
          icon: 'none',
          title: msg
        })
        return
      }
      wx.showToast({
        title: '添加成功'
      })
      this.closePopupTap()
      this.loadCartGoodsNum()
    }
  },
  async buyNow(e) {
    if (!(await this.checkUserLogin())) {
      return
    }
    this.setData({
      hideShopPopup: false,
      operationType: 2
    })
  },
  toBuyNow () {
    if (!this.checkGoodsSelectAndStock()) {
      return
    }
    const curSkuId = this.data.selectSkuId
    const goodsDetail = this.data.goodsDetail
    const skuList = goodsDetail.skuList || []
    let skuInst = null
    for (let i = 0; i < skuList.length; i++) {
      if (skuList[i].id === curSkuId) {
        skuInst = skuList[i]
        break
      }
    }
    if (!skuInst && skuList.length > 0) {
      skuInst = skuList[0]
    }
    if (!skuInst) {
      return
    }
    // 已选规格文案
    const specList = goodsDetail.specList || []
    const selectSpecs = this.data.selectSpecs
    const selectedValues = []
    for (let i = 0; i < specList.length; i++) {
      const spec = specList[i]
      const curAttrId = selectSpecs[spec.specId]
      for (let j = 0; j < spec.attrList.length; j++) {
        if (spec.attrList[j].attrId === curAttrId) {
          selectedValues.push(spec.attrList[j].value)
        }
      }
    }
    const goodsInfo = {
      goodsId: goodsDetail.id,
      skuId: skuInst.id,
      title: goodsDetail.title,
      price: skuInst.price || goodsDetail.price,
      picture: skuInst.picture || goodsDetail.picture,
      specs: selectedValues.join('; '),
      productStyle: this.buildProductStyle(),
      num: this.data.buyNum
    }
    // 直接购买，通过缓存传递
    wx.setStorageSync("goods", [goodsInfo])
    wx.navigateTo({
      url: '/pages/settlement/index'
    })
  },
  // 检查选择规格的状态和库存
  checkGoodsSelectAndStock () {
    if (this.data.outofStockStatus) {
      return
    }
    if (!this.data.selectReady) {
      wx.showToast({
        title: this.data.selectGoodsSku.stockName,
        icon: 'none'
      })
      return false
    }
    return true
  },
  onBuyNumChange(e) {
    this.setData({
      buyNum: e.detail.value
    })
  },
  onBuyNumLimit(e) {
    const max = e.detail.max
    wx.showToast({
      title: max < 999 ? `库存不足，最多可购买${max}件` : `不能超过${max}件`,
      icon: 'none'
    })
  },
  closePopupTap() {
    this.setData({
      hideShopPopup: true
    })
  },
  onShareAppMessage: function() {
    return {
      title: this.data.goodsDetail.title,
      path: `/pages/goods-details/index?id=${this.data.goodsId}`
    }
  },
  // 检查用户登录，未授权时弹微信原生授权弹窗
  async checkUserLogin() {
    return await requireLogin()
  }
})