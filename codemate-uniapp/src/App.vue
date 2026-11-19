<script setup lang="ts">
import { onLaunch } from '@dcloudio/uni-app'

onLaunch(() => {
  console.info('CodeMate uni-app launched')
})
</script>

<style>
page {
  min-height: 100%;
  color: #f8fafc;
  background: #0f172a;
  font-family: -apple-system, BlinkMacSystemFont, "PingFang SC", "Microsoft YaHei", sans-serif;
}

view,
text,
button,
input,
textarea,
scroll-view {
  box-sizing: border-box;
}

button::after {
  border: 0;
}
</style>
