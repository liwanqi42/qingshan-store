import order from '../../models/order'
import cart from '../../models/cart'

// 手机号脱敏：15812345678 -> 158****5678
function maskPhone(phone) {
  if (!phone) {
    return ''
  }
  if (phone.length < 7) {
    return phone
  }
  return phone.substring(0, 3) + '****' + phone.substring(phone.length - 4)
}

// 地址脱敏：保留省市区，遮住详细地址（门牌号）
function maskAddress(fullAddress, detailAddress) {
  if (!fullAddress) {
    return ''
  }
  if (detailAddress && fullAddress.length > detailAddress.length
      && fullAddress.slice(-detailAddress.length) === detailAddress) {
    return fullAddress.slice(0, fullAddress.length - detailAddress.length) + '****'
  }
  // 匹配不到详细地址时，退化为遮住末尾 4 个字符
  if (fullAddress.length <= 4) {
    return '****'
  }
  return fullAddress.substring(0, fullAddress.length - 4) + '****'
}

// 支付方式
const PAY_METHOD = {
  1: '微信支付',
  2: '支付宝',
  3: '银行卡'
}

// 小程序端状态 -> 收货地址上方的货物状态
const SHIP_STATUS = {
  '-1': '交易关闭',
  0: '待付款',
  1: '待发货',
  2: '待收货',
  3: '已签收',
  4: '售后中',
  5: '售后完成'
}

// 小程序端状态 -> 导航栏标题
const STATUS_TITLE = {
  '-1': '交易关闭',
  0: '待付款',
  1: '待发货',
  2: '待收货',
  3: '交易成功',
  4: '售后中',
  5: '售后完成'
}

// 未发货只能仅退款，同意后直接退款
const AFTER_SALES_TYPES_BEFORE_SHIP = [
  {
    id: 1,
    label: '仅退款'
  }
]

// 已发货只能退货退款/换货/补寄，退货退款需寄回商品后退款
const AFTER_SALES_TYPES_AFTER_SHIP = [
  {
    id: 2,
    label: '退货退款'
  },
  {
    id: 3,
    label: '换货'
  },
  {
    id: 4,
    label: '补寄'
  }
]

Page({
  data: {
    orderNo: '',
    origin: 0,
    refundPopup: false,
    selectType: 0,
    selectReason: 0,
    orderDetail: {},
    showMore: false,
    shipStatus: '',
    payDetailOpen: false,
    orderInfoOpen: false,
    rawMobile: '',
    displayMobile: '',
    displayFullAddress: '',
    showRawContact: false,
    payMethodText: '',
    // 从订单列表点击“申请售后”进入时自动打开售后弹窗
    refundRequested: false,
    // 售后类型：未发货仅退款；已发货退货退款/换货/补寄
    afterSalesTypes: AFTER_SALES_TYPES_AFTER_SHIP,
    refundReason: [
      {
        id: 1,
        reason: '我不想买了'
      },
      {
        id: 2,
        reason: '信息填写错误，重新拍'
      },
      {
        id: 3,
        reason: '卖家缺货'
      },

      {
        id: 4,
        reason: '其它原因'
      }
    ],
    refundAllow: false,  // 是否允许退款
    countdownText: '',   // 待付款订单自动取消倒计时
    payDisabled: false   // 倒计时结束后禁用付款
  },
  onLoad(e) {
    console.log(e)
    this.setData({
      origin: parseInt(e.origin, 10) || 0,
      orderNo: e.orderNo || '',
      refundRequested: e.refund === '1'
    })
  },
  onShow() {
    this.closePopup()
    this.loadOrderDetail()
  },
  // 加载订单
  async loadOrderDetail() {
    wx.showLoading({
      mask: true,
      title: '加载中'
    })
    const res = await order.getOrderDetail(this.data.orderNo)
    const { error_code, msg } = res
    if (error_code !== undefined) {
      console.log(msg)
      wx.hideLoading()
      return
    }
    const status = res.status
    // 待发货、待收货、已完成的订单均可申请售后；已有售后记录时不再展示入口
    const refundAllow = (status === 1 || status === 2 || status === 3)
      && (!res.refundApply || res.refundApply.refundNo === '')
    wx.setNavigationBarTitle({
      title: STATUS_TITLE[status] || '订单详情'
    })
    this.setData({
      orderDetail: res,
      shipStatus: SHIP_STATUS[status] || '',
      rawMobile: res.address.mobile || '',
      displayMobile: maskPhone(res.address.mobile),
      displayFullAddress: maskAddress(res.address.fullAddress, res.address.address),
      payMethodText: PAY_METHOD[res.payMethod] || '-',
      refundAllow: refundAllow,
      // 未发货（待发货）只能仅退款，已发货（待收货/已完成）走退货退款等流程
      afterSalesTypes: status === 1
        ? AFTER_SALES_TYPES_BEFORE_SHIP : AFTER_SALES_TYPES_AFTER_SHIP,
      // 有任意“更多”操作时才显示更多入口
      showMore: status === 0 || status === 2 || status === 3
        || status === -1 || status === 5 || refundAllow
    })
    wx.hideLoading()
    // 待付款订单展示自动取消倒计时
    this.startAutoCancelCountdown(res)
    // 从列表的“申请售后”进入，详情加载完成后直接打开申请售后弹窗
    if (this.data.refundRequested) {
      this.setData({
        refundRequested: false
      })
      if (refundAllow) {
        this.openRefundPopup()
      }
    }
  },
  // 展开/收起实付款明细
  togglePayDetail() {
    this.setData({
      payDetailOpen: !this.data.payDetailOpen
    })
  },
  // 展开/收起订单信息
  toggleOrderInfo() {
    this.setData({
      orderInfoOpen: !this.data.orderInfoOpen
    })
  },
  // 收货信息脱敏/明文切换
  toggleContact() {
    this.setData({
      showRawContact: !this.data.showRawContact
    })
  },
  // 客服
  contactService() {
    wx.showToast({
      icon: 'none',
      title: '客服功能暂未开放'
    })
  },
  // 更多操作
  showMoreActions() {
    const detail = this.data.orderDetail
    if (!detail || detail.id === undefined) {
      return
    }
    const actions = []
    if (detail.status === 0) {
      actions.push({ name: '取消订单', action: 'cancel' })
    }
    if (detail.status === 2 || detail.status === 3) {
      actions.push({ name: '查看物流', action: 'logistics' })
    }
    if (this.data.refundAllow) {
      actions.push({ name: '申请售后', action: 'refund' })
    }
    if (detail.status === -1 || detail.status === 3 || detail.status === 5) {
      actions.push({ name: '删除订单', action: 'delete' })
    }
    if (actions.length === 0) {
      return
    }
    wx.showActionSheet({
      itemList: actions.map(item => item.name),
      success: res => {
        const action = actions[res.tapIndex]
        if (!action) {
          return
        }
        if (action.action === 'cancel') {
          this.cancelOrderById(detail.id)
        } else if (action.action === 'logistics') {
          wx.navigateTo({
            url: '/pages/logistics/index?orderNo=' + detail.orderNo
          })
        } else if (action.action === 'refund') {
          this.openRefundPopup()
        } else if (action.action === 'delete') {
          this.deleteOrderById(detail.id)
        }
      }
    })
  },
  // 打开申请售后弹窗
  openRefundPopup() {
    wx.setNavigationBarTitle({
      title: '申请售后'
    })
    this.setData({
      refundPopup: true,
      selectType: 0,
      selectReason: 0
    })
  },
  // 申请售后
  orderRefund() {
    this.openRefundPopup()
  },
  // 确认售后申请
  async confirmRefund() {
    const refundType = this.data.selectType
    if (refundType === 0) {
      wx.showToast({
        title: '请选择售后类型',
        icon: 'none'
      })
      return
    }
    const reasonId = this.data.selectReason
    if (reasonId === 0) {
      wx.showToast({
        title: '请选择售后原因',
        icon: 'none'
      })
      return
    }
    const refundReason = this.data.refundReason
    let reason = ''
    for (let i = 0; i < refundReason.length; i++) {
      if (refundReason[i].id === reasonId) {
        reason = refundReason[i].reason
      }
    }
    const res = await order.refundApply(this.data.orderNo, refundType, reason)
    const { error_code, msg } = res
    if (error_code !== undefined) {
      wx.showToast({
        icon: 'none',
        title: msg || '申请失败'
      })
      return
    }
    wx.navigateTo({
      url: '/pages/refund-details/index?refundNo=' + res.refundNo
    })
  },
  stopPropagation() {
    // 阻止冒泡
  },
  closePopup() {
    const status = this.data.orderDetail.status
    wx.setNavigationBarTitle({
      title: STATUS_TITLE[status] || '订单详情'
    })
    this.setData({
      refundPopup: false,
      selectType: 0,
      selectReason: 0
    })
  },
  selectType(e) {
    this.setData({
      selectType: parseInt(e.currentTarget.dataset.id, 10)
    })
  },
  selectReason(e) {
    this.setData({
      selectReason: parseInt(e.currentTarget.dataset.id, 10)
    })
  },
  // 取消订单
  cancelOrderById(id) {
    wx.showModal({
      title: '取消订单',
      content: '确认取消订单？',
      success: res => {
        if (!res.confirm) {
          return
        }
        order.cancelOrder(id).then(result => {
          if (result && result.error_code !== undefined) {
            wx.showToast({
              icon: 'none',
              title: result.msg || '取消失败'
            })
            return
          }
          this.loadOrderDetail()
        })
      }
    })
  },
  // 删除订单
  deleteOrderById(id) {
    wx.showModal({
      title: '删除订单',
      content: '确认删除订单？',
      success: res => {
        if (!res.confirm) {
          return
        }
        order.deleteOrder(id).then(result => {
          if (result && result.error_code !== undefined) {
            wx.showToast({
              icon: 'none',
              title: result.msg || '删除失败'
            })
            return
          }
          wx.navigateBack()
        })
      }
    })
  },
  // 确认收货
  confirmTakeGoods(e) {
    const orderId = e.currentTarget.dataset.id
    wx.showModal({
      title: '确认收货',
      content: '确认已经收到商品？',
      success: res => {
        if (!res.confirm) {
          return
        }
        order.confirmTakeGoods(orderId).then(result => {
          if (result && result.error_code !== undefined) {
            wx.showToast({
              icon: 'none',
              title: result.msg || '操作失败'
            })
            return
          }
          this.loadOrderDetail()
        })
      }
    })
  },
  // 催发货
  remindToShip() {
    const orderId = this.data.orderDetail.id
    if (!orderId) {
      return
    }
    order.remindToShip(orderId).then(result => {
      if (result && result.error_code !== undefined) {
        wx.showToast({
          icon: 'none',
          title: result.msg || '提醒失败'
        })
        return
      }
      wx.showToast({
        icon: 'success',
        title: '已提醒商家发货'
      })
    })
  },
  // 待付款订单：按服务端返回的剩余秒数启动自动取消倒计时
  startAutoCancelCountdown(detail) {
    this.clearAutoCancelTimers()
    if (!detail || detail.status !== 0) {
      this.setData({
        countdownText: '',
        payDisabled: false
      })
      return
    }
    const remaining = detail.remainingSeconds
    if (remaining === null || remaining === undefined || isNaN(Number(remaining))) {
      // 旧接口没有剩余秒数时不展示倒计时，避免显示错误的关闭时间
      this.setData({
        countdownText: '',
        payDisabled: false
      })
      return
    }
    // 用本地时间推算截止点：以服务端剩余秒数为准，避免设备与服务端时钟不一致
    this.autoCancelAt = Date.now() + Math.max(0, Number(remaining)) * 1000
    this.setData({
      payDisabled: Number(remaining) <= 0
    })
    this.updateCountdown()
    this.countdownTimer = setInterval(() => this.updateCountdown(), 1000)
  },
  updateCountdown() {
    const diff = Math.max(0, Math.floor((this.autoCancelAt - Date.now()) / 1000))
    if (diff <= 0) {
      this.clearCountdownTimer()
      this.setData({
        countdownText: '订单已超时，正在关闭...',
        payDisabled: true
      })
      // 定时任务每 60 秒执行一次，倒计时归零后订单可能还没真正关闭
      this.pollOrderClosed()
      return
    }
    const hours = Math.floor(diff / 3600)
    const minutes = Math.floor((diff % 3600) / 60)
    const seconds = diff % 60
    const pad = n => (n < 10 ? '0' + n : '' + n)
    const timeText = (hours > 0 ? hours + ':' + pad(minutes) : pad(minutes)) + ':' + pad(seconds)
    this.setData({
      countdownText: '订单将在 ' + timeText + ' 后自动关闭'
    })
  },
  // 倒计时归零后轮询订单详情，等自动取消任务真正把订单关闭
  pollOrderClosed() {
    this.clearPollTimer()
    this.closePollAttempts = 0
    const poll = async () => {
      this.closePollAttempts++
      const res = await order.getOrderDetail(this.data.orderNo)
      if (res && res.error_code === undefined && res.status !== 0) {
        this.loadOrderDetail()
        return
      }
      if (this.closePollAttempts >= 20) {
        this.setData({
          countdownText: '订单关闭延迟，请下拉刷新'
        })
        return
      }
      this.pollTimer = setTimeout(poll, 3000)
    }
    this.pollTimer = setTimeout(poll, 3000)
  },
  clearCountdownTimer() {
    if (this.countdownTimer) {
      clearInterval(this.countdownTimer)
      this.countdownTimer = null
    }
  },
  clearPollTimer() {
    if (this.pollTimer) {
      clearTimeout(this.pollTimer)
      this.pollTimer = null
    }
  },
  clearAutoCancelTimers() {
    this.clearCountdownTimer()
    this.clearPollTimer()
  },
  // 支付窗口（演示环境：调用后端模拟支付接口）
  async orderPay(e) {
    if (this.data.payDisabled) {
      wx.showToast({
        icon: 'none',
        title: '订单已超时，正在关闭'
      })
      return
    }
    const orderNo = e.currentTarget.dataset.orderNo || this.data.orderNo
    if (!orderNo) {
      return
    }
    // TODO(wechat-pay): 接入真实微信支付后，改为 order.payment(orderNo) 获取预支付参数，
    // 再调用 wx.requestPayment({ timeStamp, nonceStr, package: packageStr, signType, paySign })
    const res = await order.mockPay(orderNo)
    if (res && res.error_code !== undefined) {
      wx.showToast({
        icon: 'none',
        title: res.msg || '支付失败'
      })
      return
    }
    wx.showToast({
      title: '支付成功'
    })
    this.loadOrderDetail()
  },
  // 加入购物车：逐项调用加购接口
  async addToCart() {
    const goodsList = this.data.orderDetail.goodsList || []
    if (goodsList.length === 0) {
      return
    }
    wx.showLoading({
      mask: true,
      title: '加入中'
    })
    let failed = 0
    for (const goods of goodsList) {
      const res = await cart.addCartItem(goods)
      if (res && res.error_code !== undefined) {
        failed++
      }
    }
    wx.hideLoading()
    if (failed > 0) {
      wx.showToast({
        icon: 'none',
        title: failed === goodsList.length ? '加入失败，商品可能已下架' : '部分商品加入失败'
      })
      return
    }
    wx.showToast({
      title: '已加入购物车'
    })
  },
  // 再买一单：后端把订单商品重新加入购物车，然后跳到购物车
  async buyAgain() {
    const id = this.data.orderDetail.id
    if (id === undefined) {
      return
    }
    const res = await order.repetition(id)
    if (res && res.error_code !== undefined) {
      wx.showToast({
        icon: 'none',
        title: res.msg || '操作失败'
      })
      return
    }
    wx.showToast({
      title: '已加入购物车'
    })
    setTimeout(() => {
      // 购物车是 tabBar 页面，必须用 switchTab
      wx.switchTab({
        url: '/pages/shop-cart/index'
      })
    }, 800)
  },
  // 售后详情
  goRefundDetail() {
    wx.navigateTo({
      url: '/pages/refund-details/index?refundNo=' + this.data.orderDetail.refundApply.refundNo
    })
  },
  // 复制订单号
  copyOrderNo() {
    wx.setClipboardData({
      data: this.data.orderDetail.orderNo
    })
  },
  // 页面隐藏时停止计时，回到页面后由 onShow 重新加载并按剩余时间继续
  onHide() {
    this.clearAutoCancelTimers()
  },
  // 订单支付成功的回退页
  onUnload() {
    this.clearAutoCancelTimers()
    const origin = this.data.origin
    if (origin === 1) {
      wx.reLaunch({
        url: '/pages/my/index'
      })
    }
  }
})
