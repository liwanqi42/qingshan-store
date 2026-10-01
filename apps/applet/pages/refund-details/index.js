import order from '../../models/order'

// 售后类型
const REFUND_TYPE_TEXT = {
  1: '仅退款',
  2: '退货退款',
  3: '换货',
  4: '补寄'
}

// 需要退款的售后类型
const MONEY_REFUND_TYPES = [1, 2]

Page({
  data: {
    refundNo: '',
    countdown: '',  // 倒计时
    orderDetail: {},
    refundTypeText: '',
    isMoneyRefund: true,
    timer: null,
    // 后端原始售后状态：1申请中 2待买家退货 3买家已退货待商家收货 4售后完成 5已拒绝 6已关闭
    refundStatus: 0,
    returnPopup: false,
    returnCompany: '',
    returnTracking: '',
    returnSubmitting: false
  },
  onLoad(e) {
    this.setData({
      refundNo: e.refundNo
    })
  },
  onShow() {
    this.clearCountdown()
    this.loadRefundDetail()
  },
  async loadRefundDetail() {
    this.clearCountdown()
    const res = await order.refundDetail(this.data.refundNo)
    const { error_code, msg } = res
    if (error_code !== undefined) {
      console.log(msg)
      return
    }
    // 历史数据没有类型时按仅退款展示
    const refundType = res.refundType || 1
    this.setData({
      orderDetail: res,
      refundTypeText: REFUND_TYPE_TEXT[refundType] || '',
      isMoneyRefund: MONEY_REFUND_TYPES.indexOf(refundType) >= 0,
      refundStatus: res.refundStatus || 0
    })
    // 只有等待商家处理的申请才展示倒计时
    if (res.status === 0 && res.refundStatus === 1) {
      this.createCountdown(res.applyTime)
    }
  },
  // 创建倒计时器
  createCountdown(applyTime) {
    const that = this
    const timer = setInterval(function () {
      let tmpTime = applyTime.replace(/-/g,':').replace(' ',':');
      tmpTime = tmpTime.split(':');
      let applyTimestamp = new Date(tmpTime[0],(tmpTime[1]-1),tmpTime[2],tmpTime[3],tmpTime[4],tmpTime[5]);
      let expireTime = new Date(applyTimestamp.getTime() + 86400 * 1000)

      // 时间差，单位：秒
      let timeDiff = parseInt((expireTime.getTime() - (new Date()).getTime()) / 1000, 10)
      if (timeDiff < 0) {
        that.clearCountdown()
        return
      }
      const hours = parseInt(timeDiff / 3600, 10)
      const minutes = parseInt((timeDiff - hours * 3600) / 60, 10)
      const seconds = timeDiff - hours * 3600 - minutes * 60
      that.setData({
        countdown: '还剩'+ hours +'时'+ minutes +'分' + seconds + '秒'
      })
    }, 1000)
    this.setData({
      timer: timer
    })
  },
  // 清理倒计时器
  clearCountdown() {
    const timer = this.data.timer
    if (timer) {
      clearInterval(timer)
    }
  },
  onHide() {
    this.clearCountdown()
  },
  onUnload() {
    this.clearCountdown()
  },
  undoRefundApply() {
    const that = this
    wx.showModal({
      title: '撤销申请',
      content: '确认撤销订单？',
      success(res) {
        if (res.confirm) {
          order.undoOrderRefund(that.data.refundNo).then(res => {
            const { error_code, msg } = res
            if (error_code !== undefined) {
              console.log(msg)
              return
            }
            wx.navigateBack()
          })
        }
      }
    })
  },
  // 打开填写退货物流弹窗
  openReturnPopup() {
    this.setData({
      returnPopup: true,
      returnCompany: '',
      returnTracking: ''
    })
  },
  closeReturnPopup() {
    if (this.data.returnSubmitting) {
      return
    }
    this.setData({
      returnPopup: false
    })
  },
  onReturnCompanyInput(e) {
    this.setData({
      returnCompany: e.detail.value
    })
  },
  onReturnTrackingInput(e) {
    this.setData({
      returnTracking: e.detail.value
    })
  },
  stopPropagation() {
    // 阻止冒泡
  },
  // 提交退货物流：待买家退货 -> 买家已退货待商家收货
  async submitReturnLogistics() {
    if (this.data.returnSubmitting) {
      return
    }
    const expressCompany = (this.data.returnCompany || '').trim()
    const trackingNumber = (this.data.returnTracking || '').trim()
    if (!expressCompany || !trackingNumber) {
      wx.showToast({
        icon: 'none',
        title: '请填写快递公司和物流单号'
      })
      return
    }
    const orderId = this.data.orderDetail.orderId
    if (!orderId) {
      wx.showToast({
        icon: 'none',
        title: '订单信息异常，请返回重试'
      })
      return
    }
    this.setData({
      returnSubmitting: true
    })
    wx.showLoading({
      mask: true,
      title: '提交中'
    })
    const res = await order.refundReturn(orderId, expressCompany, trackingNumber)
    wx.hideLoading()
    this.setData({
      returnSubmitting: false
    })
    if (res && res.error_code !== undefined) {
      wx.showToast({
        icon: 'none',
        title: res.msg || '提交失败'
      })
      return
    }
    this.setData({
      returnPopup: false
    })
    wx.showToast({
      title: '提交成功'
    })
    this.loadRefundDetail()
  }
})