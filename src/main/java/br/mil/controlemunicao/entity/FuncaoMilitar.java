package br.mil.controlemunicao.entity;

import lombok.Getter;

@Getter
public enum FuncaoMilitar {
    OFICIAL_MUNICAO("Oficial de Munição"),
    SUBSTITUTO_OFICIAL_MUNICAO("Substituto do Oficial de Munição"),
    OFICIAL_TIRO("Oficial de Tiro"),
    SUBSTITUTO_OFICIAL_TIRO("Substituto do Oficial de Tiro"),
    ESCOLTA("Escolta"),
    MOTORISTA("Motorista"),
    CHEFE_VIATURA("Chefe de Viatura");

    private final String descricao;

    FuncaoMilitar(String descricao) { this.descricao = descricao; }
}
