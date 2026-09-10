package br.mil.controlemunicao.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "movimentacao_escolta")
@Getter
@Setter
public class MovimentacaoEscolta extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "movimentacao_id", nullable = false)
    private Movimentacao movimentacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "militar_id", nullable = false)
    private Militar militar;

    @Column(length = 255)
    private String observacao;
}
