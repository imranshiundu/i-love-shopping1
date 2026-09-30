/**
 * Size variants for product imagery: thumbnails (cart lines, tables,
 * hover previews) and cards fetch a genuinely smaller file than the
 * full-size gallery, instead of re-using the 800x600 original everywhere.
 */
export const IMAGE_VARIANTS = {
  thumb: { w: 240, h: 180 },
  card: { w: 400, h: 500 },
} as const

/**
 * Returns a size-appropriate variant of a product image URL.
 * Picsum-style URLs embed the pixel dimensions in the path
 * (`/seed/x/800/600`), so swapping the trailing dimensions lets every view
 * request a smaller file. URLs without a trailing `/NNN/NNN` pattern
 * (e.g. raw.githubusercontent.com) are returned unchanged.
 */
export function sizedImageUrl(
  url: string | null | undefined,
  variant: keyof typeof IMAGE_VARIANTS,
): string | undefined {
  if (!url) return undefined
  const { w, h } = IMAGE_VARIANTS[variant]
  return url.replace(/\/(\d{3,4})\/(\d{3,4})$/, `/${w}/${h}`)
}
