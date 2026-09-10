package br.mil.controlemunicao.controller;

import br.mil.controlemunicao.entity.StatusMovimentacao;
import br.mil.controlemunicao.repository.EstoquePaiolRepository;
import br.mil.controlemunicao.repository.MovimentacaoRepository;
import br.mil.controlemunicao.repository.MunicaoRepository;
import br.mil.controlemunicao.repository.PaiolRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final MunicaoRepository municaoRepository;
    private final PaiolRepository paiolRepository;
    private final EstoquePaiolRepository estoquePaiolRepository;
    private final MovimentacaoRepository movimentacaoRepository;

    public DashboardController(MunicaoRepository municaoRepository,
                              PaiolRepository paiolRepository,
                              EstoquePaiolRepository estoquePaiolRepository,
                              MovimentacaoRepository movimentacaoRepository) {
        this.municaoRepository = municaoRepository;
        this.paiolRepository = paiolRepository;
        this.estoquePaiolRepository = estoquePaiolRepository;
        this.movimentacaoRepository = movimentacaoRepository;
    }

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model) {
        model.addAttribute("totalMunicoes", municaoRepository.count());
        model.addAttribute("totalPaiols", paiolRepository.count());
        model.addAttribute("totalEstoque", estoquePaiolRepository.findAll().stream().mapToInt(estoque -> estoque.getQuantidadeAtual() == null ? 0 : estoque.getQuantidadeAtual()).sum());
        model.addAttribute("totalReservado", estoquePaiolRepository.findAll().stream().mapToInt(e -> e.getQuantidadeReservada() == null ? 0 : e.getQuantidadeReservada()).sum());
        model.addAttribute("totalDisponivel", estoquePaiolRepository.findAll().stream().mapToInt(e -> e.getQuantidadeDisponivel()).sum());
        model.addAttribute("aguardandoSeparacao", movimentacaoRepository.findByStatus(StatusMovimentacao.AUTORIZADA).size());
        model.addAttribute("movimentacoesSeparadas", movimentacaoRepository.findByStatus(StatusMovimentacao.SEPARADA).size());
        model.addAttribute("aguardandoDevolucao", movimentacaoRepository.findByStatus(StatusMovimentacao.DEVOLUCAO_PENDENTE).size());
        model.addAttribute("movimentacoesPendentes", movimentacaoRepository.findByStatus(StatusMovimentacao.SOLICITADA).size());
        model.addAttribute("movimentacoesTotais", movimentacaoRepository.count());
        return "dashboard";
    }
}
