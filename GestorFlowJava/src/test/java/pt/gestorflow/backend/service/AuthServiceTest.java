package pt.gestorflow.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import pt.gestorflow.backend.dto.LoginDTO;
import pt.gestorflow.backend.dto.LoginResponseDTO;
import pt.gestorflow.backend.model.Utilizador;
import pt.gestorflow.backend.repository.UtilizadorRepository;
import pt.gestorflow.backend.security.TokenService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

// Esta anotação diz ao JUnit para usar o Mockito para criar objetos falsos
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    // @Mock cria versões "falsas" (duplos) das tuas dependências
    @Mock
    private UtilizadorRepository utilizadorRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    // @InjectMocks cria o teu serviço real, mas injeta lá dentro as dependências falsas acima
    @InjectMocks
    private AuthService authService;

    private Utilizador utilizadorMock;
    private LoginDTO loginDTO;

    @BeforeEach
    void setUp() {
        // Preparamos dados fictícios antes de cada teste correr
        utilizadorMock = new Utilizador();
        utilizadorMock.setId(1L);
        utilizadorMock.setEmail("teste@gestorflow.pt");
        utilizadorMock.setSenha("SenhaEncriptada123");
        utilizadorMock.setNomeUtilizador("PedroTeste");

        loginDTO = new LoginDTO();
        loginDTO.setEmail("teste@gestorflow.pt");
        loginDTO.setSenha("SenhaReal123");
    }

    @Test
    void login_ComCredenciaisValidas_RetornaToken() {
        // 1. ARRANGE (Preparar o cenário)
        // Dizemos ao Mockito como se deve comportar quando o AuthService o chamar
        when(utilizadorRepository.findByEmail(loginDTO.getEmail())).thenReturn(Optional.of(utilizadorMock));
        when(passwordEncoder.matches(loginDTO.getSenha(), utilizadorMock.getSenha())).thenReturn(true);
        when(tokenService.gerarToken(utilizadorMock)).thenReturn("token.jwt.falso");

        // 2. ACT (Executar a ação que queremos testar)
        LoginResponseDTO resposta = authService.login(loginDTO);

        // 3. ASSERT (Verificar se o resultado é o esperado)
        assertNotNull(resposta);
        assertEquals("token.jwt.falso", resposta.getToken());
        assertEquals("PedroTeste", resposta.getNome());
        assertEquals("teste@gestorflow.pt", resposta.getEmail());

        // Verifica se os métodos foram realmente chamados
        verify(utilizadorRepository, times(1)).findByEmail(anyString());
        verify(tokenService, times(1)).gerarToken(any(Utilizador.class));
    }

    @Test
    void login_ComEmailInexistente_LancaExcecao() {
        // 1. ARRANGE: Fingimos que a base de dados não encontrou ninguém
        when(utilizadorRepository.findByEmail(loginDTO.getEmail())).thenReturn(Optional.empty());

        // 2 & 3. ACT & ASSERT: Executamos e confirmamos que a exceção certa é lançada
        IllegalArgumentException excecao = assertThrows(IllegalArgumentException.class, () -> {
            authService.login(loginDTO);
        });

        assertEquals("Credenciais Inválidas.", excecao.getMessage());

        // Garante que o sistema parou e não tentou gerar tokens
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(tokenService, never()).gerarToken(any());
    }

    @Test
    void login_ComPasswordIncorreta_LancaExcecao() {
        // 1. ARRANGE: O email existe, mas o encoder diz que a password não bate certo
        when(utilizadorRepository.findByEmail(loginDTO.getEmail())).thenReturn(Optional.of(utilizadorMock));
        when(passwordEncoder.matches(loginDTO.getSenha(), utilizadorMock.getSenha())).thenReturn(false);

        // 2 & 3. ACT & ASSERT
        IllegalArgumentException excecao = assertThrows(IllegalArgumentException.class, () -> {
            authService.login(loginDTO);
        });

        assertEquals("Credenciais Inválidas.", excecao.getMessage());

        // Garante que o token nunca é gerado para um intruso
        verify(tokenService, never()).gerarToken(any());
    }
}