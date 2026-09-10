package br.mil.controlemunicao.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "item_devolucao")
@Getter
@Setter
public class ItemDevolucao extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "devolucao_id", nullable = false)
    private Devolucao devolucao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_movimentacao_id", nullable = false)
    private ItemMovimentacao itemMovimentacao;

    @Column(nullable = false)
    private Integer quantidadeConsumida;

    @Column(nullable = false)
    private Integer quantidadeDevolvida;

    @Column(nullable = false)
    private Integer quantidadeEstojo;

    @Column(length = 500)
    private String justificativa;

    @Column(nullable = false)
    private boolean processado = false;

    @Column(nullable = false)
    private boolean consumoProcessado = false;

    @Column(nullable = false)
    private boolean devolucaoProcessada = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paiol_destino_id")
    private Paiol paiolDestino;

    private LocalDateTime dataHoraProcessamentoEstoque;

    @Column(length = 255)
    private String observacao;
}
