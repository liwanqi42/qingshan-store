import config from '../../config'
import banner from '../../models/banner'
import grid from '../../models/grid'
import goods from '../../models/goods'
import shop from '../../models/shop'

Page({
  data: {
    inputVal: "", // 搜索框内容

    loadingHidden: false, // loading
    loadingMoreHidden: true,

    banners: [],
    categories: [],
    goodsList: [],
    shopStatus: null, // 店铺营业状态：1-营业中 0-打烊中
    curPage: 0,
    pageSize: 10,
  },
  tabClick: function(e) {
    wx.navigateTo({
      url: '/pages/goods/list?categoryId=' + e.currentTarget.id,
    })
  },
  toDetailsTap: function(e) {
    wx.navigateTo({
      url: "/pages/goods-details/index?id=" + e.currentTarget.dataset.id
    })
  },
  onLoad(e) {
    wx.showShareMenu({
      withShareTicket: true
    }) 
    wx.setNavigationBarTitle({
      title: config.shopName
    })
    this.loadHomeBanner()
    this.loadHomeGrid()
    this.getGoodsList()
  },
  onShow() {
    this.loadShopStatus()
  },
  async loadShopStatus() {
    const status = await shop.getShopStatus()
    // 请求失败时返回错误对象，此时保持原状态
    if (typeof status !== 'number') {
      return
    }
    this.setData({
      shopStatus: status
    });
  },
  async loadHomeBanner() {
    const banners = await banner.getHomeBanner(1, 3)
    // 后端暂未提供轮播图接口，失败时保持空列表，避免把错误对象渲染成轮播项
    if (!Array.isArray(banners)) {
      this.setData({
        banners: []
      });
      return
    }
    this.setData({
      banners: banners
    });
  },
  async loadHomeGrid(){
    const data = await grid.getGridList(1, 8)
    const { error_code, msg } = data
    if (error_code !== undefined) {
      console.log(msg)
      return
    }
    this.setData({
      categories: data
    });
  },
  onPageScroll(e) {
  },
  async getGoodsList() {
    wx.showLoading({
      mask: true,
      title: '加载中'
    })
    const curPage = this.data.curPage + 1
    const res = await goods.getGoodsList('', 0, 0, curPage, this.data.pageSize)
    const {error_code, msg} = res
    if (error_code !== undefined) {
      console.log(msg)
      wx.hideLoading()
      return
    }
    let goodsList = this.data.goodsList
    if (curPage === 1) {
      goodsList = res.list
    } else {
      goodsList = goodsList.concat(res.list)
    }
    const loadingMoreHidden = res.list.length === this.data.pageSize
    this.setData({
      curPage: curPage,
      loadingMoreHidden: loadingMoreHidden,
      goodsList: goodsList
    });
    wx.hideLoading()
  },
  onShareAppMessage: function() {    
    return {
      title: config.shareProfile,
      path: '/pages/index/index'
    }
  },
  onReachBottom: function() {
    if (!this.data.loadingMoreHidden) {
      return
    }
    this.getGoodsList()
  },
  onPullDownRefresh: function() {
    this.setData({
      curPage: 0
    });
    this.loadHomeBanner()
    this.loadShopStatus()
    this.getGoodsList()
    wx.stopPullDownRefresh()
  },
  bindinput(e) {
    this.setData({
      inputVal: e.detail.value
    })
  },
  bindconfirm(e) {
    this.setData({
      inputVal: e.detail.value
    })
    wx.navigateTo({
      url: '/pages/goods/list?k=' + this.data.inputVal,
    })
  },
})