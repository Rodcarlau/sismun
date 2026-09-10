package br.mil.controlemunicao.controller;

import br.mil.controlemunicao.entity.EstoquePaiol;
import br.mil.controlemunicao.entity.Paiol;
import br.mil.controlemunicao.repository.EstoquePaiolRepository;
import br.mil.controlemunicao.repository.PaiolRepository;
import br.mil.controlemunicao.repository.LoteMunicaoRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import br.mil.controlemunicao.service.EstoqueService;

import java.util.List;

@Controller
@RequestMapping("/estoque")
public class EstoqueController {

    private final EstoquePaiolRepository estoquePaiolRepository;
    private final PaiolRepository paiolRepository;
    private final LoteMunicaoRepository loteMunicaoRepository;
    private final EstoqueService estoqueService;

    public EstoqueController(EstoquePaiolRepository estoquePaiolRepository, PaiolRepository paiolRepository, LoteMunicaoRepository loteMunicaoRepository, EstoqueService estoqueService) {
        this.estoquePaiolRepository = estoquePaiolRepository;
        this.paiolRepository = paiolRepository;
        this.loteMunicaoRepository = loteMunicaoRepository;
        this.estoqueService = estoqueService;
    }

    @GetMapping
    public String listar(Model model) {
        List<EstoquePaiol> estoque = estoquePaiolRepository.findAll();
        model.addAttribute("estoques", estoque);
        model.addAttribute("totaisConsolidados", estoqueService.totaisConsolidados());
        model.addAttribute("paiols", paiolRepository.findAll());
        var omsPorPaiol = new java.util.LinkedHashMap<Long, java.util.List<br.mil.controlemunicao.entity.OrganizacaoMilitar>>();
        for (Paiol paiol : paiolRepository.findAll()) {
            omsPorPaiol.put(paiol.getId(), estoque.stream().filter(e -> e.getPaiol().getId().equals(paiol.getId()))
                    .map(e -> e.getLoteMunicao().getOrganizacaoProprietaria()).filter(java.util.Objects::nonNull).distinct()
                    .sorted(java.util.Comparator.comparing(br.mil.controlemunicao.entity.OrganizacaoMilitar::getNome, String.CASE_INSENSITIVE_ORDER)).toList());
        }
        model.addAttribute("omsPorPaiol", omsPorPaiol);
        return "estoque/lista-organizada";
    }

    @GetMapping("/novo")
    public String novo(Model model) { EstoquePaiol estoque = new EstoquePaiol(); estoque.setQuantidadeAtual(0); model.addAttribute("estoque", estoque); carregarOpcoes(model); return "estoque/form"; }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) { model.addAttribute("estoque", estoquePaiolRepository.findById(id).orElseThrow()); carregarOpcoes(model); return "estoque/form"; }

    @PostMapping
    public String salvar(@ModelAttribute EstoquePaiol estoque, RedirectAttributes redirectAttributes) {
        estoqueService.salvarCadastro(estoque); redirectAttributes.addFlashAttribute("successMessage", "Estoque salvo com sucesso."); return "redirect:/estoque";
    }

    private void carregarOpcoes(Model model) { model.addAttribute("paiols", paiolRepository.findAll()); model.addAttribute("lotes", loteMunicaoRepository.findAll()); }
}
