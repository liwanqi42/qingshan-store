<template>
  <div class="image-upload-list">
    <div
      v-for="(url, index) in value"
      :key="url + '-' + index"
      class="image-item"
      :style="boxStyle"
    >
      <img :src="url" alt="preview" />
      <div class="image-mask">
        <el-button type="text" size="mini" @click="remove(index)">删除</el-button>
      </div>
    </div>
    <el-upload
      v-if="value.length < limit"
      class="uploader"
      action="#"
      :show-file-list="false"
      :http-request="handleUpload"
      :before-upload="beforeUpload"
      :disabled="disabled || loading"
      accept="image/*"
    >
      <div v-loading="loading" class="upload-placeholder" :style="boxStyle">
        <i class="el-icon-plus" />
        <span>上传图片</span>
      </div>
    </el-upload>
    <div class="upload-tip">最多 {{ limit }} 张，单张不超过 5MB</div>
  </div>
</template>

<script>
import { uploadFile } from '@/api/common'

export default {
  name: 'ImageUploadList',
  props: {
    value: {
      type: Array,
      default: () => []
    },
    type: {
      type: Number,
      default: 1
    },
    limit: {
      type: Number,
      default: 9
    },
    size: {
      type: Number,
      default: 88
    },
    disabled: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      loading: false
    }
  },
  computed: {
    boxStyle() {
      return {
        width: this.size + 'px',
        height: this.size + 'px'
      }
    }
  },
  methods: {
    beforeUpload(file) {
      const isImage = file.type && file.type.startsWith('image/')
      if (!isImage) {
        this.$message.error('只能上传图片文件')
        return false
      }
      if (file.size > 5 * 1024 * 1024) {
        this.$message.error('图片大小不能超过 5MB')
        return false
      }
      if (this.value.length >= this.limit) {
        this.$message.warning(`最多上传 ${this.limit} 张图片`)
        return false
      }
      return true
    },
    handleUpload({ file }) {
      this.loading = true
      return uploadFile(file, this.type)
        .then(res => {
          if (res.data) {
            this.$emit('input', [...this.value, res.data])
          }
        })
        .catch(() => {})
        .finally(() => {
          this.loading = false
        })
    },
    remove(index) {
      const next = [...this.value]
      next.splice(index, 1)
      this.$emit('input', next)
    }
  }
}
</script>

<style lang="scss" scoped>
.image-upload-list {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  gap: 10px;
}

.image-item {
  position: relative;
  overflow: hidden;
  border: 1px solid $border-color;
  border-radius: $border-radius-base;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  &:hover .image-mask {
    opacity: 1;
  }
}

.image-mask {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.45);
  opacity: 0;
  transition: opacity 0.2s;

  ::v-deep .el-button {
    color: #fff;
  }
}

.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border: 1px dashed $border-color;
  border-radius: $border-radius-base;
  color: $text-secondary;
  cursor: pointer;
  transition: border-color 0.2s;

  &:hover {
    border-color: $color-primary;
  }

  i {
    margin-bottom: 6px;
    font-size: 22px;
  }

  span {
    font-size: 12px;
  }
}

.upload-tip {
  width: 100%;
  color: $text-secondary;
  font-size: 12px;
  line-height: 1;
}
</style>
