package br.mil.controlemunicao;

import br.mil.controlemunicao.entity.Paiol;
import br.mil.controlemunicao.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
@WithMockUser(username="admin",roles="ADMINISTRADOR")
class PaiolPrincipalWebTest {
    @Autowired MockMvc mvc; @Autowired PaiolRepository paiols; @Autowired OrganizacaoMilitarRepository oms; @Autowired EstoquePaiolRepository estoques;

    @Test void permiteSomenteUmPrincipalEExigeOmDetentora() throws Exception {
        var om=oms.findAll().get(0); String codigo="PX-"+UUID.randomUUID();
        mvc.perform(post("/paiols").with(csrf()).param("nome","Principal").param("codigo",codigo).param("organizacaoMilitar",om.getId().toString()).param("principal","true").param("omDetentoraMunicao",om.getId().toString()).param("ativo","true")).andExpect(status().is3xxRedirection());
        Paiol principal=paiols.findByPrincipalTrue().orElseThrow(); assertThat(principal.getOmDetentoraMunicao().getId()).isEqualTo(om.getId());
        mvc.perform(post("/paiols").with(csrf()).param("nome","Outro").param("codigo","PY-"+UUID.randomUUID()).param("organizacaoMilitar",om.getId().toString()).param("principal","true").param("omDetentoraMunicao",om.getId().toString()).param("ativo","true")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Já existe um Paiol Principal")));
        assertThat(paiols.findAll().stream().filter(Paiol::isPrincipal)).hasSize(1);
    }

    @Test void telasExibemAccordionPorOmEVirolaComSiglaDaDetentora() throws Exception {
        var estoque=estoques.findAll().get(0); var paiol=estoque.getPaiol(); var om=oms.findAll().get(0); paiol.setPrincipal(true); paiol.setOmDetentoraMunicao(om); paiols.saveAndFlush(paiol);
        mvc.perform(get("/estoque")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("accordion"))).andExpect(content().string(org.hamcrest.Matchers.containsString(om.getSigla())));
        mvc.perform(get("/movimentacoes/novo")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("OM: "+om.getSigla())));
    }
}
