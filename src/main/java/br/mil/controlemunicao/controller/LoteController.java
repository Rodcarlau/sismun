package br.mil.controlemunicao.controller;

import br.mil.controlemunicao.entity.LoteMunicao;
import br.mil.controlemunicao.repository.LoteMunicaoRepository;
import br.mil.controlemunicao.repository.MunicaoRepository;
import br.mil.controlemunicao.repository.OrganizacaoMilitarRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

@Controller
@RequestMapping("/lotes")
public class LoteController {

    private final LoteMunicaoRepository loteMunicaoRepository;
    private final MunicaoRepository municaoRepository;
    private final OrganizacaoMilitarRepository organizacaoMilitarRepository;

    public LoteController(LoteMunicaoRepository loteMunicaoRepository, MunicaoRepository municaoRepository,
                          OrganizacaoMilitarRepository organizacaoMilitarRepository) {
        this.loteMunicaoRepository = loteMunicaoRepository;
        this.municaoRepository = municaoRepository;
        this.organizacaoMilitarRepository = organizacaoMilitarRepository;
    }

    @GetMapping
    public String listar(Model model) {
        var lotes = loteMunicaoRepository.findAll();
        model.addAttribute("lotes", lotes);
        model.addAttribute("municoesAbas", lotes.stream().map(LoteMunicao::getMunicao).filter(java.util.Objects::nonNull).distinct().toList());
        return "lote/lista-abas";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("lote", new LoteMunicao());
        carregarOpcoes(model);
        return "lote/form";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("lote", loteMunicaoRepository.findById(id).orElseThrow());
        carregarOpcoes(model);
        return "lote/form";
    }

    @PostMapping
    public String salvar(@ModelAttribute LoteMunicao lote, RedirectAttributes redirectAttributes) {
        loteMunicaoRepository.save(lote);
        redirectAttributes.addFlashAttribute("successMessage", "Lote salvo com sucesso.");
        return "redirect:/lotes";
    }

    private void carregarOpcoes(Model model) {
        model.addAttribute("municoes", municaoRepository.findAll());
        model.addAttribute("organizacoes", organizacaoMilitarRepository.findAll());
    }
}
