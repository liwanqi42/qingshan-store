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
    timer: null
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
    const res = await order.refundDetail(this.data.refundNo)
    const { error_code, msg } = res
    if (error_code !== undefined) {
      console.log(msg)
    }
    // 历史数据没有类型时按仅退款展示
    const refundType = res.refundType || 1
    this.setData({
      orderDetail: res,
      refundTypeText: REFUND_TYPE_TEXT[refundType] || '',
      isMoneyRefund: MONEY_REFUND_TYPES.indexOf(refundType) >= 0
    })
    if (res.status === 0) {
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
  }
})