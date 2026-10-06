<template>
  <div class="app-container">
    <el-card shadow="never">
      <div class="toolbar">
        <el-form :inline="true" :model="queryForm" class="search-form" @submit.native.prevent>
          <el-form-item label="商品编号">
            <el-input
              v-model="queryForm.code"
              placeholder="商品编号或SKU编号"
              clearable
              @keyup.enter.native="handleSearch"
            />
          </el-form-item>
          <el-form-item label="商品名称">
            <el-input
              v-model="queryForm.name"
              placeholder="请输入商品名称"
              clearable
              @keyup.enter.native="handleSearch"
            />
          </el-form-item>
          <el-form-item label="商品分类">
            <el-select v-model="queryForm.categoryId" placeholder="全部" clearable>
              <el-option
                v-for="item in categories"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="queryForm.status" placeholder="全部" clearable>
              <el-option label="上架" :value="1" />
              <el-option label="下架" :value="0" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" @click="handleSearch">
              查询
            </el-button>
            <el-button icon="el-icon-refresh" @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
        <div class="toolbar-actions">
          <el-button
            icon="el-icon-delete"
            :disabled="!selectedRows.length"
            @click="handleBatchDelete"
          >
            批量删除
          </el-button>
          <el-button type="primary" icon="el-icon-plus" @click="handleAdd">
            新增商品
          </el-button>
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="list"
        border
        stripe
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="46" />
        <el-table-column label="图片" width="90">
          <template slot-scope="{ row }">
            <el-image
              v-if="row.image"
              :src="row.image"
              fit="cover"
              class="product-image"
            />
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="productCode" label="商品编号" width="110" />
        <el-table-column prop="name" label="商品名称" min-width="140" />
        <el-table-column prop="categoryName" label="分类" min-width="110" />
        <el-table-column label="价格" width="100">
          <template slot-scope="{ row }">¥{{ formatMoney(row.price) }}</template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" width="90" />
        <el-table-column label="状态" width="100">
          <template slot-scope="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              active-color="#67c23a"
              inactive-color="#f56c6c"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="更新时间" min-width="170">
          <template slot-scope="{ row }">
            {{ formatDateTime(row.updateTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" size="small" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button type="text" size="small" class="danger-text" @click="handleDelete(row)">
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

    <product-form
      :visible.sync="formVisible"
      :product="editingProduct"
      :categories="categories"
      @success="handleFormSuccess"
    />
  </div>
</template>

<script>
import {
  getProductPage,
  deleteProducts,
  enableOrDisableProduct
} from '@/api/product'
import { getCategoryList } from '@/api/category'
import ProductForm from './components/ProductForm.vue'

export default {
  name: 'ProductView',
  components: {
    ProductForm
  },
  data() {
    return {
      queryForm: {
        code: '',
        name: '',
        categoryId: '',
        status: ''
      },
      categories: [],
      list: [],
      total: 0,
      page: 1,
      pageSize: 10,
      loading: false,
      formVisible: false,
      editingProduct: null,
      selectedRows: []
    }
  },
  created() {
    this.loadCategories()
    this.fetchList()
  },
  methods: {
    loadCategories() {
      getCategoryList(1)
        .then(res => {
          this.categories = res.data || []
        })
        .catch(() => {})
    },
    fetchList() {
      this.loading = true
      getProductPage({
        code: this.queryForm.code.trim(),
        name: this.queryForm.name.trim(),
        categoryId: this.queryForm.categoryId || undefined,
        status:
          this.queryForm.status === '' ? undefined : this.queryForm.status,
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
      this.queryForm = { code: '', name: '', categoryId: '', status: '' }
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
    handleSelectionChange(rows) {
      this.selectedRows = rows
    },
    handleAdd() {
      this.editingProduct = null
      this.formVisible = true
    },
    handleEdit(row) {
      this.editingProduct = { ...row }
      this.formVisible = true
    },
    handleFormSuccess() {
      this.fetchList()
    },
    handleStatusChange(row) {
      const status = row.status
      const previous = status === 1 ? 0 : 1
      enableOrDisableProduct(status, row.id)
        .then(() => {
          this.$message.success(status === 1 ? '商品已上架' : '商品已下架')
        })
        .catch(() => {
          row.status = previous
        })
    },
    handleDelete(row) {
      this.removeProducts([row.id], `确定删除商品“${row.name}”吗？`)
    },
    handleBatchDelete() {
      this.removeProducts(
        this.selectedRows.map(row => row.id),
        '确定删除选中的商品吗？'
      )
    },
    removeProducts(ids, message) {
      this.$confirm(message, '提示', { type: 'warning' })
        .then(() => {
          deleteProducts(ids)
            .then(() => {
              this.$message.success('商品已删除')
              this.fetchList()
            })
            .catch(() => {})
        })
        .catch(() => {})
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
.toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.search-form {
  flex: 1;
  min-width: 0;
}

.toolbar-actions {
  display: flex;
  flex-shrink: 0;
  gap: 10px;
}

.product-image {
  width: 56px;
  height: 56px;
  border-radius: 4px;
}

.pagination {
  margin-top: 20px;
  text-align: right;
}

.danger-text {
  color: $color-danger;
}
</style>
