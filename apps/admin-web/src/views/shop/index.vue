<template>
  <div class="app-container">
    <el-card v-loading="loading" shadow="never">
      <div class="shop-header">
        <div>
          <h3>店铺营业状态</h3>
          <p>控制用户端是否可以正常下单</p>
        </div>
        <el-tag :type="status === 1 ? 'success' : 'info'" size="medium">
          {{ status === 1 ? '营业中' : '打烊中' }}
        </el-tag>
      </div>

      <div class="status-options">
        <div
          class="status-option"
          :class="{ active: status === 1 }"
          @click="handleChange(1)"
        >
          <i class="el-icon-sunny" />
          <span class="option-title">营业中</span>
          <p>顾客可以正常浏览和下单</p>
        </div>
        <div
          class="status-option"
          :class="{ active: status === 0 }"
          @click="handleChange(0)"
        >
          <i class="el-icon-moon" />
          <span class="option-title">打烊中</span>
          <p>顾客仅可浏览，不能下单</p>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script>
import { getShopStatus, setShopStatus } from '@/api/shop'

export default {
  name: 'ShopView',
  data() {
    return {
      status: 0,
      loading: false,
      changing: false
    }
  },
  created() {
    this.fetchStatus()
  },
  methods: {
    fetchStatus() {
      this.loading = true
      getShopStatus()
        .then(res => {
          this.status = Number(res.data || 0)
        })
        .catch(() => {})
        .finally(() => {
          this.loading = false
        })
    },
    handleChange(status) {
      if (this.changing || status === this.status) {
        return
      }
      this.$confirm(
        status === 1 ? '确定将店铺设为营业中吗？' : '确定将店铺设为打烊中吗？',
        '提示',
        { type: 'warning' }
      )
        .then(() => {
          this.changing = true
          setShopStatus(status)
            .then(() => {
              this.status = status
              this.$message.success(
                status === 1 ? '店铺已开始营业' : '店铺已打烊'
              )
            })
            .catch(() => {})
            .finally(() => {
              this.changing = false
            })
        })
        .catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.shop-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;

  h3 {
    margin: 0 0 6px;
    font-size: 18px;
    color: $text-primary;
  }

  p {
    margin: 0;
    font-size: 13px;
    color: $text-secondary;
  }
}

.status-options {
  display: flex;
  gap: 16px;
}

.status-option {
  flex: 1;
  padding: 24px;
  text-align: center;
  border: 1px solid $border-color-light;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover,
  &.active {
    border-color: $color-primary;
    background-color: rgba(64, 158, 255, 0.06);
  }

  i {
    font-size: 34px;
    color: $color-primary;
  }

  .option-title {
    display: block;
    margin-top: 12px;
    font-size: 16px;
    font-weight: 600;
    color: $text-primary;
  }

  p {
    margin: 8px 0 0;
    font-size: 13px;
    color: $text-secondary;
  }
}

@media (max-width: 700px) {
  .status-options {
    flex-direction: column;
  }
}
</style>
