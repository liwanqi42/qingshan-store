export default {
  version: '0.1.0',
  // TODO(security): 上线前改为 HTTPS 域名，并在微信公众平台配置 request 合法域名，见 SECURITY_TODO.md
  // TODO(prod): 同时把 project.config.json 的 "urlCheck" 改为 true（或删除该字段），
  //   并删除已被提交的 project.private.config.json / 加入 .gitignore
  baseURL: 'http://192.168.1.8:8080',
  // 开发环境用弹窗模拟支付结果（成功/取消）；TODO(wechat-pay): 接入真实微信支付后改为 false
  mockPay: true,
  // TODO(prod): 以下为占位值，上线前换成真实店铺名称和客服电话
  shopName: '青山商城',
  shareProfile: '青山商城',
  phoneNumber: '188700000000'
}
