package br.mil.controlemunicao.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "estoque_paiol",
    uniqueConstraints = @UniqueConstraint(name = "uk_estoque_paiol_lote", columnNames = {"paiol_id", "lote_municao_id"}))
@Getter
@Setter
public class EstoquePaiol extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paiol_id", nullable = false)
    private Paiol paiol;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lote_municao_id", nullable = false)
    private LoteMunicao loteMunicao;

    @Column(nullable = false)
    private Integer quantidadeAtual;

    @Column(nullable = false)
    private Integer quantidadeReservada = 0;

    public int getQuantidadeDisponivel() {
        return (quantidadeAtual == null ? 0 : quantidadeAtual) - (quantidadeReservada == null ? 0 : quantidadeReservada);
    }

    @Column(nullable = false)
    private LocalDateTime dataEntrada;

    @Column(nullable = false)
    private LocalDateTime ultimaMovimentacao;

    @Version
    private Long versao;
}
