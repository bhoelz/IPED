/**
 * Typed CustomEvent factory functions for the island ↔ host communication contract.
 *
 * Islands dispatch these events on their host element; the surrounding SSR page
 * (Spring/Thymeleaf/HTMX) listens via standard addEventListener — no shared JS state.
 *
 * Convention: event names are kebab-case, detail payloads are plain objects,
 * `bubbles: true` so the host page can catch them at any ancestor.
 */

export interface ItemSelectedDetail {
  /** Composite "{sourceId}:{docId}" string, or a plain numeric docId as string. */
  itemId: string;
  sourceId?: string;
  name?: string;
  mediaType?: string;
}

export interface SelectionChangedDetail {
  count: number;
  itemIds: string[];
  sourceId?: string;
}

export interface ResultsLoadedDetail {
  total: number;
  shown: number;
  rangeLabel: string;
  hasPrev: boolean;
  hasNext: boolean;
}

export interface IslandErrorDetail {
  island: string;
  message: string;
  cause?: unknown;
}

// ---------------------------------------------------------------------------
// Factory helpers
// ---------------------------------------------------------------------------

export function itemSelectedEvent(detail: ItemSelectedDetail): CustomEvent<ItemSelectedDetail> {
  return new CustomEvent('item-selected', {detail, bubbles: true, composed: true});
}

export function selectionChangedEvent(detail: SelectionChangedDetail): CustomEvent<SelectionChangedDetail> {
  return new CustomEvent('selection-changed', {detail, bubbles: true, composed: true});
}

export function resultsLoadedEvent(detail: ResultsLoadedDetail): CustomEvent<ResultsLoadedDetail> {
  return new CustomEvent('results-loaded', {detail, bubbles: true, composed: true});
}

export function islandErrorEvent(detail: IslandErrorDetail): CustomEvent<IslandErrorDetail> {
  return new CustomEvent('island-error', {detail, bubbles: true, composed: true});
}

// ---------------------------------------------------------------------------
// Phase 2 event types
// ---------------------------------------------------------------------------

export interface TimeRangeSelectedDetail {
  start: string;  // ISO date string (bucket date, e.g. "2022-03")
  end: string;
}

export interface SimilarImageSearchDetail {
  itemId: string;
}

export function timeRangeSelectedEvent(detail: TimeRangeSelectedDetail): CustomEvent<TimeRangeSelectedDetail> {
  return new CustomEvent('time-range-selected', {detail, bubbles: true, composed: true});
}

export function similarImageSearchEvent(detail: SimilarImageSearchDetail): CustomEvent<SimilarImageSearchDetail> {
  return new CustomEvent('similar-image-search', {detail, bubbles: true, composed: true});
}

// ---------------------------------------------------------------------------
// Phase 5 event types
// ---------------------------------------------------------------------------

export type ViewerType = 'text' | 'html' | 'image' | 'pdf' | 'audio' | 'video' | 'email' | 'hex' | 'unsupported';

export interface ViewerReadyDetail {
  itemId: string;
  viewerType: ViewerType;
  mediaType: string;
}

export function viewerReadyEvent(detail: ViewerReadyDetail): CustomEvent<ViewerReadyDetail> {
  return new CustomEvent('viewer-ready', {detail, bubbles: true, composed: true});
}

// ---------------------------------------------------------------------------
// Map island event types
// ---------------------------------------------------------------------------

export interface MapMarkerSelectedDetail extends ItemSelectedDetail {
  /** WGS-84 latitude of the clicked marker. */
  latitude: number;
  /** WGS-84 longitude of the clicked marker. */
  longitude: number;
}

export function mapMarkerSelectedEvent(detail: MapMarkerSelectedDetail): CustomEvent<MapMarkerSelectedDetail> {
  return new CustomEvent('map-marker-selected', {detail, bubbles: true, composed: true});
}
