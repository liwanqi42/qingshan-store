import request from '@/utils/request'

export function uploadFile(file, type) {
  const data = new FormData()
  data.append('file', file)
  if (type !== undefined && type !== null) {
    data.append('type', type)
  }

  return request({
    url: '/admin/common/upload',
    method: 'post',
    data
  })
}
