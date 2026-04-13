package pt.gestorflow.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode; // <--- Import obrigatório
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "tx_iva")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true) // <--- Obrigatório para a herança funcionar com o Lombok
public class TxIva extends Auditable { // <--- Escudo de Auditoria Ativado

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String descricao; // Ex: "Taxa Normal"

    private BigDecimal valor; // Ex: 23.00
}