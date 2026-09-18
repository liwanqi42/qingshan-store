export function maskPhone(phone) {
  const value = String(phone || '')

  if (value.length !== 11) {
    return value
  }
  return `${value.slice(0, 3)}****${value.slice(7)}`
}

export function maskIdNumber(idNumber) {
  const value = String(idNumber || '')

  if (!value || value.includes('*') || value.length <= 8) {
    return value
  }
  const stars = '*'.repeat(value.length - 8)
  return `${value.slice(0, 4)}${stars}${value.slice(-4)}`
}
