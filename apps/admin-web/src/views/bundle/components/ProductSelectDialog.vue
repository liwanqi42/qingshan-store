<template>
  <el-dialog
    title="选择商品"
    :visible.sync="dialogVisible"
    width="720px"
    append-to-body
  >
    <div class="select-toolbar">
      <el-input
        v-model="queryName"
        placeholder="搜索商品名称"
        clearable
        class="select-input"
        @keyup.enter.native="fetchProducts"
      />
      <el-button type="primary" icon="el-icon-search" @click="fetchProducts">
        查询
      </el-button>
    </div>

    <el-table
      v-loading="loading"
      :data="list"
      border
      highlight-current-row
      max-height="360"
      @current-change="handleSelect"
    >
      <el-table-column label="图片" width="80">
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
      <el-table-column prop="name" label="商品名称" min-width="140" />
      <el-table-column prop="categoryName" label="分类" min-width="100" />
      <el-table-column label="价格" width="100">
        <template slot-scope="{ row }">¥{{ formatMoney(row.price) }}</template>
      </el-table-column>
    </el-table>

    <div class="copies-row">
      <span class="copies-label">商品份数</span>
      <el-input-number v-model="copies" :min="1" :max="99" />
    </div>

    <span slot="footer">
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="confirm">确定</el-button>
    </span>
  </el-dialog>
</template>

<script>
import { getProductPage } from '@/api/product'

export default {
  name: 'ProductSelectDialog',
  props: {
    visible: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      queryName: '',
      list: [],
      loading: false,
      selectedProduct: null,
      copies: 1
    }
  },
  computed: {
    dialogVisible: {
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
        this.selectedProduct = null
        this.copies = 1
        this.fetchProducts()
      }
    }
  },
  methods: {
    fetchProducts() {
      this.loading = true
      getProductPage({
        name: this.queryName.trim(),
        status: 1,
        page: 1,
        pageSize: 100
      })
        .then(res => {
          const data = res.data || {}
          this.list = data.records || []
        })
        .catch(() => {})
        .finally(() => {
          this.loading = false
        })
    },
    handleSelect(row) {
      this.selectedProduct = row
    },
    confirm() {
      if (!this.selectedProduct) {
        this.$message.warning('请选择商品')
        return
      }
      this.$emit('confirm', {
        product: this.selectedProduct,
        copies: this.copies || 1
      })
      this.dialogVisible = false
    },
    formatMoney(value) {
      return Number(value || 0).toFixed(2)
    }
  }
}
</script>

<style lang="scss" scoped>
.select-toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
}

.select-input {
  width: 260px;
}

.product-image {
  width: 48px;
  height: 48px;
  border-radius: 4px;
}

.copies-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 16px;
}

.copies-label {
  font-size: 14px;
  color: $text-regular;
}
</style>
