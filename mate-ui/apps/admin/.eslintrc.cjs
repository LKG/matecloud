/**
 * Admin app ESLint config — thin wrapper over the shared
 * @matecloud/eslint-config ruleset (mate-ui/tooling/eslint-config).
 *
 * The shared config stays on ESLint 8's legacy .eslintrc format for now
 * because the workspace ships eslint@^8.57.0; migrating to flat config
 * (eslint 9+) is tracked separately.
 */
module.exports = {
  root: true,
  extends: ['@matecloud/eslint-config'],
  // Admin-specific overrides
  rules: {
    // auto-imports.d.ts / components.d.ts are codegen, ignore
    '@typescript-eslint/no-empty-object-type': 'off',
  },
  ignorePatterns: [
    'dist/',
    'node_modules/',
    'auto-imports.d.ts',
    'components.d.ts',
    '*.d.ts',
  ],
}
