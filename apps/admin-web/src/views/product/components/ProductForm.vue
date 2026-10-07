<template>
  <el-dialog
    :title="form.id ? '编辑商品' : '新增商品'"
    :visible.sync="dialogVisible"
    width="960px"
    append-to-body
    @closed="handleClosed"
  >
    <el-form
      ref="form"
      :model="form"
      :rules="rules"
      label-width="100px"
    >
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="商品编号" prop="productCode">
            <el-input
              v-model="form.productCode"
              placeholder="如 3132，SKU 编号为“商品编号-序号”，如 3132-1"
              clearable
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="商品名称" prop="name">
            <el-input v-model="form.name" placeholder="请输入商品名称" clearable />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="商品分类" prop="categoryId">
            <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 100%">
              <el-option
                v-for="item in categories"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="价格" prop="price">
            <el-input-number v-model="form.price" :min="0" :precision="2" :step="1" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="封面图片" required>
        <image-upload v-model="form.image" :type="1" />
        <div class="field-tip">用于商品列表和详情页首图</div>
      </el-form-item>
      <el-form-item label="商品轮播图">
        <image-upload-list v-model="form.images" :type="2" :limit="6" />
      </el-form-item>
      <el-form-item label="详情长图">
        <image-upload-list v-model="form.detailImages" :type="3" :limit="20" />
      </el-form-item>
      <el-form-item label="描述">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="2"
          placeholder="请输入商品描述"
        />
      </el-form-item>
      <el-form-item label="状态">
        <el-radio-group v-model="form.status">
          <el-radio :label="1">上架</el-radio>
          <el-radio :label="0">下架</el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="商品规格">
        <div class="spec-editor">
          <div
            v-for="(group, index) in specGroups"
            :key="group.key"
            class="spec-group"
          >
            <div class="spec-field">
              <span class="spec-field-label">规格名</span>
              <el-input
                v-model="group.name"
                class="spec-name"
                placeholder="如：颜色"
                clearable
              />
            </div>
            <div class="spec-field spec-field-values">
              <span class="spec-field-label">规格值</span>
              <div class="spec-values">
                <el-tag
                  v-for="value in group.values"
                  :key="value"
                  size="small"
                  class="spec-value-tag"
                  closable
                  @close="removeSpecValue(index, value)"
                >
                  {{ value }}
                </el-tag>
                <el-input
                  v-model="group.inputValue"
                  class="spec-value-input"
                  size="small"
                  placeholder="输入规格值后按回车，如黑色"
                  @keydown.enter.native="addSpecValue(index, $event)"
                  @blur.native="addSpecValue(index)"
                  @compositionstart.native="group.composing = true"
                  @compositionend.native="group.composing = false"
                />
              </div>
            </div>
            <el-button
              type="text"
              size="mini"
              class="danger-text"
              @click="removeSpecGroup(index)"
            >
              删除规格
            </el-button>
          </div>
          <div class="spec-actions">
            <el-button size="mini" icon="el-icon-plus" @click="addSpecGroup">
              添加规格
            </el-button>
            <span class="field-tip">
              一种规格（如颜色、尺码）占一行，同一规格下可添加多个规格值
            </span>
          </div>
          <div v-if="specIncomplete" class="spec-warning">
            请补全规格名和规格值，补全后会自动生成 SKU 组合
          </div>
          <div v-else-if="singleValueSpecWarning" class="spec-warning">
            每个规格目前只有 1 个规格值，只会生成 1 个 SKU。如果这些值属于同一种规格，
            请合并到一行：规格名填「颜色」，规格值依次添加黑色、灰色、白色。
          </div>
          <div v-else-if="skuCombinationCount" class="spec-summary">
            当前将生成 {{ skuCombinationCount }} 个 SKU 组合
          </div>
        </div>
      </el-form-item>

      <el-form-item label="SKU 列表">
        <el-table :data="skuList" border size="mini" class="sku-table">
          <el-table-column label="编号" width="170">
            <template slot-scope="{ row, $index }">
              <span :class="{ 'placeholder-text': !form.productCode.trim() }">
                {{ displaySkuCode(row, $index) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="规格" min-width="120">
            <template slot-scope="{ row }">
              {{ formatAttrs(row.attrs) }}
            </template>
          </el-table-column>
          <el-table-column label="图片" width="100" align="center">
            <template slot-scope="{ row }">
              <image-upload v-model="row.image" :type="2" :size="56" />
            </template>
          </el-table-column>
          <el-table-column label="价格" width="140">
            <template slot-scope="{ row }">
              <el-input-number
                v-model="row.price"
                :min="0"
                :precision="2"
                size="mini"
              />
            </template>
          </el-table-column>
          <el-table-column label="库存" width="120">
            <template slot-scope="{ row }">
              <el-input-number v-model="row.stock" :min="0" size="mini" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="70" align="center">
            <template slot-scope="{ $index }">
              <el-button
                type="text"
                size="mini"
                class="danger-text"
                @click="removeSku($index)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <div v-if="!skuList.length" class="spec-warning">
          请至少保留一个 SKU
        </div>
      </el-form-item>
    </el-form>
    <span slot="footer">
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">
        保存
      </el-button>
    </span>
  </el-dialog>
</template>

<script>
import {
  getProductById,
  addProduct,
  updateProduct
} from '@/api/product'
import ImageUpload from '@/components/ImageUpload/index.vue'
import ImageUploadList from '@/components/ImageUploadList/index.vue'

let specKeySeed = 0

function nextSpecKey() {
  specKeySeed += 1
  return `spec-${specKeySeed}`
}

function parseAttrs(attrs) {
  if (!attrs) {
    return {}
  }
  if (typeof attrs === 'string') {
    try {
      return JSON.parse(attrs)
    } catch (e) {
      return {}
    }
  }
  return attrs
}

function parseAttrValues(attrValues) {
  if (Array.isArray(attrValues)) {
    return attrValues.filter(Boolean)
  }
  if (typeof attrValues === 'string') {
    try {
      const parsed = JSON.parse(attrValues)
      return Array.isArray(parsed) ? parsed.filter(Boolean) : []
    } catch (e) {
      return []
    }
  }
  return []
}

export default {
  name: 'ProductForm',
  components: {
    ImageUpload,
    ImageUploadList
  },
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    product: {
      type: Object,
      default: null
    },
    categories: {
      type: Array,
      default: () => []
    }
  },
  data() {
    const validatePrice = (rule, value, callback) => {
      if (value === null || value === undefined || Number(value) <= 0) {
        callback(new Error('价格必须大于 0'))
        return
      }
      callback()
    }

    return {
      form: {
        id: undefined,
        productCode: '',
        name: '',
        categoryId: '',
        price: 0,
        image: '',
        images: [],
        detailImages: [],
        description: '',
        status: 0
      },
      specGroups: [],
      skuList: [],
      specIncomplete: false,
      // 上次同步时的规格快照，用于判断是新增/删除规格值还是规格结构变化
      specSnapshot: [],
      // 表单回填期间不触发SKU自动生成，避免把已删除的SKU重新生成出来
      formLoading: false,
      rules: {
        productCode: [
          { required: true, message: '请输入商品编号', trigger: 'blur' },
          {
            pattern: /^[0-9A-Za-z_-]+$/,
            message: '商品编号只能包含字母、数字、下划线和中划线',
            trigger: 'blur'
          }
        ],
        name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
        categoryId: [
          { required: true, message: '请选择商品分类', trigger: 'change' }
        ],
        price: [{ validator: validatePrice, trigger: 'blur' }]
      },
      submitting: false
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
    completeSpecGroups() {
      return this.activeSpecGroups().filter(
        group =>
          group.name &&
          group.name.trim() &&
          Array.isArray(group.values) &&
          group.values.length
      )
    },
    skuCombinationCount() {
      if (!this.completeSpecGroups.length) {
        return 0
      }
      return this.completeSpecGroups.reduce(
        (total, group) => total * group.values.length,
        1
      )
    },
    singleValueSpecWarning() {
      return (
        this.completeSpecGroups.length > 1 &&
        this.completeSpecGroups.every(group => group.values.length === 1)
      )
    }
  },
  watch: {
    visible(value) {
      if (value) {
        this.handleOpen()
      }
    },
    specGroups: {
      deep: true,
      handler() {
        if (this.formLoading) {
          return
        }
        this.syncSkus()
      }
    },
    'form.price'(value, oldValue) {
      if (this.formLoading) {
        return
      }
      // 商品价格作为SKU默认价格：未单独定价的SKU跟随商品价格变化
      const next = Number(value || 0)
      const previous = Number(oldValue || 0)
      this.skuList.forEach(sku => {
        const price = Number(sku.price || 0)
        if (price === 0 || price === previous) {
          sku.price = next
        }
      })
    }
  },
  methods: {
    handleOpen() {
      if (this.product && this.product.id) {
        this.fillForm(this.product)
        getProductById(this.product.id)
          .then(res => {
            if (res.data) {
              this.fillForm(res.data)
            }
          })
          .catch(() => {})
      } else {
        this.fillForm(null)
      }

      this.$nextTick(() => {
        if (this.$refs.form) {
          this.$refs.form.clearValidate()
        }
      })
    },
    fillForm(data) {
      const product = data || {}
      this.formLoading = true
      this.form = {
        id: product.id,
        productCode: product.productCode || '',
        name: product.name || '',
        categoryId: product.categoryId || '',
        price: Number(product.price || 0),
        image: product.image || '',
        images: Array.isArray(product.images) ? product.images.filter(Boolean) : [],
        detailImages: Array.isArray(product.detailImages)
          ? product.detailImages.filter(Boolean)
          : [],
        description: product.description || '',
        status: product.status === undefined ? 0 : product.status
      }

      const attrs = Array.isArray(product.attrs) ? product.attrs : []
      this.specGroups = attrs
        .map(attr => ({
          key: nextSpecKey(),
          name: attr.name || '',
          values: parseAttrValues(attr.attrValues),
          inputValue: '',
          composing: false
        }))
        .filter(group => group.name || group.values.length)

      const skus = Array.isArray(product.skuList) ? product.skuList : []
      this.skuList = skus.map(sku => ({
        id: sku.id,
        skuCode: sku.skuCode || '',
        attrs: parseAttrs(sku.attrs),
        price: Number(sku.price || 0),
        stock: Number(sku.stock || 0),
        image: sku.image || '',
        status: sku.status === undefined ? 1 : sku.status
      }))

      // 兼容只有SKU、没有规格表的旧数据：从SKU属性反推规格组
      if (!this.specGroups.length && this.skuList.length) {
        this.specGroups = this.deriveSpecGroups(this.skuList)
      }

      if (!this.skuList.length) {
        this.skuList = [this.createDefaultSku()]
      }

      // 记录当前规格，后续只有用户修改规格时才增量增删SKU
      this.specSnapshot = this.buildSpecSnapshot(this.activeSpecGroups())
      this.$nextTick(() => {
        this.formLoading = false
      })
    },
    createDefaultSku() {
      return {
        id: undefined,
        skuCode: '',
        attrs: {},
        price: Number(this.form.price || 0),
        stock: 0,
        image: '',
        status: 1
      }
    },
    deriveSpecGroups(skuList) {
      const groups = []
      skuList.forEach(sku => {
        const attrs = parseAttrs(sku.attrs)
        Object.keys(attrs).forEach(name => {
          let group = groups.find(item => item.name === name)
          if (!group) {
            group = { key: nextSpecKey(), name, values: [], inputValue: '', composing: false }
            groups.push(group)
          }
          const value = attrs[name]
          if (value !== undefined && value !== null && group.values.indexOf(value) === -1) {
            group.values.push(value)
          }
        })
      })
      return groups
    },
    addSpecGroup() {
      this.specGroups.push({
        key: nextSpecKey(),
        name: '',
        values: [],
        inputValue: '',
        composing: false
      })
    },
    removeSpecGroup(index) {
      this.specGroups.splice(index, 1)
    },
    removeSku(index) {
      const groups = this.activeSpecGroups()
      if (!groups.length && this.skuList.length === 1) {
        this.$message.warning('无规格商品至少保留一个 SKU')
        return
      }
      this.skuList.splice(index, 1)
      // 删除后规格值若已没有对应SKU，同步从规格中移除，避免再次修改规格时被重新生成
      if (groups.length) {
        this.pruneUnusedSpecValues(groups)
      }
    },
    activeSpecGroups() {
      return this.specGroups.filter(
        group =>
          (group.name && group.name.trim()) ||
          (Array.isArray(group.values) && group.values.length)
      )
    },
    buildSpecSnapshot(groups) {
      return groups.map(group => ({
        key: group.key,
        name: (group.name || '').trim(),
        values: Array.isArray(group.values) ? group.values.slice() : []
      }))
    },
    addSpecValue(index, event) {
      const group = this.specGroups[index]
      if (!group) {
        return
      }
      // 中文输入法确认候选词时的回车不当作添加规格值
      if (event && (event.isComposing || event.keyCode === 229)) {
        return
      }
      if (group.composing) {
        return
      }
      const raw = (group.inputValue || '').trim()
      if (!raw) {
        return
      }
      raw
        .split(/[,，、\n]/)
        .map(value => value.trim())
        .filter(Boolean)
        .forEach(value => {
          if (group.values.indexOf(value) === -1) {
            group.values.push(value)
          }
        })
      group.inputValue = ''
    },
    removeSpecValue(index, value) {
      const group = this.specGroups[index]
      if (!group) {
        return
      }
      group.values = group.values.filter(item => item !== value)
    },
    pruneUnusedSpecValues(groups) {
      groups.forEach(group => {
        const name = (group.name || '').trim()
        if (!name) {
          return
        }
        const used = {}
        this.skuList.forEach(sku => {
          const attrs = parseAttrs(sku.attrs)
          if (attrs[name] !== undefined && attrs[name] !== null) {
            used[attrs[name]] = true
          }
        })
        const nextValues = group.values.filter(value => used[value])
        if (nextValues.length !== group.values.length) {
          group.values = nextValues
        }
      })
    },
    syncSkus() {
      const groups = this.activeSpecGroups()

      // 有规格尚未填写完整时先不刷新，避免误清空已维护的价格库存
      const incomplete = groups.some(
        group =>
          !group.name ||
          !group.name.trim() ||
          !Array.isArray(group.values) ||
          !group.values.length
      )
      this.specIncomplete = incomplete
      if (incomplete) {
        return
      }

      if (!groups.length) {
        const hasDefaultSku =
          this.skuList.length === 1 &&
          Object.keys(parseAttrs(this.skuList[0].attrs)).length === 0
        if (!hasDefaultSku) {
          this.skuList = [this.createDefaultSku()]
        }
        this.specSnapshot = []
        return
      }

      const normalizedGroups = groups.map(group => ({
        key: group.key,
        name: group.name.trim(),
        values: group.values.slice()
      }))
      const previous = this.specSnapshot

      // 规格名修改时，同步调整已有SKU的属性键，保留编号与价格库存
      this.renameAttrsInSkus(normalizedGroups, previous)

      const structural =
        previous.length !== normalizedGroups.length ||
        normalizedGroups.some((group, index) => {
          const prev = previous[index]
          return !prev || prev.key !== group.key || prev.name !== group.name
        })

      if (structural) {
        // 增删规格组/修改规格名：按当前规格重新组合SKU
        this.rebuildSkus(normalizedGroups)
      } else {
        // 只增删规格值：只增删受影响的SKU，已删除的SKU不会重新生成
        this.applySpecValueChanges(normalizedGroups, previous)
      }

      this.specSnapshot = normalizedGroups
    },
    renameAttrsInSkus(groups, previous) {
      if (!previous.length) {
        return
      }
      groups.forEach(group => {
        const prev = previous.find(item => item.key === group.key)
        if (!prev || !prev.name || prev.name === group.name) {
          return
        }
        this.skuList.forEach(sku => {
          const attrs = parseAttrs(sku.attrs)
          if (!Object.prototype.hasOwnProperty.call(attrs, prev.name)) {
            return
          }
          const nextAttrs = {}
          Object.keys(attrs).forEach(name => {
            nextAttrs[name === prev.name ? group.name : name] = attrs[name]
          })
          sku.attrs = nextAttrs
        })
      })
    },
    rebuildSkus(groups) {
      const existingBySignature = {}
      this.skuList.forEach(sku => {
        const attrs = parseAttrs(sku.attrs)
        if (!Object.keys(attrs).length) {
          return
        }
        existingBySignature[this.comboSignature(attrs, groups)] = sku
      })

      let combos = [[]]
      groups.forEach(group => {
        const next = []
        combos.forEach(prefix => {
          group.values.forEach(value => {
            next.push(prefix.concat(value))
          })
        })
        combos = next
      })

      this.skuList = combos.map(values => {
        const attrs = {}
        groups.forEach((group, index) => {
          attrs[group.name] = values[index]
        })
        const existing = existingBySignature[values.join('|~|')]
        if (existing) {
          return { ...existing, attrs }
        }
        return this.createSku(attrs)
      })
    },
    applySpecValueChanges(groups, previous) {
      const removedValues = groups.map((group, index) => {
        const prevValues = previous[index] ? previous[index].values : []
        return prevValues.filter(value => group.values.indexOf(value) === -1)
      })

      // 删除规格值：移除包含该值的SKU
      if (removedValues.some(values => values.length)) {
        this.skuList = this.skuList.filter(sku => {
          const attrs = parseAttrs(sku.attrs)
          return !groups.some(
            (group, index) => removedValues[index].indexOf(attrs[group.name]) > -1
          )
        })
      }

      // 新增规格值：为新增值与其余规格值的组合补充SKU
      const addedValues = groups.map((group, index) => {
        const prevValues = previous[index] ? previous[index].values : []
        return group.values.filter(value => prevValues.indexOf(value) === -1)
      })
      const existingSignatures = {}
      this.skuList.forEach(sku => {
        existingSignatures[this.comboSignature(parseAttrs(sku.attrs), groups)] = true
      })

      const additions = []
      groups.forEach((group, groupIndex) => {
        addedValues[groupIndex].forEach(value => {
          let combos = [{ [group.name]: value }]
          groups.forEach((other, otherIndex) => {
            if (otherIndex === groupIndex) {
              return
            }
            const next = []
            combos.forEach(combo => {
              other.values.forEach(otherValue => {
                next.push({ ...combo, [other.name]: otherValue })
              })
            })
            combos = next
          })
          combos.forEach(combo => {
            const attrs = {}
            groups.forEach(item => {
              attrs[item.name] = combo[item.name]
            })
            const signature = this.comboSignature(attrs, groups)
            if (existingSignatures[signature]) {
              return
            }
            existingSignatures[signature] = true
            additions.push(this.createSku(attrs))
          })
        })
      })

      if (additions.length) {
        this.skuList = this.skuList.concat(additions)
      }
    },
    createSku(attrs) {
      return {
        id: undefined,
        skuCode: '',
        attrs,
        price: Number(this.form.price || 0),
        stock: 0,
        image: '',
        status: 1
      }
    },
    comboSignature(attrs, groups) {
      const parsed = parseAttrs(attrs)
      return groups.map(group => parsed[group.name] || '').join('|~|')
    },
    validateSpecs() {
      const groups = this.activeSpecGroups()
      if (!groups.length) {
        return true
      }
      if (
        groups.some(
          group =>
            !group.name ||
            !group.name.trim() ||
            !Array.isArray(group.values) ||
            !group.values.length
        )
      ) {
        this.$message.warning('请补全规格名和规格值')
        return false
      }
      const names = groups.map(group => group.name.trim())
      if (new Set(names).size !== names.length) {
        this.$message.warning('规格名不能重复')
        return false
      }
      return true
    },
    handleClosed() {
      if (this.$refs.form) {
        this.$refs.form.clearValidate()
      }
    },
    formatAttrs(attrs) {
      const value = parseAttrs(attrs)
      const entries = Object.entries(value)
      if (!entries.length) {
        return '默认规格'
      }
      return entries.map(([key, item]) => `${key}:${item}`).join('，')
    },
    displaySkuCode(row, index) {
      const productCode = (this.form.productCode || '').trim()
      if (!productCode) {
        return row.skuCode || '保存后生成'
      }
      // 与后端保持一致：商品编号-序号，如 5090-1、5090-2
      return `${productCode}-${index + 1}`
    },
    submit() {
      this.$refs.form.validate(valid => {
        if (!valid) {
          return
        }
        if (!this.form.image) {
          this.$message.warning('请上传商品封面图片')
          return
        }
        if (!this.validateSpecs()) {
          return
        }
        if (!this.skuList.length) {
          this.$message.warning('请至少保留一个 SKU')
          return
        }
        const invalidSku = this.skuList.some(
          sku => Number(sku.price) <= 0 || Number(sku.stock) < 0
        )
        if (invalidSku) {
          this.$message.warning('SKU 价格必须大于 0，库存不能为负数')
          return
        }

        this.submitting = true
        const groups = this.specGroups.filter(
          group => group.name && group.name.trim() && group.values.length
        )
        const payload = {
          id: this.form.id,
          productCode: this.form.productCode.trim(),
          name: this.form.name.trim(),
          categoryId: this.form.categoryId,
          price: Number(this.form.price),
          stock: this.skuList.reduce(
            (sum, sku) => sum + (Number(sku.stock) || 0),
            0
          ),
          image: this.form.image,
          images: this.form.images.filter(Boolean),
          detailImages: this.form.detailImages.filter(Boolean),
          description: this.form.description,
          status: this.form.status,
          attrs: groups.map(group => ({
            name: group.name.trim(),
            attrValues: group.values
          })),
          skuList: this.skuList.map(sku => ({
            id: sku.id,
            price: Number(sku.price),
            stock: Number(sku.stock) || 0,
            status: sku.status === undefined ? 1 : sku.status,
            image: sku.image || null,
            attrs: parseAttrs(sku.attrs)
          }))
        }

        const action = this.form.id
          ? updateProduct(payload)
          : addProduct(payload)

        action
          .then(() => {
            this.$message.success(this.form.id ? '商品修改成功' : '商品新增成功')
            this.dialogVisible = false
            this.$emit('success')
          })
          .catch(() => {})
          .finally(() => {
            this.submitting = false
          })
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.field-tip {
  margin-left: 8px;
  color: $text-secondary;
  font-size: 12px;
}

.spec-editor {
  width: 100%;
}

.spec-group {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 8px;
}

.spec-field {
  display: flex;
  flex-shrink: 0;
  align-items: flex-start;
  gap: 6px;
}

.spec-field-values {
  flex: 1;
  min-width: 0;
}

.spec-field-label {
  flex-shrink: 0;
  color: $text-secondary;
  font-size: 12px;
  line-height: 32px;
}

.spec-name {
  width: 130px;
  flex-shrink: 0;
}

.spec-group > .danger-text {
  margin-top: 6px;
}

.spec-values {
  display: flex;
  flex: 1;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.spec-value-tag {
  margin: 0;
}

.spec-value-input {
  width: 220px;
}

.spec-actions {
  display: flex;
  align-items: center;
}

.spec-warning {
  margin-top: 6px;
  color: $color-danger;
  font-size: 12px;
}

.spec-summary {
  margin-top: 6px;
  color: $text-secondary;
  font-size: 12px;
}

.sku-table {
  width: 100%;
}

.placeholder-text {
  color: $text-secondary;
}

.danger-text {
  color: $color-danger;
}
</style>
