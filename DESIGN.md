# CrewCaptain Design Guide (v2.0)

Primary domain: crewcaptain.de

This design guide is crafted for AI agents to generate, validate, and hand off design assets for CrewCaptain. It covers branding, UI tokens, typography, accessibility, imagery, copy, and deliverables. English-only.

---

## 1) Purpose & Scope

- Purpose: Provide a precise, machine-readable blueprint for generating branding assets, UI design tokens, logos, landing pages, and related deliverables for CrewCaptain.
- Scope: Branding identity, color/typography system, design tokens, logo concepts, UI components, accessibility rules, landing page copy variants, and asset handoff packaging.
- Audience: Designers, frontend engineers, branding AI agents, marketing teams, and QA reviewers.

---

## 2) Brand Identity

- Brand Essence: A privacy-first, manager-focused platform that helps multi-manager teams organize people context, 1:1 history, development goals, and follow-ups in one self-hosted workspace.
- Vibe: Calm, capable, human-centric; leadership with warmth, clarity, and personal touch.
- Positioning: Not HR software; a private cockpit for people context and progress.

- Voice & Tone:
  - Tone: confident, warm, practical; avoid HR jargon.
  - Core messages: private manager workspace, memory-driven leadership, single source of truth for people context, data ownership and export.
  - Primary Tagline: "Your private cockpit for people context."
  - Secondary Taglines:
    - Remember more. Lead better.
    - Lead with memory. Act with clarity.
    - The captain's log for modern managers.
    - Navigate your crew with context and care.

---

## 3) Domain & Visual Language

- Primary domain: crewcaptain.de
- Testable future domains: crewcaptain.app, crewcaptain.io, thecrewcaptain.co
- Visual motif: Modern nautical navigation — compass, direction, guidance. Clean geometric forms that scale from favicon to full lockup.

### Logo Direction (Primary: Concept A — Compass Rose)

The chosen logo direction is an abstract geometric compass rose with an integrated human element:

- The compass rose communicates direction, guidance, and navigation — mapping to "guiding your team."
- The compass needle doubles as a subtle person silhouette, reinforcing "people-first leadership."
- Geometric construction ensures clean scaling from 16px favicon to full-size lockup.
- Avoid overly literal nautical imagery (anchors, ship wheels) which feel heavy for a software product.

Deliverables (for design handoff): SVGs for compass rose mark, horizontal and stacked lockups, app/icon variants, monochrome and color versions, favicon sizes (16, 32, 48, 192, 512px).

### Iconography System

Custom icon set reinforcing the nautical/navigation theme:

| Feature | Icon Concept |
|---------|-------------|
| People/Crew | Simplified crew silhouettes (2-3 people) |
| 1:1 Meetings | Two compass needles meeting at a point |
| Action Items | Small nautical signal flag |
| Goals/PDP | North star or lighthouse |
| Notes | Captain's log book |
| Morale | Weather indicators (sun, clouds, storm) |
| Dashboard | Simplified helm (geometric) |

---

## 4) Color System

### Primary Palette

| Role | HEX | Usage |
|------|-----|-------|
| Primary (Deep Navy) | `#162340` | Headers, navigation, primary surfaces, sidebar |
| Primary Light | `#1E3258` | Hover states, secondary surfaces |
| Primary Dark | `#0D1520` | Dark mode base, deep backgrounds |
| Secondary (Rich Teal) | `#1A9E8F` | Interactive elements, links, controls |
| Secondary Light | `#3DBDAE` | Hover states, light accents |
| Secondary Dark | `#147A6E` | Active states, pressed buttons |
| Accent (Warm Amber) | `#E8763A` | Primary CTAs, highlights, key actions |
| Accent Light | `#F09560` | Hover states for accent elements |
| Accent Dark | `#C4612A` | Active/pressed accent states |

### Neutrals

| Role | HEX | Usage |
|------|-----|-------|
| Text Primary | `#1A1D23` | Body text, headings on light bg |
| Text Secondary | `#4A5568` | Supporting text, labels |
| Text Muted | `#6B7280` | Placeholder text, disabled states |
| Background | `#F8F9FB` | Page background |
| Surface | `#FFFFFF` | Cards, panels, modals |
| Border | `#E2E5EA` | Default borders |
| Border Light | `#F0F1F3` | Subtle dividers |

### Semantic Colors

| Role | HEX | Usage |
|------|-----|-------|
| Success | `#2D8F6F` | Positive states, confirmations |
| Warning | `#D4A843` | Caution states, attention needed |
| Error | `#C94A4A` | Errors, destructive actions |
| Error Background | `#FEF2F2` | Error message backgrounds |
| Warning Background | `#FFFBEB` | Warning message backgrounds |

### Morale Colors

| Status | HEX | Visual |
|--------|-----|--------|
| GREEN | `#2D8F6F` | Forest green — positive, thriving |
| YELLOW | `#D4A843` | Gold — attention needed |
| RED | `#C94A4A` | Muted red — concern |
| UNKNOWN | `#9CA3AF` | Gray — no data |

### Dark Mode Palette

For future dark mode implementation (reinforces "private cockpit" metaphor):

| Role | HEX |
|------|-----|
| Dark Surface | `#0D1520` (very dark navy) |
| Dark Card | `#162340` (primary becomes card bg) |
| Dark Text | `#E8ECF2` (soft white) |
| Dark Border | `#2A3A55` |
| Accents | Same teal and amber (pop on dark backgrounds) |

### Usage Principles

- **60-30-10 Rule**: 60% neutrals (surfaces/backgrounds), 30% navy (headers, sidebars, navigation), 10% teal+amber (CTAs, highlights, interactive elements).
- The accent amber should feel scarce and meaningful — only for primary CTAs and key highlights.
- Ensure accessible contrast for text: minimum 4.5:1 for normal text, 3:1 for large text (WCAG AA).

---

## 5) Typography

### Font Stack

| Role | Font | Fallbacks | Usage |
|------|------|-----------|-------|
| Headings | Plus Jakarta Sans | system-ui, -apple-system, 'Segoe UI', sans-serif | H1-H3, brand name, page titles |
| UI / Body | Inter | system-ui, -apple-system, 'Segoe UI', Arial, sans-serif | Body text, labels, buttons, form fields |
| Monospace | JetBrains Mono | ui-monospace, SFMono-Regular, Menlo, Consolas, monospace | Code, data displays, technical content |

### Why Plus Jakarta Sans for Headings

- Geometric warmth — modern and approachable without being generic.
- Distinctive from the ubiquitous Montserrat/Source Sans Pro used by most SaaS products.
- Excellent weight range (200-800) for flexible heading hierarchy.
- Free, well-hinted, and optimized for screen rendering.
- Pairs naturally with Inter for body text.

### Typography Scale

| Level | Size | Weight | Line Height | Font |
|-------|------|--------|-------------|------|
| H1 | 32px | 700 (Bold) | 1.2 | Plus Jakarta Sans |
| H2 | 24px | 600 (SemiBold) | 1.3 | Plus Jakarta Sans |
| H3 | 20px | 600 (SemiBold) | 1.3 | Plus Jakarta Sans |
| Body | 14px | 400 (Regular) | 1.5 | Inter |
| Body Large | 16px | 400 (Regular) | 1.5 | Inter |
| Small | 13px | 400 (Regular) | 1.4 | Inter |
| Caption | 12px | 400 (Regular) | 1.4 | Inter |
| Button | 14px | 500 (Medium) | 1 | Inter |

### Accessibility

- High contrast text on all backgrounds (minimum 4.5:1 ratio)
- Scalable text — use rem/em units in production
- Proper line heights for readability
- Web font stacks include system fallbacks for fast initial render

---

## 6) Design System & UI Tokens

### Design Tokens (JSON)

```json
{
  "color": {
    "primary": "#162340",
    "primaryLight": "#1E3258",
    "primaryDark": "#0D1520",
    "secondary": "#1A9E8F",
    "secondaryLight": "#3DBDAE",
    "secondaryDark": "#147A6E",
    "accent": "#E8763A",
    "accentLight": "#F09560",
    "accentDark": "#C4612A",
    "neutral": {
      "text": "#1A1D23",
      "textSecondary": "#4A5568",
      "textMuted": "#6B7280",
      "bg": "#F8F9FB",
      "surface": "#FFFFFF",
      "border": "#E2E5EA",
      "borderLight": "#F0F1F3"
    },
    "semantic": {
      "success": "#2D8F6F",
      "warning": "#D4A843",
      "error": "#C94A4A",
      "errorBg": "#FEF2F2",
      "warningBg": "#FFFBEB"
    },
    "morale": {
      "green": "#2D8F6F",
      "yellow": "#D4A843",
      "red": "#C94A4A",
      "unknown": "#9CA3AF"
    }
  },
  "font": {
    "heading": "'Plus Jakarta Sans', system-ui, -apple-system, 'Segoe UI', sans-serif",
    "ui": "'Inter', system-ui, -apple-system, 'Segoe UI', Arial, sans-serif",
    "mono": "'JetBrains Mono', ui-monospace, SFMono-Regular, Menlo, Consolas, monospace"
  },
  "typography": {
    "h1": { "size": 32, "weight": 700, "lineHeight": 1.2 },
    "h2": { "size": 24, "weight": 600, "lineHeight": 1.3 },
    "h3": { "size": 20, "weight": 600, "lineHeight": 1.3 },
    "body": { "size": 14, "weight": 400, "lineHeight": 1.5 },
    "bodyLarge": { "size": 16, "weight": 400, "lineHeight": 1.5 },
    "small": { "size": 13, "weight": 400, "lineHeight": 1.4 },
    "caption": { "size": 12, "weight": 400, "lineHeight": 1.4 }
  },
  "radius": {
    "small": 4,
    "medium": 8,
    "large": 12,
    "full": 9999
  },
  "spacing": [0, 4, 8, 12, 16, 20, 24, 32, 40, 48],
  "shadow": {
    "sm": "0 1px 2px rgba(22, 35, 64, 0.06)",
    "md": "0 2px 8px rgba(22, 35, 64, 0.09)",
    "lg": "0 4px 16px rgba(22, 35, 64, 0.13)",
    "hover": "0 6px 20px rgba(22, 35, 64, 0.15)"
  }
}
```

### CSS Custom Properties

See `frontend/src/app/globals.css` for the live implementation of these tokens as CSS custom properties.

### Components (baseline)

- Button (primary, secondary, ghost variants)
- TextInput, TextArea
- Card (with hover elevation)
- Badge (morale, status)
- Navigation (top bar with brand)
- Tooltip
- EmptyState

### Elevation/Shadows

- Cards use `shadow-sm` at rest, `shadow-md` on hover — creating a sense of layered information.
- Modals and dropdowns use `shadow-lg`.
- Shadows use the primary navy color at low opacity for cohesion.

---

## 7) Motion & Interaction

### Principles

- Motion is purposeful — it communicates state changes, not decoration.
- Transitions are quick (150-200ms) and use ease-out curves.
- Avoid motion that blocks user interaction.

### Defined Transitions

| Element | Property | Duration | Easing |
|---------|----------|----------|--------|
| Cards | box-shadow, border-color, transform | 150ms | ease-out |
| Buttons | background-color, transform | 120ms | ease-out |
| Navigation links | color, opacity | 100ms | ease |
| Page content | opacity | 200ms | ease-in-out |
| Modals | opacity, transform | 200ms | ease-out |

### Micro-interactions

- Cards lift slightly on hover (`translateY(-1px)` + shadow increase)
- Buttons scale down subtly on press (`scale(0.98)`)
- Loading states use a minimal compass-rose spinner (brand-aligned)
- Success confirmations use a brief checkmark animation

### Future Considerations

- Scroll-triggered reveals for landing page sections
- Compass logo mark subtle rotation on page load
- Animated hero on landing page showing product in use

---

## 8) Imagery & Accessibility

### Imagery

- Use compass/navigation motifs with human silhouettes; avoid generic HR stock imagery.
- Prefer soft, rounded shapes; minimalistic illustrations or icons.
- The compass rose can serve as a subtle watermark or section divider pattern.
- Product screenshots should show real UI, not mockups.

### Accessibility Requirements

- Contrast: text minimum 4.5:1 for normal text, 3:1 for large text (WCAG 2.1 AA)
- Focus states: visible keyboard focus outlines (2px solid secondary color, 2px offset)
- Alt text: provide meaningful descriptions for all decorative visuals
- Responsive typography: scale for readability on small screens
- Color is never the sole indicator — always pair with text labels or icons
- Morale indicators use both color AND text labels

---

## 9) Copy & Messaging Guidelines

### Brand Voice

- Tone: warm, confident, practical; no HR jargon
- Personality: like a knowledgeable colleague, not a corporate tool

### Key Messages

- Private manager workspace — your data, your control
- Memory-driven leadership — never forget context
- One place for people context — 1:1s, goals, notes, actions
- Data ownership and exportability — self-hosted, no vendor lock-in

### Landing Page Copy

**Variant A (Direct)**
- Headline: CrewCaptain — Your private cockpit for people context
- Subhead: Manage your crew with memory and clear actions. Self-hosted, data-owned, and scalable.
- CTAs: Get Started · View Demo

**Variant B (Story-focused)**
- Headline: The captain's log for modern managers.
- Subhead: Capture 1:1 history, track development goals, and follow up on actions — without HR clutter. Self-hosted and private by design.
- CTAs: Try it on Docker · Learn More

---

## 10) Localization Strategy (English-first)

- Primary language: English
- If multilingual future is planned, define an i18n strategy with copy keys and localization process. For now, keep content in English.

---

## 11) Deliverables for AI Agent Handoff

- Logo: Compass rose mark (SVG/PNG) with horizontal/stacked lockups, app/icon variants, monochrome/color versions, favicon set
- Brand tokens: JSON design tokens (colors, typography, spacing, radii, shadows)
- Landing page: two EN variants (copy blocks, CTAs, hero/subhead)
- UI kit: components (buttons, inputs, cards, badges, nav) with token-driven styling
- Copy-ready microcopy: form labels, placeholders, tooltips, error messages
- Asset packaging: folder structure and naming conventions

---

## 12) File Naming Conventions & Directory Structure (AI-friendly)

```
assets/
  logos/
    compass-rose-mark.svg
    compass-rose-lockup-horizontal.svg
    compass-rose-lockup-stacked.svg
    compass-rose-monochrome.svg
  icons/
  fonts/
tokens/
  design-tokens.json
  colors.json
  typography.json
  spacing.json
pages/
  index_en.html
copy/
  hero_en.md
  subhead_en.md
  cta_en.md
docs/
  branding_guide.md
  accessibility.md
deliverables/
  logo_variants/
  ui_kit/
  landing_pages/
```

---

## 13) Acceptance Criteria for AI Agent

- Generate compass rose logo concept with SVG/PNG variants and ready-to-use app icon assets.
- Produce a cohesive design-token set (colors, typography, spacing, radii, shadows) in JSON plus CSS variable equivalents.
- Create two landing page variants (EN) with hero, subhead, and CTAs ready to paste into a marketing site.
- Output an accessible UI kit with buttons, inputs, cards, and nav styled to the tokens.
- Provide a brand usage guide and a one-page design brief for designers/branding AI.
- Ensure domain-accurate guidance for CrewCaptain.de (and safe future domains if needed).
- All color combinations must pass WCAG 2.1 AA contrast requirements.
- Typography must use Plus Jakarta Sans for headings and Inter for body text.
