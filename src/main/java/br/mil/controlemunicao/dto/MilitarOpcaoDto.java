package br.mil.controlemunicao.dto;

import br.mil.controlemunicao.entity.Militar;

public record MilitarOpcaoDto(Long id, String nomeCompleto, String postoGraduacao, String funcao, String organizacao) {
    public static MilitarOpcaoDto de(Militar m) {
        return new MilitarOpcaoDto(m.getId(), m.getNomeCompleto(), m.getPostoGraduacao().getDescricao(), m.getFuncao()==null?"Função não informada":m.getFuncao().getDescricao(), m.getOrganizacaoMilitar().getSigla());
    }
}
