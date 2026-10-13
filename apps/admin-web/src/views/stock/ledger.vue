<template>
  <div class="app-container">
    <el-card shadow="never">
      <el-form :inline="true" :model="queryForm" class="search-form" @submit.native.prevent>
        <el-form-item label="类型">
          <el-select v-model="queryForm.type" placeholder="全部" clearable>
            <el-option label="入库" :value="1" />
            <el-option label="出库" :value="2" />
          </el-select>
        </el-form-item>
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
        <el-form-item label="操作日期">
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

      <el-alert
        title="库存台账仅记录手工入库/出库审核通过产生的库存变化，销售下单、退款等不在此台账中。"
        type="info"
        :closable="false"
        show-icon
        class="ledger-tip"
      />

      <div class="toolbar">
        <span class="toolbar-title">库存流水台账</span>
        <el-button
          icon="el-icon-download"
          :loading="exporting"
          @click="handleExport"
        >
          导出Excel
        </el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="orderNo" label="单据编号" min-width="190" />
        <el-table-column label="类型" width="80" align="center">
          <template slot-scope="{ row }">
            <el-tag
              :type="Number(row.type) === 2 ? 'danger' : 'success'"
              size="mini"
            >
              {{ stockTypeLabel(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="productCode" label="商品编号" width="110" />
        <el-table-column prop="productName" label="商品名称" min-width="140" />
        <el-table-column label="SKU编号" width="120">
          <template slot-scope="{ row }">{{ row.skuCode || '-' }}</template>
        </el-table-column>
        <el-table-column label="规格" min-width="160">
          <template slot-scope="{ row }">{{ formatSkuAttrs(row.skuAttrs) }}</template>
        </el-table-column>
        <el-table-column label="变动数量" width="100" align="right">
          <template slot-scope="{ row }">
            <span :class="Number(row.type) === 2 ? 'qty-out' : 'qty-in'">
              {{ Number(row.type) === 2 ? '-' : '+' }}{{ row.quantity }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="stockBefore" label="变动前" width="90" align="right" />
        <el-table-column prop="stockAfter" label="变动后" width="90" align="right" />
        <el-table-column label="操作人" width="100">
          <template slot-scope="{ row }">{{ row.createUserName || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作时间" min-width="170">
          <template slot-scope="{ row }">{{ formatDateTime(row.createTime) }}</template>
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
  </div>
</template>

<script>
import { getStockLedgerPage, exportStockLedger } from '@/api/stock'
import { downloadBlob } from '@/utils/download'
import {
  stockTypeLabel,
  formatSkuAttrs,
  formatDateTime
} from '@/utils/stock'

export default {
  name: 'StockLedgerView',
  data() {
    return {
      queryForm: {
        type: '',
        orderNo: '',
        code: ''
      },
      dateRange: [],
      list: [],
      total: 0,
      page: 1,
      pageSize: 10,
      loading: false,
      exporting: false
    }
  },
  created() {
    this.fetchList()
  },
  methods: {
    stockTypeLabel,
    formatSkuAttrs,
    formatDateTime,
    buildQuery() {
      const type = this.queryForm.type
      const range = this.dateRange || []
      return {
        type:
          type === '' || type === null || type === undefined ? undefined : type,
        orderNo: this.queryForm.orderNo.trim(),
        code: this.queryForm.code.trim(),
        beginDate: range[0] || undefined,
        endDate: range[1] || undefined
      }
    },
    fetchList() {
      this.loading = true
      getStockLedgerPage({
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
      this.queryForm = { type: '', orderNo: '', code: '' }
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
    handleExport() {
      if (!this.total) {
        this.$message.warning('没有可导出的数据')
        return
      }
      this.exporting = true
      exportStockLedger(this.buildQuery())
        .then(res => {
          downloadBlob(res, '库存流水台账.xlsx')
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

.ledger-tip {
  margin-bottom: 14px;
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

.pagination {
  margin-top: 20px;
  text-align: right;
}

.qty-in {
  color: $color-success;
  font-weight: 600;
}

.qty-out {
  color: $color-danger;
  font-weight: 600;
}
</style>
