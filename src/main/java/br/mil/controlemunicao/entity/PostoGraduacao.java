package br.mil.controlemunicao.entity;

public enum PostoGraduacao {
    SOLDADO("Soldado"),
    CABO("Cabo"),
    TERCEIRO_SARGENTO("3º Sargento"),
    SEGUNDO_SARGENTO("2º Sargento"),
    PRIMEIRO_SARGENTO("1º Sargento"),
    SARGENTO("Sargento — classificação pendente"),
    SUBTENENTE("Subtenente"),
    SEGUNDO_TENENTE("2º Tenente"),
    PRIMEIRO_TENENTE("1º Tenente"),
    TENENTE("Tenente — classificação pendente"),
    CAPITAO("Capitão"),
    MAJOR("Major"),
    TENENTE_CORONEL("Tenente-Coronel"),
    CORONEL("Coronel"),
    GENERAL("General");

    private final String descricao;
    PostoGraduacao(String descricao) { this.descricao = descricao; }
    public String getDescricao() { return descricao; }
}
