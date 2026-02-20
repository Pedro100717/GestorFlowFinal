import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Tesouraria } from './tesouraria';

describe('Tesouraria', () => {
  let component: Tesouraria;
  let fixture: ComponentFixture<Tesouraria>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Tesouraria]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Tesouraria);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
