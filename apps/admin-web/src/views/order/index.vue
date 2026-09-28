<template>
  <div class="app-container">
    <div class="stat-grid">
      <div
        class="stat-card"
        :class="{ active: queryForm.status === 2 }"
        @click="handleStatClick(2)"
      >
        <span class="stat-label">待接单</span>
        <strong class="stat-value">{{ stats.pendingShipment || 0 }}</strong>
      </div>
      <div
        class="stat-card"
        :class="{ active: queryForm.status === 3 }"
        @click="handleStatClick(3)"
      >
        <span class="stat-label">待发货</span>
        <strong class="stat-value">{{ stats.shipped || 0 }}</strong>
      </div>
      <div
        class="stat-card"
        :class="{ active: queryForm.status === 4 }"
        @click="handleStatClick(4)"
      >
        <span class="stat-label">配送中</span>
        <strong class="stat-value">{{ stats.inTransit || 0 }}</strong>
      </div>
    </div>

    <el-card shadow="never">
      <el-form :inline="true" :model="queryForm" class="search-form" @submit.native.prevent>
        <el-form-item label="关键词">
          <el-input
            v-model="queryForm.keyword"
            placeholder="订单号/商品名称"
            clearable
            @keyup.enter.native="handleSearch"
          />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input
            v-model="queryForm.phone"
            placeholder="请输入手机号"
            clearable
            @keyup.enter.native="handleSearch"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="全部" clearable>
            <el-option
              v-for="(item, value) in statusMap"
              :key="value"
              :label="item.label"
              :value="Number(value)"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="下单时间">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            value-format="yyyy-MM-dd"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            unlink-panels
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleSearch">
            查询
          </el-button>
          <el-button icon="el-icon-refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="number" label="订单号" min-width="180" />
        <el-table-column label="收货人" min-width="110">
          <template slot-scope="{ row }">
            {{ row.consignee || row.userName || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" min-width="130" />
        <el-table-column label="金额" width="100">
          <template slot-scope="{ row }">¥{{ formatMoney(row.amount) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template slot-scope="{ row }">
            <el-tag :type="statusTagType(row.status)" size="mini">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="售后" width="130">
          <template slot-scope="{ row }">
            <el-tag
              v-if="row.refundStatus"
              :type="refundTagType(row.refundStatus)"
              size="mini"
            >
              {{ refundLabel(row.refundStatus) }}
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="下单时间" min-width="170">
          <template slot-scope="{ row }">
            {{ formatDateTime(row.orderTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="210" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" size="small" @click="handleDetail(row)">
              详情
            </el-button>
            <el-button
              v-if="row.status === 2 && !hasRefundRecord(row)"
              type="text"
              size="small"
              @click="handleCommand('confirm', row)"
            >
              接单
            </el-button>
            <el-button
              v-if="row.status === 2 && !hasRefundRecord(row)"
              type="text"
              size="small"
              @click="handleCommand('reject', row)"
            >
              拒单
            </el-button>
            <el-button
              v-if="row.status === 3 && !hasRefundRecord(row)"
              type="text"
              size="small"
              @click="handleCommand('delivery', row)"
            >
              派送
            </el-button>
            <el-button
              v-if="row.status === 4 && !hasRefundRecord(row)"
              type="text"
              size="small"
              @click="handleCommand('complete', row)"
            >
              完成
            </el-button>
            <el-button
              v-if="[1, 2, 3].includes(row.status) && !hasRefundRecord(row)"
              type="text"
              size="small"
              @click="handleCommand('cancel', row)"
            >
              取消订单
            </el-button>
            <el-button
              v-if="row.refundStatus === 1"
              type="text"
              size="small"
              @click="handleCommand('refundApprove', row)"
            >
              同意售后
            </el-button>
            <el-button
              v-if="row.refundStatus === 1"
              type="text"
              size="small"
              @click="handleCommand('refundReject', row)"
            >
              拒绝售后
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pagination"
        background
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        :page-size="pageSize"
        :current-page="page"
        :page-sizes="[10, 20, 50]"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </el-card>

    <order-detail
      :visible.sync="detailVisible"
      :order="detailOrder"
    />
  </div>
</template>

<script>
import {
  getOrderPage,
  getOrderDetail,
  getOrderStatistics,
  confirmOrder,
  rejectOrder,
  cancelOrder,
  approveRefund,
  rejectRefund,
  deliveryOrder,
  completeOrder
} from '@/api/order'
import OrderDetail from './components/OrderDetail.vue'

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
  1: { label: '售后申请中', type: 'warning' },
  2: { label: '待买家退货', type: 'info' },
  3: { label: '待商家收货', type: 'info' },
  4: { label: '售后完成', type: 'success' },
  5: { label: '售后被拒绝', type: 'danger' },
  6: { label: '售后关闭', type: 'info' }
}

export default {
  name: 'OrderView',
  components: {
    OrderDetail
  },
  data() {
    return {
      queryForm: {
        keyword: '',
        phone: '',
        status: ''
      },
      dateRange: [],
      list: [],
      total: 0,
      page: 1,
      pageSize: 10,
      loading: false,
      stats: {
        pendingShipment: 0,
        shipped: 0,
        inTransit: 0
      },
      detailVisible: false,
      detailOrder: null,
      statusMap: STATUS_MAP
    }
  },
  created() {
    this.fetchStats()
    this.fetchList()
  },
  methods: {
    fetchList() {
      const params = {
        keyword: this.queryForm.keyword.trim(),
        phone: this.queryForm.phone.trim(),
        status:
          this.queryForm.status === '' ? undefined : this.queryForm.status,
        page: this.page,
        pageSize: this.pageSize
      }
      if (this.dateRange && this.dateRange.length === 2) {
        params.beginTime = `${this.dateRange[0]} 00:00:00`
        params.endTime = `${this.dateRange[1]} 23:59:59`
      }

      this.loading = true
      getOrderPage(params)
        .then(res => {
          const data = res.data || {}
          this.list = data.records || []
          this.total = data.total || 0
        })
        .catch(() => {})
        .finally(() => {
          this.loading = false
        })
    },
    fetchStats() {
      getOrderStatistics()
        .then(res => {
          this.stats = res.data || {
            pendingShipment: 0,
            shipped: 0,
            inTransit: 0
          }
        })
        .catch(() => {})
    },
    handleSearch() {
      this.page = 1
      this.fetchList()
    },
    handleReset() {
      this.queryForm = { keyword: '', phone: '', status: '' }
      this.dateRange = []
      this.page = 1
      this.fetchList()
    },
    handleStatClick(status) {
      this.queryForm.status =
        this.queryForm.status === status ? '' : status
      this.page = 1
      this.fetchList()
    },
    handleSizeChange(size) {
      this.page = 1
      this.pageSize = size
      this.fetchList()
    },
    handleCurrentChange(page) {
      this.page = page
      this.fetchList()
    },
    hasRefundRecord(row) {
      return [1, 2, 3, 4].includes(row.refundStatus)
    },
    handleDetail(row) {
      this.detailOrder = { ...row }
      this.detailVisible = true
      getOrderDetail(row.id)
        .then(res => {
          if (res.data) {
            this.detailOrder = res.data
          }
        })
        .catch(() => {})
    },
    handleCommand(command, row) {
      if (command === 'confirm') {
        this.$confirm('确认接单该订单吗？', '提示', { type: 'warning' })
          .then(() => {
            confirmOrder({ id: row.id, status: 3 })
              .then(() => {
                this.$message.success('接单成功')
                this.refreshAll()
              })
              .catch(() => {})
          })
          .catch(() => {})
      } else if (command === 'reject') {
        this.promptAction('请输入拒单原因', '拒单', reason => {
          rejectOrder({ id: row.id, rejectionReason: reason }).then(() => {
            this.$message.success('拒单成功')
            this.refreshAll()
          }).catch(() => {})
        })
      } else if (command === 'delivery') {
        this.$confirm('确认派送该订单吗？', '提示', { type: 'warning' })
          .then(() => {
            deliveryOrder(row.id)
              .then(() => {
                this.$message.success('订单已派送')
                this.refreshAll()
              })
              .catch(() => {})
          })
          .catch(() => {})
      } else if (command === 'complete') {
        this.$confirm('确认完成该订单吗？', '提示', { type: 'warning' })
          .then(() => {
            completeOrder(row.id)
              .then(() => {
                this.$message.success('订单已完成')
                this.refreshAll()
              })
              .catch(() => {})
          })
          .catch(() => {})
      } else if (command === 'cancel') {
        this.promptAction('请输入取消原因', '取消订单', reason => {
          cancelOrder({ id: row.id, cancelReason: reason }).then(() => {
            this.$message.success('订单已取消')
            this.refreshAll()
          }).catch(() => {})
        })
      } else if (command === 'refundApprove') {
        this.promptAction('请输入售后处理备注', '同意售后', remark => {
          approveRefund({
            id: row.id,
            refundReason: remark,
            refundAmount: row.refundAmount || row.amount,
            remark
          }).then(() => {
            this.$message.success('已同意售后')
            this.refreshAll()
          }).catch(() => {})
        })
      } else if (command === 'refundReject') {
        this.promptAction('请输入拒绝原因', '拒绝售后', reason => {
          rejectRefund({ id: row.id, rejectReason: reason }).then(() => {
            this.$message.success('已拒绝售后')
            this.refreshAll()
          }).catch(() => {})
        })
      }
    },
    promptAction(message, title, callback) {
      this.$prompt(message, title, {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputPattern: /\S+/,
        inputErrorMessage: '内容不能为空'
      })
        .then(({ value }) => {
          callback(value.trim())
        })
        .catch(() => {})
    },
    refreshAll() {
      this.fetchList()
      this.fetchStats()
    },
    statusLabel(status) {
      return (STATUS_MAP[status] && STATUS_MAP[status].label) || '未知'
    },
    statusTagType(status) {
      return (STATUS_MAP[status] && STATUS_MAP[status].type) || 'info'
    },
    refundLabel(status) {
      return (REFUND_MAP[status] && REFUND_MAP[status].label) || '售后'
    },
    refundTagType(status) {
      return (REFUND_MAP[status] && REFUND_MAP[status].type) || 'info'
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
.stat-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  display: flex;
  flex-direction: column;
  padding: 18px 20px;
  background-color: $bg-white;
  border: 1px solid transparent;
  border-radius: $border-radius-base;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.06);
  cursor: pointer;
  transition: all 0.2s;

  &:hover,
  &.active {
    border-color: $color-primary;
  }
}

.stat-label {
  font-size: 13px;
  color: $text-secondary;
}

.stat-value {
  margin-top: 8px;
  font-size: 26px;
  color: $text-primary;
}

.search-form {
  margin-bottom: 8px;
}

.pagination {
  margin-top: 20px;
  text-align: right;
}

@media (max-width: 900px) {
  .stat-grid {
    grid-template-columns: 1fr;
  }
}
</style>
