# CrewCaptain Design Guide (v2.0 — Cyber Captain)

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
- Vibe: High-tech, capable, human-centric; leadership with clarity, precision, and a digital-native edge. A captain's cockpit for the modern era.
- Positioning: Not HR software; a private digital cockpit for people context and progress. Built for a generation of managers who grew up with technology.

- Visual Direction: **Cyberpunk-lite / Dark-first futurism**
  - Dark-first interface with layered depth (not flat black)
  - Neon accent colors used sparingly for status, CTAs, and focus states
  - Glassmorphism on cards and modals (frosted glass over dark gradients)
  - Subtle grid/scan-line textures in backgrounds
  - Monospace typography for headings and data — technical credibility
  - Glowing borders and status indicators
  - Professional and intuitive despite the aesthetic — never sacrifices usability

- Voice & Tone:
  - Tone: confident, direct, technical-but-warm; avoid HR jargon.
  - Core messages: private manager workspace, memory-driven leadership, single source of truth for people context, data ownership and export.
  - Tagline options:
    - Your private cockpit for people context.
    - Lead with memory. Act with clarity.
    - The captain's log for modern managers.
    - Navigate your crew with context and care.
    - Remember more. Lead better.
    - Command your crew data. Own your leadership memory.

---

## 3) Domain & Visual Language

- Primary domain: crewcaptain.de
- Testable future domains: crewcaptain.app, crewcaptain.io, thecrewcaptain.co
- Visual motif: Digital captain's cockpit / HUD (heads-up display) with nautical undertones. The interface should feel like piloting a ship through data — glowing instruments, layered panels, clear status readouts.

Logo Concepts (3 directions)
- Concept A: Minimalist compass icon with neon glow outline + "CC" monogram
- Concept B: Captain's wheel rendered as a HUD element with scan-line texture
- Concept C: Abstract "C" letterform with circuit-board / data-flow lines

Deliverables (for design handoff): SVGs for each concept, horizontal and stacked lockups, app/icon variants, monochrome and color versions, favicon sizes. All logos must work on dark backgrounds.

---

## 4) Color System

### Dark Theme (Primary — Default)

| Role | Token Name | Hex | Usage |
|------|-----------|-----|-------|
| Base background | `--color-bg-base` | `#0D0F14` | Page background, deepest layer |
| Surface | `--color-bg-surface` | `#161A22` | Cards, panels, content areas |
| Elevated surface | `--color-bg-elevated` | `#1E2330` | Modals, dropdowns, hover states |
| Overlay | `--color-bg-overlay` | `rgba(22, 26, 34, 0.85)` | Glassmorphism card backgrounds |
| Primary accent | `--color-primary` | `#00F0FF` | CTAs, active states, primary links |
| Primary accent hover | `--color-primary-hover` | `#33F5FF` | Hover state for primary elements |
| Primary accent muted | `--color-primary-muted` | `rgba(0, 240, 255, 0.15)` | Subtle backgrounds, glow effects |
| Secondary accent | `--color-secondary` | `#A855F7` | Secondary actions, tags, categories |
| Secondary accent hover | `--color-secondary-hover` | `#C084FC` | Hover state for secondary elements |
| Alert / Destructive | `--color-alert` | `#FF2D7B` | Errors, destructive actions, overdue items |
| Alert muted | `--color-alert-muted` | `rgba(255, 45, 123, 0.15)` | Error backgrounds |
| Success | `--color-success` | `#39FF85` | Completed items, positive status |
| Success muted | `--color-success-muted` | `rgba(57, 255, 133, 0.15)` | Success backgrounds |
| Warning | `--color-warning` | `#FFD600` | Caution states, approaching deadlines |
| Warning muted | `--color-warning-muted` | `rgba(255, 214, 0, 0.15)` | Warning backgrounds |
| Text primary | `--color-text-primary` | `#E8ECF0` | Main body text, headings |
| Text secondary | `--color-text-secondary` | `#7A8599` | Descriptions, labels, metadata |
| Text muted | `--color-text-muted` | `#4A5568` | Placeholders, disabled text |
| Border default | `--color-border` | `#2A3040` | Card borders, dividers |
| Border subtle | `--color-border-subtle` | `#1F2533` | Very subtle separators |
| Border glow | `--color-border-glow` | `rgba(0, 240, 255, 0.2)` | Interactive element borders |

### Morale Status Colors

| Status | Color | Hex | Glow |
|--------|-------|-----|------|
| GREEN | Neon green | `#39FF85` | `0 0 8px rgba(57, 255, 133, 0.4)` |
| YELLOW | Electric amber | `#FFD600` | `0 0 8px rgba(255, 214, 0, 0.4)` |
| RED | Hot magenta | `#FF2D7B` | `0 0 8px rgba(255, 45, 123, 0.4)` |
| UNKNOWN | Muted steel | `#4A5568` | none |

### Glow Effects

```css
/* Primary glow — for buttons, active nav items */
--glow-primary: 0 0 12px rgba(0, 240, 255, 0.2);
--glow-primary-strong: 0 0 20px rgba(0, 240, 255, 0.35);

/* Alert glow — for errors, destructive states */
--glow-alert: 0 0 12px rgba(255, 45, 123, 0.2);

/* Success glow — for completed states */
--glow-success: 0 0 12px rgba(57, 255, 133, 0.2);

/* Card hover glow */
--glow-card-hover: 0 0 16px rgba(0, 240, 255, 0.1);
```

### Glassmorphism

```css
/* Standard glass card */
--glass-bg: rgba(22, 26, 34, 0.85);
--glass-border: 1px solid rgba(0, 240, 255, 0.1);
--glass-blur: blur(12px);

/* Elevated glass (modals, dropdowns) */
--glass-elevated-bg: rgba(30, 35, 48, 0.9);
--glass-elevated-border: 1px solid rgba(0, 240, 255, 0.15);
--glass-elevated-blur: blur(16px);
```

---

## 5) Typography

| Role | Font | Weight | Size | Why |
|------|------|--------|------|-----|
| Headings / Stats | **JetBrains Mono** | 700 | H1: 28px, H2: 22px, H3: 18px | Monospace = technical credibility, cockpit readout feel |
| Body / UI text | **Inter** | 400/500 | 14px | Proven readability, neutral, excellent for dense UIs |
| Labels / Badges | **JetBrains Mono** | 500 | 12px | Compact, technical, great for status indicators |
| Data / Numbers | **JetBrains Mono** | 600 | 14px | Tabular alignment, dashboard data |
| Navigation | **Inter** | 500/600 | 14px | Clean, readable at small sizes |

Font stacks:
```css
--font-heading: 'JetBrains Mono', 'Fira Code', ui-monospace, SFMono-Regular, monospace;
--font-ui: 'Inter', system-ui, -apple-system, 'Segoe UI', Arial, sans-serif;
--font-mono: 'JetBrains Mono', 'Fira Code', ui-monospace, SFMono-Regular, monospace;
```

### Typography Scale

```json
{
  "h1": { "size": 28, "weight": 700, "font": "heading", "letterSpacing": "-0.5px" },
  "h2": { "size": 22, "weight": 700, "font": "heading", "letterSpacing": "-0.3px" },
  "h3": { "size": 18, "weight": 600, "font": "heading", "letterSpacing": "-0.2px" },
  "body": { "size": 14, "weight": 400, "font": "ui", "letterSpacing": "0" },
  "bodyMedium": { "size": 14, "weight": 500, "font": "ui", "letterSpacing": "0" },
  "caption": { "size": 12, "weight": 400, "font": "ui", "letterSpacing": "0.2px" },
  "label": { "size": 12, "weight": 500, "font": "mono", "letterSpacing": "0.5px", "textTransform": "uppercase" },
  "data": { "size": 14, "weight": 600, "font": "mono", "letterSpacing": "0" }
}
```

---

## 6) Design System & UI Tokens

### Spacing Grid

```
4, 8, 12, 16, 20, 24, 32, 40, 48, 64
```

### Border Radius

| Token | Value | Usage |
|-------|-------|-------|
| `--radius-small` | 4px | Badges, small elements |
| `--radius-medium` | 8px | Cards, inputs, buttons |
| `--radius-large` | 12px | Modals, large panels |
| `--radius-full` | 9999px | Pills, circular indicators |

### Shadows & Elevation

In the dark theme, elevation is communicated through background lightness and glow rather than traditional drop shadows:

| Level | Background | Border | Glow |
|-------|-----------|--------|------|
| Base (0) | `#0D0F14` | none | none |
| Surface (1) | `#161A22` | `#2A3040` | none |
| Elevated (2) | `#1E2330` | `rgba(0, 240, 255, 0.1)` | subtle |
| Overlay (3) | `rgba(22, 26, 34, 0.85)` + blur | `rgba(0, 240, 255, 0.15)` | medium |

### Component Patterns

**Buttons:**
- Primary: Filled with `--color-primary`, subtle glow shadow, text `#0D0F14`
- Secondary: Ghost/outline with primary border, text `--color-primary`
- Destructive: Filled with `--color-alert`, glow shadow
- Disabled: `--color-bg-elevated` background, `--color-text-muted` text

**Cards:**
- Background: `var(--glass-bg)` with `backdrop-filter: var(--glass-blur)`
- Border: `var(--glass-border)`
- Hover: border brightens to `rgba(0, 240, 255, 0.25)`, add `--glow-card-hover`
- Transition: `border-color 0.2s, box-shadow 0.2s`

**Inputs:**
- Background: `--color-bg-elevated`
- Border: `--color-border` (default), `--color-primary` (focus)
- Focus: add `--glow-primary` box-shadow
- Text: `--color-text-primary`
- Placeholder: `--color-text-muted`

**Navigation:**
- Background: `--color-bg-surface` with subtle bottom border glow
- Active item: left border in `--color-primary` with glow
- Brand text: `--color-primary` in monospace font
- Links: `--color-text-secondary` default, `--color-text-primary` on hover

**Status Indicators:**
- Morale dots: 8px circles with color + matching glow
- Pulsing animation on RED status (subtle, 2s cycle)
- Badge style: pill shape with muted background + bright text

**Tables / Data:**
- Monospace font for data cells
- Row hover: subtle background shift to `--color-bg-elevated`
- Header: `--color-text-secondary` in uppercase label style
- Alternating rows: barely perceptible shade difference (`#161A22` / `#181D26`)

### Design Tokens (JSON)

```json
{
  "color": {
    "bg": {
      "base": "#0D0F14",
      "surface": "#161A22",
      "elevated": "#1E2330",
      "overlay": "rgba(22, 26, 34, 0.85)"
    },
    "primary": "#00F0FF",
    "primaryHover": "#33F5FF",
    "primaryMuted": "rgba(0, 240, 255, 0.15)",
    "secondary": "#A855F7",
    "secondaryHover": "#C084FC",
    "alert": "#FF2D7B",
    "alertMuted": "rgba(255, 45, 123, 0.15)",
    "success": "#39FF85",
    "successMuted": "rgba(57, 255, 133, 0.15)",
    "warning": "#FFD600",
    "warningMuted": "rgba(255, 214, 0, 0.15)",
    "text": {
      "primary": "#E8ECF0",
      "secondary": "#7A8599",
      "muted": "#4A5568"
    },
    "border": {
      "default": "#2A3040",
      "subtle": "#1F2533",
      "glow": "rgba(0, 240, 255, 0.2)"
    },
    "morale": {
      "green": "#39FF85",
      "yellow": "#FFD600",
      "red": "#FF2D7B",
      "unknown": "#4A5568"
    }
  },
  "font": {
    "heading": "'JetBrains Mono', 'Fira Code', ui-monospace, SFMono-Regular, monospace",
    "ui": "'Inter', system-ui, -apple-system, 'Segoe UI', Arial, sans-serif",
    "mono": "'JetBrains Mono', 'Fira Code', ui-monospace, SFMono-Regular, monospace"
  },
  "typography": {
    "h1": { "size": 28, "weight": 700, "letterSpacing": "-0.5px" },
    "h2": { "size": 22, "weight": 700, "letterSpacing": "-0.3px" },
    "h3": { "size": 18, "weight": 600, "letterSpacing": "-0.2px" },
    "body": { "size": 14, "weight": 400 },
    "caption": { "size": 12, "weight": 400 },
    "label": { "size": 12, "weight": 500, "letterSpacing": "0.5px" },
    "data": { "size": 14, "weight": 600 }
  },
  "radius": {
    "small": 4,
    "medium": 8,
    "large": 12,
    "full": 9999
  },
  "spacing": [4, 8, 12, 16, 20, 24, 32, 40, 48, 64],
  "glow": {
    "primary": "0 0 12px rgba(0, 240, 255, 0.2)",
    "primaryStrong": "0 0 20px rgba(0, 240, 255, 0.35)",
    "alert": "0 0 12px rgba(255, 45, 123, 0.2)",
    "success": "0 0 12px rgba(57, 255, 133, 0.2)",
    "cardHover": "0 0 16px rgba(0, 240, 255, 0.1)"
  },
  "glass": {
    "bg": "rgba(22, 26, 34, 0.85)",
    "border": "1px solid rgba(0, 240, 255, 0.1)",
    "blur": "blur(12px)"
  }
}
```

---

## 7) Imagery & Accessibility

- Imagery
  - Use HUD/cockpit motifs with geometric shapes; avoid generic HR stock imagery.
  - Prefer angular, technical illustrations with neon accent highlights.
  - Icons: outlined style (not filled), with optional glow on active state.
  - Background textures: subtle grid patterns, scan-lines at very low opacity (2-5%).

- Accessibility (CRITICAL — non-negotiable)
  - Contrast: All text must meet WCAG AA minimum (4.5:1 for normal text, 3:1 for large text)
  - `#E8ECF0` on `#0D0F14` = 14.7:1 ✓
  - `#7A8599` on `#0D0F14` = 5.2:1 ✓
  - `#00F0FF` on `#0D0F14` = 12.1:1 ✓
  - `#4A5568` on `#0D0F14` = 3.3:1 ✓ (large text / decorative only)
  - Focus states: visible glow outlines (2px solid `--color-primary` + glow shadow)
  - Alt text: provide meaningful descriptions for all decorative visuals
  - Responsive typography: scale for readability on small screens
  - Motion: respect `prefers-reduced-motion` — disable glow pulses and transitions
  - Color alone: never use color as the only indicator — always pair with text/icon

---

## 9) Copy & Messaging Guidelines

### Brand Voice

- Brand Voice
  - Tone: direct, technical-but-warm, confident; no HR jargon
  - Feels like talking to a sharp colleague, not a corporate tool
- Key Messages to surface on site
  - Private manager workspace
  - Memory-driven leadership
  - One place for people context
  - Data ownership and exportability
  - Built for the next generation of leaders
- Landing Page Copy (two variants)
  - Variant A (Direct / Technical)
    - Headline: CrewCaptain — Your private cockpit for people context
    - Subhead: Track 1:1s, development goals, and action items in one self-hosted workspace. Your data. Your crew. Your rules.
    - CTAs: Get Started · View Demo
  - Variant B (Story-focused)
    - Headline: Lead with memory. Command with clarity.
    - Subhead: A centralized command center for managers who believe great leadership starts with remembering what matters.
    - CTAs: Deploy on Docker · Learn More

---

## 10) Localization Strategy (English-first)

- Primary language: English
- If multilingual future is planned, define an i18n strategy with copy keys and localization process. For now, keep content in English.

---

## 10) Gamification & Engagement (for younger managers)

Subtle gamification elements to increase engagement without feeling childish:

- **Progress rings** for PDP goals (animated fill with glow effect)
- **Streak counters** for consecutive 1:1s held (monospace display)
- **Micro-animations** on task completion (checkmark with brief particle/glow burst)
- **Achievement indicators** for milestones (first 1:1, 10 action items closed, etc.)
- **Activity heat map** on dashboard (contribution-graph style, using accent colors)

All gamification respects `prefers-reduced-motion` and can be disabled in user settings.

---

## 11) Deliverables for AI Agent Handoff

- Logo concepts: 3 vector directions (SVG/PNG) with horizontal/stacked lockups, app/icon variants, monochrome/color versions, favicon set — all on dark backgrounds
- Brand tokens: JSON/YAML design tokens (colors, typography, spacing, radii, glow effects)
- Landing page: two EN variants (copy blocks, CTAs, hero/subhead) — dark theme
- UI kit: components (buttons, inputs, cards, badges, nav) with token-driven styling
- Copy-ready microcopy: form labels, placeholders, tooltips, error messages
- Asset packaging: folder structure and naming conventions

---

## 12) File Naming Conventions & Directory Structure (AI-friendly)

- assets/
  - logos/
    - conceptA.svg
    - conceptB.svg
    - conceptC.svg
  - icons/
  - fonts/
- tokens/
  - colors.json
  - typography.json
  - spacing.json
  - radii.json
  - glow.json
  - glass.json
- pages/
  - index_en.html
  - index_en-docker.html
- copy/
  - hero_en.md
  - subhead_en.md
  - cta_en.md
- docs/
  - branding_guide.md
  - accessibility.md
- deliverables/
  - logo_variants/
  - ui_kit/
  - landing_pages/

---

## 13) Acceptance Criteria for AI Agent

- Generate 3 distinct logo concepts (A/B/C) with SVG/PNG variants and ready-to-use app icon assets — must work on dark backgrounds.
- Produce a cohesive design-token set (colors, typography, spacing, radii, glow, glass) in JSON or YAML plus CSS variable equivalents.
- Create two landing page variants (EN) with hero, subhead, and CTAs — dark theme with cyberpunk-lite aesthetic.
- Output an accessible UI kit with buttons, inputs, cards, and nav styled to the tokens — all meeting WCAG AA contrast requirements.
- Provide a brand usage guide and a one-page design brief for designers/branding AI.
- Ensure domain-accurate guidance for CrewCaptain.de (and safe future domains if needed).
- All components must respect `prefers-reduced-motion` media query.

---

## 14) Design Principles (TL;DR)

1. **Dark-first, depth through layers** — not flat black, but layered surfaces with subtle elevation
2. **Neon accents are spice, not the meal** — 10-15% of visual surface, max
3. **Monospace for authority** — headings and data in JetBrains Mono convey technical credibility
4. **Glass for depth** — frosted panels create spatial hierarchy without heavy shadows
5. **Glow for interactivity** — borders and shadows glow on hover/focus to signal affordance
6. **Accessibility is non-negotiable** — every color choice must pass WCAG AA
7. **Motion with purpose** — animations communicate state, never just decorate
8. **Professional cyberpunk** — this is a work tool, not a game UI. Keep it usable.
