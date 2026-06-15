import {ComponentFixture, TestBed} from '@angular/core/testing';
import {HttpTestingController, provideHttpClientTesting} from '@angular/common/http/testing';
import {provideHttpClient} from '@angular/common/http';
import {TimelineComponent} from './timeline.component';

describe('TimelineComponent', () => {
  let fixture: ComponentFixture<TimelineComponent>;
  let component: TimelineComponent;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TimelineComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    fixture   = TestBed.createComponent(TimelineComponent);
    component = fixture.componentInstance;
    httpMock  = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('creates the component', () => {
    expect(component).toBeTruthy();
  });

  it('shows no bars when no caseId is set', () => {
    fixture.detectChanges();
    expect(component['bars']().length).toBe(0);
    httpMock.expectNone(() => true);
  });

  it('loads buckets from API when caseId is set', () => {
    component.caseId = 'c1';
    fixture.detectChanges();

    const req = httpMock.expectOne(r => r.url.includes('/timeline'));
    req.flush({buckets: [{date: '2023-01', count: 5}, {date: '2023-02', count: 12}]});
    fixture.detectChanges();

    expect(component['buckets']().length).toBe(2);
    expect(component['bars']().length).toBe(2);
  });

  it('falls back to demo data when API returns an error', () => {
    component.caseId = 'c1';
    fixture.detectChanges();

    const req = httpMock.expectOne(r => r.url.includes('/timeline'));
    req.error(new ProgressEvent('network-error'));
    fixture.detectChanges();

    // Demo data has 48 months
    expect(component['buckets']().length).toBe(48);
  });

  it('dispatches time-range-selected when drag covers > 0.5% of width', () => {
    component.caseId = 'c1';
    fixture.detectChanges();
    const req = httpMock.expectOne(r => r.url.includes('/timeline'));
    req.flush({buckets: Array.from({length: 24}, (_, i) => ({date: `2020-${String(i+1).padStart(2,'0')}`, count: 10}))});
    fixture.detectChanges();

    const events: CustomEvent[] = [];
    fixture.nativeElement.addEventListener('time-range-selected', (e: Event) => events.push(e as CustomEvent));

    // Simulate a drag covering 10%–50% of the timeline
    component['dragAnchor'].set(0.1);
    component['dragCurrent'].set(0.5);
    component['endDrag']();

    expect(events.length).toBe(1);
    expect(events[0].detail.start).toBeTruthy();
    expect(events[0].detail.end).toBeTruthy();
  });

  it('does NOT dispatch time-range-selected for tiny drags (< 0.5%)', () => {
    component.caseId = 'c1';
    fixture.detectChanges();
    const req = httpMock.expectOne(r => r.url.includes('/timeline'));
    req.flush({buckets: [{date: '2023-01', count: 1}]});
    fixture.detectChanges();

    const events: CustomEvent[] = [];
    fixture.nativeElement.addEventListener('time-range-selected', (e: Event) => events.push(e as CustomEvent));

    component['dragAnchor'].set(0.1);
    component['dragCurrent'].set(0.1001); // < 0.5% threshold
    component['endDrag']();

    expect(events.length).toBe(0);
  });
});
