import user from '../../models/user'
import { resolveLogin } from '../../utils/auth'

Component({
  data: {
    visible: false,
    loading: false,
    mode: 'login', // login: 登录  register: 注册
    phone: '',
    password: '',
    confirmPassword: '',
    name: ''
  },
  methods: {
    // 由 utils/auth.requireLogin 调用
    open () {
      this.setData({
        visible: true,
        loading: false,
        mode: 'login',
        phone: '',
        password: '',
        confirmPassword: '',
        name: ''
      })
    },
    close () {
      if (this.data.loading) {
        return
      }
      this.setData({
        visible: false
      })
      resolveLogin(false)
    },
    noop () {},
    switchMode (e) {
      this.setData({
        mode: e.currentTarget.dataset.mode,
        password: '',
        confirmPassword: ''
      })
    },
    bindPhone (e) {
      this.setData({ phone: e.detail.value })
    },
    bindPassword (e) {
      this.setData({ password: e.detail.value })
    },
    bindConfirmPassword (e) {
      this.setData({ confirmPassword: e.detail.value })
    },
    bindName (e) {
      this.setData({ name: e.detail.value })
    },
    validate () {
      const phone = this.data.phone.trim()
      if (!/^1\d{10}$/.test(phone)) {
        wx.showToast({
          icon: 'none',
          title: '请输入正确的手机号'
        })
        return false
      }
      if (!this.data.password || this.data.password.length < 6 || this.data.password.length > 32) {
        wx.showToast({
          icon: 'none',
          title: '密码长度需为6-32位'
        })
        return false
      }
      if (this.data.mode === 'register' && this.data.password !== this.data.confirmPassword) {
        wx.showToast({
          icon: 'none',
          title: '两次输入的密码不一致'
        })
        return false
      }
      return true
    },
    async submit () {
      if (this.data.loading || !this.validate()) {
        return
      }
      const phone = this.data.phone.trim()
      const password = this.data.password
      this.setData({ loading: true })
      wx.showLoading({
        title: this.data.mode === 'login' ? '登录中' : '注册中',
        mask: true
      })
      try {
        if (this.data.mode === 'register') {
          await user.register(phone, password, this.data.name.trim())
        } else {
          await user.passwordLogin(phone, password)
        }
        wx.hideLoading()
        this.setData({
          visible: false,
          loading: false
        })
        resolveLogin(true)
      } catch (err) {
        wx.hideLoading()
        this.setData({ loading: false })
        wx.showToast({
          icon: 'none',
          title: (err && err.msg) || '操作失败，请稍后重试'
        })
      }
    }
  }
})
