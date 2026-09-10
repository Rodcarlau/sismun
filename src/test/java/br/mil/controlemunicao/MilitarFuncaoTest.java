package br.mil.controlemunicao;

import br.mil.controlemunicao.entity.*;
import br.mil.controlemunicao.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MilitarFuncaoTest {
    @Autowired MilitarRepository militares;
    @Autowired OrganizacaoMilitarRepository organizacoes;

    @Test void persisteFuncoesPostosLegadosENovosEAplicaFiltros() {
        OrganizacaoMilitar om=new OrganizacaoMilitar();om.setCodigo("OM-FUNCAO");om.setNome("OM Função");om.setSigla("OMF");om.setCidade("Rio");om.setUf("RJ");om.setAtiva(true);organizacoes.save(om);
        Militar antigo=novo(om,"ID-0",PostoGraduacao.TENENTE,null,true);
        Militar motorista=novo(om,"ID-1",PostoGraduacao.SEGUNDO_TENENTE,FuncaoMilitar.MOTORISTA,true);
        Militar escolta=novo(om,"ID-2",PostoGraduacao.TERCEIRO_SARGENTO,FuncaoMilitar.ESCOLTA,true);
        Militar oficial=novo(om,"ID-3",PostoGraduacao.PRIMEIRO_TENENTE,FuncaoMilitar.OFICIAL_MUNICAO,true);
        novo(om,"ID-4",PostoGraduacao.PRIMEIRO_SARGENTO,FuncaoMilitar.MOTORISTA,false);
        novo(om,"ID-5",PostoGraduacao.SEGUNDO_SARGENTO,FuncaoMilitar.CHEFE_VIATURA,true);
        militares.flush();
        assertThat(militares.findById(antigo.getId()).orElseThrow().getFuncao()).isNull();
        motorista.setFuncao(FuncaoMilitar.OFICIAL_TIRO);militares.saveAndFlush(motorista);assertThat(militares.findById(motorista.getId()).orElseThrow().getFuncao()).isEqualTo(FuncaoMilitar.OFICIAL_TIRO);
        assertThat(militares.findByAtivoTrueAndFuncaoOrderByNomeCompleto(FuncaoMilitar.MOTORISTA)).isEmpty();
        assertThat(militares.findByAtivoTrueAndFuncaoInOrderByNomeCompleto(Set.of(FuncaoMilitar.ESCOLTA,FuncaoMilitar.OFICIAL_MUNICAO))).extracting(Militar::getId).containsExactlyInAnyOrder(escolta.getId(),oficial.getId());
        assertThat(PostoGraduacao.PRIMEIRO_TENENTE.getDescricao()).isEqualTo("1º Tenente");
        assertThat(PostoGraduacao.SEGUNDO_TENENTE.getDescricao()).isEqualTo("2º Tenente");
        assertThat(PostoGraduacao.PRIMEIRO_SARGENTO.getDescricao()).isEqualTo("1º Sargento");
        assertThat(PostoGraduacao.SEGUNDO_SARGENTO.getDescricao()).isEqualTo("2º Sargento");
        assertThat(PostoGraduacao.TERCEIRO_SARGENTO.getDescricao()).isEqualTo("3º Sargento");
        assertThat(PostoGraduacao.TENENTE.getDescricao()).contains("classificação pendente");
    }
    private Militar novo(OrganizacaoMilitar om,String identidade,PostoGraduacao posto,FuncaoMilitar funcao,boolean ativo){Militar m=new Militar();m.setNomeCompleto("Militar "+identidade);m.setIdentidade(identidade);m.setPostoGraduacao(posto);m.setFuncao(funcao);m.setOrganizacaoMilitar(om);m.setAtivo(ativo);return militares.save(m);}
}
