package br.mil.controlemunicao.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "municao")
@Getter
@Setter
public class Municao extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_municao_id", nullable = false)
    private TipoMunicao tipoMunicao;

    @Column(nullable = false, length = 80)
    private String calibre;

    @Column(nullable = false, length = 255)
    private String descricao;

    @Column(nullable = false, length = 150)
    private String fabricante;

    @Column(nullable = false)
    private boolean ativo = true;
}
