package br.mil.controlemunicao;

import br.mil.controlemunicao.entity.*;
import br.mil.controlemunicao.repository.EstoquePaiolRepository;
import br.mil.controlemunicao.repository.PaiolRepository;
import br.mil.controlemunicao.service.MovimentacaoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.time.LocalDate;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ReservaConcorrenciaTest {
    @Autowired MovimentacaoService service;
    @Autowired EstoquePaiolRepository estoqueRepository;
    @Autowired PaiolRepository paiolRepository;

    @Test
    void duasReservasConcorrentesNaoUltrapassamSaldo() throws Exception {
        EstoquePaiol estoque = estoqueRepository.findAll().get(0);
        estoque.setQuantidadeAtual(100); estoque.setQuantidadeReservada(0); estoqueRepository.saveAndFlush(estoque);
        Long estoqueId = estoque.getId(); Long paiolId = estoque.getPaiol().getId();
        CountDownLatch inicio = new CountDownLatch(1); AtomicInteger sucessos = new AtomicInteger();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Callable<Void> tarefa = () -> { inicio.await(); try { Movimentacao m = new Movimentacao(); m.setDiex("CONC-" + Thread.currentThread().getId()); m.setDataSolicitacao(LocalDate.now()); m.setDataApanha(LocalDate.now()); m.setPaiolOrigem(paiolRepository.findById(paiolId).orElseThrow()); service.criarSolicitacao(m, estoqueId, 80, null); sucessos.incrementAndGet(); } catch (RuntimeException ignored) { } return null; };
        Future<Void> a=executor.submit(tarefa), b=executor.submit(tarefa); inicio.countDown(); a.get(10,TimeUnit.SECONDS); b.get(10,TimeUnit.SECONDS); executor.shutdownNow();
        EstoquePaiol finalizado=estoqueRepository.findById(estoqueId).orElseThrow();
        assertEquals(1,sucessos.get()); assertEquals(80,finalizado.getQuantidadeReservada()); assertEquals(20,finalizado.getQuantidadeDisponivel()); assertEquals(100,finalizado.getQuantidadeAtual());
    }
}