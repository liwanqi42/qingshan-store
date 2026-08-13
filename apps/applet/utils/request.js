import CONFIG from '../config'
import { getToken, saveToken, removeToken } from '../utils/token'
const baseURL = CONFIG.baseURL

// 后端微信登录接口（qingshan-server UserController）
const LOGIN_URL = '/user/user/login'

// 微信登录是串行的：并发请求同时 401 时只触发一次 wx.login
let loginPromise = null

// 拼接 query 参数
function buildURL (url, params) {
  if (!params) {
    return url
  }
  const query = Object.keys(params)
    .filter(key => params[key] !== undefined && params[key] !== null && params[key] !== '')
    .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(params[key])}`)
    .join('&')
  if (!query) {
    return url
  }
  return url + (url.indexOf('?') === -1 ? '?' : '&') + query
}

// 解析后端统一返回结果 { code, msg, data }，code 为 1 表示成功
function unwrapResult (body) {
  if (body && typeof body === 'object' && !Array.isArray(body) && body.code !== undefined) {
    if (body.code === 1) {
      const data = body.data
      // 后端分页结果为 { total, records }，兼容小程序页面使用的 { list }
      if (data && typeof data === 'object' && Array.isArray(data.records)) {
        data.list = data.records
      }
      return data === undefined || data === null ? {} : data
    }
    // 业务失败：转换成页面统一的 error_code 形式
    return {
      error_code: body.code,
      msg: body.msg
    }
  }
  // 非统一格式的返回原样返回
  return body
}

// 微信登录，extraData 会随登录请求一起提交（如手机号授权数据）
function doLogin (extraData) {
  return new Promise((resolve, reject) => {
    wx.login({
      success(loginRes) {
        if (!loginRes.code) {
          reject({ error_code: -1, msg: '微信登录失败' })
          return
        }
        wx.request({
          method: 'post',
          url: baseURL + LOGIN_URL,
          data: Object.assign({ code: loginRes.code }, extraData || {}),
          header: {
            'content-type': 'application/json;charset=utf-8;',
          },
          success(res) {
            const data = unwrapResult(res.data)
            if (!data || data.error_code !== undefined || !data.token) {
              removeToken()
              reject(data && data.error_code !== undefined
                ? data
                : { error_code: -1, msg: '登录失败' })
              return
            }
            saveToken(data.token)
            resolve(data)
          },
          fail() {
            reject({ error_code: -1, msg: '网络异常，请稍后重试' })
          }
        })
      },
      fail() {
        reject({ error_code: -1, msg: '微信登录失败' })
      }
    })
  })
}

// 微信登录，返回 { id, openid, phone, token }
export function wxLogin (extraData) {
  // 带手机号等附加参数时不能复用静默登录的 Promise
  if (extraData && Object.keys(extraData).length > 0) {
    return doLogin(extraData)
  }
  if (loginPromise) {
    return loginPromise
  }
  loginPromise = doLogin()
  // 无论成功失败都重置，避免后续请求复用已结束的 Promise
  const reset = () => { loginPromise = null }
  loginPromise.then(reset, reset)
  return loginPromise
}

// Add a request interceptor
function requestIntercepter (originConfig) {
  if (!originConfig.url) {
    console.error('request need url')
    throw new Error({
      source: 'requestInterceptors',
      message: 'request need url',
    })
  }
  const reqConfig = {...originConfig}
  reqConfig.header = {
    'content-type': 'application/json;charset=utf-8;',
  }
  const token = getToken()
  if (token) {
    // 后端 qingshan.jwt.user-token-name 配置为 authentication
    reqConfig.header.authentication = token
  }
  return reqConfig
}

// Add a response interceptor
function responseIntercepter (res, config) {
  if (res.statusCode === 200) {
    return unwrapResult(res.data)
  }
  // 登录过期：清理 token，由页面重新触发登录
  if (res.statusCode === 401) {
    removeToken()
    return {
      error_code: 401,
      msg: '登录已过期，请重新登录'
    }
  }
  // 其它 HTTP 错误同样转换成页面统一的 error_code 形式
  const body = res.data || {}
  return {
    error_code: res.statusCode,
    msg: body.msg || body.message || body.error || '请求失败'
  }
}

function request (config) {
  const reqConfig = requestIntercepter(config)
  return new Promise((resolve, reject) => {
    wx.request({
      method: reqConfig.method,
      url: baseURL + buildURL(reqConfig.url, reqConfig.params),
      data: reqConfig.data,
      timeout: 5 * 1000,
      header: reqConfig.header,
      success(res) {
        resolve(res)
      },
      fail(err) {
        console.log(err)
        reject(err)
      }
    })
  })
    .then(res => responseIntercepter(res, reqConfig))
    .catch(err => {
      if (err && err.error_code !== undefined) {
        return err
      }
      return { error_code: -1, msg: '网络异常，请稍后重试' }
    })
}

export function get (url, params = {}) {
  return request({
    method: 'get',
    url,
    params,
  })
}

export function post (url, data = {}, params = {}) {
  return request({
    method: 'post',
    url,
    data,
    params
  })
}

export function put(url, data = {}, params) {
  return request({
    method: 'put',
    url,
    data,
    params,
  })
}

export function _delete (url, params) {
  return request({
    method: 'delete',
    url,
    params
  })
}
