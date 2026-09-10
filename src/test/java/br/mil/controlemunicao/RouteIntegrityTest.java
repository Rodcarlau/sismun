package br.mil.controlemunicao;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class RouteIntegrityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginPageLoads() throws Exception {
        mockMvc.perform(get("/login"))
            .andExpect(status().isOk());
    }

    @Test
    void dashboardAndMainModulesLoadForAdmin() throws Exception {
        mockMvc.perform(get("/dashboard")
                .with(SecurityMockMvcRequestPostProcessors.user("admin").password("admin123").roles("ADMINISTRADOR")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Controle de Munição")))
            .andExpect(content().string(containsString("Dashboard")));

        mockMvc.perform(get("/municoes")
                .with(SecurityMockMvcRequestPostProcessors.user("admin").password("admin123").roles("ADMINISTRADOR")))
            .andExpect(status().isOk());

        mockMvc.perform(get("/paiols")
                .with(SecurityMockMvcRequestPostProcessors.user("admin").password("admin123").roles("ADMINISTRADOR")))
            .andExpect(status().isOk());

        mockMvc.perform(get("/estoque")
                .with(SecurityMockMvcRequestPostProcessors.user("admin").password("admin123").roles("ADMINISTRADOR")))
            .andExpect(status().isOk());
    }

    @Test
    void cadastroFormsLoadForAdmin() throws Exception {
        mockMvc.perform(get("/organizacoes/novo")
                .with(SecurityMockMvcRequestPostProcessors.user("admin").password("admin123").roles("ADMINISTRADOR")))
            .andExpect(status().isOk());

        mockMvc.perform(get("/municoes/novo")
                .with(SecurityMockMvcRequestPostProcessors.user("admin").password("admin123").roles("ADMINISTRADOR")))
            .andExpect(status().isOk());

        mockMvc.perform(get("/paiols/novo")
                .with(SecurityMockMvcRequestPostProcessors.user("admin").password("admin123").roles("ADMINISTRADOR")))
            .andExpect(status().isOk());

        mockMvc.perform(get("/movimentacoes/novo")
                .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMINISTRADOR")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Salvar movimentação")));

        String[] newForms = {"/lotes/novo", "/militares/novo", "/viaturas/novo", "/estoque/novo", "/usuarios/novo"};
        for (String route : newForms) {
            mockMvc.perform(get(route).with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isOk());
        }
    }

    @Test
    void allMenuModulesLoadForAdmin() throws Exception {
        String[] routes = {"/organizacoes", "/lotes", "/militares", "/viaturas", "/movimentacoes",
            "/devolucoes", "/usuarios", "/auditoria"};

        for (String route : routes) {
            mockMvc.perform(get(route)
                    .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isOk());
        }
    }
}
