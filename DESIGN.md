# CrewCaptain Design Guide (v1.1)

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
  - Tagline options (from branding input): 
    - A compass for people-centric leadership.
    - Lead with memory. Act with clarity.
    - Navigate your crew with context and care.
    - Your private cockpit for people context.
    - Remember more. Lead better.
    - Guiding teams with memory and direction.

---

## 3) Domain & Visual Language

- Primary domain: crewcaptain.de
- Testable future domains: crewcaptain.app, crewcaptain.io, thecrewcaptain.co
- Visual motif: Nautical captain theme with a clean, modern UI. Logo should scale well from app icon to full logo.

Logo Concepts (3 directions)
- Concept A: Nautical compass + subtle “C” or crew icon
- Concept B: Captain’s wheel with small person-figures
- Concept C: Minimal CC monogram with crest/shield feel

Deliverables (for design handoff): SVGs for each concept, horizontal and stacked lockups, app/icon variants, monochrome and color versions, favicon sizes.

---

## 4) Color System

Starter palette
- Primary: deep navy or dark teal
  - #1B2A4B or #0A5A66
- Secondary: teal/blue family
  - #2FB4A3 or #2A6F97
- Accent: warm clay/orange
  - #E07A3A or #E0893A
- Neutrals: charcoal and off-white
  - #2B2B2B and #F6F7F8

Usage guidance
- Primary: headers, primary UI surfaces
- Secondary: UI accents, controls
- Accent: CTAs and highlights (sparingly)
- Neutrals: backgrounds and body text
- Ensure accessible contrast for text (AA/AAA guidance as a baseline)

---

## 5) Typography

- UI / Body: Inter or Open Sans (readable, neutral)
- Headings: Source Sans Pro or Montserrat (professional yet approachable)
- Accessibility: high contrast, scalable text, proper line heights
- Web font stacks: provide fallbacks; optimize for performance

---

## 6) Design System & UI Tokens

- Color Roles
  - Primary: headers, surfaces
  - Secondary: UI accents, controls
  - Accent: CTAs, highlights
  - Neutrals: backgrounds, text
- Typography Scale (example)
  - H1: 32px, bold
  - H2: 24px, medium
  - H3: 20px, medium
  - Body: 14–16px
  - Caption: 12px
- Spacing Grid (reference)
  - 4, 8, 12, 16, 24, 32, 48
- Components (baseline)
  - Button, TextInput, Card, Badge, Nav, Tooltip
- Elevation/Shadows
  - Subtle shadows; prefer flat design with soft radii
- Tokens Output Formats (AI-friendly)
  - JSON or YAML design-tokens file
  - CSS variables for quick frontend adoption

Example design-tokens JSON snippet:
{
  "color": {
    "primary": "#1B2A4B",
    "secondary": "#2FB4A3",
    "accent": "#E07A3A",
    "neutral": {
      "text": "#2B2B2B",
      "bg": "#F6F7F8",
      "surface": "#FFFFFF"
    }
  },
  "font": {
    "ui": "Inter, system-ui, -apple-system, 'Segoe UI', Arial"
  },
  "typography": {
    "h1": {"size": 32, "weight": 700},
    "h2": {"size": 24, "weight": 500},
    "h3": {"size": 20, "weight": 500},
    "body": {"size": 14, "weight": 400},
    "caption": {"size": 12, "weight": 400}
  },
  "radius": {
    "small": 4,
    "medium": 8,
    "large": 12
  }
}

---

## 7) Imagery & Accessibility

- Imagery
  - Use compass/ship motifs with human silhouettes; avoid generic HR stock imagery.
  - Prefer soft, rounded shapes; minimalistic illustrations or icons.
- Accessibility
  - Contrast: text minimum 4.5:1 for normal text, 3:1 for large text
  - Focus states: visible keyboard focus outlines
  - Alt text: provide meaningful descriptions for all decorative visuals
  - Responsive typography: scale for readability on small screens

---

## 8) Copy & Messaging Guidelines

- Brand Voice
  - Tone: warm, confident, practical; no HR jargon
- Key Messages to surface on site
  - Private manager workspace
  - Memory-driven leadership
  - One place for people context
  - Data ownership and exportability
- Landing Page Copy (two variants)
  - Variant A (Direct)
    - Headline: CrewCaptain — A private cockpit for people context
    - Subhead: Manage your crew with memory and clear actions. Self-hosted, data-owned, and scalable.
    - CTAs: Get Started · View Demo
  - Variant B (Story-focused)
    - Headline: Lead with memory. Guide with clarity.
    - Subhead: A centralized platform for managers to capture 1:1 history, development goals, and follow-ups—without HR clutter.
    - CTAs: Try it on Docker · Learn More

---

## 9) Localization Strategy (English-first)

- Primary language: English
- If multilingual future is planned, define an i18n strategy with copy keys and localization process. For now, keep content in English.

---

## 10) Deliverables for AI Agent Handoff

- Logo concepts: 3 vector directions (SVG/PNG) with horizontal/stacked lockups, app/icon variants, monochrome/color versions, favicon set
- Brand tokens: JSON/YAML design tokens (colors, typography, spacing, radii)
- Landing page: two EN variants (copy blocks, CTAs, hero/subhead)
- UI kit: components (buttons, inputs, cards, badges, nav) with token-driven styling
- Copy-ready microcopy: form labels, placeholders, tooltips, error messages
- Asset packaging: folder structure and naming conventions

---

## 11) File Naming Conventions & Directory Structure (AI-friendly)

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

## 12) Acceptance Criteria for AI Agent

- Generate 3 distinct logo concepts (A/B/C) with SVG/PNG variants and ready-to-use app icon assets.
- Produce a cohesive design-token set (colors, typography, spacing, radii) in JSON or YAML plus CSS variable equivalents.
- Create two landing page variants (EN) with hero, subhead, and CTAs ready to paste into a marketing site.
- Output an accessible UI kit with buttons, inputs, cards, and nav styled to the tokens.
- Provide a brand usage guide and a one-page design brief for designers/branding AI.
- Ensure domain-accurate guidance for CrewCaptain.de (and safe future domains if needed).
