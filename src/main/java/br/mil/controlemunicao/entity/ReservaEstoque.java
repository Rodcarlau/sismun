package br.mil.controlemunicao.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "reserva_estoque", uniqueConstraints = @UniqueConstraint(name = "uk_reserva_item", columnNames = "item_movimentacao_id"))
@Getter @Setter
public class ReservaEstoque extends BaseEntity {
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_movimentacao_id", nullable = false)
    private ItemMovimentacao itemMovimentacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estoque_paiol_id", nullable = false)
    private EstoquePaiol estoque;

    @Column(nullable = false) private Integer quantidade;
    @Column(nullable = false) private Integer quantidadeConsumida = 0;
    @Column(nullable = false) private Integer quantidadeLiberada = 0;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private SituacaoReserva situacao = SituacaoReserva.ATIVA;
    @Column(nullable = false) private LocalDateTime dataHoraReserva;
    private LocalDateTime dataHoraSeparacao;
    private LocalDateTime dataHoraLiberacao;
    private LocalDateTime dataHoraBaixa;
    @ManyToOne(fetch = FetchType.LAZY) private Usuario usuarioResponsavel;
}
