<template>
  <div class="app-container">
    <el-alert
      title="字典配置已保存到服务端，所有管理员共享同一份数据；内置字典可停用但不可删除，可随时恢复默认。"
      type="info"
      :closable="false"
      show-icon
      class="mb-20"
    />

    <div class="dict-layout">
      <el-card shadow="never" class="dict-type-card">
        <div class="card-header">
          <span class="card-title">字典类型</span>
          <div class="card-actions">
            <el-button size="mini" icon="el-icon-refresh-left" @click="handleReset">
              恢复默认
            </el-button>
            <el-button size="mini" type="primary" icon="el-icon-plus" @click="handleAddType">
              新增
            </el-button>
          </div>
        </div>

        <div class="dict-type-list">
          <div
            v-for="item in dictTypes"
            :key="item.type"
            class="dict-type-item"
            :class="{ active: item.type === selectedType }"
            @click="selectType(item.type)"
          >
            <div class="dict-type-main">
              <div class="dict-type-name">
                {{ item.name }}
                <el-tag
                  :type="item.status === 1 ? 'success' : 'info'"
                  size="mini"
                  class="type-status"
                >
                  {{ item.status === 1 ? '启用' : '停用' }}
                </el-tag>
              </div>
              <div class="dict-type-code">{{ item.type }}</div>
            </div>
            <div class="dict-type-meta">{{ item.count }} 项</div>
            <div class="dict-type-actions">
              <el-button type="text" size="mini" @click.stop="handleEditType(item)">
                编辑
              </el-button>
              <el-button
                type="text"
                size="mini"
                class="danger-text"
                @click.stop="handleDeleteType(item)"
              >
                删除
              </el-button>
            </div>
          </div>
          <div v-if="!dictTypes.length" class="empty-tip">暂无字典类型</div>
        </div>
      </el-card>

      <el-card shadow="never" class="dict-data-card">
        <div class="card-header">
          <span class="card-title">
            字典数据{{ currentDict ? ` - ${currentDict.name}` : '' }}
          </span>
          <el-button
            size="mini"
            type="primary"
            icon="el-icon-plus"
            :disabled="!selectedType"
            @click="handleAddItem"
          >
            新增字典项
          </el-button>
        </div>

        <el-table :data="currentItems" border stripe empty-text="暂无字典数据">
          <el-table-column prop="label" label="字典标签" min-width="120" />
          <el-table-column prop="value" label="字典值" min-width="100" />
          <el-table-column label="标签样式" width="120">
            <template slot-scope="{ row }">
              <el-tag :type="row.tagType || undefined" size="mini">
                {{ row.label }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="sort" label="排序" width="80" />
          <el-table-column label="状态" width="90">
            <template slot-scope="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'" size="mini">
                {{ row.status === 1 ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template slot-scope="{ row }">
              <el-button type="text" size="small" @click="handleEditItem(row)">
                编辑
              </el-button>
              <el-button
                type="text"
                size="small"
                class="danger-text"
                @click="handleDeleteItem(row)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>

    <el-dialog
      :title="isEditType ? '编辑字典类型' : '新增字典类型'"
      :visible.sync="typeVisible"
      width="460px"
      append-to-body
    >
      <el-form
        ref="typeForm"
        :model="typeForm"
        :rules="typeRules"
        label-width="90px"
      >
        <el-form-item label="字典类型" prop="type">
          <el-input
            v-model="typeForm.type"
            :disabled="isEditType"
            placeholder="如 employee_sex"
          />
        </el-form-item>
        <el-form-item label="字典名称" prop="name">
          <el-input v-model="typeForm.name" placeholder="请输入字典名称" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="typeForm.sort" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="typeForm.status" />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="typeVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitType">保存</el-button>
      </span>
    </el-dialog>

    <el-dialog
      :title="isEditItem ? '编辑字典项' : '新增字典项'"
      :visible.sync="itemVisible"
      width="460px"
      append-to-body
    >
      <el-form
        ref="itemForm"
        :model="itemForm"
        :rules="itemRules"
        label-width="90px"
      >
        <el-form-item label="字典标签" prop="label">
          <el-input v-model="itemForm.label" placeholder="请输入字典标签" />
        </el-form-item>
        <el-form-item label="字典值" prop="value">
          <el-input
            v-model="itemForm.value"
            :disabled="isEditItem"
            placeholder="请输入字典值"
          />
        </el-form-item>
        <el-form-item label="标签样式">
          <el-select v-model="itemForm.tagType" style="width: 100%">
            <el-option
              v-for="option in tagTypeOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="itemForm.sort" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="itemForm.status" />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="itemVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitItem">保存</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { mapGetters, mapState } from 'vuex'

export default {
  name: 'SystemDictView',
  data() {
    const validateTypeCode = (rule, value, callback) => {
      if (!value) {
        callback(new Error('请输入字典类型'))
        return
      }
      if (!/^[a-z][a-z0-9_]*$/.test(value)) {
        callback(new Error('类型需以小写字母开头，只能包含小写字母、数字和下划线'))
        return
      }
      if (
        !this.isEditType &&
        this.dictTypes.some(item => item.type === value)
      ) {
        callback(new Error('字典类型已存在'))
        return
      }
      callback()
    }

    const validateItemValue = (rule, value, callback) => {
      if (!value) {
        callback(new Error('请输入字典值'))
        return
      }
      const duplicate = this.currentItems.some(
        item =>
          String(item.value) === String(value) &&
          String(item.value) !== String(this.itemFormOriginalValue)
      )
      if (duplicate) {
        callback(new Error('字典值已存在'))
        return
      }
      callback()
    }

    return {
      selectedType: '',
      saving: false,
      typeVisible: false,
      typeForm: {
        id: null,
        type: '',
        name: '',
        sort: 0,
        status: true
      },
      typeRules: {
        type: [{ validator: validateTypeCode, trigger: 'blur' }],
        name: [{ required: true, message: '请输入字典名称', trigger: 'blur' }]
      },
      itemVisible: false,
      itemForm: {
        id: null,
        label: '',
        value: '',
        tagType: '',
        sort: 0,
        status: true
      },
      itemFormOriginalValue: undefined,
      itemRules: {
        label: [{ required: true, message: '请输入字典标签', trigger: 'blur' }],
        value: [{ validator: validateItemValue, trigger: 'blur' }]
      },
      tagTypeOptions: [
        { label: '默认', value: '' },
        { label: '成功', value: 'success' },
        { label: '信息', value: 'info' },
        { label: '警告', value: 'warning' },
        { label: '危险', value: 'danger' }
      ]
    }
  },
  computed: {
    ...mapState('dict', ['dicts']),
    ...mapGetters('dict', ['dictTypes']),
    currentDict() {
      return this.selectedType ? this.dicts[this.selectedType] || null : null
    },
    currentItems() {
      if (!this.currentDict || !Array.isArray(this.currentDict.items)) {
        return []
      }
      return [...this.currentDict.items].sort(
        (a, b) => (Number(a.sort) || 0) - (Number(b.sort) || 0)
      )
    },
    isEditType() {
      return Boolean(this.typeForm.id)
    },
    isEditItem() {
      return Boolean(this.itemForm.id)
    }
  },
  watch: {
    dictTypes() {
      if (!this.selectedType && this.dictTypes.length) {
        this.selectedType = this.dictTypes[0].type
      }
      if (
        this.selectedType &&
        !this.dictTypes.some(item => item.type === this.selectedType)
      ) {
        this.selectedType = this.dictTypes.length
          ? this.dictTypes[0].type
          : ''
      }
    }
  },
  mounted() {
    this.selectFirstType()
  },
  methods: {
    selectFirstType() {
      if (!this.selectedType && this.dictTypes.length) {
        this.selectedType = this.dictTypes[0].type
      }
    },
    selectType(type) {
      this.selectedType = type
    },
    nextTypeSort() {
      const max = this.dictTypes.reduce(
        (result, item) => Math.max(result, Number(item.sort) || 0),
        0
      )
      return max + 10
    },
    nextItemSort() {
      const max = this.currentItems.reduce(
        (result, item) => Math.max(result, Number(item.sort) || 0),
        0
      )
      return max + 1
    },
    handleAddType() {
      this.typeForm = {
        id: null,
        type: '',
        name: '',
        sort: this.nextTypeSort(),
        status: true
      }
      this.typeVisible = true
      this.$nextTick(() => {
        if (this.$refs.typeForm) {
          this.$refs.typeForm.clearValidate()
        }
      })
    },
    handleEditType(item) {
      this.typeForm = {
        id: item.id,
        type: item.type,
        name: item.name,
        sort: Number(item.sort) || 0,
        status: item.status === 1
      }
      this.typeVisible = true
      this.$nextTick(() => {
        if (this.$refs.typeForm) {
          this.$refs.typeForm.clearValidate()
        }
      })
    },
    submitType() {
      this.$refs.typeForm.validate(async valid => {
        if (!valid || this.saving) {
          return
        }
        const isEdit = this.isEditType
        this.saving = true
        try {
          await this.$store.dispatch('dict/saveDictType', {
            id: this.typeForm.id,
            type: this.typeForm.type.trim(),
            name: this.typeForm.name.trim(),
            sort: Number(this.typeForm.sort) || 0,
            status: this.typeForm.status ? 1 : 0
          })
          if (!isEdit) {
            this.selectedType = this.typeForm.type.trim()
          }
          this.typeVisible = false
          this.$message.success(isEdit ? '字典类型修改成功' : '字典类型新增成功')
        } catch (e) {
          // 请求层已提示错误
        } finally {
          this.saving = false
        }
      })
    },
    handleDeleteType(item) {
      this.$confirm(`确定删除字典类型“${item.name}”吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
        .then(async () => {
          try {
            await this.$store.dispatch('dict/deleteDictType', item.id)
            if (this.selectedType === item.type) {
              this.selectedType = ''
              this.$nextTick(this.selectFirstType)
            }
            this.$message.success('字典类型已删除')
          } catch (e) {
            // 请求层已提示错误
          }
        })
        .catch(() => {})
    },
    handleAddItem() {
      if (!this.selectedType || !this.currentDict) {
        this.$message.warning('请先选择字典类型')
        return
      }
      this.itemForm = {
        id: null,
        label: '',
        value: '',
        tagType: '',
        sort: this.nextItemSort(),
        status: true
      }
      this.itemFormOriginalValue = undefined
      this.itemVisible = true
      this.$nextTick(() => {
        if (this.$refs.itemForm) {
          this.$refs.itemForm.clearValidate()
        }
      })
    },
    handleEditItem(row) {
      this.itemForm = {
        id: row.id,
        label: row.label,
        value: row.value,
        tagType: row.tagType || '',
        sort: Number(row.sort) || 0,
        status: row.status === 1
      }
      this.itemFormOriginalValue = row.value
      this.itemVisible = true
      this.$nextTick(() => {
        if (this.$refs.itemForm) {
          this.$refs.itemForm.clearValidate()
        }
      })
    },
    submitItem() {
      this.$refs.itemForm.validate(async valid => {
        if (!valid || this.saving || !this.currentDict) {
          return
        }
        const isEdit = this.isEditItem
        this.saving = true
        try {
          await this.$store.dispatch('dict/saveDictItem', {
            id: this.itemForm.id,
            typeId: this.currentDict.id,
            label: this.itemForm.label.trim(),
            value: this.itemForm.value.trim(),
            tagType: this.itemForm.tagType,
            sort: Number(this.itemForm.sort) || 0,
            status: this.itemForm.status ? 1 : 0
          })
          this.itemVisible = false
          this.$message.success(isEdit ? '字典项修改成功' : '字典项新增成功')
        } catch (e) {
          // 请求层已提示错误
        } finally {
          this.saving = false
        }
      })
    },
    handleDeleteItem(row) {
      this.$confirm(`确定删除字典项“${row.label}”吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
        .then(async () => {
          try {
            await this.$store.dispatch('dict/deleteDictItem', row.id)
            this.$message.success('字典项已删除')
          } catch (e) {
            // 请求层已提示错误
          }
        })
        .catch(() => {})
    },
    handleReset() {
      this.$confirm('确定恢复默认字典配置吗？自定义字典和修改将被清除。', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
        .then(async () => {
          try {
            await this.$store.dispatch('dict/resetDicts')
            this.selectedType = ''
            this.$nextTick(this.selectFirstType)
            this.$message.success('已恢复默认字典配置')
          } catch (e) {
            // 请求层已提示错误
          }
        })
        .catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.dict-layout {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}

.dict-type-card {
  flex-shrink: 0;
  width: 360px;
}

.dict-data-card {
  flex: 1;
  min-width: 0;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: $text-primary;
}

.card-actions {
  display: flex;
  gap: 8px;
}

.dict-type-list {
  max-height: calc(100vh - 300px);
  overflow-y: auto;
}

.dict-type-item {
  display: flex;
  align-items: center;
  padding: 12px;
  margin-bottom: 10px;
  border: 1px solid $border-color-light;
  border-radius: $border-radius-base;
  cursor: pointer;
  transition: all 0.2s;

  &:hover,
  &.active {
    border-color: $color-primary;
    background-color: rgba(64, 158, 255, 0.06);
  }
}

.dict-type-main {
  flex: 1;
  min-width: 0;
}

.dict-type-name {
  font-size: 14px;
  font-weight: 500;
  color: $text-primary;
}

.type-status {
  margin-left: 6px;
}

.dict-type-code {
  margin-top: 4px;
  font-size: 12px;
  color: $text-secondary;
}

.dict-type-meta {
  margin: 0 12px;
  font-size: 12px;
  color: $text-secondary;
}

.dict-type-actions {
  display: flex;
  flex-shrink: 0;
}

.empty-tip {
  padding: 30px;
  font-size: 13px;
  color: $text-secondary;
  text-align: center;
}

.danger-text {
  color: $color-danger;
}

@media (max-width: 900px) {
  .dict-layout {
    flex-direction: column;
  }

  .dict-type-card {
    width: 100%;
  }

  .dict-type-list {
    max-height: 300px;
  }
}
</style>
