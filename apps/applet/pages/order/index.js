import order from '../../models/order'
import cart from '../../models/cart'

// 订单时间 -> 列表里展示的 MM.DD
function formatDate(time) {
  if (!time) {
    return ''
  }
  const date = String(time).replace('T', ' ').split(' ')[0]
  const parts = date.split('-')
  return parts.length === 3 ? `${parts[1]}.${parts[2]}` : date
}

Page({
  data: {
    status: 0,
    curPage: 0,
    pageSize: 10,
    orderList: [],
    loadingMoreHidden: true,
    //自定义导航栏
    statusBarHeight: 20,
    navBarHeight: 44,
    navRightPadding: 12,
    keyword: '',
    //时间筛选：0不限 7近一周 30近一月 90近三月
    timeRange: 0,
    //管理（批量选择删除）模式
    manageMode: false,
    selectedCount: 0,
    allSelected: false,
    //“更多”下拉菜单状态
    moreMenuId: null,
    moreMenuActions: [],
    moreMenuLeft: 0,
    moreMenuTop: 0,
    moreMenuAbove: true
  },
  onLoad(res) {
    const systemInfo = wx.getSystemInfoSync()
    let menu = null
    try {
      menu = wx.getMenuButtonBoundingClientRect()
    } catch (e) {
      menu = null
    }
    const statusBarHeight = systemInfo.statusBarHeight || 20
    // 胶囊按钮信息异常时使用默认值，避免导航栏被挤出屏幕
    const menuRect = menu && menu.left > 0 && menu.height > 0 ? menu : null
    // 自定义导航栏与右上角胶囊按钮垂直居中对齐
    const navBarHeight = menuRect
      ? menuRect.height + (menuRect.top - statusBarHeight) * 2
      : 44
    const navRightPadding = menuRect ? systemInfo.windowWidth - menuRect.left + 8 : 12
    this.setData({
      status: parseInt(res.status, 10) || 0,
      statusBarHeight: statusBarHeight,
      navBarHeight: navBarHeight,
      navRightPadding: navRightPadding
    })
  },
  onShow() {
    this.setData({
      curPage: 0
    })
    this.loadOrderList()
  },
  // 加载订单列表
  async loadOrderList() {
    wx.showLoading({
      mask: true,
      title: '加载中'
    })
    const curPage = this.data.curPage + 1
    const res = await order.getOrderList(this.data.status, curPage, this.data.pageSize,
      this.data.keyword, this.data.timeRange)
    const { error_code, msg } = res
    if (error_code !== undefined) {
      console.log(msg)
      wx.hideLoading()
      return
    }
    const records = res.list || []
    const list = records.map(item => ({
      ...item,
      placeDate: formatDate(item.placeTime),
      // 已取消、已完成、交易关闭的订单可以删除
      deletable: item.status === -1 || item.status === 3 || item.status === 5,
      checked: false
    }))
    let orderList = this.data.orderList
    if (curPage === 1) {
      orderList = list
    } else {
      orderList = orderList.concat(list)
    }
    this.setData({
      orderList: orderList,
      curPage: curPage,
      loadingMoreHidden: records.length === this.data.pageSize
    })
    this.updateSelectionState()
    wx.hideLoading()
  },
  // 返回上一页，页面栈为空时回到“我的”
  goBack() {
    wx.navigateBack({
      delta: 1,
      fail: () => {
        wx.switchTab({
          url: '/pages/my/index'
        })
      }
    })
  },
  // 搜索关键词输入
  onKeywordInput(e) {
    this.setData({
      keyword: e.detail.value
    })
  },
  // 搜索订单号/商品名称
  doSearch() {
    this.setData({
      curPage: 0,
      orderList: []
    })
    this.loadOrderList()
  },
  clearSearch() {
    this.setData({
      keyword: '',
      curPage: 0,
      orderList: []
    })
    this.loadOrderList()
  },
  // 按时间筛选
  onFilter() {
    const options = ['全部时间', '近一周', '近一个月', '近三个月']
    const days = [0, 7, 30, 90]
    wx.showActionSheet({
      itemList: options,
      success: res => {
        const timeRange = days[res.tapIndex]
        if (timeRange === this.data.timeRange) {
          return
        }
        this.setData({
          timeRange: timeRange,
          curPage: 0,
          orderList: []
        })
        this.loadOrderList()
      }
    })
  },
  // 进入/退出管理模式
  toggleManage() {
    const manageMode = !this.data.manageMode
    this.hideMoreMenu()
    this.setData({
      manageMode: manageMode,
      orderList: this.data.orderList.map(item => ({
        ...item,
        checked: false
      }))
    })
    this.updateSelectionState()
  },
  // 点击订单：管理模式下勾选，否则进入详情
  onOrderTap(e) {
    if (this.data.manageMode) {
      this.toggleSelect(e)
      return
    }
    const orderItem = this.findOrder(e.currentTarget.dataset.id)
    if (orderItem) {
      wx.navigateTo({
        url: '/pages/order-details/index?orderNo=' + orderItem.orderNo
      })
    }
  },
  // 勾选/取消勾选单个订单
  toggleSelect(e) {
    const id = e.currentTarget.dataset.id
    if (!this.data.manageMode) {
      return
    }
    const orderList = this.data.orderList.map(item => {
      if (String(item.id) !== String(id) || !item.deletable) {
        return item
      }
      return {
        ...item,
        checked: !item.checked
      }
    })
    this.setData({
      orderList: orderList
    })
    this.updateSelectionState()
  },
  // 全选/取消全选
  toggleSelectAll() {
    const allSelected = !this.data.allSelected
    const orderList = this.data.orderList.map(item => ({
      ...item,
      checked: allSelected && item.deletable
    }))
    this.setData({
      orderList: orderList
    })
    this.updateSelectionState()
  },
  // 计算已选数量和全选状态
  updateSelectionState() {
    const deletable = this.data.orderList.filter(item => item.deletable)
    const selected = deletable.filter(item => item.checked)
    this.setData({
      selectedCount: selected.length,
      allSelected: deletable.length > 0 && selected.length === deletable.length
    })
  },
  // 批量删除已勾选的订单
  async deleteSelected() {
    const selected = this.data.orderList.filter(item => item.checked)
    if (selected.length === 0) {
      wx.showToast({
        icon: 'none',
        title: '请先选择订单'
      })
      return
    }
    const confirmed = await new Promise(resolve => {
      wx.showModal({
        title: '删除订单',
        content: `确认删除选中的 ${selected.length} 笔订单？`,
        success: res => resolve(res.confirm),
        fail: () => resolve(false)
      })
    })
    if (!confirmed) {
      return
    }
    wx.showLoading({
      mask: true,
      title: '删除中'
    })
    let failed = 0
    for (const item of selected) {
      const res = await order.deleteOrder(item.id)
      if (res && res.error_code !== undefined) {
        failed++
      }
    }
    wx.hideLoading()
    this.setData({
      manageMode: false
    })
    this.refreshOrderList()
    if (failed > 0) {
      wx.showToast({
        icon: 'none',
        title: failed === selected.length ? '删除失败' : `${failed}笔删除失败`
      })
    } else {
      wx.showToast({
        title: '删除成功'
      })
    }
  },
  // 支付窗口（演示环境：调用后端模拟支付接口）
  async orderPay(e) {
    const orderNo = e.currentTarget.dataset.orderNo
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
    this.setData({
      curPage: 0
    })
    this.loadOrderList()
  },
  // 在列表中按 id 查找订单
  findOrder(id) {
    return this.data.orderList.find(item => String(item.id) === String(id))
  },
  // 刷新第一页
  refreshOrderList() {
    this.setData({
      curPage: 0
    })
    this.loadOrderList()
  },
  // 更多操作：在“更多”按钮上方弹出下拉菜单
  showMoreActions(e) {
    const id = e.currentTarget.dataset.id
    const orderItem = this.findOrder(id)
    if (!orderItem) {
      return
    }
    // 同一个按钮再次点击则收起
    const openedId = this.data.moreMenuId
    if (openedId !== null) {
      this.hideMoreMenu()
      if (String(openedId) === String(id)) {
        return
      }
    }
    const actions = []
    if (orderItem.status === 0) {
      actions.push({ name: '取消订单', action: 'cancel' })
    }
    // 催发货已改为列表卡片上的常驻按钮，不再放进“更多”菜单
    if (orderItem.status === 2 || orderItem.status === 3) {
      actions.push({ name: '查看物流', action: 'logistics' })
    }
    // 待发货、待收货、已完成可申请售后
    if (orderItem.status === 1 || orderItem.status === 2 || orderItem.status === 3) {
      actions.push({ name: '申请售后', action: 'refund' })
    }
    if (orderItem.status === -1 || orderItem.status === 3 || orderItem.status === 5) {
      actions.push({ name: '删除订单', action: 'delete' })
    }
    if (actions.length === 0) {
      return
    }
    wx.createSelectorQuery()
      .select('#more-' + id)
      .boundingClientRect(rect => {
        if (!rect) {
          return
        }
        // 菜单默认在按钮下方展开；下方空间不足时改为向上
        const systemInfo = wx.getSystemInfoSync()
        const rpx = systemInfo.windowWidth / 750
        const menuHeight = actions.length * 88 * rpx
        const below = systemInfo.windowHeight - rect.bottom >= menuHeight + 10
        this.setData({
          moreMenuId: id,
          moreMenuActions: actions,
          moreMenuLeft: rect.left,
          moreMenuTop: below ? rect.bottom : rect.top,
          moreMenuAbove: !below
        })
      })
      .exec()
  },
  hideMoreMenu() {
    this.setData({
      moreMenuId: null,
      moreMenuActions: []
    })
  },
  onMoreAction(e) {
    const action = e.currentTarget.dataset.action
    const id = this.data.moreMenuId
    this.hideMoreMenu()
    if (action === 'cancel') {
      this.cancelOrderById(id)
    } else if (action === 'logistics') {
      const orderItem = this.findOrder(id)
      if (orderItem) {
        wx.navigateTo({
          url: '/pages/logistics/index?orderNo=' + orderItem.orderNo
        })
      }
    } else if (action === 'refund') {
      const orderItem = this.findOrder(id)
      if (orderItem) {
        // 进入详情页并自动打开申请售后弹窗
        wx.navigateTo({
          url: '/pages/order-details/index?orderNo=' + orderItem.orderNo + '&refund=1'
        })
      }
    } else if (action === 'delete') {
      this.deleteOrderById(id)
    }
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
          this.refreshOrderList()
        })
      }
    })
  },
  // 列表卡片上的催发货按钮
  remindToShipTap(e) {
    this.remindToShip(e.currentTarget.dataset.id)
  },
  // 催发货
  remindToShip(id) {
    order.remindToShip(id).then(result => {
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
          this.refreshOrderList()
        })
      }
    })
  },
  // 加入购物车：逐项调用加购接口
  async addToCart(e) {
    const orderItem = this.findOrder(e.currentTarget.dataset.id)
    if (!orderItem || !orderItem.goodsList || orderItem.goodsList.length === 0) {
      return
    }
    wx.showLoading({
      mask: true,
      title: '加入中'
    })
    let failed = 0
    for (const goods of orderItem.goodsList) {
      const res = await cart.addCartItem(goods)
      if (res && res.error_code !== undefined) {
        failed++
      }
    }
    wx.hideLoading()
    if (failed > 0) {
      wx.showToast({
        icon: 'none',
        title: failed === orderItem.goodsList.length ? '加入失败，商品可能已下架' : '部分商品加入失败'
      })
      return
    }
    wx.showToast({
      title: '已加入购物车'
    })
  },
  // 再买一单：后端把订单商品重新加入购物车，然后跳到购物车
  async buyAgain(e) {
    const res = await order.repetition(e.currentTarget.dataset.id)
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
  async confirmTakeGoods(e) {
    const that = this
    wx.showModal({
      title: "确认收货",
      content: '确认已经收到商品？',
      success(res) {
        if (res.confirm) {
          const orderId = e.currentTarget.dataset.id
          order.confirmTakeGoods(orderId).then(res => {
            const { error_code, msg } = res
            if (error_code !== undefined) {
              console.log(msg)
              return
            }
            that.setData({
              curPage: 0
            })
            that.loadOrderList()
          })
        }
      }
    })
  },
  // 切换状态
  switchStatus(e) {
    const status = e.currentTarget.dataset.status
    if (parseInt(status) === this.data.status) {
      return
    }
    this.setData({
      curPage: 0,
      status: parseInt(status, 10),
      orderList: []
    })
    this.loadOrderList()
  },
  onReachBottom: function() {
    if (!this.data.loadingMoreHidden) {
      return
    }
    this.loadOrderList()
  },
})