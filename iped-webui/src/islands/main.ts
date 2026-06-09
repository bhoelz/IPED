import {provideZonelessChangeDetection} from '@angular/core';
import {provideHttpClient} from '@angular/common/http';
import {createApplication} from '@angular/platform-browser';
import {createCustomElement} from '@angular/elements';

import {ResultsGridComponent} from './results-grid/results-grid.component';
import {GalleryComponent} from './gallery/gallery.component';
import {HexViewerComponent} from './hex-viewer/hex-viewer.component';
import {TimelineComponent} from './timeline/timeline.component';
import {GraphComponent} from './graph/graph.component';

/**
 * Island entry point.
 *
 * <p>Bootstraps the Angular runtime as framework-agnostic custom elements
 * instead of an app shell. Each island self-registers and upgrades wherever
 * the SSR page places its host tag; Spring owns routing and layout, Angular
 * owns only the island internals.
 *
 * <p>Register additional islands here as C-10 (timeline), C-11 (graph),
 * C-16 (preview), C-17 (hex viewer) land — one {@code customElements.define}
 * per element, same attribute-in / CustomEvent-out contract.
 */
(async () => {
  const app = await createApplication({
    providers: [provideZonelessChangeDetection(), provideHttpClient()],
  });

  if (!customElements.get('iped-results-grid')) {
    const ResultsGrid = createCustomElement(ResultsGridComponent, {injector: app.injector});
    customElements.define('iped-results-grid', ResultsGrid);
  }

  if (!customElements.get('iped-gallery')) {
    const Gallery = createCustomElement(GalleryComponent, {injector: app.injector});
    customElements.define('iped-gallery', Gallery);
  }

  if (!customElements.get('iped-hex-viewer')) {
    const HexViewer = createCustomElement(HexViewerComponent, {injector: app.injector});
    customElements.define('iped-hex-viewer', HexViewer);
  }

  if (!customElements.get('iped-timeline')) {
    const Timeline = createCustomElement(TimelineComponent, {injector: app.injector});
    customElements.define('iped-timeline', Timeline);
  }

  if (!customElements.get('iped-graph')) {
    const Graph = createCustomElement(GraphComponent, {injector: app.injector});
    customElements.define('iped-graph', Graph);
  }
})();
