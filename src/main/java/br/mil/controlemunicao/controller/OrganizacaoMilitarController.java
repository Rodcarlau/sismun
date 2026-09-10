package br.mil.controlemunicao.controller;

import br.mil.controlemunicao.entity.OrganizacaoMilitar;
import br.mil.controlemunicao.repository.OrganizacaoMilitarRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/organizacoes")
public class OrganizacaoMilitarController {

    private final OrganizacaoMilitarRepository organizacaoMilitarRepository;

    public OrganizacaoMilitarController(OrganizacaoMilitarRepository organizacaoMilitarRepository) {
        this.organizacaoMilitarRepository = organizacaoMilitarRepository;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("organizacoes", organizacaoMilitarRepository.findAll().stream().sorted(java.util.Comparator.comparing(OrganizacaoMilitar::getNome, java.util.Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))).toList());
        return "organizacao/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        OrganizacaoMilitar organizacao = new OrganizacaoMilitar(); organizacao.setCodigo(proximoCodigo());
        model.addAttribute("organizacao", organizacao);
        return "organizacao/form";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("organizacao", organizacaoMilitarRepository.findById(id).orElseThrow());
        return "organizacao/form";
    }

    @PostMapping
    public synchronized String salvar(@Valid @ModelAttribute("organizacao") OrganizacaoMilitar organizacao, BindingResult result,
                         RedirectAttributes redirectAttributes) {
        if (organizacao.getId() == null) organizacao.setCodigo(proximoCodigo());
        if (result.hasErrors()) {
            return "organizacao/form";
        }
        organizacaoMilitarRepository.save(organizacao);
        redirectAttributes.addFlashAttribute("successMessage", "Organização militar salva com sucesso.");
        return "redirect:/organizacoes";
    }

    private String proximoCodigo() {
        int maior = organizacaoMilitarRepository.findAll().stream().map(OrganizacaoMilitar::getCodigo).filter(java.util.Objects::nonNull)
                .filter(c -> c.matches("\\d+")).mapToInt(Integer::parseInt).max().orElse(0);
        String codigo;
        do codigo = String.format("%03d", ++maior); while (organizacaoMilitarRepository.existsByCodigo(codigo));
        return codigo;
    }
}
