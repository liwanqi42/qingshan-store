import address from '../../../models/address'
import { isAuthorized } from '../../../utils/auth'

const MUNICIPALITIES = ['北京市', '上海市', '天津市', '重庆市']

// 从微信选点返回的地址中拆分省/市/区（选点接口不返回区划编号）
function parseRegion(addressStr) {
  const result = {
    provinceName: '',
    cityName: '',
    districtName: '',
    detail: ''
  }
  if (!addressStr) {
    return result
  }
  let rest = addressStr
  const municipality = MUNICIPALITIES.find(item => rest.indexOf(item) === 0)
  if (municipality) {
    result.provinceName = municipality
    result.cityName = municipality
    rest = rest.slice(municipality.length)
  } else {
    const provinceMatch = rest.match(/^(.+?(?:省|自治区|特别行政区))/)
    if (provinceMatch) {
      result.provinceName = provinceMatch[1]
      rest = rest.slice(provinceMatch[1].length)
    }
    const cityMatch = rest.match(/^(.+?(?:市|自治州|地区|盟))/)
    if (cityMatch) {
      result.cityName = cityMatch[1]
      rest = rest.slice(cityMatch[1].length)
    }
  }
  const districtMatch = rest.match(/^(.+?(?:区|县|旗|市))/)
  if (districtMatch) {
    result.districtName = districtMatch[1]
    rest = rest.slice(districtMatch[1].length)
  }
  result.detail = rest.trim() || addressStr
  return result
}

Page({
  data: {
    addressId: null,
    region: ['湖北省', '武汉市', '新洲区'],
    customItem: '全部',
    addressDetail: {
      id: null,
      consignee: '',
      phone: '',
      detail: '',
      provinceName: '湖北省',
      provinceCode: '420000',
      cityName: '武汉市',
      cityCode: '420100',
      districtName: '新洲区',
      districtCode: '420115',
      label: '',
      isDefault: 0
    },
  },
  onLoad(res) {
    const addressId = res.id ? Number(res.id) : null
    this.setData({
      addressId: addressId
    })
    if (addressId) {
      this.loadAddress()
    }
  },
  bindRegionChange(e) {
    const addressDetail = this.data.addressDetail
    const names = e.detail.value || []
    const codes = e.detail.code || []
    addressDetail.provinceName = names[0] || ''
    addressDetail.cityName = names[1] || ''
    addressDetail.districtName = names[2] || ''
    addressDetail.provinceCode = codes[0] || ''
    addressDetail.cityCode = codes[1] || ''
    addressDetail.districtCode = codes[2] || ''
    this.setData({
      region: names,
      addressDetail: addressDetail
    })
  },
  async saveAddress() {
    const addressDetail = this.data.addressDetail
    if (addressDetail.consignee === '') {
      wx.showToast({
        icon: 'none',
        title: '请填写收件人！'
      })
      return
    }
    if (addressDetail.phone === '') {
      wx.showToast({
        icon: 'none',
        title: '请填写联系电话！'
      })
      return
    }
    const reg = new RegExp(/^1\d{10}$/)
    if (!reg.test(addressDetail.phone)) {
      wx.showToast({
        icon: 'none',
        title: '请输入正确手机号'
      })
      return
    }
    if (addressDetail.detail === '') {
      wx.showToast({
        icon: 'none',
        title: '请填写详细地址！'
      })
      return
    }
    if (!addressDetail.provinceName || !addressDetail.cityName) {
      wx.showToast({
        icon: 'none',
        title: '请选择地区！'
      })
      return
    }
    const res = await address.saveAddress(addressDetail)
    const { error_code, msg } = res
    if (error_code !== undefined) {
      wx.showToast({
        icon: 'none',
        title: msg
      })
      return
    }
    wx.showToast({
      title: '保存成功'
    })
    wx.navigateBack()
  },
  async loadAddress() {
    // 未授权时不请求地址数据
    if (!isAuthorized()) {
      return
    }
    const res = await address.getAddressById(this.data.addressId)
    const { error_code, msg } = res
    if (error_code !== undefined) {
      console.log(msg)
      return
    }
    this.setData({
      addressDetail: res,
      region: [res.provinceName, res.cityName, res.districtName]
    })
  },
  // 请求定位权限：已授权直接返回，拒绝后引导去设置页
  requestLocationPermission() {
    return new Promise((resolve) => {
      wx.getSetting({
        success: (settingRes) => {
          if (settingRes.authSetting['scope.userLocation']) {
            resolve(true)
            return
          }
          wx.authorize({
            scope: 'scope.userLocation',
            success: () => resolve(true),
            fail: () => {
              wx.showModal({
                title: '定位权限',
                content: '需要获取你的位置信息来自动填写收货地址，请在设置中允许',
                confirmText: '去设置',
                success: (modalRes) => {
                  if (!modalRes.confirm) {
                    resolve(false)
                    return
                  }
                  wx.openSetting({
                    success: (openRes) => resolve(!!openRes.authSetting['scope.userLocation']),
                    fail: () => resolve(false)
                  })
                }
              })
            }
          })
        },
        fail: () => resolve(false)
      })
    })
  },
  // 定位并自动填写省市区和详细地址
  async locateAddress() {
    const granted = await this.requestLocationPermission()
    if (!granted) {
      wx.showToast({
        icon: 'none',
        title: '未获得定位权限'
      })
      return
    }
    wx.chooseLocation({
      success: (locationRes) => {
        const region = parseRegion(locationRes.address)
        if (!region.provinceName && !region.cityName && !region.districtName) {
          wx.showToast({
            icon: 'none',
            title: '未能识别所选地区'
          })
          return
        }
        const addressDetail = this.data.addressDetail
        addressDetail.provinceName = region.provinceName
        addressDetail.cityName = region.cityName
        addressDetail.districtName = region.districtName
        // 选点不返回区划编号，清空旧编号避免与地区不一致
        addressDetail.provinceCode = ''
        addressDetail.cityCode = ''
        addressDetail.districtCode = ''
        addressDetail.detail = region.detail
        this.setData({
          region: [region.provinceName, region.cityName, region.districtName],
          addressDetail: addressDetail
        })
      },
      fail: (err) => {
        // 用户主动取消选点时不提示
        if (err && err.errMsg && err.errMsg.indexOf('cancel') !== -1) {
          return
        }
        wx.showToast({
          icon: 'none',
          title: '定位失败，请重试'
        })
      }
    })
  },
  bindContacts(e) {
    const addressDetail = this.data.addressDetail
    addressDetail.consignee = e.detail.value
    this.setData({
      addressDetail: addressDetail
    })
  },
  bindMobile(e) {
    const addressDetail = this.data.addressDetail
    addressDetail.phone = e.detail.value
    this.setData({
      addressDetail: addressDetail
    })
  },
  bindAddress(e) {
    const addressDetail = this.data.addressDetail
    addressDetail.detail = e.detail.value
    this.setData({
      addressDetail: addressDetail
    })
  },
})
