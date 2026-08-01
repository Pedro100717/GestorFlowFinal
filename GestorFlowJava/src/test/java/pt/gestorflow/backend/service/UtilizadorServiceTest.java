package pt.gestorflow.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import pt.gestorflow.backend.dto.PerfilResponseDTO;
import pt.gestorflow.backend.dto.PerfilUtilizadorDTO;
import pt.gestorflow.backend.dto.RegistoDTO;
import pt.gestorflow.backend.model.Utilizador;
import pt.gestorflow.backend.repository.UtilizadorRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UtilizadorServiceTest {

    @Mock
    private UtilizadorRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthService authService;

    @InjectMocks
    private UtilizadorService utilizadorService;

    private Utilizador utilizadorMock;

    @BeforeEach
    void setUp() {
        utilizadorMock = new Utilizador();
        utilizadorMock.setId(1L);
        utilizadorMock.setNomeUtilizador("pedroleite");
        utilizadorMock.setEmail("pedro@gestorflow.pt");
        utilizadorMock.setSenha("senhaEncriptadaMestre");
    }

    @Test
    void registarNovoUtilizador_ComDadosValidos_CriaUtilizadorEEncriptaSenha() {
        // ARRANGE
        RegistoDTO dto = new RegistoDTO();
        dto.setNomeUtilizador("pedroleite");
        dto.setEmail("pedro@gestorflow.pt");
        dto.setSenha("senha123");

        when(repository.existsByEmail(dto.getEmail())).thenReturn(false);
        when(repository.existsByNomeUtilizador(dto.getNomeUtilizador())).thenReturn(false);
        when(passwordEncoder.encode(dto.getSenha())).thenReturn("senhaEncriptadaMestre");
        when(repository.save(any(Utilizador.class))).thenReturn(utilizadorMock);

        // ACT
        PerfilResponseDTO resposta = utilizadorService.registarNovoUtilizador(dto);

        // ASSERT
        assertNotNull(resposta, "O perfil retornado não devia ser nulo.");
        assertEquals("pedroleite", resposta.getNomeUtilizador());
        assertEquals("pedro@gestorflow.pt", resposta.getEmail());

        // Garante que a password original nunca é gravada limpa na BD
        verify(passwordEncoder, times(1)).encode("senha123");
        verify(repository, times(1)).save(any(Utilizador.class));
    }

    @Test
    void registarNovoUtilizador_ComEmailOuNomeJaExistente_LancaExcecao() {
        // ARRANGE
        RegistoDTO dto = new RegistoDTO();
        dto.setNomeUtilizador("pedroleite");
        dto.setEmail("pedro@gestorflow.pt");

        // Simula que o email já existe na base de dados
        when(repository.existsByEmail(dto.getEmail())).thenReturn(true);

        // ACT & ASSERT
        IllegalArgumentException excecao = assertThrows(IllegalArgumentException.class, () -> {
            utilizadorService.registarNovoUtilizador(dto);
        });

        assertEquals("Os dados introduzidos são inválidos ou já estão em uso.", excecao.getMessage());

        // Blindagem: O sistema tem de parar imediatamente e nunca disparar o save
        verify(repository, never()).save(any());
    }

    @Test
    void atualizarPerfil_ComNovosDadosValidos_ModificaNomeEEmail() {
        // ARRANGE
        PerfilUtilizadorDTO dto = new PerfilUtilizadorDTO();
        dto.setNome("pedroNovo");
        dto.setEmail("pedro.novo@gestorflow.pt");

        when(authService.getUtilizadorAutenticadoId()).thenReturn(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(utilizadorMock));
        when(repository.existsByEmail(dto.getEmail())).thenReturn(false);
        when(repository.existsByNomeUtilizador(dto.getNome())).thenReturn(false);
        when(repository.save(any(Utilizador.class))).thenAnswer(i -> i.getArgument(0));

        // ACT
        PerfilResponseDTO resposta = utilizadorService.atualizarPerfil(dto);

        // ASSERT
        assertNotNull(resposta);
        assertEquals("pedroNovo", resposta.getNomeUtilizador());
        assertEquals("pedro.novo@gestorflow.pt", resposta.getEmail());
        verify(repository, times(1)).save(utilizadorMock);
    }

    @Test
    void atualizarPerfil_TentandoUsarEmailDeOutraPessoa_LancaExcecao() {
        // ARRANGE
        PerfilUtilizadorDTO dto = new PerfilUtilizadorDTO();
        dto.setNome("pedroleite");
        dto.setEmail("outro.utilizador@gestorflow.pt"); // Email já ocupado na BD

        when(authService.getUtilizadorAutenticadoId()).thenReturn(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(utilizadorMock));

        // O email é diferente do atual, e o repositório diz que já existe noutra linha
        when(repository.existsByEmail(dto.getEmail())).thenReturn(true);

        // ACT & ASSERT
        IllegalArgumentException excecao = assertThrows(IllegalArgumentException.class, () -> {
            utilizadorService.atualizarPerfil(dto);
        });

        assertEquals("Este email já está em uso por outra conta.", excecao.getMessage());
        verify(repository, never()).save(any());
    }
}