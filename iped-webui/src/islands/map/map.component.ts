import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  inject,
  Input,
  OnChanges,
  OnDestroy,
  signal,
  ViewChild,
  ViewEncapsulation,
} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {IslandBase} from '../shared/island-base';
import {islandErrorEvent, itemSelectedEvent, mapMarkerSelectedEvent} from '../shared/events';

interface GeoFeatureProperties {
  sourceId: string;
  docId: number;
  name: string;
  timestamp: string | null;
}

interface GeoFeatureCollection {
  type: 'FeatureCollection';
  total: number;
  truncated: boolean;
  features: Array<{
    id: string;
    geometry: {type: 'Point'; coordinates: [number, number, number?]};
    properties: GeoFeatureProperties;
  }>;
}

/**
 * Map island (`<iped-map>`).
 *
 * <p>Renders a Leaflet map that shows geolocated items from an IPED case.
 * Coordinates are fetched from `GET /v2/sources/{sourceId}/geo` (GeoV2 endpoint).
 *
 * <p>Boundary contract:
 * <ul>
 *   <li>Inputs: {@code source-id}, {@code api-base}, {@code tile-url},
 *       {@code tile-attribution}, {@code height}</li>
 *   <li>Events out: {@code map-marker-selected}, {@code island-error}</li>
 * </ul>
 *
 * <p>Tile URL defaults to OpenStreetMap online. For air-gapped deployments set
 * {@code tile-url} to a locally served tile server (e.g. a mbtiles/tileserver-gl
 * instance) and {@code tile-attribution} accordingly.
 */
@Component({
  selector: 'iped-map-impl',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  // ViewEncapsulation.None is required so Leaflet's CSS (imported below via SCSS)
  // applies globally and is not scoped to the component's shadow/emulated scope.
  encapsulation: ViewEncapsulation.None,
  templateUrl: './map.component.html',
  styleUrl: './map.component.scss',
})
export class MapComponent extends IslandBase implements OnChanges, AfterViewInit, OnDestroy {
  private readonly http = inject(HttpClient);

  @Input('source-id') sourceId = '';
  @Input('api-base') override apiBase = '/api';
  /** Leaflet tile URL template. Supports {s}/{z}/{x}/{y} placeholders. */
  @Input('tile-url') tileUrl = 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png';
  @Input('tile-attribution') tileAttribution =
    '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors';
  /** CSS height of the map container element. */
  @Input() height = '400px';

  @ViewChild('mapEl') mapElRef?: ElementRef<HTMLDivElement>;

  protected readonly loading      = signal(false);
  protected readonly error        = signal<string | null>(null);
  protected readonly featureCount = signal(0);
  protected readonly truncated    = signal(false);

  // Leaflet instances — typed `any` to avoid a hard compile dep on @types/leaflet
  // at the island boundary; types are only used in devDependencies.
  private L: any = null;             // the Leaflet namespace (dynamic import result)
  private leafletMap: any = null;    // L.Map
  private markers: any = null;       // L.FeatureGroup (supports getBounds)

  private viewReady = false;

  // ── Lifecycle ──────────────────────────────────────────────────────────────

  ngAfterViewInit(): void {
    this.viewReady = true;
    if (this.sourceId) this.initAndLoad();
  }

  ngOnChanges(): void {
    if (this.viewReady && this.sourceId) this.initAndLoad();
  }

  override ngOnDestroy(): void {
    this.leafletMap?.remove();
    this.leafletMap = null;
    this.L = null;
  }

  // ── Map initialisation ────────────────────────────────────────────────────

  private async initAndLoad(): Promise<void> {
    if (!this.mapElRef) return;

    if (!this.L) {
      this.L = await import('leaflet');
    }

    if (!this.leafletMap) {
      const L = this.L;
      this.leafletMap = L.map(this.mapElRef.nativeElement, {
        center: [20, 0] as [number, number],
        zoom: 2,
        zoomControl: true,
      });
      L.tileLayer(this.tileUrl, {
        attribution: this.tileAttribution,
        maxZoom: 19,
      }).addTo(this.leafletMap);
      // FeatureGroup (not layerGroup) so we can call getBounds() for fitBounds.
      this.markers = L.featureGroup().addTo(this.leafletMap);
    }

    this.loadFeatures();
  }

  // ── Data fetch ─────────────────────────────────────────────────────────────

  private loadFeatures(): void {
    const url = `${this.apiBase.replace(/\/$/, '')}/v2/sources/${encodeURIComponent(this.sourceId)}/geo`;
    this.loading.set(true);
    this.error.set(null);

    this.http.get<GeoFeatureCollection>(url).subscribe({
      next: fc  => this.renderFeatureCollection(fc),
      error: e  => {
        this.loading.set(false);
        const msg = (e?.error?.error as string | undefined) ?? e?.message ?? 'Failed to load map data';
        this.error.set(msg);
        this.dispatch(islandErrorEvent({island: 'map', message: msg, cause: e}));
      },
    });
  }

  // ── Rendering ──────────────────────────────────────────────────────────────

  private renderFeatureCollection(fc: GeoFeatureCollection): void {
    const L = this.L;
    this.markers.clearLayers();

    for (const feature of fc.features) {
      const [lon, lat] = feature.geometry.coordinates;
      const props = feature.properties;

      const marker = L.marker([lat, lon] as [number, number]);
      marker.bindPopup(
        `<div class="iped-map-popup"><strong>${escapeHtml(props.name)}</strong>` +
        (props.timestamp ? `<br><small>${escapeHtml(props.timestamp)}</small>` : '') +
        `</div>`
      );
      marker.on('click', () => {
        this.dispatch(mapMarkerSelectedEvent({
          itemId:    `${props.sourceId}:${props.docId}`,
          sourceId:  props.sourceId,
          name:      props.name,
          latitude:  lat,
          longitude: lon,
        }));
        // Also fire the standard item-selected event so other islands respond.
        this.dispatch(itemSelectedEvent({
          itemId:    `${props.sourceId}:${props.docId}`,
          sourceId:  props.sourceId,
          name:      props.name,
        }));
      });
      this.markers.addLayer(marker);
    }

    // Auto-fit bounds when features are present.
    if (fc.features.length > 0 && this.markers.getBounds().isValid()) {
      this.leafletMap.fitBounds(this.markers.getBounds(), {padding: [20, 20]});
    }

    this.featureCount.set(fc.features.length);
    this.truncated.set(fc.truncated);
    this.loading.set(false);
  }
}

function escapeHtml(s: string): string {
  return s
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}
