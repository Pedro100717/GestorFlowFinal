import { Component, OnInit } from '@angular/core';
// O RouterOutlet tem de voltar a ser importado do @angular/router
import { Router, NavigationEnd, RouterOutlet } from '@angular/router'; 
import { filter } from 'rxjs/operators';

declare let gtag: Function;

@Component({
  selector: 'app-root',
  standalone: true, // Mantém a tua arquitetura standalone
  imports: [RouterOutlet], // Injeta o RouterOutlet para o app.html o reconhecer
  templateUrl: './app.html', // Aponta para o teu HTML correto
  styleUrls: ['./app.scss'] // Assumindo a extensão correta pelos teus logs
})
export class App implements OnInit { // O nome da classe volta a ser "App"
  
  private readonly measurementId = 'G-H26RJJ3NPC';

  constructor(private router: Router) {}

  ngOnInit(): void {
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe((event: any) => {
      gtag('config', this.measurementId, {
        page_path: event.urlAfterRedirects
      });
    });
  }
}