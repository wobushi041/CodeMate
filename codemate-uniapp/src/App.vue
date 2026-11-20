<script setup lang="ts">
import { onLaunch } from '@dcloudio/uni-app'

onLaunch(() => {
  console.info('CodeMate uni-app launched')
})
</script>

<style>
page {
  min-height: 100%;
  color: #242321;
  background: #faf9f6;
  font-family: 'Source Han Sans SC', -apple-system, BlinkMacSystemFont, "PingFang SC", "Microsoft YaHei", sans-serif;
}

view,
text,
button,
input,
textarea,
scroll-view,
picker,
image {
  box-sizing: border-box;
}

button::after {
  border: 0;
}
</style>
