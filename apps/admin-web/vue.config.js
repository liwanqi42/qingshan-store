const { defineConfig } = require('@vue/cli-service')

module.exports = defineConfig({
  transpileDependencies: true,
  css: {
    loaderOptions: {
      sass: {
        additionalData: '@use "@/styles/variables.scss" as *;'
      }
    }
  },
  devServer: {
    port: 8081,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        pathRewrite: {
          '^/api': ''
        }
      },
      // 只代理带会话标识的应用 WebSocket（来单提醒、催发货提醒），
      // 避免拦截 dev-server 自身的 /ws HMR 连接
      '/ws/': {
        target: 'ws://localhost:8080',
        ws: true,
        changeOrigin: true
      }
    }
  }
})
