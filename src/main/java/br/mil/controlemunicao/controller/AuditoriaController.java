package br.mil.controlemunicao.controller;

import br.mil.controlemunicao.repository.RegistroAuditoriaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/auditoria")
public class AuditoriaController {

    private final RegistroAuditoriaRepository registroAuditoriaRepository;

    public AuditoriaController(RegistroAuditoriaRepository registroAuditoriaRepository) {
        this.registroAuditoriaRepository = registroAuditoriaRepository;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("registros", registroAuditoriaRepository.findAll(Sort.by(Sort.Direction.DESC, "dataHora")));
        return "auditoria/lista";
    }
}
