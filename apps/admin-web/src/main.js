import Vue from 'vue'
import ElementUI from 'element-ui'
import 'element-ui/lib/theme-chalk/index.css'
import axios from 'axios'
import App from './App.vue'
import router from './router'
import store from './store'
import { permission, role } from './directive/permission'
import './permission'
import './styles/index.scss'

Vue.config.productionTip = false

Vue.use(ElementUI)

Vue.directive('permission', permission)
Vue.directive('role', role)

Vue.prototype.$http = axios

new Vue({
  router,
  store,
  render: h => h(App),
}).$mount('#app')
