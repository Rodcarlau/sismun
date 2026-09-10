package br.mil.controlemunicao;

import br.mil.controlemunicao.entity.EstoquePaiol;
import br.mil.controlemunicao.entity.Movimentacao;
import br.mil.controlemunicao.repository.EstoquePaiolRepository;
import br.mil.controlemunicao.repository.MilitarRepository;
import br.mil.controlemunicao.service.MovimentacaoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(username = "admin", roles = "ADMINISTRADOR")
class MovimentacaoEditarItensWebTest {
    @Autowired MockMvc mvc;
    @Autowired MovimentacaoService service;
    @Autowired EstoquePaiolRepository estoques;
    @Autowired MilitarRepository militares;

    @Test
    void botaoEditarItensAbreFormularioSemErro() throws Exception {
        EstoquePaiol estoque = estoques.findAll().get(0);
        Movimentacao movimentacao = new Movimentacao();
        movimentacao.setDiex("EDITAR-ITENS-WEB");
        movimentacao.setDataSolicitacao(LocalDate.now());
        movimentacao.setDataApanha(LocalDate.now().plusDays(1));
        movimentacao.setPaiolOrigem(estoque.getPaiol());
        movimentacao.setOmSolicitante(estoque.getPaiol().getOrganizacaoMilitar());
        movimentacao.setOficialMunicaoSolicitante(militares.findAll().get(0));
        service.criarSolicitacao(movimentacao, estoque.getId(), 1, null);

        mvc.perform(get("/movimentacoes/{id}/editar", movimentacao.getId()))
                .andExpect(status().isOk());
    }
}
