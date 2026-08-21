import cart from '../../models/cart'
import { requireLogin, isAuthorized } from '../../utils/auth'

const MAX_NUMBER = 999

Page({
  data: {
    curPage: 1,
    pageSize: 500,
    goodsList: [],
    selectCart: {},
    selectAll: false,
    amount: 0,  // 结算金额
    selectNum: 0, // 结算数量
    maxNumber: MAX_NUMBER,
    needLogin: false // 未授权登录时展示登录提示
  },
  async onLoad() {
    wx.removeStorageSync("goods")
  },
  onShow() {
    this.setData({
      needLogin: !isAuthorized()
    })
    this.loadCartList()
  },
  async loadCartList(showLoading = true) {
    // 未授权时不请求购物车数据，避免展示静默登录账号下的内容
    if (!isAuthorized()) {
      this.setData({
        goodsList: [],
        selectCart: {},
        selectAll: false,
        amount: 0,
        selectNum: 0,
        needLogin: true
      })
      if (showLoading) {
        wx.hideLoading()
      }
      return
    }
    if (showLoading) {
      wx.showLoading({
        mask: true,
        title: '加载中'
      })
    }
    const res = await cart.getCartList(this.data.curPage, this.data.pageSize)
    const { error_code, msg } = res
    if (error_code !== undefined) {
      console.log(msg)
      if (showLoading) {
        wx.hideLoading()
      }
      return
    }
    // 后端购物车字段 -> 页面展示字段
    const list = (res.list || []).map(item => {
      const num = item.number || 0
      const stock = item.stock === null || item.stock === undefined ? null : item.stock
      const soldOut = stock !== null && stock <= 0
      return {
        ...item,
        goodsId: item.productId || item.bundleId,
        title: item.name,
        picture: item.image,
        specs: this.formatProductStyle(item.productStyle),
        price: num > 0 ? parseFloat((item.amount / num).toFixed(2)) : 0,
        num: num,
        stock: stock,
        hasStock: stock !== null,
        soldOut: soldOut,
        // 可修改数量的上限：库存与单商品上限取小
        maxNum: stock === null ? MAX_NUMBER : Math.max(1, Math.min(stock, MAX_NUMBER)),
        status: soldOut ? 2 : 0
      }
    })
    // 只保留当前列表中可结算商品的选中状态，已删除的商品不再残留
    const oldSelectCart = this.data.selectCart
    const selectCart = {}
    let selectAll = true
    let selectableCount = 0
    for (let i = 0; i < list.length; i++) {
      // 库存不足的商品不参与结算
      if (list[i].status !== 0) {
        continue
      }
      selectableCount++
      selectCart[list[i].id] = oldSelectCart[list[i].id] === true
      if (!selectCart[list[i].id]) {
        selectAll = false
      }
    }
    this.setData({
      goodsList: list,
      selectCart: selectCart,
      selectAll: selectableCount > 0 && selectAll,
      needLogin: false
    })
    this.calcGoodsAmount()
    if (showLoading) {
      wx.hideLoading()
    }
  },
  // 已选属性JSON -> 展示文案
  formatProductStyle(productStyle) {
    if (!productStyle) {
      return ''
    }
    try {
      const style = JSON.parse(productStyle)
      if (style && typeof style === 'object' && !Array.isArray(style)) {
        return Object.keys(style).map(key => style[key]).join('; ')
      }
    } catch (e) {
      // 旧数据可能是纯文本，直接展示
    }
    return productStyle
  },
  // 单选
  doSelectCart(e) {
    const selectCart = this.data.selectCart
    const id = e.currentTarget.dataset.id
    selectCart[id] = !selectCart[id]
   
    let selectAll = true
    for (let i in selectCart) {
      if (!selectCart[i]) {
        selectAll = false
      }
    }
    this.setData({
      selectCart: selectCart,
      selectAll: selectAll
    })
    this.calcGoodsAmount()
  },
  // 全选
  doSelectAll() {
    const selectAll = !this.data.selectAll
    const selectCart = this.data.selectCart
    const goodsList = this.data.goodsList
    for (let i = 0; i < goodsList.length; i++) {
      if (goodsList[i].status === 0) {
        selectCart[goodsList[i].id] = selectAll
      }
    }
    this.setData({
      selectAll: selectAll,
      selectCart: selectCart
    })
    this.calcGoodsAmount()
  },
  // 计算总金额
  calcGoodsAmount() {
    const goodsList = this.data.goodsList
    const selectCart = this.data.selectCart
    let amount = 0
    let selectNum = 0
    for (let i = 0; i < goodsList.length; i++) {
      const goods = goodsList[i]
      if (goods.status === 0 && selectCart[goods.id]) {
        amount += goods.price * goods.num
        selectNum++
      }
    }
    this.setData({
      amount: parseFloat(amount.toFixed(2)),
      selectNum: selectNum
    })
  },
  // 数量变化：先更新本地展示，连续操作停顿后再同步服务器
  onQtyChange(e) {
    const id = e.currentTarget.dataset.id
    const num = e.detail.value
    const goodsList = this.data.goodsList
    for (let i = 0; i < goodsList.length; i++) {
      if (goodsList[i].id === id) {
        goodsList[i].num = num
        this.setData({
          [`goodsList[${i}].num`]: num
        })
        this.calcGoodsAmount()
        this.scheduleSync(id, num)
        return
      }
    }
  },
  onQtyLimit(e) {
    const max = e.detail.max
    wx.showToast({
      icon: 'none',
      title: max < this.data.maxNumber
        ? `库存不足，最多可购买${max}件`
        : `不能超过${max}件`
    })
  },
  // 长按/连点会连续触发，合并成一次请求
  scheduleSync(id, number) {
    if (!this.pendingSync) {
      this.pendingSync = {}
    }
    this.pendingSync[id] = number
    clearTimeout(this.syncTimer)
    this.syncTimer = setTimeout(() => {
      this.flushSync()
    }, 300)
  },
  flushSync() {
    if (!this.pendingSync) {
      return
    }
    clearTimeout(this.syncTimer)
    const pending = this.pendingSync
    this.pendingSync = {}
    Object.keys(pending).forEach(key => {
      this.syncCartNumber(Number(key), pending[key])
    })
  },
  async syncCartNumber(id, number) {
    if (!this.syncing) {
      this.syncing = {}
      this.queuedSync = {}
    }
    // 同一商品已有请求在途时，只记录最新数量，等请求结束再发，避免覆盖顺序错乱
    if (this.syncing[id]) {
      this.queuedSync[id] = number
      return
    }
    this.syncing[id] = true
    const res = await cart.editCart(id, number)
    delete this.syncing[id]
    // 商品已被删除，忽略仍在途的数量修改结果，避免弹出“购物车记录不存在”
    if (this.deletedIds && this.deletedIds[id]) {
      delete this.queuedSync[id]
      return
    }
    const { error_code, msg } = res
    if (error_code !== undefined) {
      delete this.queuedSync[id]
      // 服务器拒绝（如库存不足）时以服务器数据为准；静默刷新，避免 loading 盖掉提示
      this.loadCartList(false)
      wx.showToast({
        icon: 'none',
        title: msg
      })
      return
    }
    if (this.queuedSync[id] !== undefined) {
      const next = this.queuedSync[id]
      delete this.queuedSync[id]
      this.syncCartNumber(id, next)
    }
  },
  onHide() {
    this.flushSync()
  },
  onUnload() {
    clearTimeout(this.syncTimer)
  },
  // 删除所选的购物车商品
  doDeleteSelected() {
    const goodsList = this.data.goodsList
    const selectCart = this.data.selectCart
    const ids = []
    for (let i = 0; i < goodsList.length; i++) {
      if (goodsList[i].status === 0 && selectCart[goodsList[i].id]) {
        ids.push(goodsList[i].id)
      }
    }
    if (ids.length === 0) {
      wx.showToast({
        icon: 'none',
        title: '请选择要删除的商品'
      })
      return
    }
    wx.showModal({
      title: '提示',
      content: `确定删除选中的 ${ids.length} 件商品吗？`,
      success: async (res) => {
        if (!res.confirm) {
          return
        }
        wx.showLoading({
          mask: true,
          title: '删除中'
        })
        // 标记为已删除，并清掉这些商品待同步的数量修改
        if (!this.deletedIds) {
          this.deletedIds = {}
        }
        ids.forEach(id => {
          this.deletedIds[id] = true
          if (this.pendingSync) {
            delete this.pendingSync[id]
          }
          if (this.queuedSync) {
            delete this.queuedSync[id]
          }
        })
        const result = await cart.deleteCart(ids)
        wx.hideLoading()
        const { error_code, msg } = result
        if (error_code !== undefined) {
          // 删除失败时恢复标记，商品仍在购物车中
          ids.forEach(id => delete this.deletedIds[id])
          wx.showToast({
            icon: 'none',
            title: msg
          })
          return
        }
        this.loadCartList(false)
        wx.showToast({
          icon: 'none',
          title: '删除成功'
        })
      }
    })
  },
  async doDeleteCart(e) {
    wx.showLoading({
      mask: true,
      title: '加载中'
    })
    const id = e.currentTarget.dataset.id
    // 删除前清掉该商品待同步的数量修改
    if (this.pendingSync) {
      delete this.pendingSync[id]
    }
    if (this.queuedSync) {
      delete this.queuedSync[id]
    }
    const res = await cart.editCart(id, 0)
    const { error_code, msg } = res
    if (error_code !== undefined) {
      wx.hideLoading()
      wx.showToast({
        icon: 'none',
        title: msg
      })
      return
    }
    this.loadCartList()
    wx.hideLoading()
  },
  goCategory() {
    wx.reLaunch({
      url: "/pages/category/category"
    });
  },
  async goLogin() {
    if (await requireLogin()) {
      this.setData({
        needLogin: false
      })
      this.loadCartList()
    }
  },
  // 点击商品图片进入商品详情
  goGoodsDetail(e) {
    const goodsId = e.currentTarget.dataset.id
    if (!goodsId) {
      return
    }
    wx.navigateTo({
      url: '/pages/goods-details/index?id=' + goodsId
    })
  },
  onPullDownRefresh() {
    this.loadCartList()
    wx.stopPullDownRefresh()
  },
  async goSettlement() {
    if (this.data.selectNum === 0) {
      wx.showToast({
        icon: 'none',
        title: '请选择结算的商品'
      })
      return
    }
    if (!(await requireLogin())) {
      return
    }
    // 提取结算的商品，通过缓存传递数据
    const goodsList = this.data.goodsList
    const selectCart = this.data.selectCart
    const selectGoods = []
    for (let i = 0; i < goodsList.length; i++) {
      const goods = goodsList[i]
      if (goods.status !== 0 || !selectCart[goods.id]) {
        continue
      }
      // 库存不足时不允许结算，先让用户调整数量
      if (goods.hasStock && goods.num > goods.stock) {
        wx.showToast({
          icon: 'none',
          title: `「${goods.title}」库存不足，请调整数量`
        })
        return
      }
      selectGoods.push(goods)
    }
    // 通过缓存传递数据
    wx.setStorageSync("goods", selectGoods)
    wx.navigateTo({
      url: '/pages/settlement/index'
    })
  }
})