# Basic Layout Route Refactor Implementation Plan

> **For agentic workers:** Use the existing project workflow and keep changes limited to frontend layout/style concerns.

**Goal:** Make every authenticated route reuse `BasicLayout.vue` for the page background, shared back header, content viewport, and five-item bottom navigation.

**Architecture:** `App.vue` renders the router directly. Authenticated pages become children of a parent route using `BasicLayout.vue`; login and registration remain standalone. Route `meta.headerMode` selects either a page-owned custom header or the shared back header, removing hard-coded path checks from the layout.

**Tech Stack:** Vue 3, Vue Router 4, TypeScript, Lucide Vue, scoped CSS.

**Global Constraints:** Preserve all API calls, forms, search behavior, buttons, route paths, and page-owned prototype headers. Use homepage background `#0F172A` in the base layout. Keep the existing five-item bottom navigation. Do not run build, lint, tests, browser automation, or screenshots unless explicitly requested.

---

### Task 1: Convert routes to layout children

**Files:**
- Modify: `matchsystem-frontend/src/config/route.ts`
- Modify: `matchsystem-frontend/src/App.vue`

- [ ] Add `BasicLayout` as the parent route component.
- [ ] Move all authenticated routes into its `children` list without changing URLs.
- [ ] Keep login and registration outside the parent layout.
- [ ] Add route metadata for shared or custom headers.
- [ ] Change `App.vue` to render only the root `<router-view />`.

### Task 2: Centralize shared layout UI

**Files:**
- Modify: `matchsystem-frontend/src/layouts/BasicLayout.vue`

- [ ] Replace path-specific header conditions with `route.meta.headerMode`.
- [ ] Replace the old Vant navigation bar with a dark shared back header.
- [ ] Keep the existing five-item Lucide bottom navigation and active-route matching.
- [ ] Use `#0F172A` for layout and shared content backgrounds.
- [ ] Calculate content height from shared header and bottom navigation variables.

### Task 3: Review route pages

**Files:**
- Review: `matchsystem-frontend/src/pages/*.vue`

- [ ] Confirm custom-header pages still own their prototype headers.
- [ ] Confirm subpages receive the shared back header only once.
- [ ] Confirm user team list pages retain search, buttons, list actions, and data requests.
- [ ] Confirm authentication pages have no shared header or bottom navigation.
- [ ] Scan for duplicate navigation components and patch residue.
