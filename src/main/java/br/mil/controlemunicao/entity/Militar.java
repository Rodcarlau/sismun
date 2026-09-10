package br.mil.controlemunicao.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "militar")
@Getter
@Setter
public class Militar extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String nomeCompleto;

    @Column(nullable = false, unique = true, length = 50)
    private String identidade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PostoGraduacao postoGraduacao;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private FuncaoMilitar funcao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizacao_militar_id", nullable = false)
    private OrganizacaoMilitar organizacaoMilitar;

    @Column(length = 100)
    private String secao;

    @Column(length = 30)
    private String telefone;

    @Column(length = 120)
    private String email;

    @Column(nullable = false)
    private boolean ativo = true;
}
