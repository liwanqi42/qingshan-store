import {get, post, put, wxLogin} from '../utils/request'
import {getToken, saveToken} from '../utils/token'
import CONFIG from '../config'

const USER_KEY = 'user'

class User {

  // 合并服务端资料到本地缓存，页面统一使用 nickName/avatar/gender
  cacheProfile (profile, base) {
    const userInfo = base || {}
    const sex = profile.sex === undefined || profile.sex === null
      ? (userInfo.sex || '')
      : String(profile.sex)
    const name = profile.name === undefined || profile.name === null
      ? (userInfo.name || '')
      : profile.name
    const updated = {
      ...userInfo,
      id: profile.id === undefined || profile.id === null ? userInfo.id : profile.id,
      phone: profile.phone || userInfo.phone,
      name,
      nickName: name || userInfo.nickName || '',
      avatar: profile.avatar === undefined || profile.avatar === null
        ? (userInfo.avatar || '')
        : profile.avatar,
      sex,
      gender: sex === '1' ? 1 : (sex === '0' ? 2 : 0)
    }
    wx.setStorageSync(USER_KEY, updated)
    return updated
  }

  // 统一保存登录结果：token + 用户信息
  saveLogin (data, loginType) {
    if (!data || data.error_code !== undefined || !data.token) {
      throw data || { error_code: -1, msg: '登录失败' }
    }
    saveToken(data.token)
    const userInfo = wx.getStorageSync(USER_KEY) || {}
    return this.cacheProfile(data, {
      ...userInfo,
      phoneAuthorized: true,
      loginType: loginType || 'password'
    })
  }

  // 手机号密码登录
  async passwordLogin (phone, password) {
    const data = await post('/user/user/login', {
      phone,
      password
    })
    return this.saveLogin(data, 'password')
  }

  // 手机号注册（注册成功后直接登录）
  async register (phone, password, name) {
    const data = await post('/user/user/register', {
      phone,
      password,
      name
    })
    return this.saveLogin(data, 'password')
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
    return this.saveLogin(data, 'wechat')
  }

  // 退出登录：通知后端把当前 token 加入黑名单
  async logout () {
    try {
      await post('/user/user/logout')
    } catch (e) {
      // 本地退出不依赖后端，网络异常时直接清理本地登录态
    }
  }

  // 本地缓存的用户信息，用于页面首屏渲染
  getUserInfo () {
    return wx.getStorageSync(USER_KEY) || {}
  }

  // 从服务端刷新资料，成功后同步本地缓存
  async getProfile () {
    if (!getToken()) {
      return { error_code: 401, msg: '请先登录' }
    }
    const res = await get('/user/user/profile')
    if (!res || res.error_code !== undefined) {
      return res
    }
    return this.cacheProfile(res, wx.getStorageSync(USER_KEY) || {})
  }

  // 更新昵称、头像、性别，服务端保存成功后同步本地缓存
  async updateUserInfo ({nickName, avatarUrl, sex}) {
    const res = await put('/user/user/profile', {
      name: nickName,
      avatar: avatarUrl,
      sex
    })
    if (!res || res.error_code !== undefined) {
      return res
    }
    return this.cacheProfile(res, wx.getStorageSync(USER_KEY) || {})
  }

  // 上传头像，返回 { url } 或 { error_code, msg }
  uploadAvatar (filePath) {
    return new Promise(resolve => {
      const token = getToken()
      if (!token) {
        resolve({ error_code: 401, msg: '请先登录' })
        return
      }
      wx.uploadFile({
        url: CONFIG.baseURL + '/user/common/upload',
        filePath: filePath,
        name: 'file',
        header: {
          authentication: token
        },
        success (res) {
          let body = {}
          try {
            body = JSON.parse(res.data)
          } catch (e) {
            body = {}
          }
          if (res.statusCode === 200 && body && body.code === 1) {
            resolve({ url: body.data })
            return
          }
          resolve({
            error_code: res.statusCode === 200 ? (body.code || -1) : res.statusCode,
            msg: (body && body.msg) || '头像上传失败'
          })
        },
        fail () {
          resolve({ error_code: -1, msg: '网络异常，请稍后重试' })
        }
      })
    })
  }
}

export default new User()
