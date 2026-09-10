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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@WithMockUser(username = "admin", roles = "ADMINISTRADOR")
class MovimentacaoCadastroWebTest {
    @Autowired MockMvc mvc;
    @Autowired EstoquePaiolRepository estoques;
    @Autowired MilitarRepository militares;

    @Test
    void carregaResponsavelDaDetentoraESalvaNovaMovimentacao() throws Exception {
        EstoquePaiol estoque = estoques.findAll().get(0);
        Militar oficial = new Militar();
        oficial.setNomeCompleto("Oficial Detentora Teste");
        oficial.setIdentidade("OF-DET-WEB");
        oficial.setPostoGraduacao(PostoGraduacao.CAPITAO);
        oficial.setFuncao(FuncaoMilitar.OFICIAL_MUNICAO);
        oficial.setOrganizacaoMilitar(estoque.getLoteMunicao().getOrganizacaoProprietaria());
        oficial.setAtivo(true);
        oficial = militares.saveAndFlush(oficial);

        mvc.perform(get("/api/militares/oficiais-municao/organizacao/{id}",
                        oficial.getOrganizacaoMilitar().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + oficial.getId() + ")]").exists());

        mvc.perform(multipart("/movimentacoes")
                        .file(new MockMultipartFile("arquivo", new byte[0]))
                        .with(csrf())
                        .param("diex", "WEB-TESTE")
                        .param("dataApanha", LocalDate.now().plusDays(1).toString())
                        .param("paiolOrigem", estoque.getPaiol().getId().toString())
                        .param("oficialMunicaoOmDetentora", oficial.getId().toString())
                        .param("itemId", "0")
                        .param("estoqueId", estoque.getId().toString())
                        .param("quantidadeItem", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/movimentacoes"));
    }

    @Test
    void itemIncompletoVoltaAoFormularioSemErroInesperado() throws Exception {
        EstoquePaiol estoque = estoques.findAll().get(0);
        mvc.perform(multipart("/movimentacoes")
                        .file(new MockMultipartFile("arquivo", new byte[0]))
                        .with(csrf())
                        .param("diex", "WEB-INCOMPLETA")
                        .param("dataApanha", LocalDate.now().plusDays(1).toString())
                        .param("paiolOrigem", estoque.getPaiol().getId().toString()))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("errorMessage"));
    }

    @Test
    void formularioExpoeFiltroPaiolTipoLoteVirola() throws Exception {
        mvc.perform(get("/movimentacoes/novo"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("mapa-filtro-estoque")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("data-paiol=")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("data-municao=")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("data-lote=")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("data-virola=")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("filtro-lote")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("filtro-virola")));
    }
}
