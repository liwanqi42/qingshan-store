const tokenKey = 'token'

/**
 * 存储token
 * 后端 JwtUtil 直接解析 JWT 字符串，不能带 "Bearer " 前缀
 */
export function saveToken (token) {
  wx.setStorageSync(tokenKey, token)
}

/**
 * 获取token值
 */
export function getToken() {
  return wx.getStorageSync(tokenKey)
}

/**
 * 移除token
 */
export function removeToken() {
  wx.removeStorageSync(tokenKey)
}
