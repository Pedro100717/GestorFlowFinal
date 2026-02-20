package pt.gestorflow.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ArtigoDTO {

    private Long id;

    @NotBlank(message = "O nome do artigo é obrigatório")
    private String nome;

    private String codigoBarras;

    // Removemos Preço, Custo e Stock Inicial daqui.
    // O Frontend vai enviar apenas se é Mercadoria ou Serviço.

    private Boolean movimentaStock = true;

    private Long familiaId;
}