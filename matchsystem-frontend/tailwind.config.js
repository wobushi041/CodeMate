/** @type {import('tailwindcss').Config} */
module.exports = {
  // 只扫描用到的工具类(vite 2.9 + tailwind 3.4:JIT 按需生成)
  content: ['./index.html', './src/**/*.{vue,js,ts,jsx,tsx}'],
  theme: {
    extend: {},
  },
  plugins: [],
  // 关闭 Preflight,避免 reset 干扰 Vant 组件原有样式
  corePlugins: {
    preflight: false,
  },
}