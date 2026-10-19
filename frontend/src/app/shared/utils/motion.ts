// how the page buttons scroll (chapters, sections, back to top): smooth, unless the visitor asked for less motion
export function scrollBehavior(): ScrollBehavior {
  return matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth';
}
