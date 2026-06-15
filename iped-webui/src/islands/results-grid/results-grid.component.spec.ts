import {ComponentFixture, TestBed} from '@angular/core/testing';
import {HttpTestingController, provideHttpClientTesting} from '@angular/common/http/testing';
import {provideHttpClient} from '@angular/common/http';
import {ResultsGridComponent} from './results-grid.component';

describe('ResultsGridComponent', () => {
  let fixture: ComponentFixture<ResultsGridComponent>;
  let component: ResultsGridComponent;
  let httpMock: HttpTestingController;

  const STUB_PAGE = {
    searchId: 'sid-1',
    total: 3,
    page: {offset: 0, limit: 25},
    items: [
      {itemId: 'src:1', name: 'report.pdf', path: '/evidence/report.pdf', mediaType: 'application/pdf', size: 12345, score: 0.9},
      {itemId: 'src:2', name: 'photo.jpg',  path: '/evidence/photo.jpg',  mediaType: 'image/jpeg',       size: 67890, score: 0.7},
      {itemId: 'src:3', name: 'data.xlsx',  path: '/evidence/data.xlsx',  mediaType: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', size: 4321, score: 0.5},
    ],
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ResultsGridComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture   = TestBed.createComponent(ResultsGridComponent);
    component = fixture.componentInstance;
    httpMock  = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('creates the component', () => {
    expect(component).toBeTruthy();
  });

  it('shows empty state when no caseId is set', async () => {
    fixture.detectChanges();
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('.empty-state')).toBeTruthy();
    httpMock.expectNone(() => true);
  });

  describe('with caseId', () => {
    beforeEach(() => {
      component.caseId = 'case-1';
      component.query  = '*';
      fixture.detectChanges();
    });

    function flushSearch(): void {
      // 1. POST /api/cases/case-1/search
      const createReq = httpMock.expectOne(r => r.method === 'POST' && r.url.includes('/search'));
      createReq.flush({searchId: 'sid-1'});
      // 2. GET /api/cases/case-1/search/sid-1/results
      const resultsReq = httpMock.expectOne(r => r.method === 'GET' && r.url.includes('sid-1/results'));
      resultsReq.flush(STUB_PAGE);
      fixture.detectChanges();
    }

    it('issues POST to create search then GET for results', () => {
      flushSearch();
      expect(component['items']()).toHaveSize(3);
    });

    it('renders a table row per result item', () => {
      flushSearch();
      const rows = (fixture.nativeElement as HTMLElement).querySelectorAll('tbody tr');
      expect(rows.length).toBe(3);
    });

    it('dispatches item-selected CustomEvent when a row is clicked', () => {
      flushSearch();
      const events: CustomEvent[] = [];
      fixture.nativeElement.addEventListener('item-selected', (e: Event) => events.push(e as CustomEvent));

      const row = (fixture.nativeElement as HTMLElement).querySelector('tbody tr') as HTMLElement;
      row.click();
      expect(events.length).toBe(1);
      expect(events[0].detail.itemId).toBe('src:1');
    });

    it('dispatches item-selected via Enter key on a row', () => {
      flushSearch();
      const events: CustomEvent[] = [];
      fixture.nativeElement.addEventListener('item-selected', (e: Event) => events.push(e as CustomEvent));

      const row = (fixture.nativeElement as HTMLElement).querySelector('tbody tr') as HTMLElement;
      row.dispatchEvent(new KeyboardEvent('keydown', {key: 'Enter', bubbles: true}));
      fixture.detectChanges();
      expect(events.length).toBe(1);
    });

    it('dispatches selection-changed when checkbox button is clicked', () => {
      flushSearch();
      const events: CustomEvent[] = [];
      fixture.nativeElement.addEventListener('selection-changed', (e: Event) => events.push(e as CustomEvent));

      const btn = (fixture.nativeElement as HTMLElement).querySelector('.check-dot') as HTMLElement;
      btn.click();
      expect(events.length).toBe(1);
      expect(events[0].detail.count).toBe(1);
    });

    it('dispatches results-loaded after successful fetch', () => {
      const events: CustomEvent[] = [];
      fixture.nativeElement.addEventListener('results-loaded', (e: Event) => events.push(e as CustomEvent));
      flushSearch();
      expect(events.length).toBe(1);
      expect(events[0].detail.total).toBe(3);
    });

    it('shows error state when POST fails', () => {
      const createReq = httpMock.expectOne(r => r.method === 'POST');
      createReq.error(new ProgressEvent('error'));
      fixture.detectChanges();
      const el = fixture.nativeElement as HTMLElement;
      expect(el.querySelector('.empty-state')).toBeTruthy();
      expect(component['error']()).toBeTruthy();
    });

    it('responds to previous-page host event', () => {
      flushSearch();
      // Advance to page 2
      component['offset'].set(25);
      component['total'].set(60);
      fixture.nativeElement.dispatchEvent(new CustomEvent('previous-page', {bubbles: false}));
      const req = httpMock.expectOne(r => r.url.includes('results'));
      expect(req.request.params.get('offset')).toBe('0');
      req.flush({...STUB_PAGE, page: {offset: 0, limit: 25}});
    });

    it('responds to next-page host event', () => {
      flushSearch();
      component['total'].set(100);
      component['offset'].set(0);
      fixture.nativeElement.dispatchEvent(new CustomEvent('next-page', {bubbles: false}));
      const req = httpMock.expectOne(r => r.url.includes('results'));
      expect(req.request.params.get('offset')).toBe('25');
      req.flush({...STUB_PAGE, page: {offset: 25, limit: 25}});
    });
  });

  it('table rows have tabindex and role for keyboard navigation', async () => {
    component.caseId = 'case-1';
    fixture.detectChanges();
    const createReq = httpMock.expectOne(r => r.method === 'POST');
    createReq.flush({searchId: 's'});
    const resultsReq = httpMock.expectOne(r => r.method === 'GET');
    resultsReq.flush(STUB_PAGE);
    fixture.detectChanges();
    const row = fixture.nativeElement.querySelector('tbody tr') as HTMLElement;
    expect(row.getAttribute('tabindex')).toBe('0');
    expect(row.getAttribute('role')).toBe('row');
  });
});
