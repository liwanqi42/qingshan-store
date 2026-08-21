// 数量选择组件：点击数字可直接输入，长按 +/- 连续加减
Component({
  properties: {
    value: { type: Number, value: 1 },
    min: { type: Number, value: 1 },
    max: { type: Number, value: 999 },
    disabled: { type: Boolean, value: false },
    // 常用数量档位
    presets: {
      type: Array,
      value: [5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 100, 200, 500]
    }
  },
  data: {
    num: 1,
    showEdit: false,
    editValue: ''
  },
  observers: {
    value(value) {
      // 页面回填（如重新拉取购物车）时同步内部数量
      // 这里只保证下限，不按上限裁剪：库存减少时仍展示购物车里的真实数量，由用户自行调低
      const num = this.normalize(value)
      if (num !== this.data.num) {
        this.setData({ num: num })
      }
    }
  },
  lifetimes: {
    attached() {
      this.setData({ num: this.normalize(this.data.value) })
    },
    detached() {
      this.stopTimer()
    }
  },
  methods: {
    // 只做下限约束，允许当前数量暂时高于上限（库存减少的情况）
    normalize(value) {
      let num = parseInt(value, 10)
      if (isNaN(num)) {
        num = this.data.min
      }
      return Math.max(num, this.data.min)
    },
    // 用户主动填写的数量需要收回到上限内
    clamp(value) {
      return Math.min(this.normalize(value), this.data.max)
    },
    // 按步长修改数量，越界时通知页面
    step(delta) {
      if (this.data.disabled) {
        return false
      }
      const next = this.data.num + delta
      // 只在继续增加时校验上限；减少数量始终允许，方便把超过库存的数量调回来
      if (delta > 0 && next > this.data.max) {
        this.triggerEvent('limit', { max: this.data.max })
        return false
      }
      if (next < this.data.min) {
        return false
      }
      this.setData({ num: next })
      this.triggerEvent('change', { value: next })
      return true
    },
    onAddTap() {
      // 长按结束后系统可能补发 tap，这里忽略一次
      if (this.suppressTap) {
        return
      }
      this.step(1)
    },
    onReduceTap() {
      if (this.suppressTap) {
        return
      }
      this.step(-1)
    },
    startLongPress(e) {
      if (this.data.disabled) {
        return
      }
      this.suppressTap = true
      const delta = Number(e.currentTarget.dataset.delta)
      this.stopTimer()
      // 到达边界（如已经是1件）就不再连续触发
      if (!this.step(delta)) {
        return
      }
      this.stepTimer = setInterval(() => {
        if (!this.step(delta)) {
          this.stopTimer()
        }
      }, 100)
    },
    stopLongPress() {
      this.stopTimer()
      setTimeout(() => {
        this.suppressTap = false
      }, 100)
    },
    stopTimer() {
      if (this.stepTimer) {
        clearInterval(this.stepTimer)
        this.stepTimer = null
      }
    },
    openEdit() {
      if (this.data.disabled) {
        return
      }
      this.setData({
        showEdit: true,
        editValue: String(this.data.num)
      })
    },
    closeEdit() {
      this.setData({ showEdit: false })
    },
    onEditInput(e) {
      this.setData({ editValue: e.detail.value })
    },
    confirmEdit() {
      const raw = parseInt(this.data.editValue, 10)
      const num = this.clamp(this.data.editValue)
      if (!isNaN(raw) && raw > this.data.max) {
        this.triggerEvent('limit', { max: this.data.max })
      }
      this.applyNum(num)
    },
    selectPreset(e) {
      this.applyNum(Number(e.currentTarget.dataset.num))
    },
    // 选中数量并收起面板
    applyNum(num) {
      const changed = num !== this.data.num
      this.setData({ showEdit: false, num: num })
      if (changed) {
        this.triggerEvent('change', { value: num })
      }
    },
    noop() {}
  }
})
