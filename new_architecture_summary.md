# Dataverse Modular Architecture - Executive Summary

> **For the full technical specification, see [new_architecture.md](new_architecture.md)**
>
> **Note:** This summary distinguishes between the **core proposal** (standalone component pattern) and **illustrative examples** (AI search, Dataverse Light) that show what becomes possible - not what's currently planned.

---

## What Is This?

We're proposing a **building-block approach** to Dataverse frontend development. Instead of building UI features that only work inside the SPA, we create **standalone components** that can be:

- Embedded in the new React SPA
- Used in the legacy JSF interface during the transition
- Deployed as external tools or previewers
- Reused in custom integrations

**This is already working.** DVWebloader V2 proves the pattern - it's a file uploader that works standalone, embedded in JSF, and as an external tool.

---

## The Core Idea

```
+-------------------------------------------------------------+
|                    STANDALONE COMPONENT                      |
|                    (e.g., File Uploader)                    |
+-------------------------------------------------------------+
|  - Self-contained HTML/JS/CSS bundle                        |
|  - Communicates via Dataverse Native API                    |
|  - Receives config via URL params or postMessage            |
+-------------------------------------------------------------+
              |                    |                    |
              v                    v                    v
    +-------------+     +-------------+     +-------------+
    |  React SPA  |     |  JSF Pages  |     | External    |
    |  (iframe)   |     |  (iframe)   |     | Tool        |
    +-------------+     +-------------+     +-------------+
```

**Build once, use everywhere.**

---

## Key Benefits

| Benefit | What It Means |
|---------|---------------|
| **Bridge the Transition** | Same component works in JSF now and SPA later |
| **Reduce Duplication** | No need to build JSF and React versions of the same feature |
| **Enable External Tools** | Rich UI components for previewers and integrations |
| **Community Contributions** | Partners can contribute components independently |
| **Flexibility** | Test new approaches without modifying the core |

---

## What's Concrete vs. Illustrative

### Proven & Working

| Item | Description |
|------|-------------|
| **DVWebloader V2** | Standalone file uploader - embedded in JSF via iframe |
| **Standalone Component Pattern** | Proven architecture for reusable UI |
| **iframe + postMessage** | Communication protocol that works |

### Planned

| Item | Description |
|------|-------------|
| **File Tree Browser** | Next component to build using this pattern |

### Illustrative (What Becomes Possible)

These sections in the full document show what the architecture *enables* - they are NOT planned work:

| Item | Purpose in Document |
|------|---------------------|
| **Backend Microservices** | Shows how components could connect to alternative services |
| **AI-Enhanced Search** | Example of swappable service architecture |
| **Dataverse Light** | Illustrates minimal deployment possibilities |

---

## How It Works

### 1. Component Development

Build the component as a standalone web application:
- Self-contained HTML/JS/CSS
- Uses Dataverse Native API for data
- Accepts configuration via URL parameters

### 2. Integration via iframe

Embed in any context using an iframe:
```html
<iframe src="component.html?datasetPid=doi:10.5072/FK2/ABC123&siteUrl=..."></iframe>
```

### 3. Communication

Parent and component communicate via `postMessage`:
- Parent sends configuration and tokens
- Component reports status and completion

---

## Real Example: DVWebloader V2

The DVWebloader demonstrates this pattern in production:

1. **Standalone**: Works as a complete web application
2. **Embedded in JSF**: Runs inside Dataverse pages via iframe
3. **External Tool**: Can be launched as an external tool

Same codebase, three deployment contexts.

---

## When This Approach Applies

**Good for:**
- New UI features that should work in both SPA and JSF
- Components that external tools might want to reuse
- Features that need to work during the JSF-to-SPA transition

**Traditional approach still works for:**
- Features tightly coupled to SPA state/routing
- Simple UI that won't need external reuse
- Quick prototypes

---

## Questions?

For technical details, implementation specifics, and code examples, see the full [Technical Architecture Document](new_architecture.md).

---

**Document Version:** 1.1  
**Created:** December 2025  
**Authors:** Eryk Kulikowski, with Claude Opus 4.5 (preview) via GitHub Copilot for VS Code  
**Based on:** new_architecture.md v1.3  
**Audience:** Management, stakeholders, non-technical readers
