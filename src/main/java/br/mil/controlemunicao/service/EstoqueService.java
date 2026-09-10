package br.mil.controlemunicao.service;

import br.mil.controlemunicao.entity.*;
import br.mil.controlemunicao.exception.EstoqueInsuficienteException;
import br.mil.controlemunicao.exception.ResourceNotFoundException;
import br.mil.controlemunicao.repository.EstoquePaiolRepository;
import br.mil.controlemunicao.repository.MovimentacaoEstoqueRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class EstoqueService {

    public record TotalConsolidado(String tipoMunicao, int quantidadeAtual, int quantidadeReservada, int quantidadeDisponivel) {}

    private final EstoquePaiolRepository estoquePaiolRepository;
    private final MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;

    public EstoqueService(EstoquePaiolRepository estoquePaiolRepository,
                          MovimentacaoEstoqueRepository movimentacaoEstoqueRepository) {
        this.estoquePaiolRepository = estoquePaiolRepository;
        this.movimentacaoEstoqueRepository = movimentacaoEstoqueRepository;
    }

    @Transactional
    public EstoquePaiol entradaEstoque(Paiol paiol, LoteMunicao loteMunicao, int quantidade) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser maior que zero.");
        }

        EstoquePaiol estoque = estoquePaiolRepository.findByPaiolAndLoteMunicao(paiol, loteMunicao)
            .orElseGet(() -> {
                EstoquePaiol novo = new EstoquePaiol();
                novo.setPaiol(paiol);
                novo.setLoteMunicao(loteMunicao);
                novo.setQuantidadeAtual(0);
                novo.setDataEntrada(LocalDateTime.now());
                novo.setUltimaMovimentacao(LocalDateTime.now());
                return novo;
            });

        int anterior = estoque.getQuantidadeAtual() == null ? 0 : estoque.getQuantidadeAtual();
        estoque.setQuantidadeAtual(anterior + quantidade);
        estoque.setUltimaMovimentacao(LocalDateTime.now());
        EstoquePaiol salvo = estoquePaiolRepository.save(estoque);

        registrarHistorico(salvo, TipoMovimentacaoEstoque.ENTRADA, quantidade, anterior, salvo.getQuantidadeAtual(), "Entrada de estoque");
        return salvo;
    }

    @Transactional
    public EstoquePaiol retirarEstoque(Paiol paiol, LoteMunicao loteMunicao, int quantidade) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser maior que zero.");
        }

        EstoquePaiol estoque = estoquePaiolRepository.findByPaiolAndLoteMunicao(paiol, loteMunicao)
            .orElseThrow(() -> new ResourceNotFoundException("Estoque não encontrado para o paiol e lote informados."));

        int anterior = estoque.getQuantidadeAtual();
        if (anterior < quantidade) {
            throw new EstoqueInsuficienteException("Estoque insuficiente para o lote selecionado.");
        }

        estoque.setQuantidadeAtual(anterior - quantidade);
        estoque.setUltimaMovimentacao(LocalDateTime.now());
        EstoquePaiol salvo = estoquePaiolRepository.save(estoque);

        registrarHistorico(salvo, TipoMovimentacaoEstoque.SAIDA, quantidade, anterior, salvo.getQuantidadeAtual(), "Retirada de estoque");
        return salvo;
    }

    @Transactional(readOnly = true)
    public int consultarSaldo(Paiol paiol, LoteMunicao loteMunicao) {
        return estoquePaiolRepository.findByPaiolAndLoteMunicao(paiol, loteMunicao)
            .map(EstoquePaiol::getQuantidadeAtual)
            .orElse(0);
    }

    @Transactional(readOnly = true)
    public List<EstoquePaiol> listarPorPaiol(Paiol paiol) {
        return estoquePaiolRepository.findByPaiol(paiol);
    }

    @Transactional(readOnly = true)
    public List<TotalConsolidado> totaisConsolidados() {
        Map<String, int[]> totais = new LinkedHashMap<>();
        for (EstoquePaiol estoque : estoquePaiolRepository.findAll()) {
            if (estoque.getLoteMunicao() == null || estoque.getLoteMunicao().getMunicao() == null) continue;
            Municao municao = estoque.getLoteMunicao().getMunicao();
            String tipo = municao.getTipoMunicao().getNome() + " - " + municao.getCalibre();
            int[] total = totais.computeIfAbsent(tipo, chave -> new int[3]);
            total[0] += estoque.getQuantidadeAtual() == null ? 0 : estoque.getQuantidadeAtual();
            total[1] += estoque.getQuantidadeReservada() == null ? 0 : estoque.getQuantidadeReservada();
            total[2] += estoque.getQuantidadeDisponivel();
        }
        return totais.entrySet().stream().map(e -> new TotalConsolidado(e.getKey(), e.getValue()[0], e.getValue()[1], e.getValue()[2])).toList();
    }

    @Transactional
    public EstoquePaiol receberTransferencia(Paiol destino, ItemMovimentacao item, int quantidade, Usuario usuario) {
        if (destino == null || destino.getId() == null) throw new IllegalArgumentException("Paiol de destino é obrigatório para concluir a transferência.");
        if (quantidade < 0) throw new IllegalArgumentException("Quantidade transferida não pode ser negativa.");
        if (quantidade == 0) return null;
        LoteMunicao lote = item.getLoteMunicao();
        LocalDateTime agora = LocalDateTime.now();
        EstoquePaiol estoque = estoquePaiolRepository.findDestinoForUpdate(destino.getId(), lote.getId()).orElseGet(() -> {
            EstoquePaiol novo = new EstoquePaiol(); novo.setPaiol(destino); novo.setLoteMunicao(lote); novo.setQuantidadeAtual(0); novo.setQuantidadeReservada(0); novo.setDataEntrada(agora); novo.setUltimaMovimentacao(agora); return novo;
        });
        int anterior = estoque.getQuantidadeAtual() == null ? 0 : estoque.getQuantidadeAtual();
        estoque.setQuantidadeAtual(anterior + quantidade); estoque.setUltimaMovimentacao(agora); estoque = estoquePaiolRepository.save(estoque);
        MovimentacaoEstoque historico = new MovimentacaoEstoque(); historico.setEstoque(estoque); historico.setItemMovimentacao(item); historico.setMovimentacao(item.getMovimentacao()); historico.setTipo(TipoMovimentacaoEstoque.ENTRADA); historico.setQuantidade(quantidade); historico.setSaldoAnterior(anterior); historico.setSaldoPosterior(estoque.getQuantidadeAtual()); historico.setReservadoAnterior(estoque.getQuantidadeReservada()); historico.setReservadoPosterior(estoque.getQuantidadeReservada()); historico.setStatusAnterior(StatusMovimentacao.EM_TRANSPORTE); historico.setStatusPosterior(StatusMovimentacao.ENTREGUE); historico.setDataHora(agora); historico.setUsuarioResponsavel(usuario); historico.setObservacao("Entrada por transferência entre paióis"); movimentacaoEstoqueRepository.save(historico);
        return estoque;
    }

    @Transactional
    public void baixarConsumoEDevolucaoNoDestino(ItemMovimentacao item, int quantidadeConsumida,
                                                  int quantidadeDevolvida, Usuario usuario) {
        Movimentacao movimentacao = item.getMovimentacao();
        if (movimentacao == null || movimentacao.getId() == null) throw new IllegalArgumentException("Movimentação válida é obrigatória para registrar o consumo.");
        // Compatibilidade com movimentações históricas de consumo direto, nas quais a entrega
        // já baixava o estoque de origem e não havia paiol de destino.
        if (movimentacao.getPaiolDestino() == null || movimentacao.getPaiolDestino().getId() == null) return;
        if (quantidadeConsumida < 0 || quantidadeDevolvida < 0) throw new IllegalArgumentException("Consumo e devolução não podem ser negativos.");
        int quantidadeBaixada = quantidadeConsumida + quantidadeDevolvida;
        if (quantidadeBaixada == 0) return;
        EstoquePaiol estoque = estoquePaiolRepository.findDestinoForUpdate(movimentacao.getPaiolDestino().getId(), item.getLoteMunicao().getId())
            .orElseThrow(() -> new ResourceNotFoundException("Estoque recebido no paiol de destino não encontrado."));
        int anterior = estoque.getQuantidadeAtual();
        if (anterior < quantidadeBaixada) throw new EstoqueInsuficienteException("Estoque insuficiente no paiol de destino para registrar consumo e devolução.");
        estoque.setQuantidadeAtual(anterior - quantidadeBaixada);
        estoque.setUltimaMovimentacao(LocalDateTime.now());
        estoquePaiolRepository.save(estoque);
        MovimentacaoEstoque historico = new MovimentacaoEstoque(); historico.setEstoque(estoque); historico.setItemMovimentacao(item); historico.setMovimentacao(movimentacao); historico.setTipo(TipoMovimentacaoEstoque.SAIDA); historico.setQuantidade(quantidadeBaixada); historico.setSaldoAnterior(anterior); historico.setSaldoPosterior(estoque.getQuantidadeAtual()); historico.setReservadoAnterior(estoque.getQuantidadeReservada()); historico.setReservadoPosterior(estoque.getQuantidadeReservada()); historico.setStatusAnterior(StatusMovimentacao.DEVOLUCAO_PENDENTE); historico.setStatusPosterior(StatusMovimentacao.FINALIZADA); historico.setDataHora(LocalDateTime.now()); historico.setUsuarioResponsavel(usuario); historico.setObservacao("Baixa no destino: consumo " + quantidadeConsumida + ", devolução " + quantidadeDevolvida); movimentacaoEstoqueRepository.save(historico);
    }

    @Transactional
    public EstoquePaiol devolverParaPaiol(Paiol destino, ItemMovimentacao item, int quantidade, Usuario usuario, Devolucao devolucao) {
        if (destino == null || destino.getId() == null) throw new IllegalArgumentException("Selecione o paiol de destino da devolução.");
        if (quantidade <= 0) throw new IllegalArgumentException("A quantidade devolvida deve ser maior que zero.");
        LocalDateTime agora=LocalDateTime.now(); LoteMunicao lote=item.getLoteMunicao();
        EstoquePaiol estoque=estoquePaiolRepository.findDestinoForUpdate(destino.getId(),lote.getId()).orElseGet(()->{EstoquePaiol novo=new EstoquePaiol();novo.setPaiol(destino);novo.setLoteMunicao(lote);novo.setQuantidadeAtual(0);novo.setQuantidadeReservada(0);novo.setDataEntrada(agora);novo.setUltimaMovimentacao(agora);return novo;});
        int anterior=estoque.getQuantidadeAtual()==null?0:estoque.getQuantidadeAtual();estoque.setQuantidadeAtual(anterior+quantidade);estoque.setUltimaMovimentacao(agora);estoque=estoquePaiolRepository.save(estoque);
        MovimentacaoEstoque h=new MovimentacaoEstoque();h.setEstoque(estoque);h.setItemMovimentacao(item);h.setMovimentacao(item.getMovimentacao());h.setDevolucao(devolucao);h.setTipo(TipoMovimentacaoEstoque.DEVOLUCAO);h.setQuantidade(quantidade);h.setSaldoAnterior(anterior);h.setSaldoPosterior(estoque.getQuantidadeAtual());h.setReservadoAnterior(estoque.getQuantidadeReservada());h.setReservadoPosterior(estoque.getQuantidadeReservada());h.setStatusAnterior(StatusMovimentacao.DEVOLUCAO_PENDENTE);h.setStatusPosterior(StatusMovimentacao.FINALIZADA);h.setDataHora(agora);h.setUsuarioResponsavel(usuario);h.setObservacao("Devolução para o paiol selecionado");movimentacaoEstoqueRepository.save(h);
        return estoque;
    }

    @Transactional
    public EstoquePaiol salvarCadastro(EstoquePaiol informado) {
        if (informado.getQuantidadeAtual() == null || informado.getQuantidadeAtual() < 0) throw new IllegalArgumentException("Estoque físico não pode ser negativo.");
        LocalDateTime agora = LocalDateTime.now();
        if (informado.getId() == null) {
            informado.setQuantidadeReservada(0); informado.setDataEntrada(agora); informado.setUltimaMovimentacao(agora);
            return estoquePaiolRepository.save(informado);
        }
        EstoquePaiol atual = estoquePaiolRepository.findByIdForUpdate(informado.getId()).orElseThrow(() -> new ResourceNotFoundException("Estoque não encontrado."));
        if (informado.getQuantidadeAtual() < atual.getQuantidadeReservada()) throw new IllegalArgumentException("Estoque físico não pode ser menor que a quantidade reservada.");
        int anterior = atual.getQuantidadeAtual(); atual.setQuantidadeAtual(informado.getQuantidadeAtual()); atual.setPaiol(informado.getPaiol()); atual.setLoteMunicao(informado.getLoteMunicao()); atual.setUltimaMovimentacao(agora);
        EstoquePaiol salvo = estoquePaiolRepository.save(atual);
        registrarHistorico(salvo, TipoMovimentacaoEstoque.AJUSTE, Math.abs(salvo.getQuantidadeAtual()-anterior), anterior, salvo.getQuantidadeAtual(), "Ajuste manual de estoque");
        return salvo;
    }

    private void registrarHistorico(EstoquePaiol estoque, TipoMovimentacaoEstoque tipo, int quantidade,
                                   int saldoAnterior, int saldoPosterior, String observacao) {
        MovimentacaoEstoque historico = new MovimentacaoEstoque();
        historico.setEstoque(estoque);
        historico.setTipo(tipo);
        historico.setQuantidade(quantidade);
        historico.setSaldoAnterior(saldoAnterior);
        historico.setSaldoPosterior(saldoPosterior);
        historico.setReservadoAnterior(estoque.getQuantidadeReservada() == null ? 0 : estoque.getQuantidadeReservada());
        historico.setReservadoPosterior(estoque.getQuantidadeReservada() == null ? 0 : estoque.getQuantidadeReservada());
        historico.setDataHora(LocalDateTime.now());
        historico.setObservacao(observacao);
        movimentacaoEstoqueRepository.save(historico);
    }
}
