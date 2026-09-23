<template>
  <div class="app-container">
    <el-card shadow="never">
      <div class="toolbar">
        <el-form :inline="true" :model="queryForm" class="search-form" @submit.native.prevent>
          <el-form-item label="套餐名称">
            <el-input
              v-model="queryForm.name"
              placeholder="请输入套餐名称"
              clearable
              @keyup.enter.native="handleSearch"
            />
          </el-form-item>
          <el-form-item label="套餐分类">
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
        <el-button type="primary" icon="el-icon-plus" @click="handleAdd">
          新增套餐
        </el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column label="图片" width="90">
          <template slot-scope="{ row }">
            <el-image
              v-if="row.image"
              :src="row.image"
              fit="cover"
              class="bundle-image"
            />
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="套餐名称" min-width="150" />
        <el-table-column prop="categoryName" label="分类" min-width="110" />
        <el-table-column label="价格" width="100">
          <template slot-scope="{ row }">¥{{ formatMoney(row.price) }}</template>
        </el-table-column>
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

    <bundle-form
      :visible.sync="formVisible"
      :bundle="editingBundle"
      :categories="categories"
      @success="handleFormSuccess"
    />
  </div>
</template>

<script>
import {
  getBundlePage,
  deleteBundles,
  enableOrDisableBundle
} from '@/api/bundle'
import { getCategoryList } from '@/api/category'
import BundleForm from './components/BundleForm.vue'

export default {
  name: 'BundleView',
  components: {
    BundleForm
  },
  data() {
    return {
      queryForm: {
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
      editingBundle: null
    }
  },
  created() {
    this.loadCategories()
    this.fetchList()
  },
  methods: {
    loadCategories() {
      getCategoryList(2)
        .then(res => {
          this.categories = res.data || []
        })
        .catch(() => {})
    },
    fetchList() {
      this.loading = true
      getBundlePage({
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
      this.queryForm = { name: '', categoryId: '', status: '' }
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
      this.editingBundle = null
      this.formVisible = true
    },
    handleEdit(row) {
      this.editingBundle = { ...row }
      this.formVisible = true
    },
    handleFormSuccess() {
      this.fetchList()
    },
    handleStatusChange(row) {
      const status = row.status
      const previous = status === 1 ? 0 : 1
      enableOrDisableBundle(status, row.id)
        .then(() => {
          this.$message.success(status === 1 ? '套餐已上架' : '套餐已下架')
        })
        .catch(() => {
          row.status = previous
        })
    },
    handleDelete(row) {
      this.$confirm(`确定删除套餐“${row.name}”吗？`, '提示', {
        type: 'warning'
      })
        .then(() => {
          deleteBundles([row.id])
            .then(() => {
              this.$message.success('套餐已删除')
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

.bundle-image {
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
