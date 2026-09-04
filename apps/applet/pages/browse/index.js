import browse from '../../models/browse'

Page({
  data: {
    editStatus: false,
    history: [],
    curPage: 1,
    pageSize: 10,
    loadingMoreHidden: true,
    selectRecord: {}, // 单选
    selectAll: false
  },
  onShow() {
    this.setData({
      history: [],
      curPage: 1,
      editStatus: false,
      loadingMoreHidden: true
    })
    this.loadBrowseHistory()
    this.clearSelect()
  },
  async loadBrowseHistory() {
    // 防止 onShow 与上拉加载并发请求同一页
    if (this.isLoading) {
      return
    }
    this.isLoading = true
    wx.showLoading({
      mask: true,
      title: '加载中'
    })
    try {
      const selectRecord = this.data.selectRecord
      const curPage = this.data.curPage
      const res = await browse.getBrowseHistory(curPage, this.data.pageSize)
      const { error_code, msg } = res
      if (error_code !== undefined) {
        console.log(msg)
        return
      }
      const list = Array.isArray(res.list) ? res.list : []
      const history = this.data.history
      for (let i = 0; i < list.length; i++) {
        const record = list[i]
        const tmpArr = record.browseTime.split(' ')[0].split('-')
        const date = tmpArr[1] + '月' + tmpArr[2] + '日'

        let goodsList = []
        for (let j = 0; j < history.length; j++) {
          const dayHistory = history[j]
          if (dayHistory.date === date) {
            goodsList = dayHistory.goodsList
          }
        }
        if (goodsList.length === 0) {
          history.push({
            date: date,
            goodsList
          })
        }
        goodsList.push(record)
        // 单选框
        selectRecord[record.id] = false
      }
      this.setData({
        curPage: curPage + 1,
        history: history,
        loadingMoreHidden: list.length === this.data.pageSize,
        selectRecord: selectRecord
      })
    } finally {
      this.isLoading = false
      wx.hideLoading()
    }
  },
  // 单选
  doSelectRecord(e) {
    const id = e.currentTarget.dataset.id
    const selectRecord = this.data.selectRecord
    selectRecord[id] = !selectRecord[id]
    
    let selectAll = true
    for (let i in selectRecord) {
      if (!selectRecord[i]) {
        selectAll = false
      }
    }
    this.setData({
      selectRecord: selectRecord,
      selectAll: selectAll
    })
  },
  // 全选
  doSelectAll() {
    const selectAll = this.data.selectAll
    const selectRecord = this.data.selectRecord
    for (let i in selectRecord) {
      selectRecord[i] = !selectAll
    }
    this.setData({
      selectAll: !selectAll,
      selectRecord: selectRecord
    })
  },
  // 清理-选择项
  clearSelect() {
    const selectRecord = this.data.selectRecord
    for (let i in selectRecord) {
      selectRecord[i] = false
    }
    this.setData({
      selectAll: false,
      selectRecord: selectRecord
    })
  },
  close() {
    this.setData({
      editStatus: false
    })
    this.clearSelect()
  },
  edit() {
    this.setData({
      editStatus: true
    })
  },
  // 上拉加载更多
  onReachBottom() {
    if (this.data.loadingMoreHidden) {
      this.loadBrowseHistory()
    }
  },
  // 清理-记录
  async deleteRecord() {
    const selectRecord = this.data.selectRecord
    const ids = []
    for (let i in selectRecord) {
      if (selectRecord[i]) {
        ids.push(parseInt(i, 10))
      }
    }
    if (ids.length === 0) {
      wx.showToast({
        icon: 'none',
        title: '请选择要删除的记录'
      })
      return
    }
    wx.showLoading({
      mask: true,
      title: '加载中'
    })
    const res = await browse.clearBrowseHistory(ids)
    const { error_code, msg } = res
    wx.hideLoading()
    if (error_code !== undefined) {
      wx.showToast({
        icon: 'none',
        title: msg || '删除失败'
      })
      return
    }
    this.onShow()
  }
})