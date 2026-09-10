package br.mil.controlemunicao;

import br.mil.controlemunicao.entity.*;
import br.mil.controlemunicao.exception.EstoqueInsuficienteException;
import br.mil.controlemunicao.repository.*;
import br.mil.controlemunicao.service.MovimentacaoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.transaction.TestTransaction;
import java.time.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MovimentacaoRollbackTest {
 @Autowired MovimentacaoService service; @Autowired EstoquePaiolRepository estoques; @Autowired LoteMunicaoRepository lotes;
@Test void faltaEmUmItemDesfazTodasAsReservas(){EstoquePaiol a=estoques.findAll().get(0);a.setQuantidadeAtual(100);a.setQuantidadeReservada(0);estoques.saveAndFlush(a);LoteMunicao l=new LoteMunicao();l.setMunicao(a.getLoteMunicao().getMunicao());l.setLote("ROLLBACK-"+System.nanoTime());l.setVirola("VR-RB");l.setDataValidade(LocalDate.now().plusYears(1));l.setOrganizacaoProprietaria(a.getLoteMunicao().getOrganizacaoProprietaria());l.setAtivo(true);l=lotes.save(l);EstoquePaiol b=new EstoquePaiol();b.setPaiol(a.getPaiol());b.setLoteMunicao(l);b.setQuantidadeAtual(10);b.setQuantidadeReservada(0);b.setDataEntrada(LocalDateTime.now());b.setUltimaMovimentacao(LocalDateTime.now());b=estoques.saveAndFlush(b);Long aId=a.getId(),bId=b.getId(),paiolId=a.getPaiol().getId();TestTransaction.flagForCommit();TestTransaction.end();TestTransaction.start();Movimentacao m=new Movimentacao();m.setDiex("ROLLBACK");m.setPaiolOrigem(estoques.findById(aId).orElseThrow().getPaiol());m.setDataSolicitacao(LocalDate.now());m.setDataApanha(LocalDate.now().plusDays(1));assertThrows(EstoqueInsuficienteException.class,()->service.criarSolicitacaoComItens(m,List.of(aId,bId),List.of(50,20),null));TestTransaction.end();TestTransaction.start();assertEquals(0,estoques.findById(aId).orElseThrow().getQuantidadeReservada());assertEquals(0,estoques.findById(bId).orElseThrow().getQuantidadeReservada());}
}
