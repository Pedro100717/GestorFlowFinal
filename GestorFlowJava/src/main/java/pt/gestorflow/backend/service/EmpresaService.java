package pt.gestorflow.backend.service;

import jakarta.persistence.EntityNotFoundException; // 🚀 Adicionado para coerência com o resto do projeto
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 🚀 Logger ativado
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.gestorflow.backend.dto.EmpresaDTO;
import pt.gestorflow.backend.exception.EmpresaNaoConfiguradaException;
import pt.gestorflow.backend.model.Empresa;
import pt.gestorflow.backend.model.Utilizador;
import pt.gestorflow.backend.repository.EmpresaRepository;
import pt.gestorflow.backend.repository.UtilizadorRepository;

import java.util.Optional;

@Slf4j // 🚀 Anotação Mágica do Lombok
@Service
@RequiredArgsConstructor
public class EmpresaService {

    private final EmpresaRepository empresaRepository;
    private final UtilizadorRepository utilizadorRepository;
    private final AuthService authService;

    @Transactional(readOnly = true)
    public Empresa obterEmpresaValidada() {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        // 🥷 NINJA: Nenhum log de sucesso aqui! O método roda centenas de vezes.
        // Se falhar, o nosso GlobalExceptionHandler encarrega-se do log.warn.
        return empresaRepository.findByUtilizadorId(utilizadorId)
                .orElseThrow(() -> new EmpresaNaoConfiguradaException(
                        "Configuração incompleta: É obrigatório configurar os dados da Empresa (NIF, Morada) antes de emitir documentos oficiais."
                ));
    }

    @Transactional(readOnly = true)
    public EmpresaDTO obterConfiguracoesAtuais() {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        log.debug("A carregar configurações da empresa para o utilizador ID: {}", utilizadorId);

        Optional<Empresa> empresaOpt = empresaRepository.findByUtilizadorId(utilizadorId);

        if (empresaOpt.isEmpty()) {
            return new EmpresaDTO();
        }

        Empresa empresa = empresaOpt.get();
        EmpresaDTO dto = new EmpresaDTO();
        dto.setNomeFiscal(empresa.getNomeFiscal());
        dto.setNif(empresa.getNif());
        dto.setMoradaCompleta(empresa.getMoradaCompleta());
        dto.setCodigoPostal(empresa.getCodigoPostal());
        dto.setLocalidade(empresa.getLocalidade());
        dto.setTelefone(empresa.getTelefone());
        dto.setEmailGeral(empresa.getEmailGeral());
        dto.setLogotipoPath(empresa.getLogotipoPath());

        return dto;
    }

    @Transactional
    public void guardarConfiguracoes(EmpresaDTO dto) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        // 🛡️ INFO: Registo de Auditoria Fiscal Crítica
        log.info("ALERTA FISCAL: O utilizador ID: {} está a atualizar as configurações estruturais da Empresa (NIF: {})", utilizadorId, dto.getNif());

        Empresa empresa = empresaRepository.findByUtilizadorId(utilizadorId)
                .orElseGet(() -> {
                    Empresa nova = new Empresa();
                    Utilizador utilizador = utilizadorRepository.findById(utilizadorId)
                            // 🚀 Correção para o padrão do projeto
                            .orElseThrow(() -> new EntityNotFoundException("Utilizador não encontrado."));
                    nova.setUtilizador(utilizador);
                    log.debug("A criar o primeiro registo de empresa para o utilizador ID: {}", utilizadorId);
                    return nova;
                });

        empresa.setNomeFiscal(dto.getNomeFiscal());
        empresa.setNif(dto.getNif());
        empresa.setMoradaCompleta(dto.getMoradaCompleta());
        empresa.setCodigoPostal(dto.getCodigoPostal());
        empresa.setLocalidade(dto.getLocalidade());
        empresa.setTelefone(dto.getTelefone());
        empresa.setEmailGeral(dto.getEmailGeral());
        empresa.setLogotipoPath(dto.getLogotipoPath());

        empresaRepository.save(empresa);
        log.debug("Configurações da empresa guardadas com sucesso.");
    }

    @Transactional
    public void guardarCaminhoLogo(String caminho) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        log.info("O utilizador ID: {} atualizou o logótipo da empresa. Novo caminho: {}", utilizadorId, caminho);

        Empresa empresa = empresaRepository.findByUtilizadorId(utilizadorId)
                .orElseThrow(() -> new EmpresaNaoConfiguradaException("Configure os dados da empresa primeiro antes de enviar o logótipo."));

        empresa.setLogotipoPath(caminho);
        empresaRepository.save(empresa);
    }
}