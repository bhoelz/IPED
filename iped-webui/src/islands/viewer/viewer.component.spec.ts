import {ComponentFixture, TestBed} from '@angular/core/testing';
import {HttpTestingController, provideHttpClientTesting} from '@angular/common/http/testing';
import {provideHttpClient} from '@angular/common/http';
import {ViewerComponent} from './viewer.component';

describe('ViewerComponent', () => {
  let fixture: ComponentFixture<ViewerComponent>;
  let component: ViewerComponent;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ViewerComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    fixture   = TestBed.createComponent(ViewerComponent);
    component = fixture.componentInstance;
    httpMock  = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('creates the component', () => {
    expect(component).toBeTruthy();
  });

  it('shows empty state when no item-id is set', () => {
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.viewer-empty')).toBeTruthy();
    httpMock.expectNone(() => true);
  });

  describe('text/* MIME types', () => {
    it('fetches text and renders <pre>', () => {
      component.itemId    = 'src:10';
      component.mediaType = 'text/plain';
      fixture.detectChanges();

      const req = httpMock.expectOne(r => r.url.includes('/text'));
      expect(req.request.headers.get('Accept')).toBe('text/plain');
      req.flush('hello world');
      fixture.detectChanges();

      const pre = fixture.nativeElement.querySelector('.viewer-text') as HTMLElement;
      expect(pre?.textContent?.trim()).toBe('hello world');
    });

    it('appends ?highlight= when highlight input is set', () => {
      component.itemId    = 'src:10';
      component.mediaType = 'text/plain';
      component.highlight = 'evidence';
      fixture.detectChanges();

      const req = httpMock.expectOne(r => r.url.includes('/text'));
      expect(req.request.urlWithParams).toContain('highlight=evidence');
      req.flush('no match here');
    });

    it('dispatches viewer-ready after text loads', () => {
      const events: CustomEvent[] = [];
      fixture.nativeElement.addEventListener('viewer-ready', (e: Event) => events.push(e as CustomEvent));

      component.itemId    = 'src:10';
      component.mediaType = 'text/plain';
      fixture.detectChanges();
      httpMock.expectOne(r => r.url.includes('/text')).flush('body');
      fixture.detectChanges();

      expect(events.length).toBe(1);
      expect(events[0].detail.viewerType).toBe('text');
      expect(events[0].detail.itemId).toBe('src:10');
    });
  });

  describe('text/html MIME type', () => {
    it('fetches with Accept: text/html and renders innerHTML', () => {
      component.itemId    = 'src:11';
      component.mediaType = 'text/html';
      fixture.detectChanges();

      const req = httpMock.expectOne(r => r.url.includes('/text'));
      expect(req.request.headers.get('Accept')).toBe('text/html');
      req.flush('<p>Hello <mark>world</mark></p>');
      fixture.detectChanges();

      const div = fixture.nativeElement.querySelector('.viewer-html') as HTMLElement;
      expect(div?.innerHTML).toContain('<mark>');
    });
  });

  describe('image/* MIME types', () => {
    it('renders <img> without an HTTP fetch', () => {
      component.itemId    = 'src:20';
      component.mediaType = 'image/jpeg';
      fixture.detectChanges();

      httpMock.expectNone(() => true);
      const img = fixture.nativeElement.querySelector('.viewer-image img') as HTMLImageElement;
      expect(img).toBeTruthy();
      expect(img.src).toContain('/content');
    });
  });

  describe('application/pdf', () => {
    it('renders <embed> without an HTTP fetch', () => {
      component.itemId    = 'src:30';
      component.mediaType = 'application/pdf';
      fixture.detectChanges();

      httpMock.expectNone(() => true);
      const embed = fixture.nativeElement.querySelector('.viewer-pdf') as HTMLEmbedElement;
      expect(embed).toBeTruthy();
      expect(embed.type).toBe('application/pdf');
    });
  });

  describe('unknown/binary MIME → hex', () => {
    it('renders iped-hex-viewer-impl for unknown types', () => {
      component.itemId    = 'src:40';
      component.mediaType = 'application/octet-stream';
      fixture.detectChanges();

      httpMock.expectNone(() => true);
      const hex = fixture.nativeElement.querySelector('iped-hex-viewer-impl');
      expect(hex).toBeTruthy();
    });

    it('uses hex renderer when media-type is empty', () => {
      component.itemId    = 'src:40';
      component.mediaType = '';
      fixture.detectChanges();

      httpMock.expectNone(() => true);
      const hex = fixture.nativeElement.querySelector('iped-hex-viewer-impl');
      expect(hex).toBeTruthy();
    });
  });

  describe('error handling', () => {
    it('shows error state and dispatches island-error when text fetch fails', () => {
      const events: CustomEvent[] = [];
      fixture.nativeElement.addEventListener('island-error', (e: Event) => events.push(e as CustomEvent));

      component.itemId    = 'src:50';
      component.mediaType = 'text/plain';
      fixture.detectChanges();

      httpMock.expectOne(r => r.url.includes('/text')).error(new ProgressEvent('error'));
      fixture.detectChanges();

      const errEl = fixture.nativeElement.querySelector('.viewer-error');
      expect(errEl).toBeTruthy();
      expect(events.length).toBe(1);
      expect(events[0].detail.island).toBe('viewer');
    });
  });

  describe('viewerType computed', () => {
    const cases: [string, string][] = [
      ['text/plain',         'text'],
      ['text/csv',           'text'],
      ['text/html',          'html'],
      ['application/json',   'text'],
      ['application/xml',    'text'],
      ['image/png',          'image'],
      ['image/jpeg',         'image'],
      ['application/pdf',    'pdf'],
      ['application/octet-stream', 'hex'],
      ['',                   'hex'],
    ];

    cases.forEach(([mime, expected]) => {
      it(`maps "${mime}" → "${expected}"`, () => {
        component.itemId    = 'src:99';
        component.mediaType = mime;
        fixture.detectChanges();
        expect((component as any).viewerType()).toBe(expected);
        // flush any pending text requests
        httpMock.match(() => true).forEach(r => r.flush(''));
      });
    });
  });
});
