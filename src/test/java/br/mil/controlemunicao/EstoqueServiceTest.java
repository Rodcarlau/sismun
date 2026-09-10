package br.mil.controlemunicao;

import br.mil.controlemunicao.entity.LoteMunicao;
import br.mil.controlemunicao.entity.Municao;
import br.mil.controlemunicao.entity.OrganizacaoMilitar;
import br.mil.controlemunicao.entity.Paiol;
import br.mil.controlemunicao.entity.TipoMunicao;
import br.mil.controlemunicao.repository.LoteMunicaoRepository;
import br.mil.controlemunicao.repository.MunicaoRepository;
import br.mil.controlemunicao.repository.OrganizacaoMilitarRepository;
import br.mil.controlemunicao.repository.PaiolRepository;
import br.mil.controlemunicao.repository.TipoMunicaoRepository;
import br.mil.controlemunicao.service.EstoqueService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class EstoqueServiceTest {

    @Autowired
    private EstoqueService estoqueService;

    @Autowired
    private OrganizacaoMilitarRepository organizacaoMilitarRepository;

    @Autowired
    private TipoMunicaoRepository tipoMunicaoRepository;

    @Autowired
    private MunicaoRepository municaoRepository;

    @Autowired
    private LoteMunicaoRepository loteMunicaoRepository;

    @Autowired
    private PaiolRepository paiolRepository;

    @Test
    void deveRetirarQuantidadeValida() {
        TipoMunicao tipoMunicao = tipoMunicaoRepository.save(new TipoMunicao("Pistola Teste Estoque", "Pistola 9mm"));

        OrganizacaoMilitar om = new OrganizacaoMilitar();
        om.setNome("1º Batalhão Estoque Teste");
        om.setSigla("1º BTL-T");
        om.setCodigo("OM-ESTOQUE-001");
        om.setCidade("Rio");
        om.setUf("RJ");
        om.setAtiva(true);
        om = organizacaoMilitarRepository.save(om);

        Municao municao = new Municao();
        municao.setTipoMunicao(tipoMunicao);
        municao.setDescricao("Pistola 9mm");
        municao.setCalibre("9mm");
        municao.setFabricante("ABC");
        municao.setAtivo(true);
        municao = municaoRepository.save(municao);

        LoteMunicao lote = new LoteMunicao();
        lote.setMunicao(municao);
        lote.setLote("LOT-001");
        lote.setVirola("VR-77");
        lote.setDataValidade(LocalDate.now().plusYears(2));
        lote.setAtivo(true);
        lote.setOrganizacaoProprietaria(om);
        lote = loteMunicaoRepository.save(lote);

        Paiol paiol = new Paiol();
        paiol.setNome("Paiol Central Teste");
        paiol.setCodigo("P-ESTOQUE-01");
        paiol.setOrganizacaoMilitar(om);
        paiol.setEndereco("Endereço A");
        paiol.setAtivo(true);
        paiol = paiolRepository.save(paiol);

        var estoque = estoqueService.entradaEstoque(paiol, lote, 10);

        estoqueService.retirarEstoque(paiol, lote, 4);

        assertEquals(6, estoqueService.consultarSaldo(paiol, lote));
        assertNotNull(estoque.getId());
    }

    @Test
    void deveConsolidarMesmoTipoSemDuplicarLotesOuVirolas() {
        String sufixo = Long.toString(System.nanoTime());
        TipoMunicao tipo = tipoMunicaoRepository.save(new TipoMunicao("Tipo Consolidado " + sufixo, "Teste"));
        OrganizacaoMilitar om = new OrganizacaoMilitar(); om.setNome("OM Consolidado"); om.setSigla("OMC"); om.setCodigo("OMC-" + sufixo); om.setAtiva(true); om = organizacaoMilitarRepository.save(om);
        Municao municao = new Municao(); municao.setTipoMunicao(tipo); municao.setCalibre("9mm"); municao.setDescricao("Teste consolidado"); municao.setFabricante("Teste"); municao.setAtivo(true); municao = municaoRepository.save(municao);
        Paiol paiol = new Paiol(); paiol.setNome("Paiol consolidado"); paiol.setCodigo("PC-" + sufixo); paiol.setOrganizacaoMilitar(om); paiol.setAtivo(true); paiol = paiolRepository.save(paiol);
        LoteMunicao a = novoLote(municao, om, "A-" + sufixo, "V1");
        LoteMunicao b = novoLote(municao, om, "B-" + sufixo, "V2");
        estoqueService.entradaEstoque(paiol, a, 10); estoqueService.entradaEstoque(paiol, b, 5);
        var total = estoqueService.totaisConsolidados().stream().filter(t -> t.tipoMunicao().equals(tipo.getNome() + " - 9mm")).findFirst().orElseThrow();
        assertEquals(15, total.quantidadeAtual()); assertEquals(15, total.quantidadeDisponivel());
    }

    private LoteMunicao novoLote(Municao municao, OrganizacaoMilitar om, String loteNome, String virola) {
        LoteMunicao lote = new LoteMunicao(); lote.setMunicao(municao); lote.setLote(loteNome); lote.setVirola(virola); lote.setDataValidade(LocalDate.now().plusYears(1)); lote.setOrganizacaoProprietaria(om); lote.setAtivo(true); return loteMunicaoRepository.save(lote);
    }
}
