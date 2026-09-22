export function downloadBlob(response, fallbackName = '运营数据报表.xlsx') {
  const data = response.data instanceof Blob ? response.data : new Blob([response.data])
  const disposition =
    (response.headers &&
      (response.headers['content-disposition'] ||
        response.headers['Content-Disposition'])) ||
    ''

  let filename = fallbackName
  const utf8Match = disposition.match(/filename\*=UTF-8''([^;]+)/i)
  const normalMatch = disposition.match(/filename="?([^";]+)"?/i)

  if (utf8Match) {
    try {
      filename = decodeURIComponent(utf8Match[1])
    } catch (e) {
      filename = utf8Match[1]
    }
  } else if (normalMatch) {
    try {
      filename = decodeURIComponent(normalMatch[1])
    } catch (e) {
      filename = normalMatch[1]
    }
  }

  const url = window.URL.createObjectURL(data)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}
