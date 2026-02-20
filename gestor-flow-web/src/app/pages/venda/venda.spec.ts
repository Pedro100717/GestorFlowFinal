import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Venda } from './venda';

describe('Venda', () => {
  let component: Venda;
  let fixture: ComponentFixture<Venda>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Venda]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Venda);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
