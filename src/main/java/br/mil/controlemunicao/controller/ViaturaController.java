package br.mil.controlemunicao.controller;

import br.mil.controlemunicao.entity.TipoViatura;
import br.mil.controlemunicao.entity.Viatura;
import br.mil.controlemunicao.repository.OrganizacaoMilitarRepository;
import br.mil.controlemunicao.repository.ViaturaRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/viaturas")
public class ViaturaController {

    private final ViaturaRepository viaturaRepository;
    private final OrganizacaoMilitarRepository organizacaoMilitarRepository;

    public ViaturaController(ViaturaRepository viaturaRepository, OrganizacaoMilitarRepository organizacaoMilitarRepository) {
        this.viaturaRepository = viaturaRepository;
        this.organizacaoMilitarRepository = organizacaoMilitarRepository;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("viaturas", viaturaRepository.findAll());
        return "viatura/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) { model.addAttribute("viatura", new Viatura()); carregarOpcoes(model); return "viatura/form"; }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) { model.addAttribute("viatura", viaturaRepository.findById(id).orElseThrow()); carregarOpcoes(model); return "viatura/form"; }

    @PostMapping
    public String salvar(@ModelAttribute Viatura viatura, RedirectAttributes redirectAttributes) {
        viaturaRepository.save(viatura); redirectAttributes.addFlashAttribute("successMessage", "Viatura salva com sucesso."); return "redirect:/viaturas";
    }

    private void carregarOpcoes(Model model) { model.addAttribute("organizacoes", organizacaoMilitarRepository.findAll()); model.addAttribute("tipos", TipoViatura.values()); }
}
