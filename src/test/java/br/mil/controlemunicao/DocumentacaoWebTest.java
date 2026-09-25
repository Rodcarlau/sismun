package br.mil.controlemunicao;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DocumentacaoWebTest {
    @Autowired MockMvc mvc;

    @Test void portalRenderiza() throws Exception {
        mvc.perform(get("/documentacao").with(user("admin")))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Documentação do Sistema")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("/documentacao/baixar/completa")));
    }

    @Test void pdfDisponivel() throws Exception {
        mvc.perform(get("/documentacao/baixar/completa").with(user("admin")))
            .andExpect(status().isOk()).andExpect(content().contentType("application/pdf"))
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("SisMun-completa.pdf")));
    }

    @Test void fluxogramasDosMenusSaoVisualizaveisEBaixaveis() throws Exception {
        String[] menus = {"dashboard", "organizacoes", "municoes", "lotes", "paiols", "militares", "viaturas",
            "estoque", "movimentacoes", "devolucoes", "formularios", "usuarios", "auditoria", "documentacao"};
        for (String menu : menus) {
            mvc.perform(get("/documentacao/fluxogramas/" + menu).with(user("admin")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("flow-stage")));
            mvc.perform(get("/documentacao/fluxogramas/" + menu + ".svg").with(user("admin")))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith("image/svg+xml"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<svg")));
            mvc.perform(get("/documentacao/fluxogramas/" + menu + ".pdf").with(user("admin")))
                .andExpect(status().isOk()).andExpect(content().contentType("application/pdf"));
        }
    }

    @Test void pdfConsolidadoDisponivel() throws Exception {
        mvc.perform(get("/documentacao/fluxogramas.pdf").with(user("admin")))
            .andExpect(status().isOk()).andExpect(content().contentType("application/pdf"))
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("fluxogramas_sismun.pdf")));
    }
}
