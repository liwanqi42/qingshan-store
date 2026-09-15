import axios from 'axios'
import { Message } from 'element-ui'
import { getToken } from './auth'
import { getBaseApi } from './index'

const service = axios.create({
  baseURL: getBaseApi(),
  timeout: 15000
})

service.interceptors.request.use(
  config => {
    const token = getToken()
    if (token) {
      config.headers.token = token
    }
    return config
  },
  error => Promise.reject(error)
)

service.interceptors.response.use(
  response => {
    // 文件导出等场景需要保留完整响应，便于读取文件名
    if (response.config.responseType === 'blob') {
      return response
    }

    const res = response.data

    if (!res || typeof res.code === 'undefined') {
      return res
    }

    if (res.code === 1) {
      return res
    }

    const message = res.msg || '请求失败'
    Message.error(message)
    return Promise.reject(new Error(message))
  },
  error => {
    let message = '请求失败，请稍后重试'

    if (error.code === 'ECONNABORTED') {
      message = '请求超时，请稍后重试'
    } else if (error.response) {
      const { status, data } = error.response

      if (status === 401) {
        message = '登录状态已失效，请重新登录'
      } else if (data && data.msg) {
        message = data.msg
      } else if (status === 404) {
        message = '请求的资源不存在'
      } else if (status >= 500) {
        message = '服务器异常，请稍后重试'
      } else {
        message = `请求失败（${status}）`
      }
    } else if (error.request) {
      message = '网络异常，请检查网络连接'
    }

    Message.error(message)
    return Promise.reject(error)
  }
)

export default service
