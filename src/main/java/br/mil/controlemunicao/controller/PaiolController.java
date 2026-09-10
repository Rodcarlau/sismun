package br.mil.controlemunicao.controller;

import br.mil.controlemunicao.entity.Paiol;
import br.mil.controlemunicao.repository.OrganizacaoMilitarRepository;
import br.mil.controlemunicao.repository.PaiolRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/paiols")
public class PaiolController {

    private final PaiolRepository paiolRepository;
    private final OrganizacaoMilitarRepository organizacaoMilitarRepository;

    public PaiolController(PaiolRepository paiolRepository, OrganizacaoMilitarRepository organizacaoMilitarRepository) {
        this.paiolRepository = paiolRepository;
        this.organizacaoMilitarRepository = organizacaoMilitarRepository;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("paiols", paiolRepository.findAll());
        return "paiol/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        Paiol paiol = new Paiol(); paiol.setCodigo(proximoCodigo());
        model.addAttribute("paiol", paiol);
        model.addAttribute("organizacoes", organizacaoMilitarRepository.findAll());
        return "paiol/form";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("paiol", paiolRepository.findById(id).orElseThrow());
        model.addAttribute("organizacoes", organizacaoMilitarRepository.findAll());
        return "paiol/form";
    }

    @PostMapping
    public synchronized String salvar(@Valid @ModelAttribute("paiol") Paiol paiol, BindingResult result,
                         RedirectAttributes redirectAttributes, Model model) {
        if (paiol.getId() == null) paiol.setCodigo(proximoCodigo());
        if (paiol.isPrincipal()) {
            if (paiol.getOmDetentoraMunicao() == null || paiol.getOmDetentoraMunicao().getId() == null) {
                result.rejectValue("omDetentoraMunicao", "required", "Selecione a OM Detentora da Munição.");
            } else if (paiolRepository.existsByPrincipalTrueAndIdNot(paiol.getId() == null ? -1L : paiol.getId())) {
                result.rejectValue("principal", "unique", "Já existe um Paiol Principal cadastrado.");
            } else if (organizacaoMilitarRepository.findById(paiol.getOmDetentoraMunicao().getId()).isEmpty()) {
                result.rejectValue("omDetentoraMunicao", "invalid", "OM Detentora da Munição não encontrada.");
            }
        } else {
            paiol.setOmDetentoraMunicao(null);
        }
        if (result.hasErrors()) {
            model.addAttribute("organizacoes", organizacaoMilitarRepository.findAll());
            return "paiol/form";
        }
        paiolRepository.save(paiol);
        redirectAttributes.addFlashAttribute("successMessage", "Paiol salvo com sucesso.");
        return "redirect:/paiols";
    }

    private String proximoCodigo() {
        int maior = paiolRepository.findAll().stream().map(Paiol::getCodigo).filter(java.util.Objects::nonNull)
                .filter(c -> c.matches("P-\\d+")).map(c -> c.substring(2)).mapToInt(Integer::parseInt).max().orElse(0);
        String codigo;
        do codigo = String.format("P-%02d", ++maior); while (paiolRepository.existsByCodigo(codigo));
        return codigo;
    }
}
