import { ComponentFixture, TestBed } from '@angular/core/testing';

// 1. Corrigido o nome na importação
import { OrcamentosComponent } from './orcamentos';

describe('OrcamentosComponent', () => {
  let component: OrcamentosComponent;
  let fixture: ComponentFixture<OrcamentosComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      // 2. Usar o nome correto aqui também
      imports: [OrcamentosComponent]
    })
    .compileComponents();

    // 3. E aqui
    fixture = TestBed.createComponent(OrcamentosComponent);
    component = fixture.componentInstance;
    
    // Na inicialização do componente, se for standalone, o detectChanges resolve a maioria dos problemas de UI no teste
    fixture.detectChanges(); 
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});