export default [
  {
    path: 'admin/admins',
    name: 'AdminList',
    component: () => import('./AdminList.vue'),
    meta: { title: 'menu.admin' },
  },
  {
    path: 'admin/roles',
    name: 'RoleList',
    component: () => import('./RoleList.vue'),
    meta: { title: 'menu.role' },
  },
  {
    path: 'admin/menus',
    name: 'MenuTree',
    component: () => import('./MenuTree.vue'),
    meta: { title: 'menu.menu' },
  },
  {
    path: 'admin/dict',
    name: 'DictManager',
    component: () => import('./DictManager.vue'),
    meta: { title: 'menu.dict' },
  },
]
