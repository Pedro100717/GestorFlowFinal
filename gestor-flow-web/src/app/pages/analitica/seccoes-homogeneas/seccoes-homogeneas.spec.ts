import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SeccoesHomogeneas } from './seccoes-homogeneas';

describe('SeccoesHomogeneas', () => {
  let component: SeccoesHomogeneas;
  let fixture: ComponentFixture<SeccoesHomogeneas>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SeccoesHomogeneas]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SeccoesHomogeneas);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
