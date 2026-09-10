package br.mil.controlemunicao;

import br.mil.controlemunicao.controller.MilitarController;
import br.mil.controlemunicao.entity.Militar;
import br.mil.controlemunicao.entity.OrganizacaoMilitar;
import br.mil.controlemunicao.entity.PostoGraduacao;
import br.mil.controlemunicao.repository.MilitarRepository;
import br.mil.controlemunicao.repository.OrganizacaoMilitarRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ConcurrentModel;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MilitarControllerSalvarTest {
    @Autowired MilitarController controller;
    @Autowired MilitarRepository militares;
    @Autowired OrganizacaoMilitarRepository organizacoes;

    private OrganizacaoMilitar om;

    @BeforeEach
    void preparar() {
        om = new OrganizacaoMilitar();
        om.setCodigo("OM-MIL-SALVAR");
        om.setNome("OM Militar Salvar");
        om.setSigla("OMS");
        om.setCidade("Rio");
        om.setUf("RJ");
        om.setAtiva(true);
        organizacoes.saveAndFlush(om);
    }

    @Test
    void cadastraEAlteraMilitarSemErro() {
        Militar formulario = militar("ID-SALVAR", "Militar Novo");
        assertThat(controller.salvar(formulario, new ConcurrentModel(), new RedirectAttributesModelMap()))
                .isEqualTo("redirect:/militares");

        Militar salvo = militares.findAll().stream()
                .filter(m -> "ID-SALVAR".equals(m.getIdentidade())).findFirst().orElseThrow();
        Militar alteracao = militar("ID-SALVAR", "Militar Alterado");
        alteracao.setId(salvo.getId());

        assertThat(controller.salvar(alteracao, new ConcurrentModel(), new RedirectAttributesModelMap()))
                .isEqualTo("redirect:/militares");
        assertThat(militares.findById(salvo.getId()).orElseThrow().getNomeCompleto())
                .isEqualTo("Militar Alterado");
    }

    @Test
    void identidadeRepetidaVoltaAoFormularioComMensagem() {
        militares.saveAndFlush(militar("ID-REPETIDA", "Primeiro"));
        ConcurrentModel model = new ConcurrentModel();

        assertThat(controller.salvar(militar("ID-REPETIDA", "Segundo"), model,
                new RedirectAttributesModelMap())).isEqualTo("militar/form");
        assertThat(model.getAttribute("errorMessage")).isEqualTo("Já existe um militar cadastrado com esta identidade.");
    }

    private Militar militar(String identidade, String nome) {
        Militar militar = new Militar();
        militar.setNomeCompleto(nome);
        militar.setIdentidade(identidade);
        militar.setPostoGraduacao(PostoGraduacao.CAPITAO);
        militar.setOrganizacaoMilitar(om);
        militar.setAtivo(true);
        return militar;
    }
}
