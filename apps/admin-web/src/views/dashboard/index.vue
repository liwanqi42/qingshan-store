<template>
  <div class="app-container dashboard">
    <div class="page-header">
      <div>
        <h2>工作台</h2>
        <p>{{ today }} · 今日经营概览</p>
      </div>
      <el-button
        icon="el-icon-refresh"
        :loading="loading"
        @click="fetchData"
      >
        刷新数据
      </el-button>
    </div>

    <div v-loading="loading">
      <div class="metric-grid">
        <div
          v-for="item in metrics"
          :key="item.label"
          class="metric-card"
        >
          <div class="metric-icon">
            <i :class="item.icon" />
          </div>
          <div class="metric-body">
            <div class="metric-label">{{ item.label }}</div>
            <div class="metric-value">
              {{ item.value }}
              <span v-if="item.unit" class="metric-unit">{{ item.unit }}</span>
            </div>
          </div>
        </div>
      </div>

      <el-row :gutter="16" class="overview-row">
        <el-col :xs="24" :lg="14">
          <el-card shadow="never">
            <div class="card-title">订单总览</div>
            <div class="overview-grid order-grid">
              <div
                v-for="item in orderOverview"
                :key="item.label"
                class="overview-item"
              >
                <span class="overview-label">{{ item.label }}</span>
                <span class="overview-value">{{ item.value }}</span>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="24" :lg="10">
          <el-card shadow="never">
            <div class="card-title">商品 / 套餐总览</div>
            <div class="overview-grid">
              <div
                v-for="item in saleOverview"
                :key="item.label"
                class="overview-item"
              >
                <span class="overview-label">{{ item.label }}</span>
                <span class="overview-value">{{ item.value }}</span>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script>
import {
  getBusinessData,
  getOrderOverview,
  getProductOverview,
  getBundleOverview
} from '@/api/workspace'

function formatDate(date) {
  const pad = value => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(
    date.getDate()
  )}`
}

export default {
  name: 'DashboardView',
  data() {
    return {
      today: formatDate(new Date()),
      loading: false,
      businessData: {},
      orderData: {},
      productData: {},
      bundleData: {},
      timer: null
    }
  },
  computed: {
    metrics() {
      const data = this.businessData || {}
      return [
        {
          label: '今日营业额',
          value: this.formatMoney(data.turnover),
          unit: '元',
          icon: 'el-icon-money'
        },
        {
          label: '有效订单',
          value: data.validOrderCount || 0,
          unit: '单',
          icon: 'el-icon-s-order'
        },
        {
          label: '订单完成率',
          value: this.formatPercent(data.orderCompletionRate),
          unit: '',
          icon: 'el-icon-data-line'
        },
        {
          label: '平均客单价',
          value: this.formatMoney(data.unitPrice),
          unit: '元',
          icon: 'el-icon-price-tag'
        },
        {
          label: '新增用户',
          value: data.newUsers || 0,
          unit: '人',
          icon: 'el-icon-user'
        }
      ]
    },
    orderOverview() {
      const data = this.orderData || {}
      return [
        { label: '待付款', value: data.pendingPaymentOrders || 0 },
        { label: '待接单', value: data.awaitingShipmentOrders || 0 },
        { label: '待发货', value: data.shippedOrders || 0 },
        { label: '配送中', value: data.inTransitOrders || 0 },
        { label: '已完成', value: data.completedOrders || 0 },
        { label: '已取消', value: data.cancelledOrders || 0 },
        { label: '全部订单', value: data.allOrders || 0 }
      ]
    },
    saleOverview() {
      const product = this.productData || {}
      const bundle = this.bundleData || {}
      return [
        { label: '商品已上架', value: product.onSale || 0 },
        { label: '商品已下架', value: product.offSale || 0 },
        { label: '套餐已上架', value: bundle.onSale || 0 },
        { label: '套餐已下架', value: bundle.offSale || 0 }
      ]
    }
  },
  created() {
    this.fetchData()
  },
  mounted() {
    this.timer = setInterval(() => {
      if (document.visibilityState === 'visible') {
        this.fetchData()
      }
    }, 60000)
  },
  beforeDestroy() {
    if (this.timer) {
      clearInterval(this.timer)
      this.timer = null
    }
  },
  methods: {
    fetchData() {
      this.loading = true
      Promise.all([
        getBusinessData(),
        getOrderOverview(),
        getProductOverview(),
        getBundleOverview()
      ])
        .then(([business, orders, products, bundles]) => {
          this.businessData = business.data || {}
          this.orderData = orders.data || {}
          this.productData = products.data || {}
          this.bundleData = bundles.data || {}
        })
        .catch(() => {})
        .finally(() => {
          this.loading = false
        })
    },
    formatMoney(value) {
      return Number(value || 0).toFixed(2)
    },
    formatPercent(value) {
      return `${(Number(value || 0) * 100).toFixed(1)}%`
    }
  }
}
</script>

<style lang="scss" scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;

  h2 {
    margin: 0 0 6px;
    font-size: 22px;
    color: $text-primary;
  }

  p {
    margin: 0;
    font-size: 13px;
    color: $text-secondary;
  }
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.metric-card {
  display: flex;
  align-items: center;
  padding: 20px;
  background-color: $bg-white;
  border-radius: $border-radius-base;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.06);
}

.metric-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 48px;
  height: 48px;
  margin-right: 14px;
  font-size: 24px;
  color: $color-primary;
  background-color: rgba(64, 158, 255, 0.1);
  border-radius: 10px;
}

.metric-label {
  font-size: 13px;
  color: $text-secondary;
}

.metric-value {
  margin-top: 6px;
  font-size: 26px;
  font-weight: 600;
  color: $text-primary;
}

.metric-unit {
  margin-left: 4px;
  font-size: 13px;
  font-weight: 400;
  color: $text-secondary;
}

.overview-row {
  margin-top: 4px;
}

.card-title {
  margin-bottom: 16px;
  font-size: 16px;
  font-weight: 600;
  color: $text-primary;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.order-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.overview-item {
  display: flex;
  flex-direction: column;
  padding: 14px;
  background-color: #f7f8fa;
  border-radius: $border-radius-base;
}

.overview-label {
  font-size: 13px;
  color: $text-secondary;
}

.overview-value {
  margin-top: 8px;
  font-size: 22px;
  font-weight: 600;
  color: $text-primary;
}

@media (max-width: 900px) {
  .order-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
