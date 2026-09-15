import Vue from 'vue'
import VueRouter from 'vue-router'
import Vuex from 'vuex'
import ElementUI from 'element-ui'
import 'element-ui/lib/theme-chalk/index.css'
import axios from 'axios'
import App from './App.vue'
import './styles/index.scss'

Vue.config.productionTip = false

Vue.use(VueRouter)
Vue.use(Vuex)
Vue.use(ElementUI)

Vue.prototype.$http = axios

new Vue({
  render: h => h(App),
}).$mount('#app')
