import { inject } from "@angular/core";
import { Router, CanActivateFn } from "@angular/router";

export const authGuard: CanActivateFn = (route, state) => {
    const router = inject(Router);
    const token = localStorage.getItem('token');

    // 🚀 1. DEFESA SÓLIDA: Um token JWT real (Bearer) tem de ter 3 partes separadas por um ponto.
    // Isto evita que alguém engane o sistema escrevendo apenas "123" no localStorage.
    const isTokenEstruturalmenteValido = token && token.split('.').length === 3;

    if (isTokenEstruturalmenteValido) {
        return true; // Passe livre
    } else {
        // 🚀 2. UX DE EXCELÊNCIA: Passamos a rota de destino (state.url) como parâmetro para o Login.
        // Assim, o componente de Login sabe para onde nos devolver depois do sucesso!
        router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
        return false; // Barrado
    }
}