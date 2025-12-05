# Embedded DVWebloader V2 - Implementation Status

## Overview

DVWebloader V2 can be embedded directly within Dataverse JSF pages using an iframe approach (similar to file previewers). This provides CSS isolation and keeps the API token secure within the iframe URL.

## Git Branches

All repositories are located in `/home/eryk/projects/`:

| Repository | Branch | Description |
|------------|--------|-------------|
| `dataverse` | `feature/embed-webloader-v2` | Feature flag, JSF changes, Java backend |
| `dataverse-frontend` | `feature/standalone-file-uploader` | Standalone uploader bundle, embedded HTML, StandaloneFileUploaderPanel |
| `dataverse-client-javascript` | `feature/configurable-uploads` | Exports `FilesConfig` for standalone mode |
| `dvwebloader` | `feature/v2-reusing-spa` | V2 version reusing SPA components |
| `rdm-build` | `main` | Previewers Docker image, nginx config, embeddedDvWebloader.html |
| `rdm-integration` | `feature/embed-webloader-v2` | Docker compose with feature flag enabled |

**To resume work**: Check out the branches listed above in each repository.

## Current Status

### What's Working
- ✅ Feature flag `EMBED_WEBLOADER_V2` (`dataverse.feature.embed-webloader-v2`)
- ✅ Conditional rendering in `editFilesFragment.xhtml` - shows iframe when V2 enabled
- ✅ Dynamic iframe height using `postMessage` + `ResizeObserver` - iframe grows with content
- ✅ S3 tagging setting respected (`dataverse.files.<driverId>.disable-tagging`)
- ✅ Nginx headers allowing iframe embedding (`X-Frame-Options`, `Content-Security-Policy`)
- ✅ File upload works correctly
- ✅ "Done" button at bottom works (returns to dataset page)
- ✅ Helper text "All file types are supported..." shown in embedded mode

### Known Issues to Fix

#### 1. Background Color Mismatch
The iframe content has a different background than the surrounding JSF page.

**Current state**: `embeddedDvWebloader.html` has `background-color: transparent`

**Problem**: The surrounding JSF page has a light gray background (`#f5f5f5` or similar), but the iframe content appears white/different.

**Possible fixes**:
- Option A: Match the exact Dataverse background color in `embeddedDvWebloader.html`
- Option B: Pass background color as URL parameter from JSF to iframe
- Option C: Investigate if transparent isn't working due to browser/CSS specificity

**File to modify**: `/home/eryk/projects/rdm-build/images/previewers/dvwebloader-v2/embeddedDvWebloader.html`

#### 2. Iframe Doesn't Shrink
When files are removed or upload completes, the iframe grows but never shrinks back.

**Root cause**: `ResizeObserver` only triggers when content changes, but `scrollHeight` doesn't decrease when content shrinks because the iframe has `min-height`.

**Solution needed**: 
- Reset height before measuring, or use `offsetHeight` of actual content
- Possibly observe the `#root` element directly instead of `body`
- May need `MutationObserver` in addition to `ResizeObserver`

**Files to modify**:
- `/home/eryk/projects/rdm-build/images/previewers/dvwebloader-v2/embeddedDvWebloader.html` (postMessage sender)
- `/home/eryk/projects/dataverse/src/main/webapp/editFilesFragment.xhtml` (postMessage receiver)

#### 3. Redirect After Upload Doesn't Work
After successful upload, the React component tries to redirect to the dataset page, but this fails because:
- The iframe cannot navigate the parent window (cross-origin restriction)
- The redirect inside iframe just loads dataset page inside the iframe

**Decision**: Redirect is NOT needed. The user workflow is:
1. Upload files (can upload multiple batches)
2. Click "Done" button at bottom when finished
3. "Done" button already works and navigates back to dataset

**Action**: Modify `StandaloneFileUploaderPanel.tsx` to NOT redirect after upload when in embedded mode:
- Detect embedded mode: `window.parent !== window`
- When embedded: Show success toast, reset uploader to initial state, shrink iframe
- When popup: Keep current behavior (redirect after delay)

---

## File Locations

### Dataverse (Java/JSF)
- `src/main/java/edu/harvard/iq/dataverse/settings/FeatureFlags.java` - Feature flag definition
- `src/main/java/edu/harvard/iq/dataverse/SettingsWrapper.java` - `isEmbeddedWebloaderV2()` method
- `src/main/java/edu/harvard/iq/dataverse/EditDatafilesPage.java` - `getEmbeddedWebloaderUrl()` method
- `src/main/java/edu/harvard/iq/dataverse/util/WebloaderUtil.java` - URL generation with `useS3Tagging`
- `src/main/webapp/editFilesFragment.xhtml` - Iframe embedding and postMessage listener

### dataverse-frontend (React)
- `src/standalone-uploader/StandaloneFileUploaderPanel.tsx` - Standalone wrapper for embedded/popup mode
- `src/standalone-uploader/embeddedDvWebloader.html` - Minimal HTML for iframe embedding
- `src/sections/shared/file-uploader/FileUploaderPanelCore.tsx` - Core uploader component

### rdm-build (Deployment)
- `images/previewers/dvwebloader-v2/embeddedDvWebloader.html` - Production embedded HTML
- `images/previewers/dvwebloader-v2/dist/` - Built JS bundle location
- `images/previewers/conf/nginx.conf.template` - Nginx config with iframe headers

### dvwebloader (External repo)
- `src/embeddedDvWebloader.html` - Should be kept in sync

---

## Configuration

To enable embedded DVWebloader V2:

```bash
# 1. Set WebloaderUrl (must contain "v2" in path)
curl -X PUT -d 'http://localhost:8888/dvwebloader-v2/dist/dvwebloaderV2.html' \
  http://localhost:8080/api/admin/settings/:WebloaderUrl

# 2. Enable feature flag (JVM option)
-Ddataverse.feature.embed-webloader-v2=true

# 3. Optional: Disable S3 tagging for storage driver
-Ddataverse.files.minio1.disable-tagging=true
```

---

## Build Commands

```bash
# Rebuild dataverse-frontend standalone uploader bundle
cd /home/eryk/projects/dataverse-frontend
npm run build-uploader

# Copy bundle to rdm-build
cp dist-uploader/* /home/eryk/projects/rdm-build/images/previewers/dvwebloader-v2/dist/

# Rebuild previewers Docker image
cd /home/eryk/projects/rdm-build
make build-previewers

# Recompile Dataverse
cd /home/eryk/projects/dataverse
mvn compile -DskipTests

# Restart containers to pick up changes
cd /home/eryk/projects/rdm-integration
# (restart previewers container)
```

---

## Next Steps

1. **Fix background color** - Match JSF page background in `embeddedDvWebloader.html`

2. **Fix iframe shrinking** - Improve height calculation in postMessage:
   ```javascript
   function sendHeightToParent() {
       const root = document.getElementById('root');
       const height = root ? root.offsetHeight + 20 : 400; // +20 for padding
       window.parent.postMessage({ type: 'dvwebloader-resize', height: height }, '*');
   }
   ```

3. **Handle embedded mode in React** - In `StandaloneFileUploaderPanel.tsx`:
   ```typescript
   const isEmbedded = window.parent !== window;
   
   const handleFilesAddedSuccess = useCallback(() => {
       if (isEmbedded) {
           // Don't redirect - user will click Done when finished
           toast.success(t('fileUploader.filesAddedToDatasetSuccessfully'));
           // Optionally: reset uploader state for more uploads
       } else {
           // Popup mode: redirect after delay
           setTimeout(() => {
               window.location.href = getDatasetUrl();
           }, 1500);
       }
   }, [isEmbedded, getDatasetUrl, t]);
   ```

4. **Test workflow**:
   - Upload files → success toast → can upload more
   - Click "Done" → returns to dataset page
   - Iframe shrinks after upload completes
