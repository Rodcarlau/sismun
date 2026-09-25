package br.mil.controlemunicao;

import br.mil.controlemunicao.entity.*;
import br.mil.controlemunicao.exception.BusinessException;
import br.mil.controlemunicao.exception.EstoqueInsuficienteException;
import br.mil.controlemunicao.repository.*;
import br.mil.controlemunicao.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class TransferenciaPaiolTest {
    @Autowired MovimentacaoService movimentacaoService;
    @Autowired EstoqueService estoqueService;
    @Autowired EstoquePaiolRepository estoqueRepository;
    @Autowired PaiolRepository paiolRepository;
    @Autowired DevolucaoService devolucaoService;
    @Autowired DevolucaoRepository devolucaoRepository;
    @Autowired ItemDevolucaoRepository itemDevolucaoRepository;
    @Autowired MockMvc mockMvc;
    EstoquePaiol origem;

    @BeforeEach void preparar() {
        origem=estoqueRepository.findAll().get(0); origem.setQuantidadeAtual(1000); origem.setQuantidadeReservada(0); estoqueRepository.saveAndFlush(origem);
    }

    @Test void transferenciaNormal() { Paiol b=novoPaiol(); transferir(origem,b,600); assertEquals(400,saldo(origem.getPaiol(),origem.getLoteMunicao())); assertEquals(600,saldo(b,origem.getLoteMunicao())); }
    @Test void destinoExistenteRecebeSoma() { Paiol b=novoPaiol(); estoqueService.entradaEstoque(b,origem.getLoteMunicao(),200); transferir(origem,b,300); assertEquals(500,saldo(b,origem.getLoteMunicao())); }
    @Test void transferenciaEmCadeia() { Paiol b=novoPaiol(),c=novoPaiol(); transferir(origem,b,600); EstoquePaiol estoqueB=estoqueRepository.findByPaiolAndLoteMunicao(b,origem.getLoteMunicao()).orElseThrow(); transferir(estoqueB,c,300); assertEquals(400,saldo(origem.getPaiol(),origem.getLoteMunicao())); assertEquals(300,saldo(b,origem.getLoteMunicao())); assertEquals(300,saldo(c,origem.getLoteMunicao())); }
    @Test void consumoTotalPermiteSaldoZero() { origem.setQuantidadeAtual(500);estoqueRepository.saveAndFlush(origem);Paiol b=novoPaiol();transferir(origem,b,500);assertEquals(0,saldo(origem.getPaiol(),origem.getLoteMunicao()));assertEquals(500,saldo(b,origem.getLoteMunicao())); }
    @Test void saldoInsuficienteNaoAlteraOrigemNemDestino() { origem.setQuantidadeAtual(300);estoqueRepository.saveAndFlush(origem);Paiol b=novoPaiol();assertThrows(EstoqueInsuficienteException.class,()->solicitar(origem,b,500));assertEquals(300,saldo(origem.getPaiol(),origem.getLoteMunicao()));assertEquals(0,saldo(b,origem.getLoteMunicao())); }
    @Test void processamentoDuplicadoNaoMovimentaNovamente() { Paiol b=novoPaiol();Movimentacao m=transferir(origem,b,600);assertThrows(BusinessException.class,()->movimentacaoService.confirmarEntrega(m.getId(),600,null,null));assertEquals(400,saldo(origem.getPaiol(),origem.getLoteMunicao()));assertEquals(600,saldo(b,origem.getLoteMunicao())); }
    @Test void consumoParcialMantemSaldoNoDestino() { Paiol b=novoPaiol();Movimentacao m=transferir(origem,b,600);prestarContas(m,400,0);assertEquals(200,saldo(b,origem.getLoteMunicao())); }
    @Test void saldoDocumentalPendenteImpedeFinalizacao() { Paiol b=novoPaiol();Movimentacao m=transferir(origem,b,600);prestarContas(m,400,0);assertEquals(StatusMovimentacao.DEVOLUCAO_PENDENTE,movimentacaoService.buscarPorId(m.getId()).getStatus()); }
    @Test void consumoTotalZeraDestino() { Paiol b=novoPaiol();Movimentacao m=transferir(origem,b,600);prestarContas(m,600,0);assertEquals(0,saldo(b,origem.getLoteMunicao())); }
    @Test void consumoAcimaDoEfetivoEhBloqueado() { Paiol b=novoPaiol();Movimentacao m=transferir(origem,b,500);Devolucao d=devolucao(m);ItemDevolucao item=item(d);assertThrows(BusinessException.class,()->devolucaoService.registrarConsumo(d.getId(),List.of(item.getId()),List.of(501),null));assertEquals(500,saldo(b,origem.getLoteMunicao())); }
    @Test void consumoParcialComDevolucaoRetiraDoDestinoERepoeOrigem() { Paiol b=novoPaiol();Movimentacao m=transferir(origem,b,600);prestarContas(m,400,200);assertEquals(600,saldo(origem.getPaiol(),origem.getLoteMunicao()));assertEquals(0,saldo(b,origem.getLoteMunicao())); }
    @Test void saldoSemDevolucaoPodeOriginarNovaMovimentacao() { Paiol b=novoPaiol(),c=novoPaiol();Movimentacao primeira=transferir(origem,b,600);prestarContas(primeira,400,0);EstoquePaiol estoqueB=estoqueRepository.findByPaiolAndLoteMunicao(b,origem.getLoteMunicao()).orElseThrow();transferir(estoqueB,c,150);assertEquals(50,saldo(b,origem.getLoteMunicao()));assertEquals(150,saldo(c,origem.getLoteMunicao())); }
    @Test void saldoRemanescenteInsuficienteBloqueiaNovaMovimentacao() { Paiol b=novoPaiol(),c=novoPaiol();Movimentacao primeira=transferir(origem,b,600);prestarContas(primeira,550,0);EstoquePaiol estoqueB=estoqueRepository.findByPaiolAndLoteMunicao(b,origem.getLoteMunicao()).orElseThrow();assertThrows(EstoqueInsuficienteException.class,()->solicitar(estoqueB,c,100));assertEquals(50,saldo(b,origem.getLoteMunicao()));assertEquals(0,saldo(c,origem.getLoteMunicao())); }
    @Test void detalhesCarregaComPrestacaoPendenteEFinalizada() throws Exception { Paiol b=novoPaiol();Movimentacao m=transferir(origem,b,100);abrirDetalhes(m);prestarContas(m,80,0);abrirDetalhes(m); }
    @Test void consumoEDevolucaoSaoEtapasSeparadasDaMesmaMovimentacao() { Paiol b=novoPaiol();Movimentacao m=transferir(origem,b,600);Devolucao d=devolucao(m);ItemDevolucao item=item(d);devolucaoService.registrarConsumo(d.getId(),List.of(item.getId()),List.of(400),null);item=itemDevolucaoRepository.findById(item.getId()).orElseThrow();assertTrue(item.isConsumoProcessado());assertFalse(item.isDevolucaoProcessada());assertEquals(200,saldo(b,origem.getLoteMunicao()));devolucaoService.registrarDevolucao(d.getId(),List.of(item.getId()),List.of(200),null,null,null,null,null);item=itemDevolucaoRepository.findById(item.getId()).orElseThrow();assertTrue(item.isDevolucaoProcessada());assertEquals(m.getId(),item.getItemMovimentacao().getMovimentacao().getId());assertEquals(0,saldo(b,origem.getLoteMunicao()));assertEquals(600,saldo(origem.getPaiol(),origem.getLoteMunicao())); }
    @Test void devolucaoAntesDoConsumoEhBloqueada() { Paiol b=novoPaiol();Movimentacao m=transferir(origem,b,600);Devolucao d=devolucao(m);ItemDevolucao item=item(d);assertThrows(BusinessException.class,()->devolucaoService.registrarDevolucao(d.getId(),List.of(item.getId()),List.of(200),null,null,null,null,null));assertEquals(600,saldo(b,origem.getLoteMunicao())); }
    @Test void consumoEDevolucaoDuplicadosSaoBloqueados() { Paiol b=novoPaiol();Movimentacao m=transferir(origem,b,100);Devolucao d=devolucao(m);ItemDevolucao item=item(d);devolucaoService.registrarConsumo(d.getId(),List.of(item.getId()),List.of(40),null);assertThrows(BusinessException.class,()->devolucaoService.registrarConsumo(d.getId(),List.of(item.getId()),List.of(1),null));devolucaoService.registrarDevolucao(d.getId(),List.of(item.getId()),List.of(20),null,null,null,null,null);assertThrows(BusinessException.class,()->devolucaoService.registrarDevolucao(d.getId(),List.of(item.getId()),List.of(1),null,null,null,null,null));assertEquals(40,saldo(b,origem.getLoteMunicao())); }
    @Test void devolucaoComItemZeroNaoCriaEntradaNemFalha() { origem.setQuantidadeAtual(500);estoqueRepository.saveAndFlush(origem);Paiol complementar=novoPaiol(),atividade=novoPaiol();estoqueService.entradaEstoque(complementar,origem.getLoteMunicao(),500);EstoquePaiol estoqueComplementar=estoqueRepository.findByPaiolAndLoteMunicao(complementar,origem.getLoteMunicao()).orElseThrow();Movimentacao m=solicitarComComplemento(origem,estoqueComplementar,atividade,500,500);concluir(m);Devolucao d=devolucao(m);var itens=itemDevolucaoRepository.findByDevolucaoId(d.getId());devolucaoService.registrarConsumo(d.getId(),itens.stream().map(ItemDevolucao::getId).toList(),List.of(300,500),null);devolucaoService.registrarDevolucao(d.getId(),List.of(itens.get(0).getId()),List.of(200),List.of(origem.getPaiol().getId()),null,null,null,null,null);assertEquals(200,saldo(origem.getPaiol(),origem.getLoteMunicao()));assertEquals(0,saldo(complementar,origem.getLoteMunicao()));assertEquals(0,saldo(atividade,origem.getLoteMunicao())); }
    @Test void devolucaoSemCamposObrigatoriosRetornaValidacaoSemErro500() throws Exception { Paiol b=novoPaiol();Movimentacao m=transferir(origem,b,100);Devolucao d=devolucao(m);mockMvc.perform(post("/devolucoes/"+d.getId()+"/devolucao").with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMINISTRADOR")).with(SecurityMockMvcRequestPostProcessors.csrf())).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/dashboard"));assertFalse(item(d).isDevolucaoProcessada()); }
    @Test void origemPrincipalEComplementarSomamQuantidadeCorreta() { origem.setQuantidadeAtual(700);estoqueRepository.saveAndFlush(origem);Paiol complementar=novoPaiol(),destino=novoPaiol();estoqueService.entradaEstoque(complementar,origem.getLoteMunicao(),300);EstoquePaiol estoqueComplementar=estoqueRepository.findByPaiolAndLoteMunicao(complementar,origem.getLoteMunicao()).orElseThrow();Movimentacao m=solicitarComComplemento(origem,estoqueComplementar,destino,700,300);concluir(m);assertEquals(0,saldo(origem.getPaiol(),origem.getLoteMunicao()));assertEquals(0,saldo(complementar,origem.getLoteMunicao()));assertEquals(1000,saldo(destino,origem.getLoteMunicao()));assertEquals(1000,movimentacaoService.buscarPorId(m.getId()).getQuantidadeSolicitada()); }
    @Test void complementoInsuficienteBloqueiaTudoSemEstoqueNegativo() { origem.setQuantidadeAtual(700);estoqueRepository.saveAndFlush(origem);Paiol complementar=novoPaiol(),destino=novoPaiol();estoqueService.entradaEstoque(complementar,origem.getLoteMunicao(),200);EstoquePaiol estoqueComplementar=estoqueRepository.findByPaiolAndLoteMunicao(complementar,origem.getLoteMunicao()).orElseThrow();assertThrows(EstoqueInsuficienteException.class,()->solicitarComComplemento(origem,estoqueComplementar,destino,700,300));assertEquals(700,saldo(origem.getPaiol(),origem.getLoteMunicao()));assertEquals(200,saldo(complementar,origem.getLoteMunicao()));assertEquals(0,saldo(destino,origem.getLoteMunicao())); }
    @Test void multiplasOrigensComDevolucaoDistribuidaPreservamOrigemAdministrativa() {
        origem.setQuantidadeAtual(500); estoqueRepository.saveAndFlush(origem); Paiol complementar=novoPaiol(),atividade=novoPaiol(); estoqueService.entradaEstoque(complementar,origem.getLoteMunicao(),500); EstoquePaiol estoqueComplementar=estoqueRepository.findByPaiolAndLoteMunicao(complementar,origem.getLoteMunicao()).orElseThrow(); Long omAdministrativa=origem.getLoteMunicao().getOrganizacaoProprietaria().getId();
        Movimentacao m=solicitarComComplemento(origem,estoqueComplementar,atividade,500,500); concluir(m); Devolucao d=devolucao(m); var itens=itemDevolucaoRepository.findByDevolucaoId(d.getId());
        devolucaoService.registrarConsumo(d.getId(),itens.stream().map(ItemDevolucao::getId).toList(),List.of(300,400),null);
        devolucaoService.registrarDevolucao(d.getId(),itens.stream().map(ItemDevolucao::getId).toList(),List.of(200,100),List.of(origem.getPaiol().getId(),complementar.getId()),null,null,null,null,null);
        assertEquals(200,saldo(origem.getPaiol(),origem.getLoteMunicao())); assertEquals(100,saldo(complementar,origem.getLoteMunicao())); assertEquals(0,saldo(atividade,origem.getLoteMunicao())); assertTrue(itemDevolucaoRepository.findByDevolucaoId(d.getId()).stream().allMatch(i->i.getItemMovimentacao().getLoteMunicao().getOrganizacaoProprietaria().getId().equals(omAdministrativa)));
    }

    private Movimentacao transferir(EstoquePaiol estoqueOrigem,Paiol destino,int quantidade) { Movimentacao m=solicitar(estoqueOrigem,destino,quantidade);movimentacaoService.autorizar(m.getId(),null);movimentacaoService.confirmarSeparacao(m.getId(),null);movimentacaoService.despachar(m.getId(),null);return movimentacaoService.confirmarEntrega(m.getId(),quantidade,null,null); }
    private Movimentacao solicitar(EstoquePaiol estoqueOrigem,Paiol destino,int quantidade) { Movimentacao m=new Movimentacao();m.setDiex("TRANSF-"+UUID.randomUUID());m.setDataSolicitacao(LocalDate.now());m.setDataApanha(LocalDate.now());m.setPaiolOrigem(estoqueOrigem.getPaiol());m.setPaiolDestino(destino);return movimentacaoService.criarSolicitacao(m,estoqueOrigem.getId(),quantidade,null); }
    private Paiol novoPaiol() { Paiol p=new Paiol();p.setNome("Paiol "+UUID.randomUUID());p.setCodigo("P-"+UUID.randomUUID());p.setOrganizacaoMilitar(origem.getPaiol().getOrganizacaoMilitar());p.setAtivo(true);return paiolRepository.save(p); }
    private int saldo(Paiol paiol,LoteMunicao lote) { return estoqueRepository.findByPaiolAndLoteMunicao(paiol,lote).map(EstoquePaiol::getQuantidadeAtual).orElse(0); }
    private void prestarContas(Movimentacao m,int consumida,int devolvida) { Devolucao d=devolucaoRepository.findByMovimentacaoId(m.getId()).orElseThrow();ItemDevolucao item=itemDevolucaoRepository.findByDevolucaoId(d.getId()).get(0);devolucaoService.registrarQuantidades(d.getId(),List.of(item.getId()),List.of(consumida),List.of(devolvida),null,null,null,null,null); }
    private void abrirDetalhes(Movimentacao m) throws Exception { mockMvc.perform(get("/movimentacoes/"+m.getId()).with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMINISTRADOR"))).andExpect(status().isOk()); }
    private Devolucao devolucao(Movimentacao m){return devolucaoRepository.findByMovimentacaoId(m.getId()).orElseThrow();}
    private ItemDevolucao item(Devolucao d){return itemDevolucaoRepository.findByDevolucaoId(d.getId()).get(0);}
    private Movimentacao solicitarComComplemento(EstoquePaiol principal,EstoquePaiol complementar,Paiol destino,int qPrincipal,int qComplementar){Movimentacao m=new Movimentacao();m.setDiex("COMP-"+UUID.randomUUID());m.setDataSolicitacao(LocalDate.now());m.setDataApanha(LocalDate.now());m.setPaiolOrigem(principal.getPaiol());m.setPaiolDestino(destino);return movimentacaoService.criarSolicitacaoComItens(m,List.of(principal.getId(),complementar.getId()),List.of(qPrincipal,qComplementar),null);}
    private Movimentacao concluir(Movimentacao m){movimentacaoService.autorizar(m.getId(),null);movimentacaoService.confirmarSeparacao(m.getId(),null);movimentacaoService.despachar(m.getId(),null);var itens=movimentacaoService.itens(m.getId());return movimentacaoService.confirmarEntregaItens(m.getId(),itens.stream().map(ItemMovimentacao::getId).toList(),itens.stream().map(ItemMovimentacao::getQuantidadeSeparada).toList(),null,null);}
}
