import { cva, type VariantProps } from 'class-variance-authority'

/**
 * MateButton variants (RFC-053 #1 — base atoms + CVA).
 *
 * Variant logic lives here as a type-safe, enumerable function instead of
 * being scattered across template string concatenation or element-override
 * CSS. The returned class names are styled in `../base.css`, which consumes
 * only the `--mc-btn-*` component-semantic tokens (see tokens.css).
 *
 * Passing an unknown `variant`/`size` is a compile-time error.
 */
export const buttonVariants = cva('mc-btn', {
  variants: {
    variant: {
      primary: 'mc-btn--primary',
      secondary: 'mc-btn--secondary',
      ghost: 'mc-btn--ghost',
      danger: 'mc-btn--danger',
    },
    size: {
      sm: 'mc-btn--sm',
      md: 'mc-btn--md',
      lg: 'mc-btn--lg',
    },
    block: {
      true: 'mc-btn--block',
    },
  },
  defaultVariants: {
    variant: 'primary',
    size: 'md',
  },
})

export type ButtonVariantProps = VariantProps<typeof buttonVariants>
