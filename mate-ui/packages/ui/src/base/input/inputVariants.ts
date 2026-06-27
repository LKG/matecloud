import { cva, type VariantProps } from 'class-variance-authority'

/**
 * MateInput variants (RFC-053 #1 — base atoms + CVA).
 * Styled in `../base.css` using `--mc-field-*` tokens.
 * Mirrors 主流对话产品's base/input (size / destructive / disabled).
 */
export const inputVariants = cva('mc-field', {
  variants: {
    size: {
      sm: 'mc-field--sm',
      md: 'mc-field--md',
      lg: 'mc-field--lg',
    },
    destructive: {
      true: 'mc-field--destructive',
    },
    disabled: {
      true: 'mc-field--disabled',
    },
  },
  defaultVariants: {
    size: 'md',
  },
})

export type InputVariantProps = VariantProps<typeof inputVariants>
