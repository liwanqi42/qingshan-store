import address from '../../models/address'
import { requireLogin, isAuthorized } from '../../utils/auth'

Page({
  data: {
    selectAddressId: 0,
    addressList: []
  },
  onLoad(e) {
    this.setData({
      selectAddressId: parseInt(e.id, 10) || 0
    })
  },
  onShow() {
    this.loadAddressList()
  },
  // 加载所有地址
  async loadAddressList() {
    // 未授权时不请求地址数据
    if (!isAuthorized()) {
      this.setData({
        addressList: []
      })
      return
    }
    const res = await address.getAddressList()
    const { error_code, msg } = res
    if (error_code !== undefined) {
      console.log(msg)
      return
    }
    this.setData({
      addressList: res.list
    })
  },
  // 选择地址
  selectAddress(e) {
    let addressId = e.currentTarget.dataset.id
    this.setData({
      selectAddressId: addressId
    })
  },
  // 添加地址
  async addAddress() {
    if (!(await requireLogin())) {
      return
    }
    wx.navigateTo({
      url: '/pages/address/edit/index'
    })
  },
  confirmAddress() {
    const addressList = this.data.addressList
    const selectAddressId = this.data.selectAddressId
    if (selectAddressId === 0) {
      wx.showToast({
        icon: 'none',
        title: '还没有选择地址'
      })
      return
    }
    let curAddress = {}
    for (let i = 0; i < addressList.length; i++) {
      const address = addressList[i]
      if (String(address.id) === String(selectAddressId)) {
        curAddress = address
        break
      }
    }
    let pages = getCurrentPages();
    let prevPage = pages[ pages.length - 2 ]; 
    // 为上一个页面赋值
    prevPage.setData({
      address: curAddress
    })
    wx.navigateBack({
      delta: 1
    })
  }
})