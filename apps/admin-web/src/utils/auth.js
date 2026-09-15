import Cookies from 'js-cookie'

export const TOKEN_KEY = 'Admin-Token'

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
