import { Component, OnInit } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';

// Declara a função global gtag
declare let gtag: Function;

@Component({
  selector: 'app-root',
  templateUrl: './app.html',
  styleUrls: ['./app.scss'] // (Ou .css, dependendo do que usas)
})
export class AppComponent implements OnInit {
  
  // O teu ID real
  private readonly measurementId = 'G-H26RJJ3NPC';

  constructor(private router: Router) {}

  ngOnInit(): void {
    // Fica à escuta de cada vez que o utilizador muda de página com sucesso
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe((event: any) => {
      // Avisa o Google Analytics da nova página
      gtag('config', this.measurementId, {
        page_path: event.urlAfterRedirects
      });
    });
  }
}