# Dataverse Modular Architecture - Executive Summary

> **For the full technical specification, see [new_architecture.md](new_architecture.md)**

---

## What Is This?

We're proposing a **building-block approach** to Dataverse development. Instead of one large monolithic application, we create **reusable components** that can be mixed and matched like LEGO bricks.

---

## The Big Picture

```
┌────────────────────────────────────────────────────────────────┐
│                                                                │
│   🔍 Search    📁 File Browser    📝 Metadata    ⬆️ Uploader   │
│                                                                │
│        ▼              ▼               ▼              ▼         │
│   ┌────────────────────────────────────────────────────────┐   │
│   │              Shared Services Layer                     │   │
│   │         (Search, Storage, AI, Metadata)                │   │
│   └────────────────────────────────────────────────────────┘   │
│                              ▼                                 │
│   ┌────────────────────────────────────────────────────────┐   │
│   │              Data Storage                              │   │
│   │       (Files, Databases, Search Index)                 │   │
│   └────────────────────────────────────────────────────────┘   │
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

---

## Key Benefits

| Benefit | What It Means |
|---------|---------------|
| **🚀 Faster Development** | Build new tools by combining existing components instead of starting from scratch |
| **🔄 Flexibility** | Swap out parts (e.g., use AI-powered search) without rebuilding everything |
| **📦 Lightweight Options** | Deploy only what you need - from full Dataverse to minimal "Dataverse Light" |
| **🛠️ Easier Maintenance** | Smaller, focused modules are easier to update and fix |
| **🌍 Community Growth** | Partners can contribute individual components |
| **🔮 Future-Proof** | Adopt new technologies gradually, not all at once |

---

## The Building Blocks

### User-Facing Components (What People See)

| Component | Status | Description |
|-----------|--------|-------------|
| **File Uploader** | ✅ Ready | Drag-and-drop file uploads with progress tracking |
| **Search & Discovery** | ✅ In SPA | Find datasets with filters and facets |
| **File Browser** | 🚧 Planned | Navigate files in folders like a file manager |
| **Metadata Editors** | ✅ In SPA | Edit dataset and file information |
| **DDI-CDI Generator** | ✅ Ready | Auto-generate standardized metadata for data files |

### Behind-the-Scenes Services

| Service | Purpose |
|---------|---------|
| **Search Service** | Find content (can be standard or AI-enhanced) |
| **Storage Service** | Store and retrieve files (S3, Azure, etc.) |
| **Metadata Service** | Manage dataset descriptions and validation |
| **AI Service** | Smart features like semantic search and auto-suggestions |

---

## Real-World Applications

### 1. Building Custom Tools Faster

**Before:** Build everything from scratch  
**After:** Combine pre-built components

*Example:* The rdm-integration tool (already in production) demonstrates this - it combines file synchronization with metadata generation.

### 2. "Dataverse Light"

A minimal deployment for institutions that need:
- Basic search and discovery
- File upload and download
- Essential metadata editing

*Perfect for:* Smaller organizations, departmental repositories, or specialized use cases.

### 3. AI-Enhanced Search

Swap the standard search for AI-powered alternatives:
- Natural language queries ("find datasets about climate change in Europe")
- Similar dataset recommendations
- Automatic metadata suggestions

---

## Implementation Timeline

| Phase | Timeline | Focus |
|-------|----------|-------|
| **Phase 1** | Now | File Uploader (complete), fix remaining issues |
| **Phase 2** | Q1 2025 | File Tree Browser |
| **Phase 3** | Q2 2025 | Extract search and metadata editors as standalone |
| **Phase 4** | Q3 2025 | Swappable search service |
| **Phase 5** | Q4 2025 | AI enhancements |
| **Phase 6** | 2026 | Dataverse Light release |

---

## What We've Already Built

| Project | What It Does |
|---------|--------------|
| **DVWebloader V2** | Proof of concept - standalone uploader embedded in Dataverse |
| **rdm-integration** | Production tool for file sync and DDI-CDI metadata |
| **cdi-viewer** | JSON-LD metadata viewer with validation |
| **dataverse-frontend** | Modern React-based interface (components to extract) |

---

## When Is This Right For You?

### ✅ Good Fit

- Large or growing Dataverse installations
- Need for custom external tools
- Interest in AI-enhanced features
- Multiple teams contributing to development
- Long-term technology modernization goals

### ⚠️ May Be Overkill

- Small, simple installations
- Standard features are sufficient
- Very limited development resources

---

## Questions?

For technical details, implementation specifics, and code examples, see the full [Technical Architecture Document](new_architecture.md).

---

**Document Version:** 1.0  
**Created:** December 2025  
**Based on:** new_architecture.md v1.3  
**Audience:** Management, stakeholders, non-technical readers
