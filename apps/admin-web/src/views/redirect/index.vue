<template>
  <div />
</template>

<script>
export default {
  name: 'RedirectPage',
  beforeRouteEnter(to, from, next) {
    next(vm => {
      const path = (to.params.path || 'dashboard').replace(/^\/+/, '')
      vm.$router.replace({
        path: `/${path}`,
        query: to.query
      })
    })
  }
}
</script>
