/**
 * 用户名：3-20 位字母、数字或下划线
 */
export function isValidUsername(username) {
  return /^[a-zA-Z0-9_]{3,20}$/.test(String(username || ''))
}

/**
 * 密码：6-20 位非空白字符
 */
export function isValidPassword(password) {
  return /^\S{6,20}$/.test(String(password || ''))
}

/**
 * 手机号
 */
export function isValidPhone(phone) {
  return /^1[3-9]\d{9}$/.test(String(phone || ''))
}

/**
 * 身份证号：15 位或 18 位
 */
export function isValidIdNumber(idNumber) {
  return /(^\d{15}$)|(^\d{17}(\d|X|x)$)/.test(String(idNumber || ''))
}

/**
 * 邮箱
 */
export function isValidEmail(email) {
  return /^[\w.-]+@[\w-]+(\.[\w-]+)+$/.test(String(email || ''))
}
