import {provideZonelessChangeDetection} from '@angular/core';
import {provideHttpClient} from '@angular/common/http';
import {createApplication} from '@angular/platform-browser';
import {createCustomElement} from '@angular/elements';

/**
 * Island entry point.
 *
 * <p>Bootstraps the Angular runtime as framework-agnostic custom elements
 * instead of an app shell. Each island self-registers and upgrades wherever
 * the SSR page places its host tag; Spring owns routing and layout, Angular
 * owns only the island internals.
 *
 * <p>Each island component is loaded via a dynamic import so the bundler
 * (Vite/esbuild under @angular/build) emits an independent hashed chunk per
 * island. Adding or modifying one island does NOT invalidate the others'
 * cached bundles.
 *
 * <p>All islands share a single Angular injector created here, so services,
 * HTTP, and DI tokens are shared when multiple islands coexist on the same page.
 */
(async () => {
  const app = await createApplication({
    providers: [provideZonelessChangeDetection(), provideHttpClient()],
  });

  const [
    {ResultsGridComponent},
    {GalleryComponent},
    {HexViewerComponent},
    {TimelineComponent},
    {GraphComponent},
    {ViewerComponent},
    {MapComponent},
  ] = await Promise.all([
    import('./results-grid/results-grid.component'),
    import('./gallery/gallery.component'),
    import('./hex-viewer/hex-viewer.component'),
    import('./timeline/timeline.component'),
    import('./graph/graph.component'),
    import('./viewer/viewer.component'),
    import('./map/map.component'),
  ]);

  const definitions: [string, CustomElementConstructor][] = [
    ['iped-results-grid', createCustomElement(ResultsGridComponent, {injector: app.injector})],
    ['iped-gallery',      createCustomElement(GalleryComponent,     {injector: app.injector})],
    ['iped-hex-viewer',   createCustomElement(HexViewerComponent,   {injector: app.injector})],
    ['iped-timeline',     createCustomElement(TimelineComponent,     {injector: app.injector})],
    ['iped-graph',        createCustomElement(GraphComponent,        {injector: app.injector})],
    ['iped-viewer',       createCustomElement(ViewerComponent,       {injector: app.injector})],
    ['iped-map',          createCustomElement(MapComponent,          {injector: app.injector})],
  ];

  for (const [name, ctor] of definitions) {
    if (!customElements.get(name)) {
      customElements.define(name, ctor);
    }
  }
})();
