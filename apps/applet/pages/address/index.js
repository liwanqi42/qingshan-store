import address from '../../models/address'
import { requireLogin, isAuthorized } from '../../utils/auth'

Page({
  data: {
    addressList: [],
    needLogin: false
  },
  onShow() {
    if (!isAuthorized()) {
      this.setData({
        addressList: [],
        needLogin: true
      })
      return
    }
    this.setData({
      needLogin: false
    })
    this.loadAddressList()
  },
  async goLogin() {
    if (await requireLogin()) {
      this.setData({
        needLogin: false
      })
      this.loadAddressList()
    }
  },
  async loadAddressList() {
    // 未授权时不请求地址数据
    if (!isAuthorized()) {
      this.setData({
        addressList: [],
        needLogin: true
      })
      return
    }
    wx.showLoading({
      mask: true,
      title: '加载中'
    })
    const res = await address.getAddressList()
    const { error_code, msg } = res
    if (error_code !== undefined) {
      console.log(msg)
      wx.hideLoading()
      return
    }
    this.setData({
      addressList: res.list
    })
    wx.hideLoading()
  },
  deleteAddress(e) {
    const addressId = e.currentTarget.dataset.id
    const that = this
    wx.showModal({
      content: '确定要删除该地址吗？',
      success (res) {
        if (res.confirm) {
          address.deleteAddress(addressId).then(res => {
            const { error_code, msg } = res
            if (error_code !== undefined) {
              wx.showToast({
                icon: 'none',
                title: msg
              })
              return
            }
            wx.showToast({
              title: '删除成功'
            })
            that.loadAddressList()
          })
        } else if (res.cancel) {
          console.log('用户点击取消')
        }
      }
    })
  },
  editAddress(e) {
    const addressId = e.currentTarget.dataset.id
    wx.navigateTo({
      url: '/pages/address/edit/index?id=' + addressId
    })
  },
  addAddress() {
    wx.navigateTo({
      url: '/pages/address/edit/index'
    })
  },
  // 设置-默认收货地址
  defaultAddress(e) {
    const addressId = e.currentTarget.dataset.id
    const that = this
    address.setDefaultAddress(addressId).then(res => {
      const { error_code, msg } = res
      if (error_code !== undefined) {
        wx.showToast({
          icon: 'none',
          title: msg
        })
        return
      }
      that.loadAddressList()
    })
  }
})
