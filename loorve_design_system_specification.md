# Design System Specification: Loorve (Light Mode - Ethereal Intelligence)

> **Target Audience / CLI Instructions**:
> This document defines the single source of truth for UI/UX styling for the Loorve application.
> When executing refactoring tasks, preserve all existing business logic, states, query parameters, and event handlers. Replace hardcoded inline styles and legacy color references with the design tokens and layout rules specified below.

---

## 1. Global System Tokens

### 1.1 Canvas & Ambient Atmospheric Lighting
- **App Canvas Background**: `#f8f9fd` to `#fcf9f8` (Porcelain Off-White)
- **Multi-chromatic Ambient Glows**:
  - Sky Tint Blob: `#e0e8ff` / `#e8eeff`
  - Lavender Tint Blob: `#f3e8ff`
  - Overlay Technique: Large radial gradients with `filter: blur(80px)` and `opacity: 0.6`

### 1.2 Surfaces & Cards
- **Primary Container Surface (Frosted Glass)**: `#ffffff` (`rgba(255, 255, 255, 0.92)` ~ `rgba(255, 255, 255, 0.95)`) with `backdrop-filter: blur(16px)`
- **Secondary Container Low (Inactive / Neutral Fill)**: `#f6f3f2` / `#f0f3fa`
- **AI Floating Card Surface**: `#f3f6ff` (`rgba(240, 244, 255, 0.85)`)
- **Urgent / Warning Container**: `#fff8f7` (Soft rose-amber tint with subtle coral rim)
- **Pro Tier Featured Surface**: `linear-gradient(135deg, #18113c 0%, #2e1a5a 50%, #0d0f28 100%)` (Deep Cosmic Midnight Violet)
- **Borders & Dividers**:
  - Global Surface Hairline: `1px solid rgba(0, 0, 0, 0.06)` ~ `rgba(0, 0, 0, 0.08)`
  - Sub-item Internal Divider: `1px solid #f3f4f6`

### 1.3 Color Tokens (Brand & Accents)
- **Gemini Primary Indigo / Electric Blue**: `#1a73e8` / `#2563eb` / `#4338ca`
- **Gemini Neon Violet / Purple**: `#7c3aed` / `#6366f1`
- **Gemini Magenta / Coral**: `#db2777` / `#d946ef`
- **Semantic Status Badges**:
  - **Success / Optimal / Synced**: Text `#059669`, Background `#ecfdf5`
  - **Notice / Target / D-Day**: Text `#1a73e8`, Background `#eff6ff`
  - **Active / Verified**: Text `#6d28d9` / `#7c3aed`, Background `#f5f3ff`
  - **Warning / Retentive Risk**: Text `#e11d48`, Background `#fff1f2`
  - **Destructive**: `#dc2626`

### 1.4 Typography Colors
- **On-Surface / Primary Text**: `#1f1f1f` / `#111827` (Deep Onyx Charcoal)
- **Secondary / Subtitle / Meta**: `#5f6368` / `#6b7280` (Mid Slate Gray)
- **Tertiary / Inactive / Placeholder**: `#9aa0a6` / `#9ca3af`
- **Inverse Text (On Gradients & Pro Cards)**: `#ffffff`
- **Destructive Action Text**: `#dc2626`

### 1.5 Interactive & Action Gradients
- **Primary Gemini Aurora CTA Button**:
  - Formula: `linear-gradient(135deg, #2563eb 0%, #7c3aed 50%, #db2777 100%)`
  - Text Color: `#ffffff`
  - Drop Shadow / Glow: `0 8px 24px -4px rgba(124, 58, 237, 0.35)`
- **Secondary Gradient Button (Violet-to-Indigo)**:
  - Formula: `linear-gradient(135deg, #2563eb 0%, #7c3aed 100%)`
  - Text Color: `#ffffff`
  - Drop Shadow / Glow: `0 8px 20px -4px rgba(124, 58, 237, 0.35)`
- **Floating Action Button (FAB)**:
  - Formula: `linear-gradient(135deg, #2563eb 0%, #7c3aed 50%, #9333ea 100%)`
  - Drop Shadow / Glow: `0 12px 28px -4px rgba(124, 58, 237, 0.45)`
- **Secondary Neutral Button / Chip**:
  - Background: `#ffffff` or `#f3f4f6`
  - Border: `1px solid rgba(0, 0, 0, 0.08)`
  - Text Color: `#1f1f1f` / `#374151`
- **Toggle Switch (Active)**:
  - Track: `#4f46e5` ~ `#6366f1`
  - Thumb: `#ffffff` (`box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2)`)

---

## 2. Radii, Spacing & Elevations

### 2.1 Corner Radii
- **Outer Shell / Viewport Frame**: `32px` (`rounded-[32px]` or `rounded-3xl`)
- **Primary Cards & Group Containers**: `24px` (`rounded-2xl` ~ `rounded-[24px]`)
- **Secondary Nested Cards / Block Items**: `16px` ~ `20px` (`rounded-xl` ~ `rounded-2xl`)
- **Input Fields & Form Elements**: `12px` ~ `14px` (`rounded-xl`)
- **Interactive Pills, CTA Buttons, Badges, Capsules**: `9999px` (`rounded-full`)

### 2.2 Elevation & Shadow Layers
- **Glass Surface Elevation**: `0 8px 24px -4px rgba(0, 0, 0, 0.04), 0 2px 6px -1px rgba(0, 0, 0, 0.02)`
- **Prominent Card Elevation**: `0 10px 30px -8px rgba(0, 0, 0, 0.05), 0 4px 12px -2px rgba(0, 0, 0, 0.02)`
- **Pro Tier Banner Elevation**: `0 16px 32px -8px rgba(30, 20, 70, 0.25)`
- **Floating Bottom Bar / Dock**: `0 -4px 24px rgba(0, 0, 0, 0.04)`
- **Active Ambient Glows**: `0 8px 20px -2px rgba(99, 102, 241, 0.25)`

### 2.3 Spacing Scale
- **Grid Increment**: Base 4px
- **Screen Lateral Inset (Padding X)**: `16px` to `20px`
- **Card Inner Inset**: `18px` to `24px`
- **Vertical Gap Between Sections**: `16px` to `24px`

---

## 3. Typography Scale & Hierarchy

**Font Stack**: `Plus Jakarta Sans`, `Pretendard`, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif.

| Hierarchy Level | Font Size / Line Height | Weight | Color Token | Letter Spacing | Usage Context |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Hero Display Metric** | 32px ~ 36px / 1.15 | ExtraBold (800) | `#1f1f1f` | -0.03em | Primary stats (`85%`, `D-24`) |
| **Page Title / Large H1** | 22px ~ 24px / 1.25 | Bold (700) | `#1f1f1f` | -0.02em | Screen header ("설정", "복습", "홈") |
| **Section Title / H2** | 18px ~ 20px / 1.3 | Bold / SemiBold (700/600) | `#1f1f1f` | -0.01em | Group labels, critical alert title |
| **Card / Task Title** | 15px ~ 16px / 1.4 | SemiBold (600) | `#1f1f1f` | 0em | Subject name, task items, settings |
| **Body / Feedback Text** | 13px ~ 14px / 1.5 | Regular / Medium (400/500) | `#5f6368` | 0em | AI retention analysis, instructions |
| **Button / Primary Action** | 14px ~ 15px / 1.2 | SemiBold (600) | `#ffffff` | 0.01em | CTA button labels, action text |
| **Section Overline** | 11px ~ 12px / 1.0 | SemiBold (700) | `#6b7280` | 0.08em | Uppercase labels (`ACCOUNT`, `DATA`) |
| **Badge / Caption Pill** | 11px ~ 12px / 1.2 | Medium / SemiBold (500/600)| Per Status | 0.02em | "D-0", "최적 페이스", status tags |

---

## 4. Screen-by-Screen Component Specifications

### 4.1 Home Dashboard (`HomeScreen`)
- **Structure**:
  1. Top Navigation Bar: Minimal height (`56px`), greeting & profile avatar.
  2. Hero Retention Card: Features the 3-stop Aurora CTA ("지금 바로 복습하기 →") and key memory-curve metrics.
  3. Urgent Alert Box: Soft rose-amber container (`#fff8f7`) displaying imminent forgetting curve warnings.
  4. Calendar Matrix: Compact horizontal node strip; active date rendered in indigo/violet pill gradient with luminous shadow.
- **Rules**: Ensure frosted glass layers do not stack more than 2 levels deep to maintain performance.

### 4.2 Review Hub (`ReviewScreen`)
- **Structure**:
  1. Weekly Retention Chart: 7-day capsule infographic with vertical pill bars (`width: 28px ~ 32px`, `height: 100px ~ 120px`, `rounded-full`). Daily progress reflected with cyan-to-violet fills and dot milestones.
  2. Review Task Cards: Linear dual-tone progress bars (`height: 6px`, `rounded-full`), subject tags, and right-aligned action buttons (`이어서 학습하기`, `복습 시작 ⚡`).
  3. FAB: Floating `+ 새 복습 블록` button at bottom-right corner (`rounded-full`, violet ambient glow).

### 4.3 Subject Detail & Study Log (`DetailScreen`)
- **Structure**:
  1. Header with Back Button and D-Day Indicator Badge (`#1a73e8` on `#eff6ff`).
  2. Summary Metric Card: Circular progress / completion percentage (`72%`) in vibrant violet/indigo stroke.
  3. Study Progress Form: Clean light input inputs (`#ffffff`, border `rgba(0,0,0,0.06)`, focus border `#7c3aed`), followed by full-width 3-stop Gemini Aurora Gradient CTA.
  4. Segmented Control: Pill capsule toggle (`학습 기록` vs `복습 일정`).
  5. Schedule List: Status indicator tags (`D-0 오늘 복습 예정`, `2차 복습 완료 ✓`, `망각 위험 ⚠`).

### 4.4 Settings & Profile (`SettingsScreen`)
- **Structure**:
  1. Profile Summary Card: Large avatar, verified status badge (`#6d28d9` on `#f5f3ff`), user handle.
  2. Loorve Pro Showcase Banner: Midnight cosmic gradient card (`#18113c`) with bright white copy and gradient upgrade CTA.
  3. Grouped Settings Panels: Enclosed cards with 24px border radius, row height `48px ~ 56px`, hairline dividers (`#f3f4f6`), and iOS-style switches.
  4. Destructive Area: Padded bottom section containing red accent link (`계정 영구 삭제`).

### 4.5 Global Floating Navigation Dock
- **Layout**: Fixed floating bar anchored above bottom safe-area margin (`bottom: 16px`, lateral margin `16px`).
- **Surface**: `rgba(255, 255, 255, 0.90)` with `backdrop-filter: blur(20px)` and border `1px solid rgba(0, 0, 0, 0.06)`.
- **Active Indicator**: Electric indigo/blue pill background (`rgba(37, 99, 235, 0.08)`) with active text and icon tinted in `#1a73e8` / `#2563eb`.

---

## 5. Copilot CLI Implementation Directive

When applying this document to refactor UI code:
1. **Preserve Logic**: Do not alter, delete, or rename any functional React/Vue hooks, state variables, dispatch functions, or navigation routes.
2. **Apply Design Tokens**: Replace hex colors, padding, and border radii with Tailwind classes or CSS variables directly matching the tables above.
3. **Typography Enforcement**: Enforce font weight, letter spacing (`tracking`), and line height values per the typography matrix.
4. **Interactive States**: Include hover and active press scale feedback (`active:scale-[0.98] transition-transform`) for all gradient CTA buttons.