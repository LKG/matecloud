import { cva, type VariantProps } from 'class-variance-authority'

/**
 * MateTag variants (RFC-053 #1 — base atoms + CVA).
 * Styled in `../base.css` using `--mc-tag-*` + status tokens.
 */
export const tagVariants = cva('mc-tag', {
  variants: {
    variant: {
      default: 'mc-tag--default',
      primary: 'mc-tag--primary',
      success: 'mc-tag--success',
      warning: 'mc-tag--warning',
      danger: 'mc-tag--danger',
      info: 'mc-tag--info',
    },
    size: {
      sm: 'mc-tag--sm',
      md: 'mc-tag--md',
    },
  },
  defaultVariants: {
    variant: 'default',
    size: 'md',
  },
})

export type TagVariantProps = VariantProps<typeof tagVariants>
