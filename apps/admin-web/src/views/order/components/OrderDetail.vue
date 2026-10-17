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
        <el-descriptions-item v-if="order.refundType" label="售后类型">
          {{ refundTypeLabel(order.refundType) }}
        </el-descriptions-item>
        <el-descriptions-item
          v-if="order.refundAmount !== null && order.refundAmount !== undefined"
          label="退款金额"
        >
          ¥{{ formatMoney(order.refundAmount) }}
        </el-descriptions-item>
        <el-descriptions-item v-if="order.refundApplyTime" label="申请时间">
          {{ formatDateTime(order.refundApplyTime) }}
        </el-descriptions-item>
        <el-descriptions-item v-if="order.refundTime" label="售后完成时间">
          {{ formatDateTime(order.refundTime) }}
        </el-descriptions-item>
        <el-descriptions-item v-if="order.refundRejectReason" label="拒绝原因">
          {{ order.refundRejectReason }}
        </el-descriptions-item>
        <el-descriptions-item
          v-if="order.returnExpressCompany || order.returnTrackingNumber"
          label="退货物流"
        >
          {{ order.returnExpressCompany || '-' }}
          {{ order.returnTrackingNumber || '' }}
        </el-descriptions-item>
        <el-descriptions-item v-if="order.returnTime" label="退货时间">
          {{ formatDateTime(order.returnTime) }}
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

      <div class="logistics-bar">
        <span class="items-title">物流轨迹</span>
        <el-button
          size="mini"
          type="primary"
          icon="el-icon-plus"
          @click="toggleTraceForm"
        >
          录入轨迹
        </el-button>
      </div>

      <div
        v-if="logistics.expressCompany || logistics.trackingNumber"
        class="logistics-summary"
      >
        {{ logistics.expressCompany || '-' }}
        {{ logistics.trackingNumber || '' }}
      </div>

      <el-form
        v-if="traceFormVisible"
        ref="traceForm"
        :model="traceForm"
        :rules="traceRules"
        size="small"
        label-width="80px"
        class="trace-form"
      >
        <el-form-item label="轨迹时间" prop="traceTime">
          <el-date-picker
            v-model="traceForm.traceTime"
            type="datetime"
            value-format="yyyy-MM-dd HH:mm"
            placeholder="选择轨迹时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="物流状态">
          <el-select v-model="traceForm.status" style="width: 100%">
            <el-option
              v-for="item in traceStatusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="快递公司">
          <el-input
            v-model="traceForm.expressCompany"
            maxlength="64"
            placeholder="选填，如 顺丰速运"
          />
        </el-form-item>
        <el-form-item label="物流单号">
          <el-input
            v-model="traceForm.trackingNumber"
            maxlength="64"
            placeholder="选填"
          />
        </el-form-item>
        <el-form-item label="物流描述" prop="description">
          <el-input
            v-model="traceForm.description"
            type="textarea"
            :rows="2"
            maxlength="500"
            show-word-limit
            placeholder="如 快件已到达武汉转运中心"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="traceSaving" @click="submitTrace">
            保存
          </el-button>
          <el-button @click="traceFormVisible = false">取消</el-button>
        </el-form-item>
      </el-form>

      <el-table
        v-loading="logisticsLoading"
        :data="logistics.traces || []"
        border
        size="small"
        empty-text="暂无物流轨迹"
      >
        <el-table-column label="轨迹时间" width="120">
          <template slot-scope="{ row }">
            {{ formatDateTime(row.traceTime) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template slot-scope="{ row }">
            <el-tag :type="traceStatusTag(row.status)" size="mini">
              {{ traceStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="140" />
        <el-table-column label="操作" width="60">
          <template slot-scope="{ row }">
            <el-button
              type="text"
              size="mini"
              class="danger-text"
              @click="removeTrace(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </el-drawer>
</template>

<script>
import {
  addLogisticsTrace,
  deleteLogisticsTrace,
  getLogisticsTrace
} from '@/api/order'

const STATUS_MAP = {
  1: { label: '待付款', type: 'info' },
  2: { label: '待接单', type: 'warning' },
  3: { label: '待发货', type: 'warning' },
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

const REFUND_TYPE_MAP = {
  1: '仅退款',
  2: '退货退款',
  3: '换货',
  4: '补寄'
}

const TRACE_STATUS_MAP = {
  0: { label: '暂无轨迹', type: 'info' },
  1: { label: '已揽收', type: 'warning' },
  2: { label: '运输中', type: 'warning' },
  3: { label: '配送中', type: 'success' },
  4: { label: '已签收', type: 'success' },
  5: { label: '异常', type: 'danger' }
}

function pad(value) {
  return value < 10 ? `0${value}` : `${value}`
}

function createTraceForm() {
  const now = new Date()
  return {
    traceTime: `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} ${pad(now.getHours())}:${pad(now.getMinutes())}`,
    status: 1,
    expressCompany: '',
    trackingNumber: '',
    description: ''
  }
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
  data() {
    return {
      logistics: { traces: [] },
      logisticsLoading: false,
      traceFormVisible: false,
      traceSaving: false,
      traceForm: createTraceForm(),
      traceRules: {
        traceTime: [{ required: true, message: '请选择轨迹时间', trigger: 'change' }],
        description: [{ required: true, message: '请填写物流描述', trigger: 'blur' }]
      },
      traceStatusOptions: Object.keys(TRACE_STATUS_MAP).map(value => ({
        value: Number(value),
        label: TRACE_STATUS_MAP[value].label
      }))
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
  watch: {
    visible(value) {
      if (value) {
        this.traceFormVisible = false
        this.traceForm = createTraceForm()
        this.loadLogistics()
      }
    },
    order() {
      if (this.visible) {
        this.loadLogistics()
      }
    }
  },
  methods: {
    loadLogistics() {
      if (!this.order || !this.order.id) {
        this.logistics = { traces: [] }
        return
      }
      this.logisticsLoading = true
      getLogisticsTrace(this.order.id)
        .then(res => {
          this.logistics = res.data || { traces: [] }
        })
        .catch(() => {
          this.logistics = { traces: [] }
        })
        .finally(() => {
          this.logisticsLoading = false
        })
    },
    toggleTraceForm() {
      this.traceForm = createTraceForm()
      this.traceFormVisible = !this.traceFormVisible
      if (this.traceFormVisible) {
        this.$nextTick(() => {
          if (this.$refs.traceForm) {
            this.$refs.traceForm.clearValidate()
          }
        })
      }
    },
    submitTrace() {
      this.$refs.traceForm.validate(valid => {
        if (!valid || this.traceSaving || !this.order) {
          return
        }
        this.traceSaving = true
        addLogisticsTrace({
          orderId: this.order.id,
          ...this.traceForm,
          description: this.traceForm.description.trim()
        })
          .then(() => {
            this.$message.success('物流轨迹已保存')
            this.traceFormVisible = false
            this.traceForm = createTraceForm()
            this.loadLogistics()
          })
          .catch(() => {})
          .finally(() => {
            this.traceSaving = false
          })
      })
    },
    removeTrace(row) {
      this.$confirm('确定删除该条物流轨迹吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
        .then(() => deleteLogisticsTrace(row.id))
        .then(() => {
          this.$message.success('物流轨迹已删除')
          this.loadLogistics()
        })
        .catch(() => {})
    },
    statusLabel(status) {
      return (STATUS_MAP[status] && STATUS_MAP[status].label) || '未知'
    },
    statusTagType(status) {
      return (STATUS_MAP[status] && STATUS_MAP[status].type) || 'info'
    },
    refundStatusLabel(status) {
      return REFUND_MAP[status] || '-'
    },
    refundTypeLabel(type) {
      return REFUND_TYPE_MAP[type] || '-'
    },
    payMethodLabel(method) {
      const map = { 1: '微信支付', 2: '支付宝', 3: '银行卡' }
      return map[method] || '-'
    },
    traceStatusLabel(status) {
      return (TRACE_STATUS_MAP[status] && TRACE_STATUS_MAP[status].label) || '未知'
    },
    traceStatusTag(status) {
      return (TRACE_STATUS_MAP[status] && TRACE_STATUS_MAP[status].type) || 'info'
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

.logistics-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;

  .items-title {
    margin-bottom: 10px;
  }
}

.logistics-summary {
  margin-bottom: 10px;
  padding: 8px 12px;
  font-size: 13px;
  color: $text-secondary;
  background-color: $bg-page;
  border-radius: 4px;
}

.trace-form {
  margin-bottom: 10px;
  padding: 12px 12px 0;
  border: 1px solid $border-color-light;
  border-radius: 4px;
}

.danger-text {
  color: $color-danger;
}
</style>
