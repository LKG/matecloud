import { MateMessage } from '@matecloud/ui'
import { reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { materialApi, type MaterialCategoryList, type MaterialItem, type MaterialType } from '@matecloud/core'

/**
 * 素材管理数据与操作的组合式封装:分组 / 列表 / 分页 / 增删改。
 * 视图组件只消费它,保持表现与逻辑分离。
 */
export function useMaterial() {
  const { t } = useI18n()
  const type = ref<MaterialType>('image')
  // '' = 全部,'0' = 未分组,其余 = 分组 id
  const categoryId = ref<string>('')
  const keyword = ref('')
  const loading = ref(false)
  const items = ref<MaterialItem[]>([])
  const cat = reactive<MaterialCategoryList>({ total: 0, ungrouped: 0, categories: [] })
  const pager = reactive({ pageNum: 1, pageSize: 24, total: 0 })

  async function loadCategories() {
    const { data } = await materialApi.categories(type.value)
    cat.total = data.total
    cat.ungrouped = data.ungrouped
    cat.categories = data.categories
  }

  async function loadList() {
    loading.value = true
    try {
      const { data } = await materialApi.page({
        type: type.value,
        categoryId: categoryId.value || undefined,
        keyword: keyword.value || undefined,
        pageNum: pager.pageNum,
        pageSize: pager.pageSize,
      })
      items.value = data.list || []
      pager.total = Number(data.total) || 0
    } finally {
      loading.value = false
    }
  }

  async function refresh() {
    await Promise.all([loadCategories(), loadList()])
  }

  async function switchType(t: MaterialType) {
    type.value = t
    categoryId.value = ''
    keyword.value = ''
    pager.pageNum = 1
    await refresh()
  }

  async function selectCategory(id: string) {
    categoryId.value = id
    pager.pageNum = 1
    await loadList()
  }

  async function search() {
    pager.pageNum = 1
    await loadList()
  }

  async function changePage(p: number) {
    pager.pageNum = p
    await loadList()
  }

  async function createCategory(name: string) {
    await materialApi.createCategory(type.value, name)
    MateMessage.success(t('material.created'))
    await loadCategories()
  }

  async function renameCategory(id: string, name: string) {
    await materialApi.renameCategory(id, name)
    await loadCategories()
  }

  async function deleteCategory(id: string) {
    await materialApi.deleteCategory(id)
    if (categoryId.value === id) categoryId.value = ''
    MateMessage.success(t('material.deletedCategory'))
    await refresh()
  }

  async function renameItem(id: string, name: string) {
    await materialApi.rename(id, name)
    await loadList()
  }

  async function moveItem(id: string, cid: string | null) {
    await materialApi.move(id, cid)
    await refresh()
  }

  async function deleteItem(id: string) {
    await materialApi.delete(id)
    MateMessage.success(t('material.deleted'))
    await refresh()
  }

  async function copyUrl(id: string) {
    const { data } = await materialApi.url(id)
    try {
      await navigator.clipboard.writeText(data.url)
      MateMessage.success(t('material.linkCopied'))
    } catch {
      window.prompt(t('material.actCopy'), data.url)
    }
  }

  return {
    type, categoryId, keyword, loading, items, cat, pager,
    loadCategories, loadList, refresh, switchType, selectCategory, search, changePage,
    createCategory, renameCategory, deleteCategory,
    renameItem, moveItem, deleteItem, copyUrl,
  }
}
