package br.mil.controlemunicao.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "movimentacao_estoque")
@Getter
@Setter
public class MovimentacaoEstoque extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estoque_paiol_id", nullable = false)
    private EstoquePaiol estoque;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TipoMovimentacaoEstoque tipo;

    @Column(nullable = false)
    private Integer quantidade;

    @Column(nullable = false)
    private Integer saldoAnterior;

    @Column(nullable = false)
    private Integer saldoPosterior;

    @Column(nullable = false)
    private Integer reservadoAnterior = 0;

    @Column(nullable = false)
    private Integer reservadoPosterior = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_movimentacao_id")
    private ItemMovimentacao itemMovimentacao;

    @Enumerated(EnumType.STRING)
    private StatusMovimentacao statusAnterior;

    @Enumerated(EnumType.STRING)
    private StatusMovimentacao statusPosterior;

    @Column(nullable = false)
    private LocalDateTime dataHora;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movimentacao_id")
    private Movimentacao movimentacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "devolucao_id")
    private Devolucao devolucao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_responsavel_id")
    private Usuario usuarioResponsavel;

    @Column(length = 255)
    private String observacao;
}
