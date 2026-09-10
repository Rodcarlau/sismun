package br.mil.controlemunicao.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tipo_municao")
@Getter
@Setter
@NoArgsConstructor
public class TipoMunicao extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String nome;

    @Column(length = 255)
    private String descricao;

    public TipoMunicao(String nome, String descricao) {
        this.nome = nome;
        this.descricao = descricao;
    }
}
