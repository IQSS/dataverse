# Dataverse Modular Architecture - Vision Document

## Executive Summary

This document proposes a **component-based development approach** for Dataverse frontend contributions. The core idea: build new UI features as **standalone components** that work both within the Dataverse SPA *and* as embeddable iframes in JSF pages or external tools.

**This is not a rewrite proposal.** It's a way of structuring frontend work that:

- **Bridges the transition** - New components work in both JSF (current) and SPA (future)
- **Enables reuse** - Same component powers the SPA, external tools, and previewers
- **Reduces duplication** - No need to build JSF and React versions of the same feature
- **Proves the pattern** - DVWebloader V2 demonstrates this works in practice

> **What's actually being proposed:** Continue developing new frontend features (like the planned File Tree Browser) using the standalone component pattern proven by DVWebloader V2.

### Scope & Intent

| Category | Examples | Status |
|----------|----------|--------|
| **Core Proposal** | Standalone component pattern, iframe embedding, API-first design | ✅ Proven (DVWebloader V2) |
| **Planned Work** | File Tree Browser as standalone component | 🚧 Next contribution |

This document serves as:
1. **Justification** for the architectural approach used in existing contributions (DVWebloader V2)
2. **Proposal** to continue this pattern for future frontend work

### Related Projects

This architecture vision builds upon existing projects in the Dataverse ecosystem:

| Project | Description | Repository |
|---------|-------------|------------|
| **Dataverse Frontend** | New React-based SPA for Dataverse | [IQSS/dataverse-frontend](https://github.com/IQSS/dataverse-frontend) |
| **DVWebloader** | Web-based folder uploader for Dataverse | [gdcc/dvwebloader](https://github.com/gdcc/dvwebloader) |
| **rdm-integration** | External tool for file synchronization & DDI-CDI generation | [libis/rdm-integration](https://github.com/libis/rdm-integration) |
| **cdi-viewer** | Generic JSON-LD viewer/editor/validator with SHACL support | Local: `../cdi-viewer` |
| **rdm-build** | Docker build scripts for RDM infrastructure | Local: `../rdm-build` |
| **dataverse-client-javascript** | Official JavaScript/TypeScript client library | [IQSS/dataverse-client-javascript](https://github.com/IQSS/dataverse-client-javascript) |

---

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [Design Principles](#2-design-principles)
   - [Component Independence](#21-component-independence)
   - [Service Abstraction](#22-service-abstraction)
   - [Progressive Enhancement](#23-progressive-enhancement)
   - [Technology Agnostic](#24-technology-agnostic)
   - [Alignment with Dataverse Frontend Vision](#25-alignment-with-dataverse-frontend-vision)
   - [API Strategy](#26-api-strategy)
3. [Standalone UI Components](#3-standalone-ui-components)
   - [File Uploader](#31-file-uploader-implemented)
   - [File Tree Browser](#32-file-tree-browser-planned)
   - [Search & Discovery](#33-search--discovery-exists-in-spa)
   - [File Metadata Editor](#34-file-metadata-editor-exists-in-spa)
   - [Dataset Metadata Editor](#35-dataset-metadata-editor-exists-in-spa)
4. [Backend Architecture](#4-backend-architecture)
5. [Integration Patterns](#5-integration-patterns)
   - [Embedding in Dataverse (iframe)](#51-embedding-in-dataverse-iframe)
   - [External Tools](#52-external-tools)
6. [Component Communication](#6-component-communication)
7. [What This Enables](#7-what-this-enables)
8. [Anticipated Questions](#8-anticipated-questions)
9. [Benefits & Trade-offs](#9-benefits--trade-offs)

**Appendices:**
- [Appendix A: Component API Reference](#appendix-a-component-api-reference)
- [Appendix B: Related Project Documentation](#appendix-b-related-project-documentation)
- [Appendix C: Quick Start for Developers](#appendix-c-quick-start-for-developers)
- [Appendix D: Original Vision (Structured)](#appendix-d-original-vision-structured)

---

## 1. Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         STANDALONE UI COMPONENTS                            │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────────────────┐│
│  │   Search    │ │  File Tree  │ │  Metadata   │ │    File Uploader        ││
│  │  Component  │ │   Browser   │ │   Editor    │ │     (DVWebloader)       ││
│  └──────┬──────┘ └──────┬──────┘ └──────┬──────┘ └───────────┬─────────────┘│
│         │               │               │                    │              │
│         └───────────────┴───────────────┴────────────────────┘              │
│                                    │                                        │
│                          Dataverse Native API                               │
│                       (dataverse-client-javascript)                         │
└────────────────────────────────────┼────────────────────────────────────────┘
                                     │
┌────────────────────────────────────┼────────────────────────────────────────┐
│                         DATAVERSE BACKEND                                   │
│                          (Java / Payara)                                    │
│  ┌─────────────────────────────────▼──────────────────────────────────────┐ │
│  │                        Native API (/api/v1/*)                          │ │
│  │   Collections │ Datasets │ Files │ Search │ Users │ Metadata           │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                    │                                        │
│  ┌─────────────┐  ┌─────────────┐  │  ┌─────────────┐  ┌─────────────┐      │
│  │    Solr     │  │ PostgreSQL  │◄─┴─►│  S3/MinIO   │  │   Payara    │      │
│  │  (Search)   │  │   (Data)    │     │  (Files)    │  │  (Runtime)  │      │
│  └─────────────┘  └─────────────┘     └─────────────┘  └─────────────┘      │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Key Concepts

1. **Standalone UI Components** - React components that work in SPA mode AND as embeddable iframes
2. **Dataverse Native API** - The existing REST API serves as the communication layer
3. **dataverse-client-javascript** - Official TypeScript client for API access
4. **Existing Infrastructure** - Solr, PostgreSQL, S3 - no changes required

---

## 2. Design Principles

### 2.1 Component Independence

Each UI component is:
- **Self-contained** - Has its own build, can run standalone
- **Embeddable** - Works in iframe with postMessage communication
- **Configurable** - Accepts configuration via URL params, props, or self-configures via API calls

> ⚠️ **Open Question (for Tech Hours):** The configuration approach needs resolution. URL params work for simple cases, but complex components may need a cleaner contract. The current approach assumes components can self-configure via API calls (e.g., fetching dataset metadata after receiving just a dataset PID). This needs confirmation, and current PRs may need updates based on the decision.

- **Stateless** - Receives data via API, doesn't maintain global state

### 2.2 API-First Design

Components communicate through the Dataverse Native API:
- **Single source of truth** - All data access via `/api/v1/*`
- **Well-documented** - Existing API documentation applies
- **TypeScript client** - `dataverse-client-javascript` provides typed access
- **Consistent** - Same API whether component runs in SPA, iframe, or external tool

### 2.3 Progressive Enhancement

- Core Dataverse functionality remains intact
- New components enhance, don't replace
- Use new React components without migrating to the SPA (for installations that postpone that step)
- Feature flags control which components are active

### 2.4 Technology Agnostic

- UI components: React (TypeScript)
- External tools: Any language (Python, Go, Java, Angular, etc.)
- Communication: REST + WebSocket for real-time

### 2.5 Alignment with Dataverse Frontend Vision

This architecture aligns with the goals of the [Dataverse Frontend](https://github.com/IQSS/dataverse-frontend) project:

> **The goals of Dataverse Frontend:**
> - Modernize the application
> - Separate the frontend and backend logic, transition away from Monolithic Architecture
> - Reimagine the current Dataverse backend as a headless API-first instance
> - The Dataverse Frontend becomes a stand-alone SPA (Single Page Application)
> - Modularize the UI to allow third-party extension of the base project
> - Increase cadence of development, decrease time between release cycles
> — [dataverse-frontend README](https://github.com/IQSS/dataverse-frontend/blob/develop/README.md)

### 2.6 API Strategy

- **Dataverse Native API** (`/api/v1/*`) is the single source of truth for all data operations
- **dataverse-client-javascript** provides the TypeScript client for React components
- Components use the same API whether running in SPA, iframe, or external tool
- New API endpoints → new use cases in client library → available to all components

---

## 3. Standalone UI Components

### Component Status Summary

| Component | SPA Status | Standalone Status | Location in dataverse-frontend |
|-----------|------------|-------------------|--------------------------------|
| **File Uploader** | ✅ Complete | ✅ Complete | `src/standalone-uploader/` |
| **File Tree Browser** | 🚧 Planned | 🚧 Planned | `src/sections/dataset/dataset-files/files-tree/` (proposed) |
| **Search & Discovery** | ✅ Complete | — (extractable) | `src/sections/collection/collection-items-panel/` |
| **File Metadata Editor** | ✅ Complete | — (extractable) | `src/sections/edit-file-metadata/` |
| **Dataset Metadata Editor** | ✅ Complete | — (extractable) | `src/sections/edit-dataset-metadata/` |

**Key Insight:** Most components already exist in the SPA. The standalone extraction pattern (demonstrated by File Uploader) could be applied to other components if needed for external tools or other use cases.

### 3.1 File Uploader (IMPLEMENTED)

**Status:** ✅ Working (DVWebloader V2)

> DVWebloader V2 reuses the file upload components from the new Dataverse SPA ([dataverse-frontend](https://github.com/IQSS/dataverse-frontend)) and the official JavaScript client library ([dataverse-client-javascript](https://github.com/IQSS/dataverse-client-javascript)). This ensures consistency with the main Dataverse application and reduces code duplication.
> — [DVWebloader README](https://github.com/gdcc/dvwebloader/blob/main/README.md)

**Modes:**
- SPA mode - Full React Router integration
- Popup mode - Opens in new window (legacy DVWebloader behavior)
- Embedded mode - iframe in JSF pages (new)

**Features:**
- Drag & drop files and folders
- Progress tracking per file
- S3 direct upload support
- MD5 checksum calculation (optional)
- Retry mechanism for failed uploads
- i18n support

**Configuration Options:**

| Option | Default | Description |
|--------|---------|-------------|
| `siteUrl` | (required) | The Dataverse installation URL |
| `datasetPid` | (required) | The dataset's persistent identifier |
| `key` | (required) | User's API token for authentication |
| `dvLocale` | `en` | Language locale (e.g., `en`, `de`, `fr`) |
| `useS3Tagging` | `true` | Set to `false` for S3-compatible storage (e.g., MinIO) |
| `maxRetries` | `3` | Maximum retries for multipart upload parts |
| `uploadTimeoutMs` | `0` | Timeout in ms (`0` = unlimited) |
| `disableMD5Checksum` | `false` | Set to `true` to skip checksum calculation |

**Project References:**

| Location | Purpose |
|----------|--------|
| [`dataverse-frontend/src/standalone-uploader/`](https://github.com/IQSS/dataverse-frontend) | React component source |
| [`rdm-build/images/previewers/dvwebloader-v2/`](../rdm-build/images/previewers/dvwebloader-v2/) | Build configuration & output |
| [`dvwebloader/src/dvwebloaderV2.html`](../dvwebloader/src/dvwebloaderV2.html) | Standalone HTML wrapper |
| [`dataverse/embedded_dvwebloader.md`](embedded_dvwebloader.md) | Embedded mode implementation status |

**Key Files:**
```
dataverse-frontend/src/standalone-uploader/
├── index.tsx                        # Entry point
├── StandaloneFileUploaderPanel.tsx  # Standalone wrapper component
└── config.ts                        # URL param parsing

rdm-build/images/previewers/dvwebloader-v2/
├── vite.config.ts                   # Build configuration
├── embeddedDvWebloader.html         # Minimal HTML for iframe embedding
└── dist/dvwebloader-v2.js           # Bundled output (~1.6MB, ~420KB gzipped)
```

**Bundle Architecture:**
- React 18
- React Bootstrap components
- dataverse-client-javascript (for API communication)
- i18next (for internationalization)
- All CSS is inlined into the JavaScript bundle

**API Integration:**

The File Uploader uses `dataverse-client-javascript` for all API communication.

**Lessons Learned:**
- iframe isolation solves CSS conflicts with JSF pages
- postMessage for height sync works well
- API token in URL (iframe) is acceptable for same-origin embedding
- Need to handle embedded vs popup mode differently in React component

---

### 3.2 File Tree Browser (PLANNED - builds on existing)

**Status:** 🚧 Planned (will build on existing FilesTable components)

**Purpose:** Browse, select, and download files/folders in a dataset with hierarchical tree view

**Existing Related Components in `dataverse-frontend`:**

| Component | Location | Reusable For |
|-----------|----------|-------------|
| `FilesTable` | [`src/sections/dataset/dataset-files/files-table/FilesTable.tsx`](https://github.com/IQSS/dataverse-frontend/blob/develop/src/sections/dataset/dataset-files/files-table/FilesTable.tsx) | Row rendering, selection, actions |
| `FilesTableHeader` | [`files-table/FilesTableHeader.tsx`](https://github.com/IQSS/dataverse-frontend/blob/develop/src/sections/dataset/dataset-files/files-table/FilesTableHeader.tsx) | Column definitions, sorting |
| `FilesTableBody` | [`files-table/FilesTableBody.tsx`](https://github.com/IQSS/dataverse-frontend/blob/develop/src/sections/dataset/dataset-files/files-table/FilesTableBody.tsx) | Row rendering with file info |
| `RowSelectionMessage` | [`files-table/row-selection/RowSelectionMessage.tsx`](https://github.com/IQSS/dataverse-frontend/blob/develop/src/sections/dataset/dataset-files/files-table/row-selection/RowSelectionMessage.tsx) | "Select all" / clear selection UI |
| `FileCriteriaForm` | [`dataset-files/file-criteria-form/`](https://github.com/IQSS/dataverse-frontend/tree/develop/src/sections/dataset/dataset-files/file-criteria-form) | Search, sort, filter controls |

**Current FilesTable (flat list):**
```tsx
// From FilesTable.tsx - existing implementation
export function FilesTable({ files, fileRepository, ... }) {
  const { table, fileSelection, selectAllFiles, clearFileSelection } = 
    useFilesTable(files, paginationInfo, fileRepository, datasetRepository)

  return (
    <>
      <RowSelectionMessage
        fileSelection={fileSelection}
        selectAllRows={selectAllFiles}
        totalFilesCount={paginationInfo.totalItems}
        clearRowSelection={clearFileSelection}
      />
      <ZipDownloadLimitMessage ... />
      <Table>
        <FilesTableHeader headers={table.getHeaderGroups()} />
        <FilesTableBody rows={table.getRowModel().rows} />
      </Table>
    </>
  )
}
```

**Proposed Tree View Extension:**
```
┌─────────────────────────────────────────────────────────┐
│ 📁 Dataset: Climate Data 2024    [⬇️ Download Selected] │
├─────────────────────────────────────────────────────────┤
│ ☐ 📁 raw-data/                               [▼]        │
│   ☑ 📄 measurements-jan.csv        1.2 MB               │
│   ☑ 📄 measurements-feb.csv        1.1 MB               │
│   ☐ 📄 measurements-mar.csv        1.3 MB               │
│ ☑ 📁 processed/                              [▼]        │
│   ☑ 📄 analysis.ipynb              245 KB               │
│   ☑ 📄 results.json                12 KB                │
│ ☐ 📁 documentation/                          [▶]        │
│                                                         │
│ Selected: 5 files (3.6 MB)                              │
└─────────────────────────────────────────────────────────┘
```

**Key Behaviors to Add:**
- **Hierarchical display** - Folders with expand/collapse
- **Lazy loading** - Load folder contents on expand
- **Tri-state checkboxes** - Folder: all/some/none selected
- **Keyboard navigation** - Arrow keys, space to toggle
- **Virtual scrolling** - Handle 10,000+ files efficiently

**Note:** The JSF application already has a tree view (`<p:tree>` in `filesFragment.xhtml`). The new component will provide a modern React alternative.

**Implementation Plan:**
1. Add to `dataverse-frontend` as `src/sections/dataset/dataset-files/files-tree/`
2. Create standalone build option (like uploader)
3. Use same selection/download infrastructure as FilesTable

---

### 3.3 Search & Discovery (EXISTS IN SPA)

**Status:** ✅ Working in SPA

The search components (`CollectionItemsPanel`, `FilterPanel`, `SearchInput`, `ItemsList`) already exist in `dataverse-frontend`. Could be extracted as standalone following the File Uploader pattern if needed for external tools.

---

### 3.4 File Metadata Editor (EXISTS IN SPA)

**Status:** ✅ Working in SPA

The file metadata editing components (`EditFileMetadata`, `EditFilesList`) already exist in `dataverse-frontend`. Could be extracted as standalone following the File Uploader pattern if needed for external tools.

---

### 3.5 Dataset Metadata Editor (EXISTS IN SPA)

**Status:** ✅ Working in SPA

The dataset metadata editing components (`EditDatasetMetadata`, `DatasetMetadataForm`, `CreateDataset`) already exist in `dataverse-frontend`. Could be extracted as standalone following the File Uploader pattern if needed for external tools.

---

## 4. Backend Architecture

> **Key Point:** This proposal does not change the Dataverse backend. All standalone components communicate with the existing Dataverse Native API.

### Current Architecture (No Changes Required)

The existing Dataverse backend already provides everything needed:

| Capability | Provided By | API |
|------------|-------------|-----|
| **Search** | Solr | `/api/search` |
| **File Storage** | S3/MinIO (or local) | `/api/files/`, Direct Upload API |
| **Metadata** | PostgreSQL | `/api/datasets/`, `/api/files/` |
| **Authentication** | Dataverse auth | API tokens, sessions |
| **Permissions** | Role-based access | Per-collection, per-dataset |

### Why This Matters

Because standalone components use the Native API:

1. **No backend changes** - Components work with any Dataverse installation
2. **Version compatibility** - API versioning protects against breaking changes
3. **Same security model** - Existing authentication and authorization apply
4. **Existing documentation** - Native API is already well-documented

### Future Possibility: Alternative Backends

> 💭 **Theoretical:** Because components communicate via the Native API contract, it would be *possible* to create alternative implementations (e.g., an AI-enhanced search service). This is not planned - it simply becomes possible due to the decoupled architecture.

---

## 5. Integration Patterns

### 5.1 Embedding in Dataverse (iframe)

**Current Implementation (DVWebloader V2):**

```
┌─────────────────────────────────────────────────────────┐
│  Dataverse JSF Page                                     │
│  ┌───────────────────────────────────────────────────┐  │
│  │  <iframe src="component.html?params...">          │  │
│  │    ┌─────────────────────────────────────────┐    │  │
│  │    │  React Standalone Component             │    │  │
│  │    │  - Isolated styles                      │    │  │
│  │    │  - Own React instance                   │    │  │
│  │    │  - Communicates via postMessage         │    │  │
│  │    └─────────────────────────────────────────┘    │  │
│  └───────────────────────────────────────────────────┘  │
│                                                         │
│  <script>                                               │
│    window.addEventListener('message', (e) => {          │
│      if (e.data.type === 'resize') { ... }              │
│      if (e.data.type === 'navigate') { ... }            │
│    });                                                  │
│  </script>                                              │
└─────────────────────────────────────────────────────────┘
```

**Communication Protocol:**
```typescript
// Child → Parent messages
interface ResizeMessage {
  type: 'resize';
  height: number;
}

interface NavigateMessage {
  type: 'navigate';
  url: string;
  target: 'parent' | 'self';
}

interface ActionCompleteMessage {
  type: 'action-complete';
  action: 'upload' | 'save' | 'delete';
  data?: any;
}

// Parent → Child messages
interface ConfigMessage {
  type: 'config';
  config: ComponentConfig;
}

interface RefreshMessage {
  type: 'refresh';
}
```

---

### 5.2 External Tools

#### 5.2.1 Example: rdm-integration

The [rdm-integration](https://github.com/libis/rdm-integration) project is an external tool for data synchronization from various repositories (GitHub, GitLab, IRODS, OneDrive, etc.) into Dataverse.

**Relevance to this proposal:** The development of rdm-integration would have been significantly easier if standalone UI components had been available. Instead of building custom Angular components for file browsing, metadata editing, and uploads, we could have embedded the existing React components from dataverse-frontend.

This is a key motivation for the standalone component approach: **accelerate external tool development** by providing reusable UI building blocks.

---

#### 5.2.2 Proposed: Composable External Tools

**Example: File Management Tool (using multiple standalone components)**

```
┌─────────────────────────────────────────────────────────┐
│  External Tool: Advanced File Manager                   │
├─────────────────────────────────────────────────────────┤
│  ┌─────────────────────┐  ┌─────────────────────────┐   │
│  │   File Tree         │  │   File Metadata         │   │
│  │   Browser           │  │   Editor                │   │
│  │   Component         │  │   Component             │   │
│  │                     │  │                         │   │
│  │   📁 data/          │  │   Name: results.csv     │   │
│  │   ├─ 📄 results.csv │◀─│   Size: 1.2 MB          │   │
│  │   └─ 📄 readme.md   │  │   Type: text/csv        │   │
│  │                     │  │   Description: [____]   │   │
│  │   📁 docs/          │  │                         │   │
│  └─────────────────────┘  └─────────────────────────┘   │
│                                                         │
│  ┌─────────────────────────────────────────────────────┐│
│  │           File Uploader Component                   ││
│  │   [+ Select files] [Drop files here]                ││
│  └─────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────┘
```

**Tool Manifest:**
```json
{
  "name": "Advanced File Manager",
  "scope": "dataset",
  "components": [
    { "type": "file-tree", "position": "left" },
    { "type": "file-metadata", "position": "right" },
    { "type": "file-uploader", "position": "bottom" }
  ],
  "permissions": ["read", "write", "delete"]
}
```

---

## 6. Component Communication

### 6.1 Same-Origin Components (SPA mode)

```typescript
// Shared state via React Context or Redux
import { useDatasetContext } from '@/contexts/DatasetContext';

function FileTreeBrowser() {
  const { dataset, selectedFiles, setSelectedFiles } = useDatasetContext();
  // ...
}

function FileMetadataPanel() {
  const { selectedFiles } = useDatasetContext();
  const selectedFile = selectedFiles[0];
  // Show metadata for selected file
}
```

### 6.2 Cross-Origin Components (iframe mode)

```typescript
// Parent page
class ComponentOrchestrator {
  private components: Map<string, HTMLIFrameElement> = new Map();
  
  registerComponent(name: string, iframe: HTMLIFrameElement) {
    this.components.set(name, iframe);
    
    window.addEventListener('message', (e) => {
      if (e.source === iframe.contentWindow) {
        this.handleMessage(name, e.data);
      }
    });
  }
  
  handleMessage(component: string, message: ComponentMessage) {
    switch (message.type) {
      case 'file-selected':
        // Notify metadata panel to show this file
        this.sendToComponent('metadata-panel', {
          type: 'show-file',
          fileId: message.fileId
        });
        break;
      // ...
    }
  }
  
  sendToComponent(name: string, message: any) {
    const iframe = this.components.get(name);
    iframe?.contentWindow?.postMessage(message, '*');
  }
}
```

### 6.3 Event Bus Pattern (for complex orchestration)

```typescript
// Shared event bus (works across iframes via postMessage)
interface EventBus {
  emit(event: string, data: any): void;
  on(event: string, handler: (data: any) => void): void;
  off(event: string, handler: (data: any) => void): void;
}

// Events
type Events = {
  'file:selected': { fileId: string };
  'file:uploaded': { fileId: string; name: string };
  'metadata:changed': { fileId: string; metadata: object };
  'search:executed': { query: string; results: SearchResult[] };
};
```

---

## 7. What This Enables

> **Note:** This is not a project roadmap with committed timelines. It describes what has been done, what is currently planned, and what becomes possible with this architecture.

### Already Proven ✅

The standalone component pattern is working in production:

- [x] **File Uploader** - Standalone component in `src/standalone-uploader/`
- [x] **iframe embedding** - postMessage communication protocol
- [x] **Feature flags** - `EMBED_WEBLOADER_V2` controls activation in Dataverse
- [x] **SPA components** - Search, file metadata, dataset metadata editors exist

### Currently Planned 🚧

**File Tree Browser** - The next contribution following this pattern:

- [ ] Design tree component API (building on existing `FilesTable`)
- [ ] Implement folder expand/collapse with lazy loading
- [ ] Add virtual scrolling for large datasets (10k+ files)
- [ ] Create standalone build option from the start
- [ ] Enable embedding in JSF pages (like DVWebloader V2)

**Why build it this way?** Instead of creating a JSF tree component that will be retired when the SPA is fully adopted, we build a React component that works in both contexts. One implementation, multiple deployment targets.

### Future Possibilities 💡

These become possible with the component architecture but are **not currently planned**:

**Component Extraction:**
> Other SPA components (search, metadata editors) could be extracted as standalone bundles following the File Uploader pattern. This would enable their use in external tools and previewers.

- [ ] Search Component - Extract `CollectionItemsPanel` + `FilterPanel`
- [ ] File Metadata Editor - Extract `EditFileMetadata`
- [ ] Dataset Metadata Editor - Extract `EditDatasetMetadata`

---

## 8. Anticipated Questions

This section addresses likely pushback and concerns about the standalone component approach.

### "Why not just wait for the SPA?"

The SPA transition has been gradual, and institutions need features *now*. The standalone component approach bridges this gap:
- New components work in JSF today and SPA tomorrow
- No duplicate implementation effort
- Components don't impede SPA development - they contribute to it

### "Iframes feel old-school"

True, but they solve real problems that modern alternatives don't address as cleanly:
- **CSS isolation** - No conflicts with JSF's PrimeFaces styles
- **React version isolation** - Dataverse JSF doesn't need to care about React 18
- **Security boundary** - Same-origin policy applies naturally
- **Web Components** would be the modern alternative, but they don't solve the React-version isolation problem and have their own complexity

### "This adds complexity"

Fair. The postMessage protocol, standalone builds, and dual-mode components add cognitive overhead. The question is whether that complexity pays for itself:
- **Reuse across JSF, SPA, and external tools** - One implementation, multiple contexts
- **Proven pattern** - DVWebloader V2 demonstrates it works
- **Documented** - This document + working examples reduce learning curve

### "Bundle size is chunky (420KB gzipped)"

For the uploader, this is acceptable because:
- It's loaded on-demand (only when user needs to upload)
- The component is feature-rich (drag-drop, progress, retry, MD5)
- Lazy loading and code splitting can help for frequently-loaded components

---

## 9. Benefits & Trade-offs

### Why This Approach?

The core value proposition is **building once for multiple contexts**:

| Scenario | Without Standalone Components | With Standalone Components |
|----------|------------------------------|---------------------------|
| New feature in JSF | Build JSF component (retired later) | Embed React component via iframe |
| Same feature in SPA | Build React component | Same React component |
| External tool needs it | Build again or copy code | Import as standalone bundle |
| Previewer needs it | Yet another implementation | Same standalone bundle |

**Real example:** DVWebloader V2 is one codebase that works as:
- SPA route in dataverse-frontend
- Popup window (legacy behavior)
- Embedded iframe in JSF pages
- Potential use in external tools

### Benefits

| Benefit | Description |
|---------|-------------|
| **Bridge the Transition** | New features work in JSF today and SPA tomorrow |
| **Reduce Duplication** | One implementation serves multiple deployment contexts |
| **Enable External Tools** | Previewers and tools can reuse core UI components |
| **Maintainability** | Smaller, focused modules are easier to update |
| **Testability** | Components can be tested in isolation via Storybook |
| **Community Contributions** | Contributors can work on components independently |

### Trade-offs

| Trade-off | Mitigation |
|-----------|------------|
| **iframe overhead** | Acceptable for complex components; postMessage is fast |
| **Bundle size** | ~420KB gzipped for uploader; lazy loading helps |
| **Debugging across frames** | Browser DevTools handle this well |
| **Learning curve** | This document + existing examples (DVWebloader V2) |

### When This Approach Applies

**This architecture pattern is valuable when:**
- Building new UI components for dataverse-frontend that could be reused elsewhere
- Creating external tools that need rich, interactive interfaces
- Developing features that should work in both SPA and JSF contexts
- Contributing components that other institutions might want to customize

**Traditional approaches remain appropriate when:**
- Building features tightly coupled to SPA routing/state
- Simple UI that doesn't need external reuse
- Quick prototypes not intended for broad adoption

---

## Appendix A: Component API Reference

### File Uploader

```typescript
interface FileUploaderConfig {
  datasetPid: string;
  siteUrl: string;
  apiToken: string;
  localeCode: string;
  useS3Tagging: boolean;
  maxRetries?: number;
  uploadTimeoutMs?: number;
}

// URL format
// component.html?datasetPid=...&siteUrl=...&apiToken=...&localeCode=...&useS3Tagging=true
```

### File Tree Browser (Proposed)

```typescript
interface FileTreeConfig {
  datasetPid: string;
  siteUrl: string;
  apiToken: string;
  mode: 'view' | 'select' | 'manage';
  allowDownload: boolean;
  allowDelete: boolean;
  onSelect?: (fileIds: string[]) => void;
}
```

### Search Component (Proposed)

```typescript
interface SearchConfig {
  siteUrl: string;
  apiToken?: string;  // Optional for public search
  defaultFilters?: Filters;
  resultView: 'list' | 'grid' | 'table' | 'custom';
  facets: string[];  // Which facets to show
  onResultClick?: (datasetId: string) => void;
}
```

---

## Appendix B: Related Project Documentation

### Core Projects

| Project | Key Documentation | Description |
|---------|-------------------|-------------|
| **dataverse-frontend** | [README](https://github.com/IQSS/dataverse-frontend/blob/develop/README.md) | SPA goals, environments, technology stack |
| | [DEVELOPER_GUIDE](https://github.com/IQSS/dataverse-frontend/blob/develop/DEVELOPER_GUIDE.md) | Architecture design, coding standards |
| | [CHANGELOG](https://github.com/IQSS/dataverse-frontend/blob/develop/CHANGELOG.md) | Version history |
| **dvwebloader** | [README](https://github.com/gdcc/dvwebloader/blob/main/README.md) | V1/V2 versions, configuration, integration |
| | [Wiki](https://github.com/gdcc/dvwebloader/wiki) | Detailed usage documentation |
| **rdm-integration** | [README](../rdm-integration/README.md) | Setup, plugins, architecture overview |
| | [ddi-cdi.md](../rdm-integration/ddi-cdi.md) | DDI-CDI metadata generation guide |
| | [FAST_REDEPLOY.md](../rdm-integration/FAST_REDEPLOY.md) | Development workflow |
| **cdi-viewer** | [ARCHITECTURE.md](../cdi-viewer/ARCHITECTURE.md) | JSON-LD viewer architecture, patterns |
| | [GENERIC_USAGE.md](../cdi-viewer/docs/GENERIC_USAGE.md) | Generic JSON-LD usage guide |

### Build Infrastructure

| Project | Key Documentation | Description |
|---------|-------------------|-------------|
| **rdm-build** | [README](../rdm-build/README.md) | Docker image build scripts |
| | [images/previewers/README](../rdm-build/images/previewers/README.md) | Previewers image (includes DVWebloader) |
| | [images/previewers/dvwebloader-v2/README](../rdm-build/images/previewers/dvwebloader-v2/README.md) | V2 build instructions |

### Implementation Status Documents

| Document | Location | Description |
|----------|----------|-------------|
| **Embedded DVWebloader V2** | [`embedded_dvwebloader.md`](embedded_dvwebloader.md) | Current implementation status, known issues, next steps |
| **This Document** | [`new_architecture.md`](new_architecture.md) | Architecture vision and roadmap |

### External Resources

- [Dataverse Guides](https://guides.dataverse.org/en/latest/)
- [Dataverse API Documentation](http://guides.dataverse.org/en/latest/api/index.html)
- [GDCC (Global Dataverse Community Consortium)](https://www.gdcc.io/)
- [DDI-CDI Specification](https://ddialliance.org/Specification/DDI-CDI/)
- [Dataverse Frontend Chromatic (Storybook)](https://www.chromatic.com/builds?appId=646f68aa9beb01b35c599acd)

---

## Appendix C: Quick Start for Developers

### Running the Full Stack Locally

The rdm-integration project provides a complete local development environment:

```bash
# Clone required repositories side-by-side
git clone https://github.com/libis/rdm-integration
git clone https://github.com/IQSS/dataverse
git clone https://github.com/IQSS/dataverse-frontend

# Start the full stack (Dataverse + Keycloak + MinIO + rdm-integration)
cd rdm-integration
make up

# Access points:
# - http://localhost:4180      - rdm-integration app
# - http://localhost:8080      - Dataverse UI
# - http://localhost:8090      - Keycloak admin
# Default login: admin / admin
```

### Building DVWebloader V2

```bash
cd rdm-build/images/previewers/dvwebloader-v2
npm install
npm run build
# Output: dist/dvwebloader-v2.js
```

### Running dataverse-frontend Development Server

```bash
cd dataverse-frontend
npm install
cd packages/design-system && npm run build && cd ../..
npm start
# Access: http://localhost:5173
```

---

## Appendix D: Original Vision (Structured)

This appendix preserves the original requirements and vision that led to this architecture document, structured for clarity.

### Core Concept

Extract **standalone UI components from the SPA** that can work both within the Dataverse SPA and as **embeddable iframes** in JSF pages or external tools.

### Standalone Components Envisioned

1. **Search Component**
   - Includes: facets, search box, list of results
   - Custom result views as separate standalone components

2. **File Tree Browser**
   - Hierarchical tree view with file/folder selection
   - Lazy loading and collapsible folders for efficiency
   - Multi-select: whole folders, deselect individual files, select specific files
   - Download functionality for selected files/folders
   - Designed from scratch to be part of SPA with standalone build option

3. **File Uploader** (DVWebloader V2)
   - Already implemented as proof of concept
   - Embedded in JSF pages via iframe
   - Uses standalone build from dataverse-frontend

4. **File Metadata Viewer/Editor**
   - View and edit file-level metadata
   - Extracted from existing SPA components

5. **Dataset Metadata Viewer/Editor**
   - View and edit dataset-level metadata
   - For JSON-LD editing/validation pattern, see [cdi-viewer](../cdi-viewer/ARCHITECTURE.md)

### Composition Patterns

1. **External Tool Development**
   - Combine: Tree Browser + Uploader + File Metadata Editor + Dataset Metadata Editor
   - Example: rdm-integration project demonstrates this pattern
   - Accelerates external tool development on dataset level

### Architectural Principles

- **Flexible** - Components can be used independently or together
- **Modularized** - Each component is self-contained
- **Modernized** - React/TypeScript, modern build tools
- **Moldable** - Easy to customize for different needs
- **Loosely coupled** - Components communicate via defined APIs
- **Maintainable** - Smaller, focused modules

### Reference Implementations

| Project | Demonstrates |
|---------|--------------|
| **DVWebloader V2** | Standalone component extraction, iframe embedding |
| **rdm-integration** | External tool with file sync, metadata editing, DDI-CDI generation |
| **cdi-viewer** | Browser-based JSON-LD viewer/editor with SHACL validation |
| **dataverse-frontend** | SPA with components to extract |

---

**Document Version:** 1.4
**Created:** December 2025
**Updated:** December 2025
**Authors:** Eryk Kulikowski, with Claude Opus 4.5 (preview) via GitHub Copilot for VS Code
**Status:** Proposal
