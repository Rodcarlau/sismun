package br.mil.controlemunicao.service;

import br.mil.controlemunicao.entity.*;
import br.mil.controlemunicao.exception.*;
import br.mil.controlemunicao.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class ReservaEstoqueService {
    private final EstoquePaiolRepository estoqueRepository;
    private final ReservaEstoqueRepository reservaRepository;
    private final MovimentacaoEstoqueRepository historicoRepository;

    public ReservaEstoqueService(EstoquePaiolRepository estoqueRepository, ReservaEstoqueRepository reservaRepository,
                                 MovimentacaoEstoqueRepository historicoRepository) {
        this.estoqueRepository = estoqueRepository;
        this.reservaRepository = reservaRepository;
        this.historicoRepository = historicoRepository;
    }

    @Transactional
    public ReservaEstoque reservar(ItemMovimentacao item, Long estoqueId, int quantidade, Usuario usuario) {
        validarQuantidadePositiva(quantidade);
        EstoquePaiol estoque = bloquearEstoque(estoqueId);
        if (!estoque.getLoteMunicao().getId().equals(item.getLoteMunicao().getId())) {
            throw new BusinessException("O lote selecionado não pertence ao estoque informado.");
        }
        validarDisponivel(estoque, quantidade);
        int reservadoAnterior = estoque.getQuantidadeReservada();
        estoque.setQuantidadeReservada(reservadoAnterior + quantidade);
        estoque.setUltimaMovimentacao(LocalDateTime.now());
        item.setQuantidadeReservada(quantidade);
        ReservaEstoque reserva = new ReservaEstoque();
        reserva.setItemMovimentacao(item); reserva.setEstoque(estoque); reserva.setQuantidade(quantidade);
        reserva.setDataHoraReserva(LocalDateTime.now()); reserva.setUsuarioResponsavel(usuario);
        reservaRepository.save(reserva);
        registrar(estoque, item, TipoMovimentacaoEstoque.RESERVA, quantidade, estoque.getQuantidadeAtual(), estoque.getQuantidadeAtual(),
            reservadoAnterior, estoque.getQuantidadeReservada(), null, StatusMovimentacao.SOLICITADA, usuario, "Reserva criada");
        return reserva;
    }

    @Transactional
    public ReservaEstoque alterarReserva(ItemMovimentacao item, int novaQuantidade, Usuario usuario) {
        validarQuantidadePositiva(novaQuantidade);
        ReservaEstoque reserva = reservaAtiva(item.getId());
        EstoquePaiol estoque = bloquearEstoque(reserva.getEstoque().getId());
        int anterior = reserva.getQuantidade();
        int diferenca = novaQuantidade - anterior;
        if (diferenca == 0) return reserva;
        int reservadoAnterior = estoque.getQuantidadeReservada();
        if (diferenca > 0) validarDisponivel(estoque, diferenca);
        estoque.setQuantidadeReservada(reservadoAnterior + diferenca);
        reserva.setQuantidade(novaQuantidade); reserva.setUsuarioResponsavel(usuario);
        item.setQuantidadeSolicitada(novaQuantidade); item.setQuantidadeReservada(novaQuantidade);
        registrar(estoque, item, diferenca >= 0 ? TipoMovimentacaoEstoque.RESERVA : TipoMovimentacaoEstoque.LIBERACAO_RESERVA,
            Math.abs(diferenca), estoque.getQuantidadeAtual(), estoque.getQuantidadeAtual(), reservadoAnterior,
            estoque.getQuantidadeReservada(), item.getMovimentacao().getStatus(), item.getMovimentacao().getStatus(), usuario,
            diferenca >= 0 ? "Acréscimo de reserva" : "Redução de reserva");
        return reserva;
    }

    @Transactional
    public void liberar(ItemMovimentacao item, SituacaoReserva situacao, Usuario usuario) {
        ReservaEstoque reserva = reservaRepository.findByItemMovimentacaoId(item.getId()).orElse(null);
        if (reserva == null || reserva.getSituacao() != SituacaoReserva.ATIVA) return;
        EstoquePaiol estoque = bloquearEstoque(reserva.getEstoque().getId());
        int reservadoAnterior = estoque.getQuantidadeReservada();
        int quantidade = reserva.getQuantidade();
        estoque.setQuantidadeReservada(reservadoAnterior - quantidade);
        reserva.setQuantidadeLiberada(reserva.getQuantidadeLiberada() + quantidade);
        reserva.setSituacao(situacao); reserva.setDataHoraLiberacao(LocalDateTime.now()); reserva.setUsuarioResponsavel(usuario);
        item.setQuantidadeReservada(0);
        registrar(estoque, item, TipoMovimentacaoEstoque.LIBERACAO_RESERVA, quantidade, estoque.getQuantidadeAtual(), estoque.getQuantidadeAtual(),
            reservadoAnterior, estoque.getQuantidadeReservada(), item.getMovimentacao().getStatus(), StatusMovimentacao.CANCELADA, usuario, "Reserva liberada");
    }

    @Transactional
    public void separar(ItemMovimentacao item, int quantidade, Usuario usuario) {
        ReservaEstoque reserva = reservaAtiva(item.getId());
        if (quantidade < 0 || quantidade > reserva.getQuantidade() || quantidade > item.getQuantidadeAutorizada())
            throw new BusinessException("Quantidade separada não pode superar a reservada ou autorizada.");
        item.setQuantidadeSeparada(quantidade); reserva.setDataHoraSeparacao(LocalDateTime.now()); reserva.setUsuarioResponsavel(usuario);
    }

    @Transactional
    public void entregar(ItemMovimentacao item, int quantidadeEntregue, String justificativa, Usuario usuario) {
        ReservaEstoque reserva = reservaAtiva(item.getId());
        if (quantidadeEntregue < 0 || quantidadeEntregue > reserva.getQuantidade() || quantidadeEntregue > item.getQuantidadeSeparada())
            throw new BusinessException("Quantidade entregue inválida para a reserva/separação existente.");
        int liberada = reserva.getQuantidade() - quantidadeEntregue;
        if (liberada > 0 && (justificativa == null || justificativa.isBlank())) throw new BusinessException("Justificativa é obrigatória para entrega parcial.");
        EstoquePaiol estoque = bloquearEstoque(reserva.getEstoque().getId());
        int fisicoAnterior = estoque.getQuantidadeAtual(), reservadoAnterior = estoque.getQuantidadeReservada();
        estoque.setQuantidadeAtual(fisicoAnterior - quantidadeEntregue);
        estoque.setQuantidadeReservada(reservadoAnterior - reserva.getQuantidade());
        estoque.setUltimaMovimentacao(LocalDateTime.now());
        item.setQuantidadeEntregue(quantidadeEntregue); item.setQuantidadeReservada(0); item.setJustificativa(justificativa);
        reserva.setQuantidadeConsumida(quantidadeEntregue); reserva.setQuantidadeLiberada(liberada);
        reserva.setSituacao(SituacaoReserva.CONSUMIDA); reserva.setDataHoraBaixa(LocalDateTime.now()); reserva.setUsuarioResponsavel(usuario);
        if (quantidadeEntregue > 0) {
            registrar(estoque, item, TipoMovimentacaoEstoque.SAIDA, quantidadeEntregue, fisicoAnterior, estoque.getQuantidadeAtual(), reservadoAnterior,
                reservadoAnterior - quantidadeEntregue, StatusMovimentacao.EM_TRANSPORTE, StatusMovimentacao.ENTREGUE, usuario, "Baixa após entrega");
        }
        if (liberada > 0) registrar(estoque, item, TipoMovimentacaoEstoque.LIBERACAO_RESERVA, liberada,
            estoque.getQuantidadeAtual(), estoque.getQuantidadeAtual(), reservadoAnterior - quantidadeEntregue, estoque.getQuantidadeReservada(),
            StatusMovimentacao.EM_TRANSPORTE, StatusMovimentacao.ENTREGUE, usuario, justificativa);
    }

    @Transactional(readOnly = true)
    public int consultarQuantidadeDisponivel(Long estoqueId) { return estoqueRepository.findById(estoqueId).orElseThrow().getQuantidadeDisponivel(); }

    @Transactional(readOnly = true)
    public Paiol paiolOrigem(ItemMovimentacao item) { return reservaRepository.findByItemMovimentacaoId(item.getId()).orElseThrow(() -> new BusinessException("Reserva da movimentação não encontrada.")).getEstoque().getPaiol(); }

    @Transactional
    public void devolver(ItemMovimentacao item, int quantidade, Usuario usuario) {
        devolver(item, quantidade, usuario, null);
    }

    @Transactional
    public void devolver(ItemMovimentacao item, int quantidade, Usuario usuario, Devolucao devolucao) {
        if (quantidade <= 0) return;
        ReservaEstoque reserva = reservaRepository.findByItemMovimentacaoId(item.getId()).orElseThrow(() -> new BusinessException("Reserva da entrega não encontrada."));
        EstoquePaiol estoque = bloquearEstoque(reserva.getEstoque().getId());
        int anterior = estoque.getQuantidadeAtual();
        estoque.setQuantidadeAtual(anterior + quantidade); estoque.setUltimaMovimentacao(LocalDateTime.now());
        MovimentacaoEstoque registro = registrar(estoque, item, TipoMovimentacaoEstoque.DEVOLUCAO, quantidade, anterior, estoque.getQuantidadeAtual(),
            estoque.getQuantidadeReservada(), estoque.getQuantidadeReservada(), StatusMovimentacao.DEVOLUCAO_PENDENTE,
            StatusMovimentacao.FINALIZADA, usuario, "Retorno de munição ao estoque");
        registro.setDevolucao(devolucao);
    }

    @Transactional
    public void removerReserva(ItemMovimentacao item, Usuario usuario) {
        ReservaEstoque reserva = reservaRepository.findByItemMovimentacaoId(item.getId()).orElse(null);
        liberar(item, SituacaoReserva.LIBERADA, usuario);
        if (reserva != null) { reservaRepository.delete(reserva); reservaRepository.flush(); }
    }

    private EstoquePaiol bloquearEstoque(Long id) { return estoqueRepository.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Estoque não encontrado.")); }
    private ReservaEstoque reservaAtiva(Long itemId) { ReservaEstoque r = reservaRepository.findByItemMovimentacaoId(itemId).orElseThrow(() -> new BusinessException("Reserva ativa não encontrada.")); if (r.getSituacao() != SituacaoReserva.ATIVA) throw new BusinessException("Reserva já processada."); return r; }
    private void validarQuantidadePositiva(int q) { if (q <= 0) throw new BusinessException("Quantidade solicitada deve ser maior que zero."); }
    private void validarDisponivel(EstoquePaiol e, int q) { if (e.getQuantidadeDisponivel() < q) { var l=e.getLoteMunicao(); throw new EstoqueInsuficienteException("Estoque disponível insuficiente para " + l.getMunicao().getTipoMunicao().getNome()+" "+l.getMunicao().getCalibre()+", lote "+l.getLote()+", virola "+l.getVirola()+". Disponível: " + e.getQuantidadeDisponivel() + ". Solicitado: " + q + "."); } }

    private MovimentacaoEstoque registrar(EstoquePaiol estoque, ItemMovimentacao item, TipoMovimentacaoEstoque tipo, int quantidade,
                           int saldoAnterior, int saldoPosterior, int reservadoAnterior, int reservadoPosterior,
                           StatusMovimentacao statusAnterior, StatusMovimentacao statusPosterior, Usuario usuario, String observacao) {
        MovimentacaoEstoque h = new MovimentacaoEstoque(); h.setEstoque(estoque); h.setItemMovimentacao(item); h.setMovimentacao(item.getMovimentacao());
        h.setTipo(tipo); h.setQuantidade(quantidade); h.setSaldoAnterior(saldoAnterior); h.setSaldoPosterior(saldoPosterior);
        h.setReservadoAnterior(reservadoAnterior); h.setReservadoPosterior(reservadoPosterior); h.setStatusAnterior(statusAnterior); h.setStatusPosterior(statusPosterior);
        h.setUsuarioResponsavel(usuario); h.setDataHora(LocalDateTime.now()); h.setObservacao(observacao); return historicoRepository.save(h);
    }
}
