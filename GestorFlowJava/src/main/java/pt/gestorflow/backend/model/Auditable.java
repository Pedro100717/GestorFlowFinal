package pt.gestorflow.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class Auditable {

    @CreatedDate
    @Column(name = "data_criacao_sistema", updatable = false)
    private LocalDateTime dataCriacaoSistema;

    @LastModifiedDate
    @Column(name = "data_ultima_modificacao")
    private LocalDateTime dataUltimaModificacao;

    // 🛡️ RECOMENDAÇÃO: Em sistemas Multi-Tenant, o 'CreatedBy' costuma ser o ID (Long)
    // ou o Nome de Utilizador. Se o teu Utilizador usa ID Long, deves ser consistente.
    @CreatedBy
    @Column(name = "criado_por", updatable = false)
    private String criadoPor;

    @LastModifiedBy
    @Column(name = "modificado_por")
    private String modificadoPor;
}