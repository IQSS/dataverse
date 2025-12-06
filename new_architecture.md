# Dataverse Modular Architecture - Vision Document

## Executive Summary

This document proposes a **modular, microservices-based architecture** for Dataverse that extracts core functionality into **standalone UI components** backed by **swappable microservices**. This approach enables:

- **Rapid external tool development** - Compose tools from pre-built components
- **"Dataverse Light"** - Lightweight deployments with essential features
- **Technology flexibility** - Swap backends (AI search, different storage, etc.)
- **Maintainability** - Loosely coupled, independently deployable modules
- **Future-proofing** - Easy adoption of new technologies

### Related Projects

This architecture vision builds upon existing projects in the Dataverse ecosystem:

| Project | Description | Repository |
|---------|-------------|------------|
| **Dataverse Frontend** | New React-based SPA for Dataverse | [IQSS/dataverse-frontend](https://github.com/IQSS/dataverse-frontend) |
| **DVWebloader** | Web-based folder uploader for Dataverse | [gdcc/dvwebloader](https://github.com/gdcc/dvwebloader) |
| **rdm-integration** | External tool for file synchronization & DDI-CDI generation | [libis/rdm-integration](https://github.com/libis/rdm-integration) |
| **rdm-build** | Docker build scripts for RDM infrastructure | Local: `../rdm-build` |
| **dataverse-client-javascript** | Official JavaScript/TypeScript client library | [IQSS/dataverse-client-javascript](https://github.com/IQSS/dataverse-client-javascript) |

---

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [Design Principles](#2-design-principles)
3. [Standalone UI Components](#3-standalone-ui-components)
   - [File Uploader](#31-file-uploader-implemented)
   - [File Tree Browser](#32-file-tree-browser-planned)
   - [Search & Discovery](#33-search--discovery-exists-in-spa)
   - [File Metadata Editor](#34-file-metadata-editor-exists-in-spa)
   - [DDI-CDI Metadata Generator](#35-ddi-cdi-metadata-generator-existing)
   - [Dataset Metadata Editor](#36-dataset-metadata-editor-exists-in-spa)
4. [Backend Microservices](#4-backend-microservices)
   - [Search Service](#41-search-service)
   - [Storage Service](#42-storage-service)
   - [Metadata Service](#43-metadata-service)
   - [AI Enhancement Service](#44-ai-enhancement-service)
5. [Integration Patterns](#5-integration-patterns)
   - [Embedding in Dataverse (iframe)](#51-embedding-in-dataverse-iframe)
   - [External Tools](#52-external-tools)
   - [Dataverse Light](#53-dataverse-light)
6. [Component Communication](#6-component-communication)
7. [Implementation Roadmap](#7-implementation-roadmap)
8. [Benefits & Trade-offs](#8-benefits--trade-offs)

---

## 1. Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           PRESENTATION LAYER                                │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────────────────┐│
│  │   Search    │ │  File Tree  │ │  Metadata   │ │    File Uploader        ││
│  │  Component  │ │   Browser   │ │   Editor    │ │     (DVWebloader)       ││
│  └──────┬──────┘ └──────┬──────┘ └──────┬──────┘ └───────────┬─────────────┘│
│         │               │               │                    │              │
│  ┌──────┴───────────────┴───────────────┴────────────────────┴───────┐      │
│  │                    Unified API Gateway / BFF                      │      │
│  └──────┬───────────────┬───────────────┬─────────────────────┬──────┘      │
└─────────┼───────────────┼───────────────┼─────────────────────┼─────────────┘
          │               │               │                     │
┌─────────┼───────────────┼───────────────┼─────────────────────┼─────────────┐
│         │       MICROSERVICES LAYER     │                     │             │
│  ┌──────▼──────┐ ┌──────▼──────┐ ┌──────▼──────┐ ┌────────────▼────────────┐│
│  │   Search    │ │   Storage   │ │  Metadata   │ │      Upload             ││
│  │   Service   │ │   Service   │ │   Service   │ │      Service            ││
│  └──────┬──────┘ └──────┬──────┘ └──────┬──────┘ └────────────┬────────────┘│
│         │               │               │                     │             │
│  ┌──────┴──────┐        │               │                     │             │
│  │ AI Service  │        │               │                     │             │
│  │   (RAG)     │        │               │                     │             │
│  └─────────────┘        │               │                     │             │
└─────────────────────────┼───────────────┼─────────────────────┼─────────────┘
                          │               │                     │
┌─────────────────────────┼───────────────┼─────────────────────┼─────────────┐
│                  DATA LAYER             │                     │             │
│  ┌──────────────┐ ┌─────▼─────┐ ┌───────▼───────┐ ┌──────────▼────────────┐ │
│  │    Solr      │ │ PostgreSQL│ │  S3/MinIO     │ │   Vector DB           │ │
│  │  (Search)    │ │   (Meta)  │ │  (Files)      │ │   (AI Embeddings)     │ │
│  └──────────────┘ └───────────┘ └───────────────┘ └───────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Key Concepts

1. **Standalone UI Components** - React components that work in SPA mode AND as embeddable iframes
2. **Swappable Microservices** - Dockerized services with defined APIs, easily replaced
3. **API Gateway / BFF** - Backend-for-Frontend that routes to appropriate microservices
4. **Data Layer** - Existing Dataverse infrastructure (Solr, PostgreSQL, S3)

---

## 2. Design Principles

### 2.1 Component Independence

Each UI component is:
- **Self-contained** - Has its own build, can run standalone
- **Embeddable** - Works in iframe with postMessage communication
- **Configurable** - Accepts configuration via URL params or props
- **Stateless** - Receives data via API, doesn't maintain global state

### 2.2 Service Abstraction

Each microservice:
- **Implements a contract** - Defined OpenAPI/GraphQL schema
- **Is replaceable** - Can swap implementations without UI changes
- **Is independently deployable** - Docker container with health checks
- **Handles its own concerns** - Single responsibility

### 2.3 Progressive Enhancement

- Core Dataverse functionality remains intact
- New components enhance, don't replace
- Gradual migration path from JSF to React
- Feature flags control which components are active

### 2.4 Technology Agnostic

- UI components: React (TypeScript)
- Microservices: Any language (Python, Go, Java, Rust)
- Communication: REST/GraphQL + WebSocket for real-time
- Storage: Pluggable (S3, Azure Blob, local filesystem)

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

**Technology Stack (from dataverse-frontend):**
- React 18 with TypeScript
- Bootstrap with custom theming
- Storybook for component library
- Cypress for E2E testing
- i18next for localization

---

## 3. Standalone UI Components

### Component Status Summary

| Component | SPA Status | Standalone Status | Location in dataverse-frontend |
|-----------|------------|-------------------|--------------------------------|
| **File Uploader** | ✅ Complete | ✅ Complete | `src/standalone-uploader/` |
| **File Tree Browser** | 🚧 Planned | 🚧 Planned | `src/sections/dataset/dataset-files/files-tree/` (proposed) |
| **Search & Discovery** | ✅ Complete | 🚧 Needs extraction | `src/sections/collection/collection-items-panel/` |
| **File Metadata Editor** | ✅ Complete | 🚧 Needs extraction | `src/sections/edit-file-metadata/` |
| **Dataset Metadata Editor** | ✅ Complete | 🚧 Needs extraction | `src/sections/edit-dataset-metadata/` |
| **DDI-CDI Generator** | ✅ Complete | ✅ External tool | `rdm-integration` (separate project) |

**Key Insight:** Most components already exist in the SPA. The primary work is extracting them as standalone bundles (following the File Uploader pattern) and adding iframe embedding support.

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

**Status:** ✅ Working in SPA (needs standalone extraction)

**Purpose:** Search datasets with facets, filters, and customizable result views

**Existing Components in `dataverse-frontend`:**

| Component | Location | Purpose |
|-----------|----------|--------|
| `CollectionItemsPanel` | [`src/sections/collection/collection-items-panel/`](https://github.com/IQSS/dataverse-frontend/tree/develop/src/sections/collection/collection-items-panel) | Main search results panel with facets and items list |
| `FilterPanel` | [`collection-items-panel/filter-panel/FilterPanel.tsx`](https://github.com/IQSS/dataverse-frontend/blob/develop/src/sections/collection/collection-items-panel/filter-panel/FilterPanel.tsx) | Facet filters sidebar (type filters, facet filters) |
| `SearchInput` | [`src/sections/homepage/search-input/SearchInput.tsx`](https://github.com/IQSS/dataverse-frontend/blob/develop/src/sections/homepage/search-input/SearchInput.tsx) | Homepage search bar with service selection |
| `ItemsList` | [`collection-items-panel/items-list/ItemsList.tsx`](https://github.com/IQSS/dataverse-frontend/blob/develop/src/sections/collection/collection-items-panel/items-list/ItemsList.tsx) | Results list with infinite scroll |
| `AdvancedSearch` | [`src/sections/advanced-search/AdvancedSearch.tsx`](https://github.com/IQSS/dataverse-frontend/blob/develop/src/sections/advanced-search/AdvancedSearch.tsx) | Advanced search form with metadata block fields |

**Current Architecture (from CollectionItemsPanel.tsx):**
```tsx
// HOW IT WORKS (from actual source code comments):
// This component loads items on the following scenarios:
// 1. When the component mounts
// 2. When the user scrolls to the bottom (infinite scroll)
// 3. When the user submits a search query
// 4. When the user changes item types in the filter panel
// 5. When the user selects or removes a facet filter
// 6. When the user navigates back/forward in the browser
// 7. When the user changes the sort and order of items
```

**Current UI Layout:**
```
┌─────────────────────────────────────────────────────────┐
│                  Homepage / Collection Page              │
├─────────────────────────────────────────────────────────┤
│  SearchInput (with service dropdown for AI search)       │
│  [🔍 Search datasets...                         ] [🔎]  │
├─────────────────────────────────────────────────────────┤
│ ┌───────────────┐ ┌─────────────────────────────────────┐ │
│ │  FilterPanel   │ │            ItemsList                 │ │
│ │               │ │  (infinite scroll, selection)        │ │
│ │ TypeFilters:  │ │  ┌─────────────────────────────────┐  │ │
│ │ ☐ Collections │ │  │ 📊 Climate Dataset 2024        │  │ │
│ │ ☑ Datasets    │ │  │ ⭐ 4.5 | 📥 1.2k downloads     │  │ │
│ │ ☐ Files       │ │  └─────────────────────────────────┘  │ │
│ │               │ │  ┌─────────────────────────────────┐  │ │
│ │ FacetsFilters:│ │  │ 📊 Genomics Study            │  │ │
│ │ Subject       │ │  │ ⭐ 4.8 | 📥 856               │  │ │
│ │ ☑ Medicine   │ │  └─────────────────────────────────┘  │ │
│ │ ☑ Biology    │ │                                       │ │
│ └───────────────┘ └─────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
```

**Swappable Search Service (already supported):**
```tsx
// From SearchInput.tsx - already supports multiple search services!
interface SearchInputProps {
  searchServices: SearchService[]  // Multiple backends supported
}

// User can select which service to use (Solr, AI, etc.)
const [searchServiceSelected, setSearchServiceSelected] = 
  useState<string>(SOLR_SERVICE_NAME)
```

**To Extract as Standalone:**
1. Create `src/standalone-search/` (similar to `standalone-uploader/`)
2. Bundle `CollectionItemsPanel` + `FilterPanel` + `SearchInput`
3. Add URL parameter configuration
4. Use same iframe embedding pattern

---

### 3.4 File Metadata Editor (EXISTS IN SPA)

**Status:** ✅ Working in SPA (needs standalone extraction)

**Purpose:** View and edit file-level metadata with validation

**Existing Components in `dataverse-frontend`:**

| Component | Location | Purpose |
|-----------|----------|--------|
| `EditFileMetadata` | [`src/sections/edit-file-metadata/EditFileMetadata.tsx`](https://github.com/IQSS/dataverse-frontend/blob/develop/src/sections/edit-file-metadata/EditFileMetadata.tsx) | Page wrapper with breadcrumbs and permissions |
| `EditFilesList` | [`src/sections/edit-file-metadata/EditFilesList.tsx`](https://github.com/IQSS/dataverse-frontend/blob/develop/src/sections/edit-file-metadata/EditFilesList.tsx) | Form for editing file metadata (name, description, path) |
| `EditFileMetadataRow` | [`src/sections/edit-file-metadata/EditFileMetadataRow.tsx`](https://github.com/IQSS/dataverse-frontend/blob/develop/src/sections/edit-file-metadata/EditFileMetadataRow.tsx) | Individual file row in the editor |

**Current Implementation:**
```tsx
// From EditFileMetadata.tsx
export const EditFileMetadata = ({ fileId, fileRepository, referrer }) => {
  const { file, isLoading } = useFile(fileRepository, fileId)
  
  return (
    <section>
      <BreadcrumbsGenerator hierarchy={file.hierarchy} ... />
      <header><h1>{t('pageTitle')}</h1></header>
      
      {canEditOwnerDataset ? (
        <Tabs defaultActiveKey="files">
          <Tabs.Tab eventKey="files" title={t('files')}>
            <EditFilesList
              fileRepository={fileRepository}
              editFileMetadataFormData={createEditFileMetadataFormData(file)}
              referrer={referrer}
              datasetPersistentId={file.hierarchy.parent?.persistentId}
            />
          </Tabs.Tab>
        </Tabs>
      ) : (
        <Alert variant="danger">Not allowed</Alert>
      )}
    </section>
  )
}
```

**Form Data Structure:**
```typescript
// From EditFilesList.tsx
export interface FileMetadataFormRow {
  id: number
  fileName: string
  fileType: string
  fileSizeString: string
  checksumValue?: string
  checksumAlgorithm?: FixityAlgorithm
  description: string
  fileDir: string  // Directory path
}

export interface EditFileMetadataFormData {
  files: FileMetadataFormRow[]
}
```

**Integration with Other Components:**
- Accessible from `FilesTable` → Edit menu → "Metadata"
- Accessible from `File` page → `EditFileMenu`
- Uses `react-hook-form` for form handling

**To Extract as Standalone:** Similar pattern to file uploader:
1. Create wrapper in `src/standalone-file-metadata/`
2. Accept fileId/datasetPid via URL params
3. Use postMessage for parent communication

---

### 3.5 DDI-CDI Metadata Generator (EXISTING)

**Status:** ✅ Production Ready (in rdm-integration)

**Purpose:** Automatically generate rich, standardized metadata descriptions for tabular data files following the [DDI-CDI](https://ddialliance.org/Specification/DDI-CDI/) (Data Documentation Initiative - Cross-Domain Integration) specification.

> DDI-CDI is an international standard for describing research data. It provides a common vocabulary and structure for documenting datasets, making it easier to share, preserve, discover, integrate, and validate data.
> — [rdm-integration/ddi-cdi.md](../rdm-integration/ddi-cdi.md)

**Features:**
- **Automatic file analysis** - Examines structure and content of tabular files
- **Metadata inference** - Determines variable types, roles, and statistics
- **SHACL-based validation** - Interactive form with validation constraints
- **RDF/Turtle output** - Standardized metadata format

**Supported File Formats:**
- CSV/TSV files
- Statistical data files (SPSS `.sav`, SAS, Stata `.dta`)
- Files with DDI metadata already ingested by Dataverse

**Architecture:**
```
┌─────────────────────────────────────────────────────────┐
│                   Angular Frontend                      │
├─────────────────────────────────────────────────────────┤
│                   SHACL Form Viewer                     │
│  (JSON-LD display, validation, inline editing)          │
├─────────────────────────────────────────────────────────┤
│                    Python Pipeline                      │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐    │
│  │ File     │→│ Data     │→│ Metadata │→│ CDI      │    │
│  │ Access   │ │ Analysis │ │ Enrichment│ │ Generation│   │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘    │
└─────────────────────────────────────────────────────────┘
```

**Key Concept for Architecture:** The DDI-CDI generator demonstrates how a specialized metadata tool can be built as an external tool that:
1. Runs analysis in background processes (Go backend + Python scripts)
2. Presents results through a SHACL-validated form interface
3. Integrates with Dataverse via the external tools API

**See:** [`rdm-integration/ddi-cdi.md`](../rdm-integration/ddi-cdi.md) for complete documentation.

---

### 3.6 Dataset Metadata Editor (EXISTS IN SPA)

**Status:** ✅ Working in SPA (needs standalone extraction)

**Purpose:** Edit dataset-level metadata with rich validation

**Existing Components in `dataverse-frontend`:**

| Component | Location | Purpose |
|-----------|----------|--------|
| `EditDatasetMetadata` | [`src/sections/edit-dataset-metadata/`](https://github.com/IQSS/dataverse-frontend/tree/develop/src/sections/edit-dataset-metadata) | Dataset metadata editing page |
| `DatasetMetadataForm` | [`src/sections/shared/form/DatasetMetadataForm/`](https://github.com/IQSS/dataverse-frontend/tree/develop/src/sections/shared/form/DatasetMetadataForm) | Reusable metadata form component |
| `CreateDataset` | [`src/sections/create-dataset/`](https://github.com/IQSS/dataverse-frontend/tree/develop/src/sections/create-dataset) | Dataset creation wizard |

**Features Already Implemented:**
- Citation metadata editing
- Controlled vocabulary support (with lookup)
- Multi-language support (i18next)
- Draft/Published state management
- Metadata block rendering based on collection configuration
- Form validation with `react-hook-form`

**Shared Form Infrastructure:**
```tsx
// The DatasetMetadataForm is already designed for reuse
import { DatasetMetadataForm } from '@/sections/shared/form/DatasetMetadataForm'

// Used in both CreateDataset and EditDatasetMetadata
<DatasetMetadataForm
  metadataBlocksInfo={metadataBlocksInfo}
  datasetMetadata={existingMetadata}  // null for create
  onSubmit={handleSubmit}
/>
```

**To Extract as Standalone:**
1. Create `src/standalone-dataset-editor/`
2. Bundle `DatasetMetadataForm` with necessary context
3. Accept datasetPid via URL params
4. Can be used for both create and edit modes

---

## 4. Backend Microservices

### 4.1 Search Service

**Purpose:** Abstract search functionality with swappable implementations

**Docker Deployment:**
```yaml
# docker-compose.yml
services:
  search-service:
    image: dataverse/search-service:${SEARCH_IMPL:-solr}
    environment:
      SEARCH_BACKEND: ${SEARCH_BACKEND:-solr}
      SOLR_URL: http://solr:8983/solr
      # OR for AI:
      # SEARCH_BACKEND: rag
      # EMBEDDING_SERVICE: http://embeddings:8000
      # VECTOR_DB: http://qdrant:6333
```

**API Contract (OpenAPI):**
```yaml
paths:
  /search:
    post:
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/SearchQuery'
      responses:
        200:
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/SearchResults'

  /suggest:
    get:
      parameters:
        - name: q
          in: query
          schema:
            type: string
      responses:
        200:
          content:
            application/json:
              schema:
                type: array
                items:
                  type: string
```

**Implementation Options:**

| Implementation | Use Case | Technology |
|----------------|----------|------------|
| `search-solr` | Standard Dataverse | Solr + existing schema |
| `search-elastic` | Alternative indexing | Elasticsearch 8.x |
| `search-rag` | AI-enhanced search | Embeddings + Vector DB |
| `search-hybrid` | Best of both | Solr + AI reranking |

---

### 4.2 Storage Service

**Purpose:** Abstract file storage operations

**Implementations:**
- S3/MinIO (current)
- Azure Blob Storage
- Google Cloud Storage
- Local filesystem
- IRODS

**API Contract:**
```typescript
interface StorageService {
  // Upload
  getUploadUrl(fileInfo: FileInfo): Promise<PresignedUrl>;
  confirmUpload(uploadId: string, checksum: string): Promise<void>;
  
  // Download
  getDownloadUrl(fileId: string): Promise<PresignedUrl>;
  streamFile(fileId: string): ReadableStream;
  
  // Management
  deleteFile(fileId: string): Promise<void>;
  copyFile(sourceId: string, destPath: string): Promise<string>;
  
  // Bulk operations
  createZip(fileIds: string[]): Promise<PresignedUrl>;
}
```

---

### 4.3 Metadata Service

**Purpose:** Manage dataset and file metadata with validation

**Features:**
- Schema validation (JSON Schema, SHACL)
- Controlled vocabulary enforcement
- Version history
- Export formats (JSON-LD, XML, RDF)

**API Contract:**
```typescript
interface MetadataService {
  // CRUD
  getMetadata(id: string, version?: string): Promise<Metadata>;
  updateMetadata(id: string, metadata: Metadata): Promise<void>;
  
  // Validation
  validate(metadata: Metadata, schema: string): Promise<ValidationResult>;
  
  // Export
  export(id: string, format: 'json-ld' | 'xml' | 'rdf'): Promise<string>;
  
  // Controlled vocabularies
  getVocabulary(name: string): Promise<Term[]>;
  lookupTerm(vocabulary: string, query: string): Promise<Term[]>;
}
```

---

### 4.4 AI Enhancement Service

**Purpose:** Provide AI capabilities to other services

**Capabilities:**
```
┌────────────────────────────────────────────────────────┐
│              AI Enhancement Service                    │
├────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐     │
│  │  Embeddings │  │   RAG       │  │ Suggestions │     │
│  │   Service   │  │   Search    │  │   Engine    │     │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘     │
│         │                │                │            │
│         ▼                ▼                ▼            │
│  ┌─────────────────────────────────────────────────┐   │
│  │              Vector Database (Qdrant)           │   │
│  └─────────────────────────────────────────────────┘   │
│                                                        │
│  Use Cases:                                            │
│  • Semantic search ("datasets about X")                │
│  • Metadata auto-completion                            │
│  • Similar dataset recommendations                     │
│  • Natural language query parsing                      │
│  • Result summarization                                │
└────────────────────────────────────────────────────────┘
```

**API Contract:**
```typescript
interface AIService {
  // Embeddings
  embed(text: string): Promise<number[]>;
  embedBatch(texts: string[]): Promise<number[][]>;
  
  // Semantic search
  semanticSearch(query: string, filters?: Filters): Promise<SearchResults>;
  
  // Generation
  summarize(datasetId: string): Promise<string>;
  suggestKeywords(abstract: string): Promise<string[]>;
  
  // Query understanding
  parseNaturalQuery(query: string): Promise<StructuredQuery>;
}
```

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

#### 5.2.1 Existing Example: rdm-integration

The [rdm-integration](https://github.com/libis/rdm-integration) project is a production-ready external tool that demonstrates the modular architecture pattern. It provides:

**Key Features:**
- **Data Synchronization** - Synchronize files from various repositories (GitHub, GitLab, IRODS, OneDrive, OSF, Globus) into Dataverse with background processing
- **DDI-CDI Metadata Generation** - Automatically generate rich, standardized metadata for tabular data files following the [DDI-CDI specification](https://ddialliance.org/Specification/DDI-CDI/)
- **Globus Transfers** - High-performance uploads and downloads via managed Globus transfers for S3-backed storage

**Architecture (from rdm-integration):**
```
┌─────────────────────────────────────────────────────────┐
│                Angular Frontend                         │
│  (served behind OAuth2 Proxy for authentication)        │
├─────────────────────────────────────────────────────────┤
│                   Go Backend                            │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐      │
│  │   Plugin    │  │    Job      │  │   Redis     │      │
│  │   System    │  │  Scheduler  │  │   State     │      │
│  └─────────────┘  └─────────────┘  └─────────────┘      │
├─────────────────────────────────────────────────────────┤
│  Plugins: GitHub | GitLab | IRODS | OneDrive | OSF |    │
│           SFTP | REDCap | Globus | Local filesystem     │
└─────────────────────────────────────────────────────────┘
```

**External Tool Configurations (registered in Dataverse):**

| Tool | Type | Scope | Route |
|------|------|-------|-------|
| RDM-integration upload | `configure` | `dataset` | `/#/connect` |
| RDM-integration download | `explore` | `dataset` | `/#/download` |
| Generate DDI-CDI | `configure` | `dataset` | `/#/ddi-cdi` |

**See:** [`rdm-integration/README.md`](../rdm-integration/README.md) and [`rdm-integration/ddi-cdi.md`](../rdm-integration/ddi-cdi.md)

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

### 5.3 Dataverse Light

**Concept:** Lightweight Dataverse deployment using standalone components

```
┌─────────────────────────────────────────────────────────┐
│                   Dataverse Light                       │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  ┌─────────────────────────────────────────────────────┐│
│  │              Search Component                       ││
│  │  [🔍 Search datasets...                    ] [🔎]   ││
│  │                                                     ││
│  │  Filters          │  Results                        ││
│  │  ☑ Published      │  ┌───────────────────────────┐  ││
│  │  ☐ My Drafts      │  │ 📊 Climate Dataset 2024   │  ││
│  │                   │  │ ⭐ 4.5 | 📥 1.2k          │  ││
│  │  Subject          │  └───────────────────────────┘  ││
│  │  [Select...]      │  ┌───────────────────────────┐  ││
│  │                   │  │ 📊 Genomics Study         │  ││
│  │                   │  │ ⭐ 4.8 | 📥 856           │  ││
│  └───────────────────┴──┴───────────────────────────┴──┘│
│                                                         │
│  ┌─────────────────────────────────────────────────────┐│
│  │              Dataset View                           ││
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐    ││
│  │  │  Metadata   │ │  File Tree  │ │   Upload    │    ││
│  │  │   Editor    │ │   Browser   │ │  Component  │    ││
│  │  └─────────────┘ └─────────────┘ └─────────────┘    ││
│  └─────────────────────────────────────────────────────┘│
│                                                         │
└─────────────────────────────────────────────────────────┘
```

**Docker Compose (Minimal):**
```yaml
version: '3.8'
services:
  # Frontend - serves static components
  frontend:
    image: dataverse/frontend:light
    ports:
      - "80:80"
  
  # API Gateway - routes to services
  gateway:
    image: dataverse/gateway:latest
    environment:
      SEARCH_SERVICE: http://search:8080
      STORAGE_SERVICE: http://storage:8080
      METADATA_SERVICE: http://metadata:8080
  
  # Core services
  search:
    image: dataverse/search-service:solr
  storage:
    image: dataverse/storage-service:s3
  metadata:
    image: dataverse/metadata-service:postgres
  
  # Data layer
  solr:
    image: solr:9
  postgres:
    image: postgres:15
  minio:
    image: minio/minio
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

## 7. Implementation Roadmap

### Phase 1: Foundation (Current)

**Status:** In Progress

- [x] File Uploader standalone component (`src/standalone-uploader/`)
- [x] iframe embedding pattern (postMessage communication)
- [x] Feature flag system in Dataverse (`EMBED_WEBLOADER_V2`)
- [x] Search components in SPA (`CollectionItemsPanel`, `FilterPanel`)
- [x] File metadata editor in SPA (`EditFileMetadata`)
- [x] Dataset metadata editor in SPA (`EditDatasetMetadata`)
- [ ] Fix remaining uploader embedded mode issues (background, shrinking)

### Phase 2: File Tree Browser (NEW)

**Timeline:** Q1 2025

- [ ] Design tree component API (build on `FilesTable`)
- [ ] Add folder expand/collapse to existing files view
- [ ] Implement virtual scrolling for large datasets
- [ ] Add standalone build option
- [ ] Integrate with uploader

### Phase 3: Extract Existing Components as Standalone

**Timeline:** Q2 2025

> These components already work in the SPA. The task is to create standalone bundles like we did for the File Uploader.

- [ ] **Search Component** - Extract `CollectionItemsPanel` + `FilterPanel`
- [ ] **File Metadata Editor** - Extract `EditFileMetadata`
- [ ] **Dataset Metadata Editor** - Extract `EditDatasetMetadata`
- [ ] Create shared standalone build infrastructure

### Phase 4: Search Service Abstraction

**Timeline:** Q3 2025

- [ ] Formalize SearchService interface (already partially exists)
- [ ] Build AI/RAG search implementation
- [ ] Add semantic search capabilities
- [ ] Integrate with existing `searchServices` dropdown in UI

### Phase 5: AI Enhancements

**Timeline:** Q4 2025

- [ ] RAG search implementation
- [ ] Embedding service for datasets
- [ ] Natural language query parsing
- [ ] Result summarization

### Phase 6: Dataverse Light

**Timeline:** 2026

- [ ] Component orchestration framework
- [ ] Minimal Docker deployment
- [ ] Configuration system
- [ ] Documentation & examples

---

## 8. Benefits & Trade-offs

### Benefits

| Benefit | Description |
|---------|-------------|
| **Rapid Development** | Build external tools by composing components |
| **Technology Flexibility** | Swap backends without UI changes |
| **Maintainability** | Small, focused modules easier to maintain |
| **Testability** | Components can be tested in isolation |
| **Scalability** | Services can scale independently |
| **Customization** | Easy to customize individual components |
| **Gradual Migration** | No big-bang rewrite needed |
| **Community** | Others can contribute components/services |

### Trade-offs

| Trade-off | Mitigation |
|-----------|------------|
| **Complexity** | Good documentation, clear contracts |
| **Performance** | Careful optimization, lazy loading |
| **Consistency** | Shared design system, style guide |
| **Debugging** | Distributed tracing, good logging |
| **Versioning** | Semantic versioning, compatibility matrix |

### When to Use This Architecture

**Good fit:**
- Large Dataverse installations
- Custom external tools needed
- Different search requirements
- AI enhancement desired
- Technology modernization goals

**May be overkill:**
- Small, simple installations
- Standard Dataverse features sufficient
- Limited development resources

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

**Document Version:** 1.1  
**Created:** December 2024  
**Updated:** December 2024 (added project references)  
**Author:** Architecture discussion with AI assistance  
**Status:** Vision / Proposal
