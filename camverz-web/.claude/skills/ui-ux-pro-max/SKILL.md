---
name: ui-ux-pro-max
description: "Use when designing or refining Camverz web pages, components, responsive layouts, visual systems, or Motion interactions. Apply these standards for production UI/UX work."
---

# Camverz UI/UX Pro Max

Design Camverz as a focused social video-networking product, not a generic landing page.

## Visual direction

- Use the graphite, coral, and aqua system already defined in `app/globals.css`.
- Keep surfaces dark, layered, and calm. Use accent color to communicate action, trust, verification, and status.
- Avoid purple-dominant gradients, excessive glow, decorative blobs, and emoji as primary interface controls.
- Use Lucide icons for controls and icon-plus-label buttons for explicit actions.
- Keep repeated content in compact cards; use unframed bands and constrained layouts for page sections.

## Typography

- Use `var(--font-display)` for headings and `var(--font-body)` for reading text.
- Establish hierarchy through size, weight, line height, and whitespace before adding color.
- Keep body copy readable with a measured line length and `var(--text-secondary)` rather than low-opacity white.
- Never use viewport-scaled font sizes that make text jump unpredictably.

## Layout and responsive behavior

- Design from 320px upward. Every interactive target must remain usable with a thumb.
- Use stable grids, `minmax()`, `clamp()` for spacing only, and `max-width` containers.
- Preserve safe areas on mobile and do not force a desktop minimum width.
- Prefer one strong primary action per surface. Secondary actions should be visually quiet.
- Check empty, loading, error, disabled, hover, focus-visible, and reduced-motion states.

## Motion

- Import Motion with `import { motion } from 'motion/react'`.
- Use motion to clarify hierarchy, route changes, state changes, and feedback.
- Prefer opacity plus small translate/scale values. Keep transitions short and physically plausible.
- Respect `prefers-reduced-motion`; do not make essential information depend on animation.
- Avoid animating layout-heavy properties when transform or opacity can communicate the same change.

## Accessibility and quality bar

- Use semantic HTML and visible focus states.
- Provide accessible labels for icon-only controls.
- Maintain WCAG AA contrast for text and controls.
- Do not hide important content behind hover-only interactions.
- Validate at narrow mobile, tablet, desktop, and wide desktop widths before considering a UI complete.
