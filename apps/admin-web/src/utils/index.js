/**
 * 获取接口基础地址
 */
export function getBaseApi() {
  return process.env.VUE_APP_BASE_API || '/api'
}

/**
 * 判断是否为外部链接
 */
export function isExternal(path) {
  return /^(https?:|mailto:|tel:)/.test(String(path || ''))
}

/**
 * 防抖
 */
export function debounce(fn, delay = 300) {
  let timer = null

  return function debounced(...args) {
    if (timer) {
      clearTimeout(timer)
    }
    timer = setTimeout(() => {
      timer = null
      fn.apply(this, args)
    }, delay)
  }
}

/**
 * 节流
 */
export function throttle(fn, interval = 300) {
  let lastTime = 0

  return function throttled(...args) {
    const now = Date.now()
    if (now - lastTime < interval) {
      return
    }
    lastTime = now
    fn.apply(this, args)
  }
}

/**
 * 深拷贝普通对象、数组和日期
 */
export function deepClone(source) {
  if (source === null || typeof source !== 'object') {
    return source
  }
  if (source instanceof Date) {
    return new Date(source.getTime())
  }
  if (Array.isArray(source)) {
    return source.map(item => deepClone(item))
  }
  return Object.keys(source).reduce((target, key) => {
    target[key] = deepClone(source[key])
    return target
  }, {})
}
