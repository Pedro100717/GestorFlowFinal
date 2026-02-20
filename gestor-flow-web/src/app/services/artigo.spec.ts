import { TestBed } from '@angular/core/testing';

import { Artigo } from './artigo';

describe('Artigo', () => {
  let service: Artigo;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(Artigo);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
