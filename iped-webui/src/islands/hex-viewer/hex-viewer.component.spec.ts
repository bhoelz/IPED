import {ComponentFixture, TestBed} from '@angular/core/testing';
import {HttpTestingController, provideHttpClientTesting} from '@angular/common/http/testing';
import {provideHttpClient} from '@angular/common/http';
import {HexViewerComponent} from './hex-viewer.component';

describe('HexViewerComponent', () => {
  let fixture: ComponentFixture<HexViewerComponent>;
  let component: HexViewerComponent;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HexViewerComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    fixture   = TestBed.createComponent(HexViewerComponent);
    component = fixture.componentInstance;
    httpMock  = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('creates the component', () => {
    expect(component).toBeTruthy();
  });

  it('shows error when item-id is empty', async () => {
    component.itemId = '';
    fixture.detectChanges();
    expect(component['error']()).toBeTruthy();
    httpMock.expectNone(() => true);
  });

  it('shows error when item-id has no colon', async () => {
    component.itemId = 'invalid-no-colon';
    fixture.detectChanges();
    expect(component['error']()).toMatch(/Invalid item ID/);
    httpMock.expectNone(() => true);
  });

  it('issues a Range request for the first page when item-id is valid', () => {
    component.itemId  = 'src1:42';
    component.apiBase = '/api';
    fixture.detectChanges();

    const req = httpMock.expectOne(r =>
        r.url.includes('/sources/src1/docs/42/content'));
    expect(req.request.headers.get('Range')).toBe('bytes=0-4095');
    req.flush(new ArrayBuffer(16), {
      headers: {'Content-Range': 'bytes 0-4095/16384'},
      status: 206,
      statusText: 'Partial Content',
    });
    fixture.detectChanges();
    expect(component['error']()).toBeNull();
    expect(component['rows']().length).toBeGreaterThan(0);
  });

  it('dispatches island-error CustomEvent on HTTP failure', () => {
    const events: CustomEvent[] = [];
    fixture.nativeElement.addEventListener('island-error', (e: Event) => events.push(e as CustomEvent));

    component.itemId = 'src1:99';
    fixture.detectChanges();
    const req = httpMock.expectOne(r => r.url.includes('/content'));
    req.error(new ProgressEvent('error'));
    fixture.detectChanges();

    expect(events.length).toBe(1);
    expect(events[0].detail.island).toBe('hex-viewer');
  });

  it('responds to next-page host event', () => {
    component.itemId = 'src1:1';
    fixture.detectChanges();
    const r1 = httpMock.expectOne(r => r.url.includes('/content'));
    r1.flush(new ArrayBuffer(4096), {
      headers: {'Content-Range': 'bytes 0-4095/32768'},
      status: 206, statusText: 'Partial Content',
    });
    fixture.detectChanges();

    fixture.nativeElement.dispatchEvent(new CustomEvent('next-page', {bubbles: false}));
    const r2 = httpMock.expectOne(r => r.url.includes('/content'));
    expect(r2.request.headers.get('Range')).toBe('bytes=4096-8191');
    r2.flush(new ArrayBuffer(4096), {
      headers: {'Content-Range': 'bytes 4096-8191/32768'},
      status: 206, statusText: 'Partial Content',
    });
  });
});
