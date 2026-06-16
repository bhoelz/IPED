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

  describe('video/* MIME types', () => {
    it('renders <video> element without HTTP fetch', () => {
      component.itemId    = 'src:70';
      component.mediaType = 'video/mp4';
      fixture.detectChanges();

      httpMock.expectNone(() => true);
      const video = fixture.nativeElement.querySelector('.viewer-video video') as HTMLVideoElement;
      expect(video).toBeTruthy();
      expect(video.src).toContain('/content');
    });

    it('renders <video> for video/webm', () => {
      component.itemId    = 'src:71';
      component.mediaType = 'video/webm';
      fixture.detectChanges();

      httpMock.expectNone(() => true);
      expect(fixture.nativeElement.querySelector('.viewer-video video')).toBeTruthy();
    });

    it('dispatches viewer-ready with viewerType=video', () => {
      const events: CustomEvent[] = [];
      fixture.nativeElement.addEventListener('viewer-ready', (e: Event) => events.push(e as CustomEvent));

      component.itemId    = 'src:72';
      component.mediaType = 'video/mp4';
      fixture.detectChanges();

      httpMock.expectNone(() => true);
      expect(events.length).toBe(1);
      expect(events[0].detail.viewerType).toBe('video');
    });
  });

  describe('email MIME types', () => {
    it('renders email headers and body via two parallel fetches', () => {
      component.itemId    = 'src:80';
      component.mediaType = 'message/rfc822';
      fixture.detectChanges();

      const metaReq = httpMock.expectOne(r => r.url.match(/\/items\/80$/) !== null);
      const bodyReq = httpMock.expectOne(r => r.url.includes('/text'));

      metaReq.flush({metadata: {
        'Message-Subject': ['Test subject'],
        'Message:Message-From': ['alice@example.com'],
        'Message:Message-To': ['bob@example.com'],
        'dcterms:created': ['2024-01-15T10:30:00Z'],
      }});
      bodyReq.flush('<p>Hello world</p>');
      fixture.detectChanges();

      const subjectEl = fixture.nativeElement.querySelector('.viewer-email-subject .viewer-email-value');
      expect(subjectEl?.textContent?.trim()).toBe('Test subject');
      const body = fixture.nativeElement.querySelector('.viewer-email-body');
      expect(body?.innerHTML).toContain('<p>Hello world</p>');
    });

    it('routes message/x-emlx to email viewer', () => {
      component.itemId    = 'src:81';
      component.mediaType = 'message/x-emlx';
      fixture.detectChanges();

      expect((component as any).viewerType()).toBe('email');
      httpMock.match(() => true).forEach(r => r.flush({}));
    });

    it('routes application/vnd.ms-outlook to email viewer', () => {
      component.itemId    = 'src:82';
      component.mediaType = 'application/vnd.ms-outlook';
      fixture.detectChanges();

      expect((component as any).viewerType()).toBe('email');
      httpMock.match(() => true).forEach(r => r.flush({}));
    });

    it('dispatches viewer-ready on email type', () => {
      const events: CustomEvent[] = [];
      fixture.nativeElement.addEventListener('viewer-ready', (e: Event) => events.push(e as CustomEvent));

      component.itemId    = 'src:83';
      component.mediaType = 'message/rfc822';
      fixture.detectChanges();

      httpMock.match(() => true).forEach(r => r.flush({metadata: {}}));
      fixture.detectChanges();

      expect(events[0].detail.viewerType).toBe('email');
    });
  });

  describe('image/tiff — PNG conversion', () => {
    it('appends ?format=png to the image URL for image/tiff', () => {
      component.itemId    = 'src:90';
      component.mediaType = 'image/tiff';
      fixture.detectChanges();

      httpMock.expectNone(() => true);
      const img = fixture.nativeElement.querySelector('.viewer-image img') as HTMLImageElement;
      expect(img).toBeTruthy();
      expect(img.src).toContain('/content?format=png');
    });

    it('does NOT append ?format=png for regular image types', () => {
      component.itemId    = 'src:91';
      component.mediaType = 'image/jpeg';
      fixture.detectChanges();

      httpMock.expectNone(() => true);
      const img = fixture.nativeElement.querySelector('.viewer-image img') as HTMLImageElement;
      expect(img.src).not.toContain('format=png');
    });
  });

  describe('audio/* MIME types', () => {
    it('renders <audio> element and fetches item metadata', () => {
      component.itemId    = 'src:60';
      component.mediaType = 'audio/mpeg';
      fixture.detectChanges();

      const req = httpMock.expectOne(r => r.url.includes('/items/60') && !r.url.includes('/content') && !r.url.includes('/text'));
      req.flush({metadata: {}});
      fixture.detectChanges();

      const audio = fixture.nativeElement.querySelector('.viewer-audio audio') as HTMLAudioElement;
      expect(audio).toBeTruthy();
      expect(audio.src).toContain('/content');
    });

    it('shows transcription and confidence when present in metadata', () => {
      component.itemId    = 'src:61';
      component.mediaType = 'audio/mpeg';
      fixture.detectChanges();

      httpMock.expectOne(r => r.url.includes('/items/61') && !r.url.includes('/content'))
        .flush({metadata: {'audio:transcription': ['Call recording text'], 'audio:transcriptConfidence': ['0.85']}});
      fixture.detectChanges();

      const transcriptEl = fixture.nativeElement.querySelector('.viewer-audio-transcript-text');
      expect(transcriptEl?.textContent?.trim()).toBe('Call recording text');
      const confEl = fixture.nativeElement.querySelector('.viewer-audio-confidence');
      expect(confEl?.textContent?.trim()).toBe('[85%]');
    });

    it('shows "no transcription" message when metadata has none', () => {
      component.itemId    = 'src:62';
      component.mediaType = 'audio/ogg';
      fixture.detectChanges();

      httpMock.expectOne(r => r.url.includes('/items/62') && !r.url.includes('/content'))
        .flush({metadata: {}});
      fixture.detectChanges();

      const emptyEl = fixture.nativeElement.querySelector('.viewer-audio-transcript-empty');
      expect(emptyEl).toBeTruthy();
    });

    it('dispatches viewer-ready with viewerType=audio', () => {
      const events: CustomEvent[] = [];
      fixture.nativeElement.addEventListener('viewer-ready', (e: Event) => events.push(e as CustomEvent));

      component.itemId    = 'src:63';
      component.mediaType = 'audio/wav';
      fixture.detectChanges();

      httpMock.expectOne(r => r.url.includes('/items/63') && !r.url.includes('/content'))
        .flush({metadata: {}});
      fixture.detectChanges();

      expect(events.length).toBeGreaterThanOrEqual(1);
      expect(events[0].detail.viewerType).toBe('audio');
      expect(events[0].detail.itemId).toBe('src:63');
    });

    it('shows duration and type from metadata', () => {
      component.itemId    = 'src:64';
      component.mediaType = 'audio/mpeg';
      fixture.detectChanges();

      httpMock.expectOne(r => r.url.includes('/items/64') && !r.url.includes('/content'))
        .flush({metadata: {'audio:xmpDM:duration': ['125.3']}});
      fixture.detectChanges();

      const metaEl = fixture.nativeElement.querySelector('.viewer-audio-meta');
      expect(metaEl?.textContent).toContain('2m');
      expect(metaEl?.textContent).toContain('5s');
    });
  });

  describe('server-side transcode fallback', () => {
    it('appends ?transcode=webm for non-browser audio (AMR)', () => {
      component.itemId    = 'src:65';
      component.mediaType = 'audio/amr';
      fixture.detectChanges();

      httpMock.expectOne(r => r.url.includes('/items/65') && !r.url.includes('/content'))
        .flush({metadata: {}});
      fixture.detectChanges();

      const audio = fixture.nativeElement.querySelector('.viewer-audio audio') as HTMLAudioElement;
      expect(audio.src).toContain('/content?transcode=webm');
    });

    it('appends ?transcode=webm for WMA audio', () => {
      component.itemId    = 'src:66';
      component.mediaType = 'audio/x-ms-wma';
      fixture.detectChanges();

      httpMock.expectOne(r => r.url.includes('/items/66') && !r.url.includes('/content'))
        .flush({metadata: {}});
      fixture.detectChanges();

      const audio = fixture.nativeElement.querySelector('.viewer-audio audio') as HTMLAudioElement;
      expect(audio.src).toContain('?transcode=webm');
    });

    it('does NOT append transcode param for browser-native MP3', () => {
      component.itemId    = 'src:67';
      component.mediaType = 'audio/mpeg';
      fixture.detectChanges();

      httpMock.expectOne(r => r.url.includes('/items/67') && !r.url.includes('/content'))
        .flush({metadata: {}});
      fixture.detectChanges();

      const audio = fixture.nativeElement.querySelector('.viewer-audio audio') as HTMLAudioElement;
      expect(audio.src).not.toContain('transcode=webm');
    });

    it('appends ?transcode=webm for non-browser video (AVI)', () => {
      component.itemId    = 'src:73';
      component.mediaType = 'video/x-msvideo';
      fixture.detectChanges();

      httpMock.expectNone(() => true);
      const video = fixture.nativeElement.querySelector('.viewer-video video') as HTMLVideoElement;
      expect(video.src).toContain('/content?transcode=webm');
    });

    it('does NOT append transcode param for browser-native MP4 video', () => {
      component.itemId    = 'src:74';
      component.mediaType = 'video/mp4';
      fixture.detectChanges();

      httpMock.expectNone(() => true);
      const video = fixture.nativeElement.querySelector('.viewer-video video') as HTMLVideoElement;
      expect(video.src).not.toContain('transcode=webm');
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
      ['audio/mpeg',         'audio'],
      ['audio/ogg',          'audio'],
      ['audio/wav',          'audio'],
      ['audio/aac',          'audio'],
      ['video/mp4',          'video'],
      ['video/webm',         'video'],
      ['video/ogg',          'video'],
      ['message/rfc822',     'email'],
      ['message/x-emlx',     'email'],
      ['application/vnd.ms-outlook', 'email'],
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
