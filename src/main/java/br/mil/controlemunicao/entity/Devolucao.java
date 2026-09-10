package br.mil.controlemunicao.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "devolucao")
@Getter
@Setter
public class Devolucao extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "movimentacao_id", nullable = false)
    private Movimentacao movimentacao;

    @Column(nullable = false)
    private LocalDate dataDevolucao;

    @Column(length = 500)
    private String observacao;

    @Column(nullable = false, length = 30)
    private String status = "PENDENTE";

    @Column(length = 255)
    private String anexoNome;

    @Column(length = 120)
    private String anexoTipo;

    @Column(columnDefinition = "bytea")
    private byte[] anexoConteudo;
}
