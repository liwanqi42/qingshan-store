import Cookies from 'js-cookie'

export const TOKEN_KEY = 'Admin-Token'
const USER_INFO_KEY = 'Admin-User-Info'
const ROLES_KEY = 'Admin-Roles'
const PERMISSIONS_KEY = 'Admin-Permissions'
const MENU_PATHS_KEY = 'Admin-Menu-Paths'

function getArray(key) {
  const raw = localStorage.getItem(key)
  if (!raw) {
    return []
  }
  try {
    const parsed = JSON.parse(raw)
    return Array.isArray(parsed) ? parsed : []
  } catch (e) {
    localStorage.removeItem(key)
    return []
  }
}

function setArray(key, value) {
  localStorage.setItem(key, JSON.stringify(Array.isArray(value) ? value : []))
}

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

export function getRoles() {
  return getArray(ROLES_KEY)
}

export function setRoles(roles) {
  setArray(ROLES_KEY, roles)
}

export function getPermissions() {
  return getArray(PERMISSIONS_KEY)
}

export function setPermissions(permissions) {
  setArray(PERMISSIONS_KEY, permissions)
}

export function getMenuPaths() {
  return getArray(MENU_PATHS_KEY)
}

export function setMenuPaths(menuPaths) {
  setArray(MENU_PATHS_KEY, menuPaths)
}

export function clearAuth() {
  removeToken()
  removeUserInfo()
  localStorage.removeItem(ROLES_KEY)
  localStorage.removeItem(PERMISSIONS_KEY)
  localStorage.removeItem(MENU_PATHS_KEY)
}
