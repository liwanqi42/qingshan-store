import order from '../../models/order'
import config from '../../config'
import { requireLogin } from '../../utils/auth'
import { removeToken } from '../../utils/token'

Page({
	data: {
    userInfo: {},
    waitPay: 0,   // 待付款
    notExpress: 0,  // 待发货
    waitReceive: 0  // 待收货
  },
  onLoad() {
    this.setData({
      userInfo: wx.getStorageSync('user') || {}
    })
  },
  async onShow() {
    // 登录可能晚于页面加载，这里重新同步一次本地用户信息
    this.setData({
      userInfo: wx.getStorageSync('user') || {}
    })
    this.loadOrderRemind()
  },
  // 加载提醒的订单数量
  async loadOrderRemind() {
    const res = await order.getOrderRemind()
    const { error_code, msg } = res
    if (error_code !== undefined) {
      console.log(msg)
      return
    }
    this.setData({
      waitPay: res.waitPay,
      notExpress: res.notExpress,
      waitReceive: res.waitReceive
    })
  },
  // 打开登录弹窗
  async doAuthInfo() {
    const authorized = await requireLogin()
    if (authorized) {
      this.setData({
        userInfo: wx.getStorageSync('user') || {}
      })
    }
  },
  async goMyCoupon() {
    if (!(await this.checkUserLogin())) {
      return
    }
    wx.navigateTo({
      url: "/pages/my/coupon/index"
    })
  },
  async goMyOrder(event) {
    if (!(await this.checkUserLogin())) {
      return
    }
    const status = event.currentTarget.dataset.id
    wx.navigateTo({
      url: '/pages/order/index?status=' + status
    })
  },
  async goMyAddress() {
    if (!(await this.checkUserLogin())) {
      return
    }
    wx.navigateTo({
      url: '/pages/address/index'
    })
  },
  async goMyBrowse() {
    if (!(await this.checkUserLogin())) {
      return
    }
    wx.navigateTo({
      url: '/pages/browse/index'
    })
  },
  callShopPhone() {
    wx.makePhoneCall({
      phoneNumber: config.phoneNumber
    })
  },
  // 退出登录：清除本地 token 和用户信息
  logout() {
    wx.showModal({
      title: '提示',
      content: '确定要退出登录吗？',
      success: (res) => {
        if (!res.confirm) {
          return
        }
        removeToken()
        wx.removeStorageSync('user')
        this.setData({
          userInfo: {},
          waitPay: 0,
          notExpress: 0,
          waitReceive: 0
        })
        wx.showToast({
          icon: 'none',
          title: '已退出登录'
        })
      }
    })
  },
  // 检查用户登录，未授权时弹微信原生授权弹窗
  async checkUserLogin() {
    const authorized = await requireLogin()
    if (authorized) {
      this.setData({
        userInfo: wx.getStorageSync('user') || {}
      })
    }
    return authorized
  }
})