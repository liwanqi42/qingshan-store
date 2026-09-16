<template>
  <section ref="appMain" class="app-main">
    <transition name="fade-transform" mode="out-in">
      <keep-alive :include="cachedViews">
        <router-view :key="$route.path" />
      </keep-alive>
    </transition>
  </section>
</template>

<script>
import { mapState } from 'vuex'

export default {
  name: 'AppMain',
  computed: {
    ...mapState('tagsView', ['cachedViews'])
  },
  watch: {
    $route() {
      this.resetScroll()
    }
  },
  methods: {
    resetScroll() {
      this.$nextTick(() => {
        const appMain = this.$refs.appMain
        if (appMain) {
          appMain.scrollTop = 0
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.app-main {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
}

.fade-transform-enter-active,
.fade-transform-leave-active {
  transition: all 0.2s;
}

.fade-transform-enter {
  opacity: 0;
  transform: translateX(-10px);
}

.fade-transform-leave-to {
  opacity: 0;
  transform: translateX(10px);
}
</style>
