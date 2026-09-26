<template>
  <div class="partner-list">
    <slot />
  </div>
</template>

<style scoped>
.partner-list {
  display: grid;
  width: 100%;
  min-width: 0;
  grid-template-columns: minmax(0, 1fr);
  justify-items: center;
  gap: 12px;
}
</style>
