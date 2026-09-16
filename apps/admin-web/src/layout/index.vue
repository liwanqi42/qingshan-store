<template>
  <div class="app-wrapper">
    <sidebar :collapse="sidebarCollapsed" />
    <div class="main-container">
      <navbar
        :collapsed="sidebarCollapsed"
        @toggle-sidebar="toggleSidebar"
      />
      <tags-view />
      <app-main />
    </div>
  </div>
</template>

<script>
import { mapState, mapMutations } from 'vuex'
import Sidebar from './components/Sidebar/index.vue'
import Navbar from './components/Navbar/index.vue'
import TagsView from './components/TagsView/index.vue'
import AppMain from './components/AppMain.vue'

export default {
  name: 'AppLayout',
  components: {
    Sidebar,
    Navbar,
    TagsView,
    AppMain
  },
  computed: {
    ...mapState('app', ['sidebarCollapsed'])
  },
  methods: {
    ...mapMutations('app', ['TOGGLE_SIDEBAR']),
    toggleSidebar() {
      this.TOGGLE_SIDEBAR()
    }
  }
}
</script>

<style lang="scss" scoped>
.app-wrapper {
  display: flex;
  width: 100%;
  height: 100%;
  overflow: hidden;
}

.main-container {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
  height: 100%;
  background-color: $bg-page;
  overflow: hidden;
  transition: width $transition-base;
}
</style>
