package pt.gestorflow.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "tx_iva")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TxIva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String descricao; // Ex: "Taxa Normal"

    private BigDecimal valor; // Ex: 23.00
}