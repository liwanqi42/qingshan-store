import Cookies from 'js-cookie'

export const TOKEN_KEY = 'Admin-Token'
const USER_INFO_KEY = 'Admin-User-Info'

export function getToken() {
  return Cookies.get(TOKEN_KEY) || ''
}

export function setToken(token) {
  if (!token) {
    removeToken()
    return
  }
  Cookies.set(TOKEN_KEY, token, { path: '/' })
}

export function removeToken() {
  Cookies.remove(TOKEN_KEY, { path: '/' })
}

export function getUserInfo() {
  const raw = localStorage.getItem(USER_INFO_KEY)
  if (!raw) {
    return null
  }
  try {
    return JSON.parse(raw)
  } catch (e) {
    removeUserInfo()
    return null
  }
}

export function setUserInfo(userInfo) {
  if (!userInfo) {
    removeUserInfo()
    return
  }
  localStorage.setItem(USER_INFO_KEY, JSON.stringify(userInfo))
}

export function removeUserInfo() {
  localStorage.removeItem(USER_INFO_KEY)
}
