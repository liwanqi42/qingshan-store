import { getToken } from './token'

// 等待登录弹窗结果的回调
let pendingResolvers = []

// 是否已完成手机号授权登录
export function isAuthorized () {
  const userInfo = wx.getStorageSync('user') || {}
  return !!getToken() && !!userInfo.phoneAuthorized
}

// 需要登录的操作：未登录时弹出页面内的手机号授权弹窗
export function requireLogin () {
  if (isAuthorized()) {
    return Promise.resolve(true)
  }
  return new Promise((resolve) => {
    const pages = getCurrentPages()
    const page = pages[pages.length - 1]
    const modal = page && page.selectComponent('#loginAuth')
    if (!modal) {
      wx.showToast({
        icon: 'none',
        title: '请先登录'
      })
      resolve(false)
      return
    }
    pendingResolvers.push(resolve)
    modal.open()
  })
}

// 由登录弹窗组件调用：通知所有等待中的操作登录结果
export function resolveLogin (success) {
  const resolvers = pendingResolvers
  pendingResolvers = []
  resolvers.forEach(resolve => resolve(!!success))
}
