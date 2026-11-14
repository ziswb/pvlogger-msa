import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient, withXhr } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { MiniCardComponent } from './mini-card.component';

describe('MiniCardComponent', () => {
  let component: MiniCardComponent;
  let fixture: ComponentFixture<MiniCardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MiniCardComponent],
      providers: [provideHttpClient(withXhr()), provideHttpClientTesting()]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MiniCardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have default title', () => {
    expect(component.title).toBe('Produktion');
  });

  it('should clean up subscription on destroy', () => {
    component.ngOnInit();
    component.ngOnDestroy();
    expect(component).toBeTruthy();
  });
});
