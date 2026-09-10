package br.mil.controlemunicao.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "organizacao_militar")
@Getter
@Setter
public class OrganizacaoMilitar extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String nome;

    @Column(nullable = false, length = 50)
    private String sigla;

    @Column(nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(length = 120)
    private String cidade;

    @Column(length = 2)
    private String uf;

    @Column(nullable = false)
    private boolean ativa = true;
}
