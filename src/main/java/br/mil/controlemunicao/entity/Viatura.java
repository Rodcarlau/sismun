package br.mil.controlemunicao.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "viatura")
@Getter
@Setter
public class Viatura extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoViatura tipo;

    @Column(nullable = false, length = 120)
    private String modelo;

    @Column(nullable = false, unique = true, length = 20)
    private String placa;

    @Column(nullable = false, length = 30)
    private String prefixo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizacao_militar_id", nullable = false)
    private OrganizacaoMilitar organizacaoMilitar;

    @Column(nullable = false)
    private boolean ativo = true;
}
