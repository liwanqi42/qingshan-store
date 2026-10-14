<template>
  <div class="app-container">
    <el-card shadow="never">
      <el-form :inline="true" :model="queryForm" class="search-form" @submit.native.prevent>
        <el-form-item label="单据编号">
          <el-input
            v-model="queryForm.orderNo"
            placeholder="请输入单据编号"
            clearable
            @keyup.enter.native="handleSearch"
          />
        </el-form-item>
        <el-form-item label="商品编号">
          <el-input
            v-model="queryForm.code"
            placeholder="商品编号或SKU编号"
            clearable
            @keyup.enter.native="handleSearch"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="全部" clearable>
            <el-option label="待审核" :value="0" />
            <el-option label="已审核" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="创建日期">
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

      <div class="toolbar">
        <span class="toolbar-title">{{ pageTitle }}</span>
        <div class="toolbar-actions">
          <el-button
            icon="el-icon-download"
            :loading="exporting"
            @click="handleExport"
          >
            导出Excel
          </el-button>
          <el-button type="primary" icon="el-icon-plus" @click="handleAdd">
            新建{{ pageTitle }}
          </el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="orderNo" label="单据编号" min-width="190" />
        <el-table-column prop="totalKinds" label="商品种数" width="90" align="center" />
        <el-table-column prop="totalQuantity" label="总数量" width="90" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="stockStatusTagType(row.status)" size="mini">
              {{ stockStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="制单人" width="100">
          <template slot-scope="{ row }">{{ row.createUserName || '-' }}</template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="170">
          <template slot-scope="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="审核人" width="100">
          <template slot-scope="{ row }">{{ row.auditUserName || '-' }}</template>
        </el-table-column>
        <el-table-column label="审核时间" min-width="170">
          <template slot-scope="{ row }">{{ formatDateTime(row.auditTime) }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
        <el-table-column label="操作" width="250" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" size="small" @click="handleDetail(row)">
              详情
            </el-button>
            <el-button type="text" size="small" @click="handlePrint(row)">
              打印
            </el-button>
            <el-button
              v-if="row.status === 0 && canAudit"
              type="text"
              size="small"
              @click="handleAudit(row)"
            >
              审核
            </el-button>
            <el-button
              v-if="row.status === 0"
              type="text"
              size="small"
              @click="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              v-if="row.status === 0"
              type="text"
              size="small"
              class="danger-text"
              @click="handleDelete(row)"
            >
              删除
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

    <stock-order-form
      :visible.sync="formVisible"
      :type="stockType"
      :order-id="editingId"
      @success="handleFormSuccess"
    />

    <el-dialog
      title="单据详情"
      :visible.sync="detailVisible"
      width="900px"
      append-to-body
    >
      <div v-if="detailOrder" class="detail-wrap">
        <div class="detail-grid">
          <div><span class="detail-label">单据编号：</span>{{ detailOrder.orderNo }}</div>
          <div>
            <span class="detail-label">单据状态：</span>
            <el-tag :type="stockStatusTagType(detailOrder.status)" size="mini">
              {{ stockStatusLabel(detailOrder.status) }}
            </el-tag>
          </div>
          <div><span class="detail-label">制单人：</span>{{ detailOrder.createUserName || '-' }}</div>
          <div><span class="detail-label">制单时间：</span>{{ formatDateTime(detailOrder.createTime) }}</div>
          <div><span class="detail-label">审核人：</span>{{ detailOrder.auditUserName || '-' }}</div>
          <div><span class="detail-label">审核时间：</span>{{ formatDateTime(detailOrder.auditTime) }}</div>
          <div class="detail-full">
            <span class="detail-label">备注：</span>{{ detailOrder.remark || '-' }}
          </div>
        </div>

        <el-table :data="detailOrder.items || []" border size="small">
          <el-table-column type="index" label="序号" width="60" align="center" />
          <el-table-column prop="productCode" label="商品编号" width="110" />
          <el-table-column prop="productName" label="商品名称" min-width="140" />
          <el-table-column label="SKU编号" width="120">
            <template slot-scope="{ row }">{{ row.skuCode || '-' }}</template>
          </el-table-column>
          <el-table-column label="规格" min-width="160">
            <template slot-scope="{ row }">{{ formatSkuAttrs(row.skuAttrs) }}</template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="80" align="right" />
          <el-table-column label="审核前" width="90" align="right">
            <template slot-scope="{ row }">
              {{ row.stockBefore === null || row.stockBefore === undefined ? '-' : row.stockBefore }}
            </template>
          </el-table-column>
          <el-table-column label="审核后" width="90" align="right">
            <template slot-scope="{ row }">
              {{ row.stockAfter === null || row.stockAfter === undefined ? '-' : row.stockAfter }}
            </template>
          </el-table-column>
        </el-table>

        <div class="detail-summary">
          共 {{ detailTotalKinds }} 种商品，合计数量：{{ detailTotalQuantity }}
        </div>
      </div>
      <span slot="footer">
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="primary" icon="el-icon-printer" @click="printDetail">
          打印
        </el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import {
  getStockOrderPage,
  getStockOrderDetail,
  auditStockOrder,
  deleteStockOrder,
  exportStockOrders
} from '@/api/stock'
import { downloadBlob } from '@/utils/download'
import {
  stockStatusLabel,
  stockStatusTagType,
  formatSkuAttrs,
  formatDateTime
} from '@/utils/stock'
import { buildStockOrderPrintHtml, openPrintWindow } from '@/utils/print'
import StockOrderForm from './components/StockOrderForm.vue'

export default {
  name: 'StockOrderView',
  components: {
    StockOrderForm
  },
  data() {
    return {
      queryForm: {
        orderNo: '',
        code: '',
        status: ''
      },
      dateRange: [],
      list: [],
      total: 0,
      page: 1,
      pageSize: 10,
      loading: false,
      exporting: false,
      formVisible: false,
      editingId: null,
      detailVisible: false,
      detailOrder: null
    }
  },
  computed: {
    stockType() {
      return Number(this.$route.meta.stockType) || 1
    },
    pageTitle() {
      return this.stockType === 1 ? '入库单' : '出库单'
    },
    canAudit() {
      const roles = this.$store.getters['user/roles'] || []
      return roles.includes('admin')
    },
    detailTotalKinds() {
      return this.detailOrder && this.detailOrder.items
        ? this.detailOrder.items.length
        : 0
    },
    detailTotalQuantity() {
      const items =
        this.detailOrder && this.detailOrder.items ? this.detailOrder.items : []
      return items.reduce((sum, item) => sum + Number(item.quantity || 0), 0)
    }
  },
  created() {
    this.fetchList()
  },
  methods: {
    stockStatusLabel,
    stockStatusTagType,
    formatSkuAttrs,
    formatDateTime,
    buildQuery() {
      const status = this.queryForm.status
      const range = this.dateRange || []
      return {
        type: this.stockType,
        orderNo: this.queryForm.orderNo.trim(),
        code: this.queryForm.code.trim(),
        status:
          status === '' || status === null || status === undefined
            ? undefined
            : status,
        beginDate: range[0] || undefined,
        endDate: range[1] || undefined
      }
    },
    fetchList() {
      this.loading = true
      getStockOrderPage({
        ...this.buildQuery(),
        page: this.page,
        pageSize: this.pageSize
      })
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
    handleSearch() {
      this.page = 1
      this.fetchList()
    },
    handleReset() {
      this.queryForm = { orderNo: '', code: '', status: '' }
      this.dateRange = []
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
    handleAdd() {
      this.editingId = null
      this.formVisible = true
    },
    handleEdit(row) {
      this.editingId = row.id
      this.formVisible = true
    },
    handleFormSuccess() {
      this.fetchList()
    },
    handleDetail(row) {
      getStockOrderDetail(row.id)
        .then(res => {
          this.detailOrder = res.data
          this.detailVisible = true
        })
        .catch(() => {})
    },
    handlePrint(row) {
      getStockOrderDetail(row.id)
        .then(res => this.printOrder(res.data))
        .catch(() => {})
    },
    printDetail() {
      if (this.detailOrder) {
        this.printOrder(this.detailOrder)
      }
    },
    printOrder(order) {
      if (!order) {
        return
      }
      const html = buildStockOrderPrintHtml(order, this.pageTitle)
      const opened = openPrintWindow(`${this.pageTitle} ${order.orderNo}`, html)
      if (!opened) {
        this.$message.warning('打印窗口被浏览器拦截，请允许弹出窗口后重试')
      }
    },
    handleAudit(row) {
      if (!this.canAudit) {
        this.$message.warning('仅管理员可审核库存单据')
        return
      }
      const action = this.stockType === 1 ? '增加' : '扣减'
      this.$confirm(
        `审核通过后将${action}库存且单据不可修改，确定审核「${row.orderNo}」吗？`,
        '审核确认',
        {
          confirmButtonText: '确定审核',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )
        .then(() => {
          auditStockOrder(row.id)
            .then(() => {
              this.$message.success('审核通过，库存已更新')
              this.fetchList()
            })
            .catch(() => {})
        })
        .catch(() => {})
    },
    handleDelete(row) {
      this.$confirm(`确定删除单据「${row.orderNo}」吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
        .then(() => {
          deleteStockOrder(row.id)
            .then(() => {
              this.$message.success('单据已删除')
              this.fetchList()
            })
            .catch(() => {})
        })
        .catch(() => {})
    },
    handleExport() {
      if (!this.total) {
        this.$message.warning('没有可导出的数据')
        return
      }
      this.exporting = true
      exportStockOrders(this.buildQuery())
        .then(res => {
          downloadBlob(res, `${this.pageTitle}明细.xlsx`)
        })
        .catch(() => {})
        .finally(() => {
          this.exporting = false
        })
    }
  }
}
</script>

<style lang="scss" scoped>
.search-form {
  margin-bottom: 8px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.toolbar-title {
  font-size: 16px;
  font-weight: 600;
  color: $text-primary;
}

.toolbar-actions {
  display: flex;
  gap: 10px;
}

.pagination {
  margin-top: 20px;
  text-align: right;
}

.danger-text {
  color: $color-danger;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 24px;
  margin-bottom: 16px;
}

.detail-full {
  grid-column: 1 / -1;
}

.detail-label {
  color: #909399;
}

.detail-summary {
  margin-top: 12px;
  text-align: right;
  font-weight: 600;
}
</style>
