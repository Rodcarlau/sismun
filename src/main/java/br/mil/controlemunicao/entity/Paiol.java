package br.mil.controlemunicao.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "paiol")
@Getter
@Setter
public class Paiol extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String nome;

    @Column(nullable = false, unique = true, length = 50)
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizacao_militar_id", nullable = false)
    private OrganizacaoMilitar organizacaoMilitar;

    @Column(length = 255)
    private String endereco;

    @Column(length = 500)
    private String observacao;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(nullable = false)
    private boolean principal = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "om_detentora_municao_id")
    private OrganizacaoMilitar omDetentoraMunicao;
}
