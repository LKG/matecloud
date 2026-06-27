import { defineConfig, presetWind, presetIcons } from 'unocss'

export default defineConfig({
  presets: [
    presetWind(),
    presetIcons({ scale: 1.2 }),
  ],
  theme: {
    colors: {
      primary: '#409eff',
    },
  },
})
