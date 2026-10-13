<template>
  <div>
    <el-dialog
      :title="dialogTitle"
      :visible.sync="dialogVisible"
      width="920px"
      append-to-body
      :close-on-click-modal="false"
      @closed="handleClosed"
    >
      <div v-loading="loading" class="stock-order-form">
        <div class="quick-entry">
          <el-input
            ref="codeInput"
            v-model="quickCode"
            class="quick-code"
            placeholder="扫描或输入商品编号 / SKU编号，回车添加"
            clearable
            @keyup.enter.native="handleQuickAdd"
          />
          <el-input-number
            v-model="quickQuantity"
            class="quick-quantity"
            :min="1"
            :max="999999"
            :precision="0"
            controls-position="right"
            @keyup.enter.native="handleQuickAdd"
          />
          <el-button
            type="primary"
            icon="el-icon-plus"
            :loading="lookupLoading"
            @click="handleQuickAdd"
          >
            添加
          </el-button>
        </div>
        <div class="quick-tip">
          输入商品编号时，如商品有多个规格会弹出规格选择；重复扫描同一商品会自动累加数量。
        </div>

        <el-table
          :data="items"
          border
          size="small"
          empty-text="请扫描或输入商品编号添加明细"
          class="item-table"
        >
          <el-table-column type="index" label="序号" width="60" align="center" />
          <el-table-column prop="productCode" label="商品编号" width="110" />
          <el-table-column prop="productName" label="商品名称" min-width="140" />
          <el-table-column label="SKU编号" width="120">
            <template slot-scope="{ row }">{{ row.skuCode || '-' }}</template>
          </el-table-column>
          <el-table-column label="规格" min-width="160">
            <template slot-scope="{ row }">{{ formatSkuAttrs(row.skuAttrs) }}</template>
          </el-table-column>
          <el-table-column label="数量" width="150">
            <template slot-scope="{ row }">
              <el-input-number
                v-model="row.quantity"
                :min="1"
                :max="999999"
                :precision="0"
                size="small"
                controls-position="right"
              />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" align="center">
            <template slot-scope="{ $index }">
              <el-button
                type="text"
                size="small"
                class="danger-text"
                @click="removeItem($index)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="summary-row">
          <span>共 {{ items.length }} 种商品</span>
          <span>合计数量：{{ totalQuantity }}</span>
        </div>

        <el-form label-width="70px" class="remark-form" @submit.native.prevent>
          <el-form-item label="备注">
            <el-input
              v-model="remark"
              type="textarea"
              :rows="3"
              maxlength="200"
              show-word-limit
              placeholder="备注（选填）"
            />
          </el-form-item>
        </el-form>
      </div>
      <span slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSubmit">
          保存待审核
        </el-button>
      </span>
    </el-dialog>

    <el-dialog
      title="选择商品规格"
      :visible.sync="skuDialogVisible"
      width="620px"
      append-to-body
    >
      <div v-if="pendingSkuItem" class="sku-dialog-tip">
        {{ pendingSkuItem.productName }}（{{ pendingSkuItem.productCode }}）有多个规格，请选择本次出入库的规格。
      </div>
      <el-table
        :data="pendingSkuItem ? pendingSkuItem.skuList : []"
        border
        size="small"
        max-height="360"
      >
        <el-table-column prop="skuCode" label="SKU编号" width="140" />
        <el-table-column label="规格" min-width="180">
          <template slot-scope="{ row }">{{ formatSkuAttrs(row.attrs) }}</template>
        </el-table-column>
        <el-table-column prop="stock" label="当前库存" width="100" align="right" />
        <el-table-column label="状态" width="80" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="mini">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="center">
          <template slot-scope="{ row }">
            <el-button
              type="text"
              size="small"
              :disabled="row.status !== 1"
              @click="handleSelectSku(row)"
            >
              选择
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <span slot="footer">
        <el-button @click="skuDialogVisible = false">取消</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import {
  lookupStockItem,
  getStockOrderDetail,
  addStockOrder,
  updateStockOrder
} from '@/api/stock'
import { formatSkuAttrs } from '@/utils/stock'

export default {
  name: 'StockOrderForm',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    type: {
      type: Number,
      default: 1
    },
    orderId: {
      type: [Number, String],
      default: null
    }
  },
  data() {
    return {
      loading: false,
      saving: false,
      lookupLoading: false,
      items: [],
      remark: '',
      quickCode: '',
      quickQuantity: 1,
      skuDialogVisible: false,
      pendingSkuItem: null,
      pendingQuantity: 1
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
    },
    typeName() {
      return this.type === 1 ? '入库单' : '出库单'
    },
    dialogTitle() {
      return this.orderId ? `编辑${this.typeName}` : `新建${this.typeName}`
    },
    totalQuantity() {
      return this.items.reduce(
        (sum, item) => sum + Number(item.quantity || 0),
        0
      )
    }
  },
  watch: {
    visible(value) {
      if (value) {
        this.init()
      }
    }
  },
  methods: {
    formatSkuAttrs,
    init() {
      this.reset()
      if (!this.orderId) {
        this.$nextTick(() => this.focusCode())
        return
      }

      this.loading = true
      getStockOrderDetail(this.orderId)
        .then(res => {
          const order = res.data || {}
          if (order.status !== 0) {
            this.$message.warning('只有待审核单据可以编辑')
            this.dialogVisible = false
            return
          }
          if (Number(order.type) !== this.type) {
            this.$message.warning('单据类型与当前页面不一致')
            this.dialogVisible = false
            return
          }

          this.items = (order.items || []).map(item => ({
            productId: item.productId,
            skuId: item.skuId,
            productCode: item.productCode,
            productName: item.productName,
            skuCode: item.skuCode,
            skuAttrs: item.skuAttrs,
            quantity: item.quantity
          }))
          this.remark = order.remark || ''
          this.$nextTick(() => this.focusCode())
        })
        .catch(() => {
          this.dialogVisible = false
        })
        .finally(() => {
          this.loading = false
        })
    },
    reset() {
      this.loading = false
      this.saving = false
      this.lookupLoading = false
      this.items = []
      this.remark = ''
      this.quickCode = ''
      this.quickQuantity = 1
      this.skuDialogVisible = false
      this.pendingSkuItem = null
      this.pendingQuantity = 1
    },
    handleClosed() {
      this.reset()
    },
    focusCode() {
      if (this.$refs.codeInput) {
        this.$refs.codeInput.focus()
      }
    },
    clearQuickInput() {
      this.quickCode = ''
      this.quickQuantity = 1
    },
    handleQuickAdd() {
      if (this.lookupLoading) {
        return
      }
      const code = (this.quickCode || '').trim()
      if (!code) {
        this.focusCode()
        return
      }

      const quantity = Number(this.quickQuantity) || 1
      this.lookupLoading = true
      lookupStockItem(code)
        .then(res => {
          const item = res.data || {}
          if (item.skuId) {
            this.appendItem(
              item,
              {
                skuId: item.skuId,
                skuCode: item.skuCode,
                attrs: item.attrs
              },
              quantity
            )
            this.clearQuickInput()
            return
          }

          const skuList = item.skuList || []
          if (!skuList.length) {
            this.appendItem(item, null, quantity)
            this.clearQuickInput()
            return
          }

          if (skuList.length === 1) {
            const sku = skuList[0]
            if (sku.status !== 1) {
              this.$message.warning('该商品规格已禁用')
              return
            }
            this.appendItem(item, sku, quantity)
            this.clearQuickInput()
            return
          }

          this.pendingSkuItem = item
          this.pendingQuantity = quantity
          this.skuDialogVisible = true
        })
        .catch(() => {
          // 查询失败时保留输入内容，方便修正后重试
        })
        .finally(() => {
          this.lookupLoading = false
          if (!this.skuDialogVisible) {
            this.focusCode()
          }
        })
    },
    appendItem(product, sku, quantity) {
      const skuId = sku ? sku.skuId : null
      const existing = this.items.find(
        item =>
          item.productId === product.productId &&
          (item.skuId || null) === (skuId || null)
      )

      if (existing) {
        existing.quantity = Number(existing.quantity || 0) + quantity
        return
      }

      this.items.push({
        productId: product.productId,
        skuId,
        productCode: product.productCode,
        productName: product.productName,
        skuCode: sku ? sku.skuCode : null,
        skuAttrs: sku ? sku.attrs : null,
        quantity
      })
    },
    handleSelectSku(sku) {
      if (!this.pendingSkuItem || sku.status !== 1) {
        return
      }
      this.appendItem(this.pendingSkuItem, sku, this.pendingQuantity)
      this.skuDialogVisible = false
      this.clearQuickInput()
      this.$nextTick(() => this.focusCode())
    },
    removeItem(index) {
      this.items.splice(index, 1)
    },
    handleSubmit() {
      if (!this.items.length) {
        this.$message.warning('请先添加商品明细')
        return
      }
      const invalid = this.items.some(
        item => !item.quantity || item.quantity < 1 || item.quantity > 999999
      )
      if (invalid) {
        this.$message.warning('数量必须为 1~999999 的整数')
        return
      }

      const payload = {
        id: this.orderId || undefined,
        type: this.type,
        remark: (this.remark || '').trim(),
        items: this.items.map(item => ({
          productId: item.productId,
          skuId: item.skuId || null,
          quantity: item.quantity
        }))
      }

      this.saving = true
      const action = this.orderId
        ? updateStockOrder(payload)
        : addStockOrder(payload)

      action
        .then(() => {
          this.$message.success(
            this.orderId ? '单据已保存' : '单据已创建，请在列表中审核'
          )
          this.dialogVisible = false
          this.$emit('success')
        })
        .catch(() => {})
        .finally(() => {
          this.saving = false
        })
    }
  }
}
</script>

<style lang="scss" scoped>
.stock-order-form {
  min-height: 180px;
}

.quick-entry {
  display: flex;
  gap: 10px;
  align-items: center;
}

.quick-code {
  flex: 1;
}

.quick-quantity {
  width: 150px;
  flex-shrink: 0;
}

.quick-tip {
  margin: 8px 0 14px;
  font-size: 12px;
  color: #909399;
}

.item-table {
  margin-bottom: 12px;
}

.summary-row {
  display: flex;
  justify-content: flex-end;
  gap: 32px;
  margin-bottom: 16px;
  font-weight: 600;
}

.remark-form {
  margin-bottom: 0;
}

.danger-text {
  color: $color-danger;
}

.sku-dialog-tip {
  margin-bottom: 12px;
  color: #606266;
  line-height: 1.6;
}
</style>
