package br.mil.controlemunicao.dto;
import br.mil.controlemunicao.entity.ItemMovimentacao;
public record ItemMovimentacaoView(ItemMovimentacao item, Long estoqueId, Long municaoId) {}
