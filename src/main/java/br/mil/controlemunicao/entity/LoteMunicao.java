package br.mil.controlemunicao.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "lote_municao", uniqueConstraints = {
    @UniqueConstraint(name = "uk_lote_municao", columnNames = {"municao_id", "lote", "virola"})
})
@Getter
@Setter
public class LoteMunicao extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "municao_id", nullable = false)
    private Municao municao;

    @Column(nullable = false, length = 80)
    private String lote;

    @Column(length = 80)
    private String virola;

    @Column(nullable = false)
    private LocalDate dataValidade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizacao_proprietaria_id", nullable = false)
    private OrganizacaoMilitar organizacaoProprietaria;

    @Column(nullable = false)
    private boolean ativo = true;
}
