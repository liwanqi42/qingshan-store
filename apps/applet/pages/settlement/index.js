import coupon from '../../models/coupon'
import address from '../../models/address'
import cart from '../../models/cart'
import order from '../../models/order'
import goods from '../../models/goods'
import CONFIG from '../../config'
import { requireLogin, isAuthorized } from '../../utils/auth'

Page({
  data: {
    curPage: 0,
    pageSize: 10,
    hasMoreCoupon: true,
    address: {},  // 收货地址
    displayPhone: '', // 按号码保护开关展示的手机号
    goodsList: [],
    goodsCount: 0,    // 商品总件数
    goodsAmount: 0,   // 商品金额
    discountAmount: 0, // 优惠金额
    freightAmount: 0, // 运费
    freightText: '免运费',
    amount: 0,  // 实付金额
    couponList: [], // 优惠券
    selectedCoupon: {}, // 使用的优惠券
    addOnGoods: [], // 随手买推荐商品
    selectedAddOn: {}, // 已选择的随手买商品
    addOnPopupVisible: false, // 随手买规格选择弹窗
    addOnDetail: {}, // 正在选择规格的随手买商品
    addOnSelectSpecs: {}, // 随手买规格选择：specId -> attrId，0 表示未选
    addOnSelectSpecAttr: {}, // 随手买规格可选状态：specId-attrId -> true
    addOnSelectSkuId: 0,
    addOnSelectStock: 0,
    addOnSelectReady: false, // 规格是否已选完整
    addOnSelectText: '', // 规格选择提示文案
    addOnSelectPrice: '',
    addOnSelectPicture: '',
    remark: '', // 订单备注
    remarkLength: 0,
    payMethods: [
      { value: 1, name: '微信支付', desc: '推荐' },
      // { value: 2, name: '支付宝', desc: '' },
      // { value: 3, name: '银行卡', desc: '' }
    ],
    payMethod: 1,
    numberProtection: true // 号码保护
  },
  async onLoad() {
    const goodsList = (wx.getStorageSync("goods") || []).map(item => this.withMaxNum(item))
    this.setData({
      curPage: 0,
      goodsList: goodsList
    })
    this.loadCouponList()
    this.loadDefaultAddress()
    this.loadAddOnGoods()
  },
  // 计算可购买上限：有库存时取库存与999的较小值，否则不限制
  withMaxNum(item) {
    if (item.maxNum) {
      return item
    }
    const stock = item.stock === null || item.stock === undefined ? null : Number(item.stock)
    return Object.assign({}, item, {
      maxNum: stock !== null && stock > 0 ? Math.min(stock, 999) : 999
    })
  },
  // 修改商品数量
  onQtyChange(e) {
    const index = Number(e.currentTarget.dataset.index)
    const num = e.detail.value
    const goodsItem = this.data.goodsList[index]
    if (!goodsItem) {
      return
    }
    // 购物车中的商品同步修改购物车记录；随手买/立即购买商品只影响本单
    if (goodsItem.id) {
      this.scheduleCartSync(goodsItem.id, num)
    }
    this.setData({
      [`goodsList[${index}].num`]: num
    }, () => this.calcOrderAmount())
  },
  // 合并连续的数量修改，停顿后再同步到购物车
  scheduleCartSync(id, number) {
    if (!this.cartSyncQueue) {
      this.cartSyncQueue = {}
    }
    this.cartSyncQueue[id] = number
    clearTimeout(this.cartSyncTimer)
    this.cartSyncTimer = setTimeout(() => {
      this.flushCartSync()
    }, 400)
  },
  flushCartSync() {
    clearTimeout(this.cartSyncTimer)
    const queue = this.cartSyncQueue || {}
    this.cartSyncQueue = {}
    return Promise.all(Object.keys(queue).map(id => cart.editCart(Number(id), queue[id])))
  },
  onHide() {
    this.flushCartSync()
  },
  onUnload() {
    this.flushCartSync()
  },
  onQtyLimit(e) {
    const max = e.detail.max
    wx.showToast({
      icon: 'none',
      title: max < 999 ? `库存不足，最多可购买${max}件` : `不能超过${max}件`
    })
  },
  onShow() {
    this.updatePhoneDisplay()
    this.calcOrderAmount()
  },
  // 加载默认收货地址
  async loadDefaultAddress() {
    // 未授权时不展示静默登录账号下的地址
    if (!isAuthorized()) {
      this.setData({
        address: {},
        displayPhone: ''
      })
      return
    }
    const res = await address.getDefaultAddress()
    if (!res || res.error_code !== undefined || !res.id) {
      this.setData({
        address: {},
        displayPhone: ''
      })
      return
    }
    this.setData({
      address: res
    }, () => this.updatePhoneDisplay())
  },
  // 根据号码保护开关生成展示用手机号
  updatePhoneDisplay() {
    const phone = (this.data.address && this.data.address.phone) || ''
    const displayPhone = this.data.numberProtection && phone
      ? phone.replace(/^(\d{3})\d{4}(\d{4})$/, '$1****$2')
      : phone
    this.setData({
      displayPhone: displayPhone
    })
  },
  // 加载随手买推荐商品
  async loadAddOnGoods() {
    const res = await goods.getGoodsList('', 0, null, 1, 8)
    if (!res || res.error_code !== undefined || !Array.isArray(res.list)) {
      return
    }
    // 排除已在订单中的商品
    const cartGoodsIds = {}
    const goodsList = this.data.goodsList
    for (let i = 0; i < goodsList.length; i++) {
      if (goodsList[i].goodsId) {
        cartGoodsIds[goodsList[i].goodsId] = true
      }
    }
    const addOnGoods = res.list
      .filter(item => item.id && item.status === 1 && item.stock > 0 && !cartGoodsIds[item.id])
      .slice(0, 6)
    this.setData({
      addOnGoods: addOnGoods
    })
  },
  // 选择/取消随手买商品
  async toggleAddOn(e) {
    const id = e.currentTarget.dataset.id
    const product = this.data.addOnGoods.find(item => item.id === id)
    if (!product) {
      return
    }
    // 已选中：从商品清单中移除
    if (this.data.selectedAddOn[id]) {
      this.removeAddOn(id)
      return
    }
    wx.showLoading({
      mask: true,
      title: '加载中'
    })
    const detail = await this.loadAddOnDetail(id)
    wx.hideLoading()
    if (!detail) {
      wx.showToast({
        icon: 'none',
        title: '商品不存在或已下架'
      })
      return
    }
    // 没有样式的商品直接加入清单，有样式的先弹窗选择
    if (detail.specList.length === 0) {
      this.addAddOnWithoutSpec(detail)
      return
    }
    this.openAddOnSpecPopup(detail)
  },
  // 加载随手买商品详情（含规格和SKU），同一商品只请求一次
  async loadAddOnDetail(id) {
    if (!this.addOnDetailCache) {
      this.addOnDetailCache = {}
    }
    if (this.addOnDetailCache[id]) {
      return this.addOnDetailCache[id]
    }
    const detail = await goods.getGoodsDetails(id)
    if (!detail || detail.error_code !== undefined || !detail.id) {
      return null
    }
    if (!Array.isArray(detail.specList)) {
      detail.specList = []
    }
    if (!Array.isArray(detail.skuList)) {
      detail.skuList = []
    }
    this.addOnDetailCache[id] = detail
    return detail
  },
  // 移除已选的随手买商品
  removeAddOn(id) {
    const selectedAddOn = Object.assign({}, this.data.selectedAddOn)
    delete selectedAddOn[id]
    this.setData({
      selectedAddOn: selectedAddOn,
      goodsList: this.data.goodsList.filter(item => !(item.isAddOn && item.goodsId === id))
    }, () => this.calcOrderAmount())
  },
  // 无样式商品：取有库存的SKU，没有SKU时按商品本身的价格和库存
  addAddOnWithoutSpec(detail) {
    const skuList = detail.skuList || []
    let sku = null
    for (let i = 0; i < skuList.length; i++) {
      if ((skuList[i].stock || 0) > 0) {
        sku = skuList[i]
        break
      }
    }
    if (skuList.length > 0 && !sku) {
      wx.showToast({
        icon: 'none',
        title: '该商品暂时缺货'
      })
      return
    }
    const stock = sku ? (sku.stock || 0) : (detail.stock || 0)
    if (stock <= 0) {
      wx.showToast({
        icon: 'none',
        title: '该商品暂时缺货'
      })
      return
    }
    this.addAddOnGoods({
      detail: detail,
      skuId: sku && sku.id > 0 ? sku.id : null,
      productStyle: '',
      specsText: '',
      price: (sku && sku.price) || detail.price,
      picture: (sku && sku.picture) || detail.picture,
      stock: stock
    })
  },
  // 打开规格选择弹窗
  openAddOnSpecPopup(detail) {
    const selectSpecs = {}
    for (let i = 0; i < detail.specList.length; i++) {
      selectSpecs[detail.specList[i].specId] = 0
    }
    this.setData({
      addOnPopupVisible: true,
      addOnDetail: detail,
      addOnSelectSpecs: selectSpecs,
      addOnSelectSpecAttr: {},
      addOnSelectSkuId: 0,
      addOnSelectStock: 0,
      addOnSelectReady: false,
      addOnSelectText: '',
      addOnSelectPrice: detail.price,
      addOnSelectPicture: detail.picture
    })
    this.updateAddOnSpecAttr()
    this.calcAddOnSelectedText()
  },
  closeAddOnPopup() {
    this.setData({
      addOnPopupVisible: false,
      addOnDetail: {}
    })
  },
  // 选择随手买商品规格
  doSelectAddOnSpec(e) {
    const specId = e.currentTarget.dataset.specId
    const attrId = e.currentTarget.dataset.attrId
    if (!this.data.addOnSelectSpecAttr[specId + '-' + attrId]) {
      return
    }
    const selectSpecs = Object.assign({}, this.data.addOnSelectSpecs)
    selectSpecs[specId] = selectSpecs[specId] === attrId ? 0 : attrId
    this.setData({
      addOnSelectSpecs: selectSpecs
    })
    this.calcAddOnSelectedText()
    this.updateAddOnSpecAttr()
  },
  // 计算已选规格文案及对应的SKU
  calcAddOnSelectedText() {
    const specList = this.data.addOnDetail.specList || []
    const selectSpecs = this.data.addOnSelectSpecs
    const missing = []
    const selected = []
    for (let i = 0; i < specList.length; i++) {
      const spec = specList[i]
      const curAttrId = selectSpecs[spec.specId]
      if (curAttrId !== 0) {
        for (let j = 0; j < spec.attrList.length; j++) {
          if (spec.attrList[j].attrId === curAttrId) {
            selected.push(spec.attrList[j].value)
            break
          }
        }
        continue
      }
      missing.push(spec.name)
    }
    const ready = missing.length === 0
    this.setData({
      addOnSelectReady: ready,
      addOnSelectText: ready
        ? (selected.length > 0 ? '已选：' + selected.join('、') : '默认规格')
        : '请选择：' + missing.join('、')
    })
    this.updateAddOnSelectedSku()
  },
  // 已选完整规格后同步SKU价格、图片和库存
  updateAddOnSelectedSku() {
    const detail = this.data.addOnDetail
    let stock = 0
    let price = detail.price
    let picture = detail.picture
    let skuId = 0
    if (this.data.addOnSelectReady) {
      const matchSku = this.findAddOnSku(this.data.addOnSelectSpecs)
      if (matchSku) {
        skuId = matchSku.id > 0 ? matchSku.id : 0
        stock = matchSku.stock || 0
        price = matchSku.price || price
        picture = matchSku.picture || picture
      } else if ((detail.skuList || []).length === 0) {
        // 没有SKU数据的商品，按商品本身的库存和价格下单
        stock = detail.stock || 0
      }
    }
    this.setData({
      addOnSelectSkuId: skuId,
      addOnSelectStock: stock,
      addOnSelectPrice: price,
      addOnSelectPicture: picture
    })
  },
  // 按已选规格匹配SKU
  findAddOnSku(selectSpecs) {
    const skuList = this.data.addOnDetail.skuList || []
    let matchSku = null
    for (let i = 0; i < skuList.length; i++) {
      const sku = skuList[i]
      let specs = []
      try {
        specs = JSON.parse(sku.specs)
      } catch (e) {
        specs = []
      }
      if (!Array.isArray(specs)) {
        specs = []
      }
      let match = true
      for (let j = 0; j < specs.length; j++) {
        if (selectSpecs[specs[j].keyId] !== specs[j].valueId) {
          match = false
          break
        }
      }
      if (match) {
        matchSku = sku
      }
    }
    return matchSku
  },
  // 无库存的规格组合置灰：只参考“其他规格”的已选值，同一规格内可直接切换
  updateAddOnSpecAttr() {
    const specList = this.data.addOnDetail.specList || []
    const skuList = this.data.addOnDetail.skuList || []
    const selectSpecs = this.data.addOnSelectSpecs
    const selectSpecAttr = {}
    for (let i = 0; i < specList.length; i++) {
      const spec = specList[i]
      for (let j = 0; j < spec.attrList.length; j++) {
        selectSpecAttr[spec.specId + '-' + spec.attrList[j].attrId] = false
      }
    }
    if (skuList.length === 0) {
      // 没有SKU数据时无法判断库存，所有规格都可选
      for (let key in selectSpecAttr) {
        selectSpecAttr[key] = true
      }
    } else {
      for (let key in selectSpecAttr) {
        const separator = key.indexOf('-')
        const specId = Number(key.slice(0, separator))
        const attrId = Number(key.slice(separator + 1))
        // 当前已选中的值始终可点，便于取消或直接切换到其他值
        if (selectSpecs[specId] === attrId) {
          selectSpecAttr[key] = true
          continue
        }
        selectSpecAttr[key] = skuList.some(sku =>
          this.addOnSkuSupportsAttr(sku, specId, attrId, selectSpecs)
        )
      }
    }
    this.setData({
      addOnSelectSpecAttr: selectSpecAttr
    })
  },
  // 判断SKU是否有库存且包含指定规格值，同时满足其他规格的已选值
  addOnSkuSupportsAttr(sku, specId, attrId, selectSpecs) {
    if (sku.stock <= 0) {
      return false
    }
    let specs = []
    try {
      specs = JSON.parse(sku.specs)
    } catch (e) {
      specs = []
    }
    if (!Array.isArray(specs)) {
      specs = []
    }
    // 空规格SKU表示所有规格组合都可用
    if (specs.length === 0) {
      return true
    }
    let matched = false
    for (let i = 0; i < specs.length; i++) {
      const skuSpec = specs[i]
      if (skuSpec.keyId === specId) {
        if (skuSpec.valueId !== attrId) {
          return false
        }
        matched = true
        continue
      }
      const curAttrId = selectSpecs[skuSpec.keyId]
      if (curAttrId && curAttrId !== skuSpec.valueId) {
        return false
      }
    }
    return matched
  },
  // 确认选择随手买商品规格并加入商品清单
  confirmAddOn() {
    if (!this.data.addOnSelectReady) {
      wx.showToast({
        icon: 'none',
        title: this.data.addOnSelectText || '请选择商品样式'
      })
      return
    }
    if (this.data.addOnSelectStock <= 0) {
      wx.showToast({
        icon: 'none',
        title: '该样式暂时缺货'
      })
      return
    }
    const detail = this.data.addOnDetail
    const specList = detail.specList || []
    const selectSpecs = this.data.addOnSelectSpecs
    const style = {}
    const selectedValues = []
    for (let i = 0; i < specList.length; i++) {
      const spec = specList[i]
      const curAttrId = selectSpecs[spec.specId]
      if (!curAttrId) {
        continue
      }
      for (let j = 0; j < spec.attrList.length; j++) {
        if (spec.attrList[j].attrId === curAttrId) {
          style[spec.name] = spec.attrList[j].value
          selectedValues.push(spec.attrList[j].value)
          break
        }
      }
    }
    this.addAddOnGoods({
      detail: detail,
      skuId: this.data.addOnSelectSkuId > 0 ? this.data.addOnSelectSkuId : null,
      productStyle: Object.keys(style).length > 0 ? JSON.stringify(style) : '',
      specsText: selectedValues.join('; '),
      price: this.data.addOnSelectPrice,
      picture: this.data.addOnSelectPicture,
      stock: this.data.addOnSelectStock
    })
    this.closeAddOnPopup()
  },
  // 把随手买商品加入商品清单
  addAddOnGoods({detail, skuId, productStyle, specsText, price, picture, stock}) {
    if (this.data.selectedAddOn[detail.id]) {
      return
    }
    // 同一商品只保留一条记录
    const goodsList = this.data.goodsList.filter(item => !(item.isAddOn && item.goodsId === detail.id))
    goodsList.push({
      goodsId: detail.id,
      productId: detail.id,
      skuId: skuId,
      title: detail.title,
      price: price,
      picture: picture,
      specs: specsText || '',
      productStyle: productStyle || '',
      num: 1,
      stock: stock,
      maxNum: Math.max(1, Math.min(stock, 999)),
      isAddOn: true
    })
    const selectedAddOn = Object.assign({}, this.data.selectedAddOn)
    selectedAddOn[detail.id] = true
    this.setData({
      selectedAddOn: selectedAddOn,
      goodsList: goodsList
    }, () => this.calcOrderAmount())
  },
  // 加载优惠券列表
  async loadCouponList() {
    const curPage = this.data.curPage + 1
    const res = await coupon.getUserCouponList(0, curPage, this.data.pageSize)
    const { error_code, msg } = res
    if (error_code !== undefined) {
      console.log(msg)
      return
    }
    const coupons = []
    for (let i = 0; i < res.list.length; i++) {
      const couponDO = res.list[i]
      // 券类型：1-满减券 2-折扣券 3-代金券 4-满金额折扣券
      let title
      switch(couponDO.type) {
        case 1:
          title = '满' + parseFloat(couponDO.fullMoney, 10) + '减' + parseFloat(couponDO.minus, 10)
          break;
        case 2:
          title = couponDO.rate * 10 + '折券'
          break;
        case 3:
          title = parseFloat(couponDO.minus, 10) + '元代金券'
          break;
        case 4:
          title = '满' + parseFloat(couponDO.fullMoney, 10) + '享' + couponDO.rate * 10 + '折'
          break;
        default:
          title = ''
      }
      const startDateArr = couponDO.startTime.split(' ')[0].split('-')
      const endDateArr = couponDO.endTime.split(' ')[0].split('-')
      coupons.push({
        cLogId: couponDO.CLogId,
        couponId: couponDO.couponId,
        title: title,
        type: couponDO.type,
        fullMoney: parseFloat(couponDO.fullMoney, 10),
        minus: parseFloat(couponDO.minus, 10),
        rate: parseFloat(couponDO.rate, 10),
        period: startDateArr[0] + '/' + startDateArr[1] + '/' + startDateArr[2] + '-' + endDateArr[0] + '/' + endDateArr[1] + '/' + endDateArr[2]
      })
    }
    let couponList = this.data.couponList
    if (curPage === 1) {
      couponList = coupons
    } else {
      couponList = couponList.concat(coupons)
    }
    this.setData({
      curPage: curPage,
      couponList: couponList,
      hasMoreCoupon: res.list.length === this.data.pageSize
    })
  },
  async selectAddress(e) {
    if (!(await requireLogin())) {
      return
    }
    const addressId = e.currentTarget.dataset.id
    wx.navigateTo({
      url: '/pages/option-address/index?id=' + addressId
    })
  },
  // 选择券
  selectCoupon(e) {
    let couponLogId = e.currentTarget.dataset.id
    let selectedCoupon = this.data.selectedCoupon

    if (couponLogId === selectedCoupon.cLogId) {
      selectedCoupon = {}
    } else {
      const couponList = this.data.couponList
      for (let i = 0; i < couponList.length; i++) {
        const coupon = couponList[i]
        if (coupon.cLogId === couponLogId) {
          selectedCoupon = coupon
          break
        }
      }
    }
    this.setData({
      selectedCoupon: selectedCoupon
    })
    this.calcOrderAmount()
  },
  // 订单备注
  bindRemark(e) {
    const remark = e.detail.value || ''
    this.setData({
      remark: remark,
      remarkLength: remark.length
    })
  },
  // 选择支付方式
  selectPayMethod(e) {
    this.setData({
      payMethod: Number(e.currentTarget.dataset.value)
    })
  },
  // 号码保护开关
  toggleNumberProtection(e) {
    this.setData({
      numberProtection: e.detail.value
    }, () => this.updatePhoneDisplay())
  },
  // 计算结算的金额
  calcOrderAmount() {
    const goodsList = this.data.goodsList
    let goodsAmount = 0
    let goodsCount = 0
    for (let i = 0; i < goodsList.length; i++) {
      const goodsItem = goodsList[i]
      const price = parseFloat(goodsItem.price) || 0
      const num = parseInt(goodsItem.num, 10) || 0
      goodsAmount += price * num
      goodsCount += num
    }
    goodsAmount = parseFloat(goodsAmount.toFixed(2))
    const discountAmount = parseFloat(this.calcDiscountAmount(goodsAmount).toFixed(2))
    const freightAmount = 0
    const amount = Math.max(0, parseFloat((goodsAmount - discountAmount + freightAmount).toFixed(2)))
    this.setData({
      goodsAmount: goodsAmount,
      goodsCount: goodsCount,
      discountAmount: discountAmount,
      freightAmount: freightAmount,
      freightText: freightAmount > 0 ? '￥' + freightAmount : '免运费',
      amount: amount
    })
  },
  // 计算优惠金额
  calcDiscountAmount(goodsAmount) {
    goodsAmount = parseFloat(goodsAmount.toFixed(2), 10)
    const selectedCoupon = this.data.selectedCoupon
    let discountAmount = 0
    if (selectedCoupon.cLogId !== undefined) {
      switch(selectedCoupon.type) {
        case 1:
          if (goodsAmount >= selectedCoupon.fullMoney) {
            discountAmount = selectedCoupon.minus
          }
          break;
        case 2:
          discountAmount = (goodsAmount - goodsAmount * selectedCoupon.rate).toFixed(2)
          break;
        case 3:
          discountAmount = selectedCoupon.minus
          break;
        case 4:
          if (goodsAmount >= selectedCoupon.fullMoney) {
            discountAmount = (goodsAmount - goodsAmount * selectedCoupon.rate).toFixed(2)
          }
          break;
        default:
          discountAmount = 0
      }
    }
    return parseFloat(discountAmount, 10)
  },
  async submitOrder() {
    if (!(await requireLogin())) {
      return
    }
    // 刚完成登录时补拉默认地址
    if (!this.data.address.id) {
      await this.loadDefaultAddress()
    }
    const addressId = this.data.address.id
    if (!addressId) {
      wx.showToast({
        icon: 'none',
        title: '请添加收货地址'
      })
      return
    }
    const amount = this.data.amount
    const goodsList = this.data.goodsList
    if (goodsList.length === 0) {
      wx.showToast({
        icon: 'none',
        title: '请选择商品'
      })
      return
    }
    // 后端按提交的明细生成订单，提交前先把待同步的数量写回购物车
    await this.flushCartSync().catch(() => {})
    // 购物车、直接购买和随手买的商品都随下单请求一起提交，
    // 前端提交的清单即本单商品，避免只下单购物车里的全部商品
    const params = {
      addressBookId: addressId,
      payMethod: this.data.payMethod,
      remark: this.data.remark.trim(),
      freightAmount: this.data.freightAmount.toFixed(2),
      amount: amount.toFixed(2),
      items: this.buildOrderItems()
    }
    const res = await order.placeOrder(params)
    const { error_code, msg } = res
    if (error_code !== undefined) {
      wx.showToast({
        icon: 'none',
        title: msg
      })
      return
    }
    const payResult = await this.payOrder(res.orderNumber)
    if (payResult === 'cancel') {
      // 用户取消支付：订单保持待付款，跳到详情页展示订单信息和自动取消倒计时。
      // 用 redirectTo 替换结算页，避免返回后重复提交订单。
      wx.showToast({
        icon: 'none',
        title: '已取消支付'
      })
      setTimeout(() => {
        wx.redirectTo({
          url: '/pages/order-details/index?orderNo=' + res.orderNumber
        })
      }, 500)
      return
    }
    if (payResult === 'success') {
      wx.showToast({
        title: '支付成功'
      })
      setTimeout(() => {
        wx.navigateTo({
          url: '/pages/order-details/index?origin=1&orderNo=' + res.orderNumber
        })
      }, 1500)
    }
  },
  /**
   * 发起支付，返回 success / cancel / fail
   *
   * 开发环境用弹窗模拟微信支付面板，可以分别测试支付成功和取消支付；
   * 关闭 config.mockPay 后走真实微信支付。
   */
  async payOrder(orderNumber) {
    if (CONFIG.mockPay) {
      const modalRes = await new Promise(resolve => {
        wx.showModal({
          title: '模拟支付',
          content: '仅开发环境：请选择本次支付结果',
          confirmText: '支付成功',
          cancelText: '取消支付',
          success: res => resolve(res.confirm ? 'success' : 'cancel'),
          fail: () => resolve('fail')
        })
      })
      if (modalRes === 'cancel') {
        return 'cancel'
      }
      if (modalRes !== 'success') {
        return 'fail'
      }
      const payRes = await order.mockPay(orderNumber)
      if (payRes && payRes.error_code !== undefined) {
        wx.showToast({
          icon: 'none',
          title: payRes.msg || '支付失败'
        })
        return 'fail'
      }
      return 'success'
    }

    // TODO(wechat-pay): 接入真实微信支付后使用以下流程
    const payRes = await order.payment(orderNumber, this.data.payMethod)
    if (!payRes || payRes.error_code !== undefined) {
      wx.showToast({
        icon: 'none',
        title: (payRes && payRes.msg) || '支付失败'
      })
      return 'fail'
    }
    return new Promise(resolve => {
      wx.requestPayment({
        timeStamp: payRes.timeStamp,
        nonceStr: payRes.nonceStr,
        package: payRes.packageStr,
        signType: payRes.signType,
        paySign: payRes.paySign,
        success: () => resolve('success'),
        fail: err => {
          if (err.errMsg && err.errMsg.indexOf('cancel') > -1) {
            resolve('cancel')
            return
          }
          wx.showToast({
            icon: 'none',
            title: '支付失败'
          })
          resolve('fail')
        }
      })
    })
  },
  // 组装下单明细：购物车商品带上购物车记录id，下单后由后端清理；
  // 直接购买和随手买商品不在购物车中，按商品/SKU/规格下单
  buildOrderItems() {
    const goodsList = this.data.goodsList
    const items = []
    for (let i = 0; i < goodsList.length; i++) {
      const goods = goodsList[i]
      const bundleId = goods.bundleId || null
      const productId = bundleId ? null : (goods.productId || goods.goodsId)
      if (!productId && !bundleId) {
        continue
      }
      items.push({
        cartId: goods.id || null,
        productId: productId,
        bundleId: bundleId,
        skuId: goods.skuId > 0 ? goods.skuId : null,
        productStyle: goods.productStyle || '',
        number: parseInt(goods.num, 10) || 1
      })
    }
    return items
  },
  scrolltolowerEvent() {
    if (!this.data.hasMoreCoupon) {
      return
    }
    this.loadCouponList()
  }
})
