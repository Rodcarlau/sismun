package br.mil.controlemunicao.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "movimentacao_motorista")
@Getter
@Setter
public class MovimentacaoMotorista extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "movimentacao_id", nullable = false)
    private Movimentacao movimentacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "motorista_id", nullable = false)
    private Militar motorista;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "viatura_id")
    private Viatura viatura;

    @Column(length = 255)
    private String observacao;
}
