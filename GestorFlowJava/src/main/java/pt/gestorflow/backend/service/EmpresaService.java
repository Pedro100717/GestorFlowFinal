package pt.gestorflow.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.gestorflow.backend.dto.EmpresaDTO;
import pt.gestorflow.backend.exception.EmpresaNaoConfiguradaException;
import pt.gestorflow.backend.model.Empresa;
import pt.gestorflow.backend.model.Utilizador;
import pt.gestorflow.backend.repository.EmpresaRepository;
import pt.gestorflow.backend.repository.UtilizadorRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EmpresaService {

    private final EmpresaRepository empresaRepository;
    private final UtilizadorRepository utilizadorRepository;
    private final AuthService authService;

    @Transactional(readOnly = true)
    public Empresa obterEmpresaValidada() {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();

        return empresaRepository.findByUtilizadorId(utilizadorId)
                .orElseThrow(() -> new EmpresaNaoConfiguradaException(
                        "Configuração incompleta: É obrigatório configurar os dados da Empresa (NIF, Morada) antes de emitir documentos oficiais."
                ));
    }

    @Transactional(readOnly = true)
    public EmpresaDTO obterConfiguracoesAtuais() {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();
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

        Empresa empresa = empresaRepository.findByUtilizadorId(utilizadorId)
                .orElseGet(() -> {
                    Empresa nova = new Empresa();
                    Utilizador utilizador = utilizadorRepository.findById(utilizadorId)
                            .orElseThrow(() -> new IllegalArgumentException("Utilizador não encontrado."));
                    nova.setUtilizador(utilizador);
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
    }

    // 🚀 NOVO MÉTODO PARA GUARDAR APENAS O CAMINHO DO LOGÓTIPO
    @Transactional
    public void guardarCaminhoLogo(String caminho) {
        Long utilizadorId = authService.getUtilizadorAutenticadoId();
        Empresa empresa = empresaRepository.findByUtilizadorId(utilizadorId)
                .orElseThrow(() -> new EmpresaNaoConfiguradaException("Configure os dados da empresa primeiro antes de enviar o logótipo."));

        empresa.setLogotipoPath(caminho);
        empresaRepository.save(empresa);
    }
}