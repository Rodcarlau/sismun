package br.mil.controlemunicao;

import br.mil.controlemunicao.entity.*;
import br.mil.controlemunicao.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(username = "admin", roles = "ADMINISTRADOR")
class ListagensMovimentacaoDevolucaoTest {
    @Autowired MockMvc mvc;
    @Autowired MovimentacaoRepository movimentacoes;
    @Autowired ItemMovimentacaoRepository itens;
    @Autowired DevolucaoRepository devolucoes;
    @Autowired ItemDevolucaoRepository itensDevolucao;
    @Autowired EstoquePaiolRepository estoques;
    @Autowired ReservaEstoqueRepository reservas;

    @Test
    void listaOrdenadaMostraConsumoECoresDistintas() throws Exception {
        Movimentacao antiga = movimentacao("LISTA-ANTIGA", LocalDate.of(2026, 1, 1), StatusMovimentacao.SOLICITADA);
        Movimentacao recente = movimentacao("LISTA-RECENTE", LocalDate.of(2026, 12, 1), StatusMovimentacao.FINALIZADA);
        Movimentacao pendente = movimentacao("LISTA-PENDENTE", LocalDate.of(2026, 6, 1), StatusMovimentacao.DEVOLUCAO_PENDENTE);
        registrarConsumo(recente, 37);

        var resultado = mvc.perform(get("/movimentacoes"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Consumo")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("bg-success")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("bg-warning text-dark")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("bg-primary")))
                .andReturn();

        @SuppressWarnings("unchecked")
        List<Movimentacao> lista = (List<Movimentacao>) resultado.getModelAndView().getModel().get("movimentacoes");
        var idsOrdenados = lista.stream().map(Movimentacao::getId).toList();
        assertThat(idsOrdenados.indexOf(recente.getId())).isLessThan(idsOrdenados.indexOf(pendente.getId()));
        assertThat(idsOrdenados.indexOf(pendente.getId())).isLessThan(idsOrdenados.indexOf(antiga.getId()));
        @SuppressWarnings("unchecked")
        var consumo = (java.util.Map<Long, Integer>) resultado.getModelAndView().getModel().get("consumoPorMovimentacao");
        assertThat(consumo.get(recente.getId())).isEqualTo(37);
    }

    @Test
    void devolucaoMostraAccordionMotivoEOmSolicitante() throws Exception {
        Movimentacao movimentacao = movimentacao("DEV-ACCORDION", LocalDate.now(), StatusMovimentacao.DEVOLUCAO_PENDENTE);
        movimentacao.setMotivoRetirada("Motivo solicitado");
        movimentacao.setOmSolicitante(estoques.findAll().get(0).getPaiol().getOrganizacaoMilitar());
        movimentacoes.saveAndFlush(movimentacao);
        registrarConsumo(movimentacao, 1);

        mvc.perform(get("/devolucoes"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("class=\"accordion\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("data-bs-toggle=\"collapse\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("painel-pendente")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("painel-finalizada")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("conteudo-devolucao-")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Paiol: " + estoques.findAll().get(0).getPaiol().getNome())))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Motivo solicitado")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(movimentacao.getOmSolicitante().getSigla())));
    }

    private Movimentacao movimentacao(String diex, LocalDate data, StatusMovimentacao status) {
        Movimentacao m = new Movimentacao();
        m.setDiex(diex);
        m.setDataSolicitacao(data);
        m.setStatus(status);
        return movimentacoes.saveAndFlush(m);
    }

    private void registrarConsumo(Movimentacao movimentacao, int quantidade) {
        var lote = estoques.findAll().get(0).getLoteMunicao();
        ItemMovimentacao item = new ItemMovimentacao();
        item.setMovimentacao(movimentacao); item.setLoteMunicao(lote); item.setQuantidadeSolicitada(quantidade);
        item.setQuantidadeReservada(quantidade); item.setQuantidadeAutorizada(quantidade); item.setQuantidadeSeparada(quantidade);
        item.setQuantidadeEntregue(quantidade); item.setQuantidadeRecebida(quantidade); item = itens.save(item);
        ReservaEstoque reserva = new ReservaEstoque(); reserva.setItemMovimentacao(item); reserva.setEstoque(estoques.findAll().get(0)); reserva.setQuantidade(quantidade); reserva.setDataHoraReserva(java.time.LocalDateTime.now()); reservas.save(reserva);
        Devolucao devolucao = new Devolucao(); devolucao.setMovimentacao(movimentacao); devolucao.setDataDevolucao(LocalDate.now());
        if (movimentacao.getStatus() == StatusMovimentacao.FINALIZADA) devolucao.setStatus("FINALIZADA");
        devolucao = devolucoes.save(devolucao);
        ItemDevolucao prestacao = new ItemDevolucao(); prestacao.setDevolucao(devolucao); prestacao.setItemMovimentacao(item);
        prestacao.setQuantidadeConsumida(quantidade); prestacao.setQuantidadeDevolvida(0); prestacao.setQuantidadeEstojo(0); prestacao.setConsumoProcessado(true);
        itensDevolucao.save(prestacao);
    }
}
