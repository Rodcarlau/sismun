package br.mil.controlemunicao.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "registro_auditoria")
@Getter
@Setter
public class RegistroAuditoria extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(nullable = false, length = 120)
    private String entidade;

    @Column
    private Long entidadeId;

    @Column(nullable = false, length = 60)
    private String acao;

    @Column(length = 500)
    private String descricao;

    @Column(nullable = false)
    private LocalDateTime dataHora;

    @Column(length = 80)
    private String enderecoIp;
}
