package br.mil.controlemunicao.service;

import br.mil.controlemunicao.dto.DevolucaoForm;
import br.mil.controlemunicao.entity.*;
import br.mil.controlemunicao.exception.*;
import br.mil.controlemunicao.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DevolucaoService {
    private final DevolucaoRepository devolucaoRepository; private final ItemDevolucaoRepository itemRepository;
    private final MovimentacaoRepository movimentacaoRepository; private final ReservaEstoqueService reservaService; private final EstoqueService estoqueService; private final PaiolRepository paiolRepository; private final RegistroEntregaEfetivaRepository registroEntregaRepository;
    public DevolucaoService(DevolucaoRepository d, ItemDevolucaoRepository i, MovimentacaoRepository m, ReservaEstoqueService r, EstoqueService e, PaiolRepository p, RegistroEntregaEfetivaRepository registroEntregaRepository) { devolucaoRepository=d; itemRepository=i; movimentacaoRepository=m; reservaService=r; estoqueService=e; paiolRepository=p; this.registroEntregaRepository=registroEntregaRepository; }

    @Transactional
    public Devolucao registrarConsumo(Long devolucaoId, List<Long> itemIds, List<Integer> quantidades, Usuario usuario) {
        Devolucao d = bloquear(devolucaoId);
        validarListas(itemIds, quantidades, "consumida");
        for (int indice=0; indice<itemIds.size(); indice++) {
            ItemDevolucao item = item(devolucaoId, itemIds.get(indice));
            if (item.isConsumoProcessado()) throw new BusinessException("O consumo deste item já foi registrado.");
            Integer informada = quantidades.get(indice);
            if (informada == null) throw new BusinessException("A quantidade consumida é obrigatória.");
            int quantidade = informada, saldo = saldoRastreavel(item);
            if (quantidade < 0 || quantidade > saldo) throw new BusinessException("A quantidade consumida não pode superar o saldo disponível da movimentação.");
            estoqueService.baixarConsumoEDevolucaoNoDestino(item.getItemMovimentacao(), quantidade, 0, usuario);
            item.setQuantidadeConsumida(quantidade); item.setConsumoProcessado(true);
            if (saldo - quantidade == 0) item.setDevolucaoProcessada(true);
            atualizarProcessamento(item);
        }
        concluirSeCompleto(d);
        return devolucaoRepository.save(d);
    }

    @Transactional
    public Devolucao registrarDevolucao(Long devolucaoId, List<Long> itemIds, List<Integer> quantidades, List<Long> paiolDestinoIds,
            byte[] anexo, String nome, String tipo, String observacao, Usuario usuario) {
        Devolucao d = bloquear(devolucaoId);
        validarListas(itemIds, quantidades, "devolvida");
        if (paiolDestinoIds == null || paiolDestinoIds.size() != itemIds.size()) throw new BusinessException("Selecione o paiol de destino de cada devolução.");
        for (int indice=0; indice<itemIds.size(); indice++) {
            ItemDevolucao item = item(devolucaoId, itemIds.get(indice));
            if (item.isDevolucaoProcessada()) throw new BusinessException("A devolução deste item já foi registrada.");
            if (!item.isConsumoProcessado()) throw new BusinessException("Registre o consumo antes da devolução.");
            Integer informada = quantidades.get(indice);
            if (informada == null) throw new BusinessException("A quantidade devolvida é obrigatória.");
            int quantidade = informada, saldo = saldoRastreavel(item);
            if (quantidade < 0 || quantidade > saldo) throw new BusinessException("A quantidade devolvida não pode superar o saldo disponível da movimentação.");
            Paiol paiolDestino=paiolRepository.findById(paiolDestinoIds.get(indice)).orElseThrow(()->new ResourceNotFoundException("Paiol de devolução não encontrado."));
            estoqueService.baixarConsumoEDevolucaoNoDestino(item.getItemMovimentacao(), 0, quantidade, usuario);
            if (quantidade > 0) estoqueService.devolverParaPaiol(paiolDestino,item.getItemMovimentacao(),quantidade,usuario,d);
            item.setQuantidadeDevolvida(quantidade); item.setPaiolDestino(paiolDestino); item.setDevolucaoProcessada(true); item.setObservacao(observacao); item.setProcessado(item.getQuantidadeConsumida() + quantidade == quantidadeEfetiva(item.getItemMovimentacao())); item.setDataHoraProcessamentoEstoque(LocalDateTime.now());
        }
        d.setObservacao(observacao); if (anexo != null && anexo.length > 0) { d.setAnexoConteudo(anexo); d.setAnexoNome(nome); d.setAnexoTipo(tipo); }
        concluirSeCompleto(d);
        return devolucaoRepository.save(d);
    }

    @Transactional
    public Devolucao registrarDevolucao(Long devolucaoId, List<Long> itemIds, List<Integer> quantidades,
            byte[] anexo, String nome, String tipo, String observacao, Usuario usuario) {
        List<Long> destinos=itemIds.stream().map(id->{ItemDevolucao i=item(devolucaoId,id);return reservaService.paiolOrigem(i.getItemMovimentacao()).getId();}).toList();
        return registrarDevolucao(devolucaoId,itemIds,quantidades,destinos,anexo,nome,tipo,observacao,usuario);
    }

    private Devolucao bloquear(Long id) { return devolucaoRepository.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Devolução não encontrada.")); }
    private ItemDevolucao item(Long devolucaoId, Long itemId) { ItemDevolucao item=itemRepository.findById(itemId).orElseThrow(() -> new ResourceNotFoundException("Item não encontrado.")); if(!item.getDevolucao().getId().equals(devolucaoId))throw new BusinessException("Item não pertence à movimentação informada."); return item; }
    private void validarListas(List<Long> ids,List<Integer> quantidades,String tipo){if(ids==null||quantidades==null||ids.isEmpty()||ids.size()!=quantidades.size())throw new BusinessException("Informe a quantidade "+tipo+" de cada material.");}
    private int quantidadeEfetiva(ItemMovimentacao item) {
        List<RegistroEntregaEfetiva> registros = registroEntregaRepository.findByItemMovimentacaoIdOrderById(item.getId());
        if (!registros.isEmpty()) return registros.stream().mapToInt(r -> r.getQuantidadeEfetiva() == null ? 0 : r.getQuantidadeEfetiva()).sum();
        return item.getQuantidadeEntregue() == null ? 0 : item.getQuantidadeEntregue();
    }
    private int saldoRastreavel(ItemDevolucao item){return quantidadeEfetiva(item.getItemMovimentacao())-item.getQuantidadeConsumida()-item.getQuantidadeDevolvida();}
    private void validarConciliacao(Devolucao d) {
        for (ItemDevolucao item : itemRepository.findByDevolucaoId(d.getId())) {
            int efetiva = quantidadeEfetiva(item.getItemMovimentacao());
            int consumida = item.getQuantidadeConsumida() == null ? 0 : item.getQuantidadeConsumida();
            int devolvida = item.getQuantidadeDevolvida() == null ? 0 : item.getQuantidadeDevolvida();
            int saldo = efetiva - consumida - devolvida;
            if (saldo != 0) {
                throw new BusinessException("Movimentação não pode ser finalizada: item "
                        + item.getItemMovimentacao().getId() + " possui saldo documental pendente de " + saldo + ".");
            }
        }
    }
    private void atualizarProcessamento(ItemDevolucao item){item.setProcessado(item.isConsumoProcessado()&&item.isDevolucaoProcessada());item.setDataHoraProcessamentoEstoque(LocalDateTime.now());}
    private void concluirSeCompleto(Devolucao d){
        if(itemRepository.findByDevolucaoId(d.getId()).stream().allMatch(ItemDevolucao::isProcessado)){
            validarConciliacao(d);
            d.setStatus("FINALIZADA"); d.getMovimentacao().setStatus(StatusMovimentacao.FINALIZADA);
            d.getMovimentacao().setDataHoraUltimaAlteracao(LocalDateTime.now()); movimentacaoRepository.save(d.getMovimentacao());
        }
    }

    @Transactional
    public Devolucao registrarQuantidades(Long devolucaoId, List<Long> itemIds, List<Integer> quantidadesConsumidas, List<Integer> quantidadesDevolvidas,
            byte[] anexo, String nome, String tipo, String observacao, Usuario usuario) {
        Devolucao d = devolucaoRepository.findByIdForUpdate(devolucaoId).orElseThrow(() -> new ResourceNotFoundException("Devolução não encontrada."));
        if ("FINALIZADA".equals(d.getStatus())) throw new BusinessException("Esta devolução já foi processada no estoque.");
        if (itemIds == null || quantidadesConsumidas == null || quantidadesDevolvidas == null || itemIds.size() != quantidadesConsumidas.size() || itemIds.size() != quantidadesDevolvidas.size()) throw new BusinessException("Informe as quantidades consumida e devolvida de cada material.");
        for (int indice = 0; indice < itemIds.size(); indice++) {
            ItemDevolucao item = itemRepository.findById(itemIds.get(indice)).orElseThrow(() -> new ResourceNotFoundException("Item de devolução não encontrado."));
            if (!item.getDevolucao().getId().equals(devolucaoId)) throw new BusinessException("Item não pertence à devolução.");
            if (item.isProcessado()) continue;
            Integer informadaConsumida = quantidadesConsumidas.get(indice);
            Integer informadaDevolvida = quantidadesDevolvidas.get(indice);
            if (informadaConsumida == null || informadaDevolvida == null) throw new BusinessException("As quantidades consumida e devolvida são obrigatórias.");
            int entregue = quantidadeEfetiva(item.getItemMovimentacao()), consumida = informadaConsumida, devolvida = informadaDevolvida;
            if (consumida < 0 || devolvida < 0 || consumida + devolvida > entregue) throw new BusinessException("A soma consumida e devolvida não pode superar a quantidade entregue.");
            item.setQuantidadeDevolvida(devolvida); item.setQuantidadeConsumida(consumida); item.setQuantidadeEstojo(0); item.setObservacao(observacao);
            estoqueService.baixarConsumoEDevolucaoNoDestino(item.getItemMovimentacao(), consumida, devolvida, usuario);
            reservaService.devolver(item.getItemMovimentacao(), devolvida, usuario, d); item.setConsumoProcessado(true); item.setDevolucaoProcessada(consumida + devolvida == entregue); item.setProcessado(item.isDevolucaoProcessada()); item.setDataHoraProcessamentoEstoque(LocalDateTime.now());
        }
        d.setObservacao(observacao); if (anexo != null && anexo.length > 0) { d.setAnexoConteudo(anexo); d.setAnexoNome(nome); d.setAnexoTipo(tipo); }
        concluirSeCompleto(d);
        return devolucaoRepository.save(d);
    }

    @Transactional
    public Devolucao registrarQuantidades(Long devolucaoId, List<Long> itemIds, List<Integer> quantidadesDevolvidas,
            byte[] anexo, String nome, String tipo, String observacao, Usuario usuario) {
        Devolucao d = devolucaoRepository.findById(devolucaoId).orElseThrow(() -> new ResourceNotFoundException("Devolução não encontrada."));
        List<ItemDevolucao> itens = itemRepository.findByDevolucaoId(devolucaoId);
        List<Integer> consumidas = itemIds.stream().map(id -> {
            ItemDevolucao item = itens.stream().filter(i -> i.getId().equals(id)).findFirst().orElseThrow(() -> new ResourceNotFoundException("Item de devolução não encontrado."));
            return quantidadeEfetiva(item.getItemMovimentacao()) - quantidadesDevolvidas.get(itemIds.indexOf(id));
        }).toList();
        return registrarQuantidades(d.getId(), itemIds, consumidas, quantidadesDevolvidas, anexo, nome, tipo, observacao, usuario);
    }

    @Transactional
    public Devolucao registrar(Long devolucaoId, DevolucaoForm form, byte[] anexo, String nome, String tipo, Usuario usuario) {
        Devolucao d = devolucaoRepository.findByIdForUpdate(devolucaoId).orElseThrow(() -> new ResourceNotFoundException("Devolução não encontrada."));
        if ("FINALIZADA".equals(d.getStatus())) throw new BusinessException("Esta devolução já foi processada no estoque.");
        if (d.getMovimentacao().getStatus() != StatusMovimentacao.DEVOLUCAO_PENDENTE) throw new BusinessException("Movimentação não está aguardando devolução.");
        ItemDevolucao item = itemRepository.findById(form.itemDevolucaoId()).orElseThrow(() -> new ResourceNotFoundException("Item de devolução não encontrado."));
        if (!item.getDevolucao().getId().equals(devolucaoId)) throw new BusinessException("Item não pertence à devolução.");
        if (item.isProcessado()) return d;
        int entregue = quantidadeEfetiva(item.getItemMovimentacao());
        if (form.quantidadeConsumida() < 0 || form.quantidadeDevolvida() < 0 || form.quantidadeDevolvida() > entregue || form.quantidadeConsumida() + form.quantidadeDevolvida() > entregue) throw new BusinessException("Quantidades de devolução inválidas para o total entregue.");
        if (form.quantidadeConsumida() + form.quantidadeDevolvida() != entregue && (form.justificativa() == null || form.justificativa().isBlank())) throw new BusinessException("Justificativa administrativa é obrigatória quando a prestação não fecha a quantidade entregue.");
        item.setQuantidadeConsumida(form.quantidadeConsumida()); item.setQuantidadeDevolvida(form.quantidadeDevolvida()); item.setQuantidadeEstojo(form.quantidadeEstojo()); item.setJustificativa(form.justificativa()); item.setObservacao(form.observacao());
        estoqueService.baixarConsumoEDevolucaoNoDestino(item.getItemMovimentacao(), form.quantidadeConsumida(), form.quantidadeDevolvida(), usuario);
        reservaService.devolver(item.getItemMovimentacao(), form.quantidadeDevolvida(), usuario, d); item.setConsumoProcessado(true); item.setDevolucaoProcessada(form.quantidadeConsumida() + form.quantidadeDevolvida() == entregue); item.setProcessado(item.isDevolucaoProcessada()); item.setDataHoraProcessamentoEstoque(LocalDateTime.now());
        d.setObservacao(form.observacao()); if (anexo != null && anexo.length > 0) { d.setAnexoConteudo(anexo); d.setAnexoNome(nome); d.setAnexoTipo(tipo); }
        concluirSeCompleto(d);
        return devolucaoRepository.save(d);
    }
}
