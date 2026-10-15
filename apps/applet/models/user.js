import {post, wxLogin} from '../utils/request'
import {saveToken} from '../utils/token'

const USER_KEY = 'user'

class User {

  // 统一保存登录结果：token + 用户信息
  saveLogin (data) {
    if (!data || data.error_code !== undefined || !data.token) {
      throw data || { error_code: -1, msg: '登录失败' }
    }
    saveToken(data.token)
    const userInfo = wx.getStorageSync(USER_KEY) || {}
    const updated = {
      ...userInfo,
      id: data.id,
      phone: data.phone || userInfo.phone,
      phoneAuthorized: true,
      loginType: 'password'
    }
    wx.setStorageSync(USER_KEY, updated)
    return updated
  }

  // 手机号密码登录
  async passwordLogin (phone, password) {
    const data = await post('/user/user/login', {
      phone,
      password
    })
    return this.saveLogin(data)
  }

  // 手机号注册（注册成功后直接登录）
  async register (phone, password, name) {
    const data = await post('/user/user/register', {
      phone,
      password,
      name
    })
    return this.saveLogin(data)
  }

  // 后续开通微信手机号一键登录时使用
  async phoneLogin (detail) {
    const extra = {}
    if (detail && detail.code) {
      extra.phoneCode = detail.code
    } else if (detail && detail.encryptedData && detail.iv) {
      extra.encryptedData = detail.encryptedData
      extra.iv = detail.iv
    }
    if (Object.keys(extra).length === 0) {
      throw { error_code: -1, msg: '未获取到手机号授权信息' }
    }
    const data = await wxLogin(extra)
    const userInfo = wx.getStorageSync(USER_KEY) || {}
    const updated = {
      ...userInfo,
      id: data.id,
      phone: data.phone || userInfo.phone,
      phoneAuthorized: true,
      loginType: 'wechat'
    }
    wx.setStorageSync(USER_KEY, updated)
    return updated
  }

  // 退出登录：通知后端把当前 token 加入黑名单
  async logout () {
    try {
      await post('/user/user/logout')
    } catch (e) {
      // 本地退出不依赖后端，网络异常时直接清理本地登录态
    }
  }

  // 后端没有单独的用户信息接口，用户资料来自登录结果并保存在本地
  getUserInfo () {
    return wx.getStorageSync(USER_KEY) || {}
  }

  // 后端暂未提供用户资料更新接口，先保存到本地
  updateUserInfo ({nickName, avatarUrl, gender, country, province, city}) {
    const userInfo = wx.getStorageSync(USER_KEY) || {}
    const updated = {
      ...userInfo,
      nickName,
      avatar: avatarUrl,
      gender,
      country,
      province,
      city
    }
    wx.setStorageSync(USER_KEY, updated)
    return updated
  }
}

export default new User()
