# Phase 0 - Swing Inventory (App/ViewerController)

## Scope
- Source focus:
  - `iped-app/src/main/java/iped/app/ui/App.java`
  - `iped-app/src/main/java/iped/app/ui/AppListener.java`
  - `iped-app/src/main/java/iped/app/ui/MenuClass.java`
  - `iped-app/src/main/java/iped/app/ui/MenuListener.java`
  - `iped-app/src/main/java/iped/app/ui/ViewerController.java`
  - `iped-app/src/main/java/iped/app/ui/GalleryTable.java`
  - `iped-app/src/main/java/iped/app/ui/BookmarksManager.java`

## A. UI Areas and Layout State
- Docking framework in use:
  - `CControl`, `DefaultSingleCDockable`, `CDockableLocationListener` (`App.java`).
- Main dock areas discovered:
  - Left: categories, evidence, filters, bookmarks, metadata, AI filters.
  - Center: result table, gallery, optional graph tab.
  - Bottom/related: hits, subitems, parent, duplicates, references, referenced-by.
  - Right: viewer docks (one per top-level viewer).
- Persisted/reloaded state:
  - `PanelsLayout.save(dockingControl)` and `PanelsLayout.load(dockingControl)` (`App.java`).
- Runtime layout switching:
  - Horizontal/vertical presets and default reset via menu (`MenuListener.java` + `App.java`).

## B. Core User Actions (inventory)
- Search and filtering (`AppListener`):
  - Query combo change -> updates search text/history and triggers result refresh.
  - Filter combo and duplicate filter toggle -> triggers result refresh.
  - Help/options/update case/export-to-zip controls.
- Menu action families (`MenuListener`):
  - Selection/checking:
    - Check/uncheck highlighted, subitems, parent, references, referenced-by.
    - Read/unread highlighted.
  - Export/copy:
    - Export highlighted/checked/tree, export to ZIP, copy highlighted/checked to CSV.
  - Bookmark/filter/column management:
    - Load/save/manage bookmarks, manage filters, manage columns, pin columns.
  - Search tooling:
    - Import keywords, clear search history, export indexed terms.
  - Similarity/AI-like filters:
    - Similar images/faces (current/external), similar docs (threshold input).
  - Navigation/workflow:
    - Navigate parent, navigate parent chat, open preview externally.
  - Layout/view:
    - Toggle timeline view, change layout, save/load/default panels layout, gallery columns.
  - Reporting/graph:
    - Create report, add selected evidence to graph.
  - UI tuning:
    - UI zoom, category/gallery/global icon sizes.

## C. Keyboard Shortcuts and Keymaps
- Explicit accelerators (`MenuClass.java`):
  - `SPACE`: check highlighted / uncheck highlighted (context menu options share accelerator).
  - `CTRL+R` / `ALT+R`: check/uncheck highlighted + subitems.
  - `CTRL+P` / `ALT+P`: check/uncheck highlighted + parent.
  - `CTRL+F` / `ALT+F`: check/uncheck highlighted + references.
  - `CTRL+D` / `ALT+D`: check/uncheck highlighted + referenced-by.
  - `CTRL+B`: manage bookmarks.
- InputMap overrides (`App.java`):
  - Disables `SPACE` and `CTRL+SPACE` default behavior in result table and gallery to avoid conflicts.
- Gallery navigation (`GalleryTable.java`):
  - Arrow and Shift+Arrow actions remapped for horizontal movement and multi-select behavior.
- Bookmark keystrokes (`BookmarksManager.java`):
  - Dynamic per-bookmark shortcuts, including alternate remove behavior (`ALT`-modified stroke).

## D. Viewer Lifecycle and State (ViewerController)
- Top-level viewer containers:
  - Dedicated docks: `HexViewerPlus`, `TextViewer`, `MetadataViewer`, `MultiViewer`.
- Content-specific viewers inside `MultiViewer`:
  - `ImageViewer`, `CADViewer`, `HtmlViewer`, `EmailViewer`, `MsgViewer`, `HtmlLinkViewer`, `IcePDFViewer`, `TiffViewer`, `AudioViewer`, `ReferencedFileViewer`, optional `LibreOfficeViewer`.
- Viewer operations/states:
  - `init()`, `dispose()`, `clear()`, `validateViewers()`, `updateViewers()`.
  - Best-viewer resolution by content type (`isSupportedType` with parent MIME fallback).
  - Dock visibility/front/selection drives eager vs lazy loading.
  - Per-viewer controls in dock actions:
    - Search in viewer, copy viewer image, previous/next hit, toolbar toggle.
  - Hit navigation enablement logic depends on viewer capabilities and current result/hits.

## E. Observed Event/State Model to Preserve
- Query/filter mutation -> refresh pipeline:
  - Reset selection/table/hits/status -> run `UICaseSearcherFilter` -> update all viewers.
- Selection mutation:
  - Checked/read/bookmark state updates and persists via `MultiBookmarks`.
- Layout mutation:
  - Dock location/visibility mutations plus save/load profile.
- Viewer mutation:
  - File/contentType/highlight changes, current viewer change, hit counters, toolbar state.

## F. Migration-Critical Notes
- Swing-specific dependencies to remove/replace:
  - DockingFrames (`CControl`, `DefaultSingleCDockable`, `ExtendedMode`).
  - AWT/Swing dialogs (`JOptionPane`, `JFileChooser`, `FileDialog`).
  - UI-thread assumptions around viewer initialization and updates.
- Equivalent web state domains required:
  - `search`, `filters`, `resultSelection`, `checkedItems`, `bookmarks`, `layout`, `viewerSession`, `jobs`.
