<template>
  <v-tooltip bottom>
    <v-btn
      small
      color="white"
      flat
      slot="activator"
      @click.stop="$global.openRoute(item.route)">
      <!--<v-icon>{{ item.icon }}</v-icon>-->
      {{ item.text }}
    </v-btn>
    <span>{{ item.text }}</span>
  </v-tooltip>
</template>

<script>
  export default {
    name: 'ToolbarNavigationItem',
    props: ['item'],
  };
</script>

<style scoped>

</style>
