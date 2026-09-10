package br.mil.controlemunicao.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "item_movimentacao")
@Getter
@Setter
public class ItemMovimentacao extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "movimentacao_id", nullable = false)
    private Movimentacao movimentacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lote_municao_id", nullable = false)
    private LoteMunicao loteMunicao;

    @Column(nullable = false)
    private Integer quantidadeSolicitada;

    @Column(nullable = false)
    private Integer quantidadeReservada = 0;

    @Column(nullable = false)
    private Integer quantidadeAutorizada;

    @Column(nullable = false)
    private Integer quantidadeSeparada = 0;

    @Column(nullable = false)
    private Integer quantidadeEntregue;

    @Column(nullable = false)
    private Integer quantidadeRecebida;

    @Column(length = 500)
    private String justificativa;

    @Column(length = 255)
    private String observacao;
}
