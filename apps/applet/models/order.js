import { get, post, put } from '../utils/request'

class Order {

  // 对应后端 OrderController：POST /user/order/submit
  async placeOrder(params) {
    return post('/user/order/submit', params)
  }

  // 演示环境模拟支付，对应后端 MockPayController：PUT /user/order/mock-pay
  // TODO(wechat-pay): 接入真实微信支付后删除本方法，只保留 payment
  async mockPay(orderNumber) {
    return put('/user/order/mock-pay', { orderNumber })
  }

  // 获取微信支付预支付参数，对应后端 OrderController：PUT /user/order/payment
  async payment(orderNumber, payMethod) {
    return put('/user/order/payment', { orderNumber, payMethod })
  }

  // 状态: -999全部 0待付款 1待发货 2待收货 3已完成 4退款中 5交易关闭
  // keyword 匹配订单号/商品名称，days 为时间范围（0不限）
  async getOrderList(status, page, size, keyword, days) {
    let url = `/user/order/historyOrders?status=${status}&page=${page}&pageSize=${size}`
    if (keyword) {
      url += `&keyword=${encodeURIComponent(keyword)}`
    }
    if (days > 0) {
      url += `&days=${days}`
    }
    return get(url)
  }

  async getOrderDetail(orderNo) {
    return get(`/user/order/detail?orderNo=${orderNo}`)
  }

  async deleteOrder(id) {
    return post(`/user/order?id=${id}`)
  }

  async cancelOrder(id) {
    return put(`/user/order/cancel?id=${id}`)
  }

  async confirmTakeGoods(id) {
    return put(`/user/order/confirm_goods?id=${id}`)
  }

  // 催发货，对应后端 OrderController：GET /user/order/reminder/{id}
  async remindToShip(id) {
    return get(`/user/order/reminder/${id}`)
  }

  // 再买一单：后端把订单商品重新加入购物车
  async repetition(id) {
    return post(`/user/order/repetition/${id}`)
  }

  // 售后类型：2退货退款 3换货 4补寄
  async refundApply(orderNo, refundType, reason) {
    return put('/user/order/refund_apply', {
      orderNo, refundType, reason
    })
  }

  async refundDetail(refundNo) {
    return get(`/user/order/refund_detail?refundNo=${refundNo}`)
  }

  async undoOrderRefund(refundNo) {
    return put(`/user/order/refund_undo?refundNo=${refundNo}`)
  }

  async getOrderRemind() {
    return get('/user/order/remind')
  }
}

export default new Order()
