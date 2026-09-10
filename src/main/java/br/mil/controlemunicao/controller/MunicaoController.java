package br.mil.controlemunicao.controller;

import br.mil.controlemunicao.entity.Municao;
import br.mil.controlemunicao.entity.TipoMunicao;
import br.mil.controlemunicao.repository.MunicaoRepository;
import br.mil.controlemunicao.repository.TipoMunicaoRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/municoes")
public class MunicaoController {

    private final MunicaoRepository municaoRepository;
    private final TipoMunicaoRepository tipoMunicaoRepository;

    public MunicaoController(MunicaoRepository municaoRepository, TipoMunicaoRepository tipoMunicaoRepository) {
        this.municaoRepository = municaoRepository;
        this.tipoMunicaoRepository = tipoMunicaoRepository;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("municoes", municaoRepository.findAll());
        model.addAttribute("tipos", tipoMunicaoRepository.findAll());
        return "municao/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("municao", new Municao());
        model.addAttribute("tipos", tipoMunicaoRepository.findAll());
        return "municao/form";
    }

    @PostMapping
    public String salvar(@Valid @ModelAttribute("municao") Municao municao, BindingResult result,
                         RedirectAttributes redirectAttributes, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("tipos", tipoMunicaoRepository.findAll());
            return "municao/form";
        }

        municaoRepository.save(municao);
        redirectAttributes.addFlashAttribute("successMessage", "Munição salva com sucesso.");
        return "redirect:/municoes";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model) {
        Municao municao = municaoRepository.findById(id).orElseThrow();
        model.addAttribute("municao", municao);
        model.addAttribute("tipos", tipoMunicaoRepository.findAll());
        return "municao/form";
    }

    @PostMapping("/inativar/{id}")
    public String inativar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Municao municao = municaoRepository.findById(id).orElseThrow();
        municao.setAtivo(!municao.isAtivo());
        municaoRepository.save(municao);
        redirectAttributes.addFlashAttribute("successMessage", "Status da munição atualizado.");
        return "redirect:/municoes";
    }
}
