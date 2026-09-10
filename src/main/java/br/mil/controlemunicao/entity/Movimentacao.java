package br.mil.controlemunicao.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "movimentacao")
@Getter
@Setter
public class Movimentacao extends BaseEntity {

    @Column(length = 50)
    private String eb;

    @OneToMany(mappedBy = "movimentacao", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemMovimentacao> itens = new ArrayList<>();

    public void adicionarItem(ItemMovimentacao item) { itens.add(item); item.setMovimentacao(this); }
    public void removerItem(ItemMovimentacao item) { itens.remove(item); item.setMovimentacao(null); }

    @Column(nullable = false, length = 100)
    private String diex;

    @Column
    private LocalDate dataSolicitacao;

    @Column
    private LocalDate dataApanha;

    @Column
    private LocalDate dataAlteracao;

    @Column
    private LocalDate dataExecucaoAtividade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "om_local_apanha_id")
    private OrganizacaoMilitar omLocalApanha;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "om_solicitante_id")
    private OrganizacaoMilitar omSolicitante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "om_destino_id")
    private OrganizacaoMilitar omDestino;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paiol_origem_id")
    private Paiol paiolOrigem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paiol_destino_id")
    private Paiol paiolDestino;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oficial_responsavel_paiol_id")
    private Militar oficialResponsavelPaiol;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oficial_municao_solicitante_id")
    private Militar oficialMunicaoSolicitante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "militar_paiol_retirada_id")
    private Militar militarPaiolRetirada;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "militar_paiol_recebimento_id")
    private Militar militarPaiolRecebimento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oficial_municao_om_detentora_id")
    private Militar oficialMunicaoOmDetentora;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_municao_id")
    private TipoMunicao tipoMunicao;

    @Column
    private Integer quantidadeSolicitada;

    @ManyToMany
    @JoinTable(name = "movimentacao_viaturas_selecionadas", joinColumns = @JoinColumn(name = "movimentacao_id"), inverseJoinColumns = @JoinColumn(name = "viatura_id"))
    private List<Viatura> viaturas = new ArrayList<>();

    @ManyToMany
    @JoinTable(name = "movimentacao_escoltas_selecionadas", joinColumns = @JoinColumn(name = "movimentacao_id"), inverseJoinColumns = @JoinColumn(name = "militar_id"))
    private List<Militar> escoltas = new ArrayList<>();

    @ManyToMany
    @JoinTable(name = "movimentacao_motoristas_selecionados", joinColumns = @JoinColumn(name = "movimentacao_id"), inverseJoinColumns = @JoinColumn(name = "militar_id"))
    private List<Militar> motoristas = new ArrayList<>();

    @Column(length = 255)
    private String anexoNome;

    @Column(length = 120)
    private String anexoTipo;

    @Column(columnDefinition = "bytea")
    private byte[] anexoConteudo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oficial_responsavel_entrega_id")
    private Militar oficialResponsavelEntrega;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oficial_responsavel_recebimento_id")
    private Militar oficialResponsavelRecebimento;

    @Column(length = 255)
    private String motivoRetirada;

    @Column(length = 500)
    private String observacao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusMovimentacao status = StatusMovimentacao.RASCUNHO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_criador_id")
    private Usuario usuarioCriador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_ultima_alteracao_id")
    private Usuario usuarioUltimaAlteracao;

    @Column(nullable = false)
    private LocalDateTime dataHoraCriacao = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime dataHoraUltimaAlteracao = LocalDateTime.now();
}
