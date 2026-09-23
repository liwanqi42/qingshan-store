<template>
  <div v-loading="loading" class="image-upload">
    <el-upload
      class="uploader"
      action="#"
      :show-file-list="false"
      :http-request="handleUpload"
      :before-upload="beforeUpload"
      :disabled="disabled || loading"
      accept="image/*"
    >
      <div v-if="value" class="image-preview">
        <img :src="value" alt="preview" />
      </div>
      <div v-else class="upload-placeholder">
        <i class="el-icon-plus" />
        <span>上传图片</span>
      </div>
    </el-upload>
    <el-button
      v-if="value && !disabled"
      type="text"
      size="mini"
      class="remove-btn"
      @click.stop="remove"
    >
      删除图片
    </el-button>
  </div>
</template>

<script>
import { uploadFile } from '@/api/common'

export default {
  name: 'ImageUpload',
  props: {
    value: {
      type: String,
      default: ''
    },
    type: {
      type: Number,
      default: 1
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
      return true
    },
    handleUpload({ file }) {
      this.loading = true
      return uploadFile(file, this.type)
        .then(res => {
          if (res.data) {
            this.$emit('input', res.data)
          }
        })
        .catch(() => {})
        .finally(() => {
          this.loading = false
        })
    },
    remove() {
      this.$emit('input', '')
    }
  }
}
</script>

<style lang="scss" scoped>
.image-upload {
  display: inline-flex;
  flex-direction: column;
  align-items: flex-start;
}

.uploader {
  ::v-deep .el-upload {
    display: block;
  }
}

.image-preview,
.upload-placeholder {
  width: 120px;
  height: 120px;
  border: 1px dashed $border-color;
  border-radius: $border-radius-base;
  cursor: pointer;
  transition: border-color 0.2s;

  &:hover {
    border-color: $color-primary;
  }
}

.image-preview {
  overflow: hidden;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: $text-secondary;

  i {
    margin-bottom: 8px;
    font-size: 24px;
  }

  span {
    font-size: 12px;
  }
}

.remove-btn {
  margin-top: 4px;
  padding: 0;
  color: $color-danger;
}
</style>
