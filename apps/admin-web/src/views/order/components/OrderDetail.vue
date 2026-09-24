<template>
  <el-drawer
    title="订单详情"
    :visible.sync="drawerVisible"
    size="560px"
    append-to-body
  >
    <div v-if="order" class="order-detail">
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="订单号">
          {{ order.number || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="订单状态">
          <el-tag :type="statusTagType(order.status)" size="mini">
            {{ statusLabel(order.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="下单时间">
          {{ formatDateTime(order.orderTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="支付方式">
          {{ payMethodLabel(order.payMethod) }}
        </el-descriptions-item>
        <el-descriptions-item label="实收金额">
          ¥{{ formatMoney(order.amount) }}
        </el-descriptions-item>
        <el-descriptions-item label="收货人">
          {{ order.consignee || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="手机号">
          {{ order.phone || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="收货地址">
          {{ order.address || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="备注">
          {{ order.remark || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="快递信息">
          {{ order.expressCompany || '-' }} {{ order.trackingNumber || '' }}
        </el-descriptions-item>
        <el-descriptions-item v-if="order.cancelReason" label="取消原因">
          {{ order.cancelReason }}
        </el-descriptions-item>
        <el-descriptions-item v-if="order.rejectionReason" label="拒单原因">
          {{ order.rejectionReason }}
        </el-descriptions-item>
        <el-descriptions-item v-if="order.refundStatus" label="售后状态">
          {{ refundStatusLabel(order.refundStatus) }}
        </el-descriptions-item>
        <el-descriptions-item v-if="order.refundReason" label="售后原因">
          {{ order.refundReason }}
        </el-descriptions-item>
      </el-descriptions>

      <div class="items-title">订单商品</div>
      <el-table :data="order.orderDetailList || []" border size="small">
        <el-table-column label="图片" width="70">
          <template slot-scope="{ row }">
            <el-image
              v-if="row.image"
              :src="row.image"
              fit="cover"
              class="item-image"
            />
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="商品" min-width="120" />
        <el-table-column prop="productAttr" label="规格" min-width="90" />
        <el-table-column prop="number" label="数量" width="70" />
        <el-table-column label="金额" width="90">
          <template slot-scope="{ row }">¥{{ formatMoney(row.amount) }}</template>
        </el-table-column>
      </el-table>
    </div>
  </el-drawer>
</template>

<script>
const STATUS_MAP = {
  1: { label: '待付款', type: 'info' },
  2: { label: '待发货', type: 'warning' },
  3: { label: '已接单', type: 'success' },
  4: { label: '配送中', type: 'success' },
  5: { label: '已完成', type: 'info' },
  6: { label: '已取消', type: 'danger' },
  7: { label: '退款', type: 'danger' }
}

const REFUND_MAP = {
  1: '售后申请中',
  2: '待买家退货',
  3: '买家已退货待商家收货',
  4: '售后完成',
  5: '售后申请被拒绝',
  6: '售后关闭'
}

export default {
  name: 'OrderDetail',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    order: {
      type: Object,
      default: null
    }
  },
  computed: {
    drawerVisible: {
      get() {
        return this.visible
      },
      set(value) {
        this.$emit('update:visible', value)
      }
    }
  },
  methods: {
    statusLabel(status) {
      return (STATUS_MAP[status] && STATUS_MAP[status].label) || '未知'
    },
    statusTagType(status) {
      return (STATUS_MAP[status] && STATUS_MAP[status].type) || 'info'
    },
    refundStatusLabel(status) {
      return REFUND_MAP[status] || '-'
    },
    payMethodLabel(method) {
      const map = { 1: '微信支付', 2: '支付宝', 3: '银行卡' }
      return map[method] || '-'
    },
    formatMoney(value) {
      return Number(value || 0).toFixed(2)
    },
    formatDateTime(value) {
      if (!value) return '-'
      return String(value).replace('T', ' ').slice(0, 19)
    }
  }
}
</script>

<style lang="scss" scoped>
.order-detail {
  padding: 0 20px 20px;
}

.items-title {
  margin: 20px 0 10px;
  font-size: 15px;
  font-weight: 600;
  color: $text-primary;
}

.item-image {
  width: 44px;
  height: 44px;
  border-radius: 4px;
}
</style>
