import order from '../../models/order'

const STATUS_MAP = {
  0: { label: '暂无轨迹', type: 'info' },
  1: { label: '已揽收', type: 'warning' },
  2: { label: '运输中', type: 'warning' },
  3: { label: '配送中', type: 'success' },
  4: { label: '已签收', type: 'success' },
  5: { label: '异常', type: 'danger' }
}

function formatTraceTime(value) {
  if (!value) {
    return ''
  }
  return String(value).replace('T', ' ').slice(0, 16)
}

Page({
  data: {
    orderNo: '',
    expressCompany: '',
    trackingNumber: '',
    shipTime: '',
    traces: [],
    loading: true
  },
  onLoad(options) {
    this.setData({
      orderNo: options.orderNo || ''
    })
    this.loadLogistics()
  },
  async loadLogistics() {
    if (!this.data.orderNo) {
      this.setData({
        loading: false
      })
      return
    }
    wx.showLoading({
      mask: true,
      title: '加载中'
    })
    const res = await order.getLogistics(this.data.orderNo)
    wx.hideLoading()
    const { error_code, msg } = res
    if (error_code !== undefined) {
      this.setData({
        loading: false
      })
      wx.showToast({
        icon: 'none',
        title: msg || '物流信息加载失败'
      })
      return
    }
    const traces = (res.traces || []).map((item, index) => {
      const status = STATUS_MAP[item.status] || STATUS_MAP[0]
      return {
        ...item,
        timeText: formatTraceTime(item.traceTime),
        statusText: status.label,
        statusType: status.type,
        isLatest: index === 0
      }
    })
    this.setData({
      expressCompany: res.expressCompany || '',
      trackingNumber: res.trackingNumber || '',
      shipTime: formatTraceTime(res.shipTime),
      traces: traces,
      loading: false
    })
  },
  // 复制物流单号
  copyTrackingNumber() {
    if (!this.data.trackingNumber) {
      return
    }
    wx.setClipboardData({
      data: this.data.trackingNumber
    })
  }
})
