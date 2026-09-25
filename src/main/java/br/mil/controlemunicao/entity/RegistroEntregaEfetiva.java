package br.mil.controlemunicao.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "registro_entrega_efetiva")
@Getter
@Setter
public class RegistroEntregaEfetiva extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_movimentacao_id", nullable = false)
    private ItemMovimentacao itemMovimentacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lote_municao_id", nullable = false)
    private LoteMunicao loteMunicao;

    @Column(nullable = false)
    private Integer quantidadeEfetiva;

    @Column(nullable = false)
    private Integer quantidadeReservada;

    @Column(nullable = false)
    private Integer quantidadeSeparada;

    @Column(length = 500)
    private String justificativa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_responsavel_id")
    private Usuario usuarioResponsavel;
}
