package br.mil.controlemunicao.dto;
import jakarta.validation.constraints.*;
public record DevolucaoForm(@NotNull Long itemDevolucaoId, @Min(0) int quantidadeConsumida,
    @Min(0) int quantidadeDevolvida, @Min(0) int quantidadeEstojo, String justificativa, String observacao) {}
