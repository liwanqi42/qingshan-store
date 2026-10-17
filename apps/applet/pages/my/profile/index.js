import user from '../../../models/user'
import {isAuthorized, requireLogin} from '../../../utils/auth'

Page({
  data: {
    name: '',
    avatar: '',
    genderIndex: 0,
    genderOptions: ['保密', '男', '女'],
    uploading: false,
    saving: false
  },
  async onLoad() {
    if (!isAuthorized()) {
      const authorized = await requireLogin()
      if (!authorized) {
        wx.navigateBack({
          delta: 1
        })
        return
      }
    }
    this.loadProfile()
  },
  // 加载服务端资料
  async loadProfile() {
    const res = await user.getProfile()
    if (res && res.error_code !== undefined) {
      wx.showToast({
        icon: 'none',
        title: res.msg || '资料加载失败'
      })
      return
    }
    const genderIndex = res.gender === 1 ? 1 : (res.gender === 2 ? 2 : 0)
    this.setData({
      name: res.nickName || '',
      avatar: res.avatar || '',
      genderIndex: genderIndex
    })
  },
  // 选择微信头像后立即上传，保存时只提交 OSS 地址
  async onChooseAvatar(e) {
    const tempPath = e.detail && e.detail.avatarUrl
    if (!tempPath || this.data.uploading) {
      return
    }
    const oldAvatar = this.data.avatar
    this.setData({
      avatar: tempPath,
      uploading: true
    })
    const res = await user.uploadAvatar(tempPath)
    if (res.error_code !== undefined) {
      this.setData({
        avatar: oldAvatar,
        uploading: false
      })
      wx.showToast({
        icon: 'none',
        title: res.msg || '头像上传失败'
      })
      return
    }
    this.setData({
      avatar: res.url,
      uploading: false
    })
  },
  onNameInput(e) {
    this.setData({
      name: e.detail.value
    })
  },
  onNameBlur(e) {
    if (e.detail && e.detail.value !== undefined) {
      this.setData({
        name: e.detail.value
      })
    }
  },
  onGenderChange(e) {
    this.setData({
      genderIndex: Number(e.detail.value) || 0
    })
  },
  // 保存资料
  async saveProfile() {
    if (this.data.saving) {
      return
    }
    if (this.data.uploading) {
      wx.showToast({
        icon: 'none',
        title: '头像上传中，请稍候'
      })
      return
    }
    const name = this.data.name.trim()
    if (!name) {
      wx.showToast({
        icon: 'none',
        title: '请输入昵称'
      })
      return
    }
    const sex = this.data.genderIndex === 1
      ? '1'
      : (this.data.genderIndex === 2 ? '0' : '')
    this.setData({
      saving: true
    })
    const res = await user.updateUserInfo({
      nickName: name,
      avatarUrl: this.data.avatar,
      sex: sex
    })
    this.setData({
      saving: false
    })
    if (res && res.error_code !== undefined) {
      wx.showToast({
        icon: 'none',
        title: res.msg || '保存失败'
      })
      return
    }
    wx.showToast({
      title: '保存成功'
    })
    setTimeout(() => {
      wx.navigateBack({
        delta: 1
      })
    }, 600)
  }
})
