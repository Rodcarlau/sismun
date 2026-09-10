package br.mil.controlemunicao.service;

import br.mil.controlemunicao.entity.*;
import br.mil.controlemunicao.exception.*;
import br.mil.controlemunicao.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.List;
import java.util.*;

@Service
public class MovimentacaoService {
    private final MovimentacaoRepository movimentacaoRepository;
    private final ItemMovimentacaoRepository itemRepository;
    private final EstoquePaiolRepository estoqueRepository;
    private final ReservaEstoqueService reservaService;
    private final DevolucaoRepository devolucaoRepository;
    private final ItemDevolucaoRepository itemDevolucaoRepository;
    private final ReservaEstoqueRepository reservaRepository;
    private final MovimentacaoEstoqueRepository historicoRepository;
    private final MilitarRepository militarRepository;
    private final PaiolRepository paiolRepository;
    private final EstoqueService estoqueService;

    public MovimentacaoService(MovimentacaoRepository movimentacaoRepository, ItemMovimentacaoRepository itemRepository,
        EstoquePaiolRepository estoqueRepository, ReservaEstoqueService reservaService, DevolucaoRepository devolucaoRepository,
        ItemDevolucaoRepository itemDevolucaoRepository, ReservaEstoqueRepository reservaRepository, MovimentacaoEstoqueRepository historicoRepository,
        MilitarRepository militarRepository, PaiolRepository paiolRepository, EstoqueService estoqueService) {
        this.movimentacaoRepository = movimentacaoRepository; this.itemRepository = itemRepository; this.estoqueRepository = estoqueRepository;
        this.reservaService = reservaService; this.devolucaoRepository = devolucaoRepository; this.itemDevolucaoRepository = itemDevolucaoRepository;
        this.reservaRepository = reservaRepository;
        this.historicoRepository = historicoRepository;
        this.militarRepository = militarRepository;
        this.paiolRepository = paiolRepository;
        this.estoqueService = estoqueService;
    }

    @Transactional
    public Movimentacao salvar(Movimentacao movimentacao) {
        validar(movimentacao); movimentacao.setDataHoraUltimaAlteracao(LocalDateTime.now());
        if (movimentacao.getId() == null) movimentacao.setDataHoraCriacao(LocalDateTime.now());
        return movimentacaoRepository.save(movimentacao);
    }

    @Transactional
    public Movimentacao criarSolicitacao(Movimentacao movimentacao, Long estoqueId, int quantidade, Usuario usuario) {
        return criarSolicitacaoComItens(movimentacao, List.of(estoqueId), List.of(quantidade), usuario);
    }

    @Transactional
    public Movimentacao criarSolicitacaoComItens(Movimentacao movimentacao, List<Long> estoqueIds, List<Integer> quantidades, Usuario usuario) {
        validar(movimentacao);
        validarCamposObrigatoriosDoFormulario(movimentacao);
        if (estoqueIds == null || quantidades == null || estoqueIds.isEmpty() || estoqueIds.size() != quantidades.size()) throw new BusinessException("Informe ao menos um material e sua quantidade.");
        validarDuplicados(estoqueIds);
        validarOrigens(movimentacao, estoqueIds);
        validarResponsaveis(movimentacao, estoqueIds);
        movimentacao.setStatus(StatusMovimentacao.SOLICITADA); movimentacao.setUsuarioCriador(usuario); movimentacao.setUsuarioUltimaAlteracao(usuario);
        movimentacao.setQuantidadeSolicitada(quantidades.stream().mapToInt(Integer::intValue).sum());
        movimentacao.setDataHoraCriacao(LocalDateTime.now()); movimentacao.setDataHoraUltimaAlteracao(LocalDateTime.now()); movimentacaoRepository.save(movimentacao);
        for (int indice = 0; indice < estoqueIds.size(); indice++) adicionarItem(movimentacao, estoqueIds.get(indice), quantidades.get(indice), usuario);
        return movimentacao;
    }

    private void adicionarItem(Movimentacao movimentacao, Long estoqueId, int quantidade, Usuario usuario) {
        if (estoqueId == null) throw new BusinessException("Lote e estoque de origem são obrigatórios.");
        EstoquePaiol estoque = estoqueRepository.findById(estoqueId).orElseThrow(() -> new ResourceNotFoundException("Estoque não encontrado."));
        validarEstoqueSelecionavel(estoque);
        if (movimentacao.getTipoMunicao() == null) movimentacao.setTipoMunicao(estoque.getLoteMunicao().getMunicao().getTipoMunicao());
        ItemMovimentacao item = novoItem(movimentacao, estoque.getLoteMunicao(), quantidade); itemRepository.save(item);
        reservaService.reservar(item, estoqueId, quantidade, usuario); movimentacao.adicionarItem(item);
    }

    @Transactional
    public Movimentacao atualizarItens(Long movimentacaoId, Movimentacao dados, List<Long> itemIds, List<Long> estoqueIds, List<Integer> quantidades, Usuario usuario) {
        Movimentacao m = movimentacaoRepository.findByIdForUpdate(movimentacaoId).orElseThrow(() -> new ResourceNotFoundException("Movimentação não encontrada."));
        exigirStatus(m, StatusMovimentacao.SOLICITADA);
        validarCamposObrigatoriosDoFormulario(dados);
        m.setEb(dados.getEb()); m.setDiex(dados.getDiex()); m.setDataSolicitacao(dados.getDataSolicitacao()); m.setDataApanha(dados.getDataApanha());
        m.setOmSolicitante(dados.getOmSolicitante()); m.setPaiolOrigem(dados.getPaiolOrigem()); m.setPaiolDestino(dados.getPaiolDestino()); m.setOficialMunicaoSolicitante(dados.getOficialMunicaoSolicitante());
        m.setMilitarPaiolRetirada(dados.getMilitarPaiolRetirada()); m.setMilitarPaiolRecebimento(dados.getMilitarPaiolRecebimento()); m.setOficialMunicaoOmDetentora(dados.getOficialMunicaoOmDetentora());
        m.setViaturas(dados.getViaturas()); m.setEscoltas(dados.getEscoltas()); m.setMotoristas(dados.getMotoristas()); m.setMotivoRetirada(dados.getMotivoRetirada()); m.setObservacao(dados.getObservacao());
        if (itemIds == null || estoqueIds == null || quantidades == null || itemIds.isEmpty() || itemIds.size()!=estoqueIds.size() || itemIds.size()!=quantidades.size()) throw new BusinessException("Informe ao menos um material e sua quantidade.");
        validarDuplicados(estoqueIds);
        validarOrigens(dados, estoqueIds);
        validarResponsaveis(dados, estoqueIds);
        Map<Long,ItemMovimentacao> existentes = new HashMap<>(); for (ItemMovimentacao i:itemRepository.findByMovimentacaoId(movimentacaoId)) existentes.put(i.getId(),i);
        Set<Long> mantidos = new HashSet<>();
        for(int x=0;x<itemIds.size();x++) {
            Long itemId=itemIds.get(x), estoqueId=estoqueIds.get(x); int quantidade=quantidades.get(x);
            if(itemId==null || itemId==0) { adicionarItem(m,estoqueId,quantidade,usuario); continue; }
            ItemMovimentacao item=existentes.get(itemId); if(item==null) throw new BusinessException("Item não pertence à movimentação."); mantidos.add(itemId);
            ReservaEstoque reserva=reservaRepository.findByItemMovimentacaoId(itemId).orElseThrow(()->new BusinessException("Reserva do item não encontrada."));
            if(reserva.getEstoque().getId().equals(estoqueId)) reservaService.alterarReserva(item,quantidade,usuario);
            else { EstoquePaiol novo=estoqueRepository.findById(estoqueId).orElseThrow(()->new ResourceNotFoundException("Estoque não encontrado.")); validarEstoqueSelecionavel(novo); reservaService.removerReserva(item,usuario); item.setLoteMunicao(novo.getLoteMunicao()); item.setQuantidadeSolicitada(quantidade); reservaService.reservar(item,estoqueId,quantidade,usuario); }
        }
        for(ItemMovimentacao item:existentes.values()) if(!mantidos.contains(item.getId())) { reservaService.removerReserva(item,usuario); historicoRepository.desvincularItem(item.getId()); m.getItens().removeIf(i->Objects.equals(i.getId(),item.getId())); itemRepository.delete(item); }
        m.setQuantidadeSolicitada(quantidades.stream().mapToInt(Integer::intValue).sum()); m.setUsuarioUltimaAlteracao(usuario); m.setDataHoraUltimaAlteracao(LocalDateTime.now()); return movimentacaoRepository.save(m);
    }

    @Transactional
    public Movimentacao alterarQuantidadeSolicitada(Long movimentacaoId, int novaQuantidade, Usuario usuario) {
        Movimentacao m = buscarPorId(movimentacaoId); exigirStatus(m, StatusMovimentacao.SOLICITADA);
        ItemMovimentacao item = itemUnico(movimentacaoId); reservaService.alterarReserva(item, novaQuantidade, usuario);
        m.setQuantidadeSolicitada(novaQuantidade); m.setDataHoraUltimaAlteracao(LocalDateTime.now()); return movimentacaoRepository.save(m);
    }

    @Transactional
    public Movimentacao autorizar(Long id, Usuario usuario) { Movimentacao m = buscarPorId(id); exigirStatus(m, StatusMovimentacao.SOLICITADA); validarParaAceite(m); for (ItemMovimentacao i : itens(id)) i.setQuantidadeAutorizada(i.getQuantidadeReservada()); return mudarStatus(m, StatusMovimentacao.AUTORIZADA, usuario); }

    @Transactional
    public Movimentacao confirmarSeparacao(Long id, Usuario usuario) { Movimentacao m = buscarPorId(id); exigirStatus(m, StatusMovimentacao.AUTORIZADA); for (ItemMovimentacao i : itens(id)) reservaService.separar(i, i.getQuantidadeAutorizada(), usuario); return mudarStatus(m, StatusMovimentacao.SEPARADA, usuario); }

    @Transactional
    public Movimentacao despachar(Long id, Usuario usuario) { Movimentacao m = buscarPorId(id); exigirStatus(m, StatusMovimentacao.SEPARADA); return mudarStatus(m, StatusMovimentacao.EM_TRANSPORTE, usuario); }

    @Transactional
    public Movimentacao confirmarEntrega(Long id, int quantidadeEntregue, String justificativa, Usuario usuario) {
        ItemMovimentacao unico=itemUnico(id); return confirmarEntregaItens(id,List.of(unico.getId()),List.of(quantidadeEntregue),justificativa,usuario);
    }

    @Transactional
    public Movimentacao confirmarEntregaItens(Long id, List<Long> itemIds, List<Integer> quantidadesEntregues, String justificativa, Usuario usuario) {
        Movimentacao m = movimentacaoRepository.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Movimentação não encontrada.")); exigirStatus(m, StatusMovimentacao.EM_TRANSPORTE); List<ItemMovimentacao> lista = itens(id);
        if(itemIds==null||quantidadesEntregues==null||itemIds.size()!=lista.size()||itemIds.size()!=quantidadesEntregues.size())throw new BusinessException("Informe a quantidade entregue de cada item.");
        Map<Long,Integer> porItem=new HashMap<>();for(int x=0;x<itemIds.size();x++)porItem.put(itemIds.get(x),quantidadesEntregues.get(x));
        for(ItemMovimentacao item:lista){Integer q=porItem.get(item.getId());if(q==null)throw new BusinessException("Quantidade entregue não informada para um item.");reservaService.entregar(item,q,justificativa,usuario);if(m.getPaiolDestino()!=null)estoqueService.receberTransferencia(m.getPaiolDestino(),item,q,usuario);}
        mudarStatus(m, StatusMovimentacao.ENTREGUE, usuario);
        Devolucao devolucao = devolucaoRepository.findByMovimentacaoId(id).orElseGet(() -> { Devolucao d = new Devolucao(); d.setMovimentacao(m); d.setDataDevolucao(LocalDate.now()); return devolucaoRepository.save(d); });
        if (itemDevolucaoRepository.findByDevolucaoId(devolucao.getId()).isEmpty()) for (ItemMovimentacao item : lista) { ItemDevolucao di = new ItemDevolucao(); di.setDevolucao(devolucao); di.setItemMovimentacao(item); di.setQuantidadeConsumida(0); di.setQuantidadeDevolvida(0); di.setQuantidadeEstojo(0); itemDevolucaoRepository.save(di); }
        return mudarStatus(m, StatusMovimentacao.DEVOLUCAO_PENDENTE, usuario);
    }

    @Transactional public void cancelar(Long id) { cancelar(id, null); }
    @Transactional public void cancelar(Long id, Usuario usuario) { Movimentacao m = buscarPorId(id); if (m.getStatus() == StatusMovimentacao.ENTREGUE || m.getStatus() == StatusMovimentacao.DEVOLUCAO_PENDENTE || m.getStatus() == StatusMovimentacao.FINALIZADA) throw new BusinessException("Movimentação entregue/finalizada não pode ser cancelada."); for (ItemMovimentacao i : itens(id)) reservaService.liberar(i, SituacaoReserva.CANCELADA, usuario); mudarStatus(m, StatusMovimentacao.CANCELADA, usuario); }

    @Transactional
    public void excluir(Long id, Usuario usuario) {
        Movimentacao m = movimentacaoRepository.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Movimentação não encontrada."));
        if (m.getStatus() != StatusMovimentacao.SOLICITADA && m.getStatus() != StatusMovimentacao.CANCELADA && m.getStatus() != StatusMovimentacao.RASCUNHO)
            throw new BusinessException("Não foi possível excluir porque a movimentação já possui etapas vinculadas.");
        List<ItemMovimentacao> lista = itemRepository.findByMovimentacaoId(id);
        for (ItemMovimentacao item : lista) reservaService.removerReserva(item, usuario);
        historicoRepository.deleteByMovimentacaoId(id);
        movimentacaoRepository.delete(m);
    }

    @Transactional(readOnly = true) public List<Movimentacao> listarTodos() { return movimentacaoRepository.findAll(); }
    @Transactional(readOnly = true) public Movimentacao buscarPorId(Long id) { Movimentacao m=movimentacaoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Movimentação não encontrada.")); m.getViaturas().size(); m.getEscoltas().forEach(x->{x.getOrganizacaoMilitar().getNome();}); m.getMotoristas().forEach(x->{x.getOrganizacaoMilitar().getNome();}); return m; }
    @Transactional(readOnly = true) public List<ItemMovimentacao> itens(Long id) { return itemRepository.findByMovimentacaoId(id); }
    private ItemMovimentacao novoItem(Movimentacao m, LoteMunicao lote, int q) { if (q <= 0) throw new BusinessException("Quantidade solicitada deve ser maior que zero."); ItemMovimentacao i = new ItemMovimentacao(); i.setMovimentacao(m); i.setLoteMunicao(lote); i.setQuantidadeSolicitada(q); i.setQuantidadeReservada(0); i.setQuantidadeAutorizada(0); i.setQuantidadeSeparada(0); i.setQuantidadeEntregue(0); i.setQuantidadeRecebida(0); return i; }
    private ItemMovimentacao itemUnico(Long id) { List<ItemMovimentacao> lista = itens(id); if (lista.size() != 1) throw new BusinessException("Esta operação requer exatamente um item."); return lista.get(0); }
    private void validar(Movimentacao m) { if (m.getDiex() == null || m.getDiex().isBlank()) throw new BusinessException("DIEx é obrigatório."); }
    private void validarDuplicados(List<Long> ids) { if(new HashSet<>(ids).size()!=ids.size()) throw new BusinessException("Este item já foi adicionado à movimentação. Altere a quantidade na linha existente."); }
    private void validarOrigens(Movimentacao m, List<Long> estoqueIds) {
        if (m.getPaiolOrigem() == null || m.getPaiolOrigem().getId() == null) throw new BusinessException("Paiol de origem é obrigatório.");
        boolean possuiOrigemPrincipal = estoqueIds.stream().map(id -> estoqueRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Estoque não encontrado."))).anyMatch(e -> e.getPaiol().getId().equals(m.getPaiolOrigem().getId()));
        if (!possuiOrigemPrincipal) throw new BusinessException("Informe ao menos um item retirado do paiol de origem principal.");
    }
    private void validarEstoqueSelecionavel(EstoquePaiol e) { LoteMunicao l=e.getLoteMunicao(); if(!l.isAtivo() || !l.getMunicao().isAtivo() || l.getDataValidade().isBefore(LocalDate.now())) throw new BusinessException("O lote selecionado está inativo ou vencido."); }
    private void exigirStatus(Movimentacao m, StatusMovimentacao esperado) { if (m.getStatus() != esperado) throw new BusinessException("Operação inválida no status " + m.getStatus() + ". Esperado: " + esperado + "."); }
    private void validarResponsaveis(Movimentacao m, List<Long> estoqueIds) {
        if (estoqueIds == null || estoqueIds.isEmpty() || estoqueIds.stream().anyMatch(Objects::isNull))
            throw new BusinessException("Selecione a munição, o lote e o estoque de origem de todos os itens.");
        Set<Long> detentoras = new HashSet<>();
        for (Long id : estoqueIds) { EstoquePaiol e=estoqueRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("Estoque não encontrado.")); OrganizacaoMilitar detentora=e.getPaiol().isPrincipal()&&e.getPaiol().getOmDetentoraMunicao()!=null?e.getPaiol().getOmDetentoraMunicao():e.getLoteMunicao().getOrganizacaoProprietaria();detentoras.add(detentora.getId()); }
        if(detentoras.size()>1) throw new BusinessException("Os itens da movimentação pertencem a Organizações Militares detentoras diferentes. Separe os itens em movimentações distintas ou ajuste os lotes selecionados.");
        Long omId=detentoras.iterator().next();
        m.setMilitarPaiolRetirada(validarMilitarPaiol(m.getMilitarPaiolRetirada(),m.getPaiolOrigem(),"retirada"));
        m.setMilitarPaiolRecebimento(validarMilitarPaiol(m.getMilitarPaiolRecebimento(),m.getPaiolDestino(),"recebimento"));
        if(m.getOficialMunicaoOmDetentora()!=null){Militar x=militar(m.getOficialMunicaoOmDetentora());if(!x.isAtivo()||!Set.of(FuncaoMilitar.OFICIAL_MUNICAO,FuncaoMilitar.SUBSTITUTO_OFICIAL_MUNICAO).contains(x.getFuncao())||!x.getOrganizacaoMilitar().getId().equals(omId))throw new BusinessException("O Oficial de Munição deve estar ativo, exercer a função permitida e pertencer à OM detentora das munições.");m.setOficialMunicaoOmDetentora(x);}
        Set<Long> ids=new HashSet<>();List<Militar> escoltas=new ArrayList<>();for(Militar r:Optional.ofNullable(m.getEscoltas()).orElseGet(ArrayList::new)){Militar x=militar(r);if(!ids.add(x.getId()))throw new BusinessException("O mesmo militar não pode aparecer duas vezes na escolta.");if(!x.isAtivo()||!Set.of(FuncaoMilitar.ESCOLTA,FuncaoMilitar.CHEFE_VIATURA,FuncaoMilitar.OFICIAL_MUNICAO,FuncaoMilitar.SUBSTITUTO_OFICIAL_MUNICAO).contains(x.getFuncao()))throw new BusinessException("A escolta contém militar inativo ou com função não permitida.");if(Set.of(FuncaoMilitar.OFICIAL_MUNICAO,FuncaoMilitar.SUBSTITUTO_OFICIAL_MUNICAO).contains(x.getFuncao())&&!x.getOrganizacaoMilitar().getId().equals(omId))throw new BusinessException("Oficial de Munição incluído na escolta deve pertencer à OM detentora.");escoltas.add(x);}m.setEscoltas(escoltas);
        ids.clear();List<Militar> motoristas=new ArrayList<>();for(Militar r:Optional.ofNullable(m.getMotoristas()).orElseGet(ArrayList::new)){Militar x=militar(r);if(!ids.add(x.getId()))throw new BusinessException("O mesmo motorista não pode aparecer duas vezes na movimentação.");if(!x.isAtivo()||x.getFuncao()!=FuncaoMilitar.MOTORISTA)throw new BusinessException("Motorista deve estar ativo e possuir a função Motorista.");motoristas.add(x);}m.setMotoristas(motoristas);
    }
    private Militar validarMilitarPaiol(Militar r,Paiol p,String tipo){if(r==null)return null;if(p==null||p.getId()==null)throw new BusinessException("Selecione o paiol de "+tipo+" antes do militar responsável.");Militar x=militar(r);Paiol paiol=paiolRepository.findById(p.getId()).orElseThrow(()->new ResourceNotFoundException("Paiol não encontrado."));if(!x.isAtivo()||!x.getOrganizacaoMilitar().getId().equals(paiol.getOrganizacaoMilitar().getId()))throw new BusinessException("O militar do paiol de "+tipo+" deve estar ativo e pertencer à mesma Organização Militar do paiol.");return x;}
    private Militar militar(Militar r){if(r==null||r.getId()==null)throw new BusinessException("Militar inválido.");return militarRepository.findById(r.getId()).orElseThrow(()->new ResourceNotFoundException("Militar não encontrado."));}
    public List<String> camposPendentesParaAceite(Movimentacao m) {
        List<String> pendentes = new ArrayList<>();
        if (m.getDiex() == null || m.getDiex().isBlank()) pendentes.add("DIEX");
        if (m.getDataApanha() == null) pendentes.add("Data da retirada");
        List<ItemMovimentacao> itensMovimentacao = m.getId() == null ? List.of() : itens(m.getId());
        if (itensMovimentacao.isEmpty()) pendentes.add("Itens da movimentação");
        else if (itensMovimentacao.stream().anyMatch(i -> i.getLoteMunicao() == null || i.getQuantidadeSolicitada() == null || i.getQuantidadeSolicitada() <= 0)) pendentes.add("Dados completos dos itens");
        return pendentes;
    }
    private void validarParaAceite(Movimentacao m) {
        List<String> pendentes = camposPendentesParaAceite(m);
        if (!pendentes.isEmpty()) throw new BusinessException("Preencha todos os campos obrigatórios antes do aceite: " + String.join(", ", pendentes) + ".");
    }
    private void validarCamposObrigatoriosDoFormulario(Movimentacao m) {
        List<String> pendentes = new ArrayList<>();
        if (m.getDiex() == null || m.getDiex().isBlank()) pendentes.add("DIEX");
        if (m.getDataApanha() == null) pendentes.add("Data da retirada");
        if (!pendentes.isEmpty()) throw new BusinessException("Preencha todos os campos obrigatórios: " + String.join(", ", pendentes) + ".");
    }
    private Movimentacao mudarStatus(Movimentacao m, StatusMovimentacao novo, Usuario u) { m.setStatus(novo); m.setUsuarioUltimaAlteracao(u); m.setDataHoraUltimaAlteracao(LocalDateTime.now()); return movimentacaoRepository.save(m); }
}
