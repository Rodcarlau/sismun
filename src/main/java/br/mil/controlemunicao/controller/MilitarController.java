package br.mil.controlemunicao.controller;

import br.mil.controlemunicao.entity.Militar;
import br.mil.controlemunicao.entity.PostoGraduacao;
import br.mil.controlemunicao.entity.FuncaoMilitar;
import br.mil.controlemunicao.repository.MilitarRepository;
import br.mil.controlemunicao.repository.OrganizacaoMilitarRepository;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/militares")
public class MilitarController {

    private final MilitarRepository militarRepository;
    private final OrganizacaoMilitarRepository organizacaoMilitarRepository;

    public MilitarController(MilitarRepository militarRepository, OrganizacaoMilitarRepository organizacaoMilitarRepository) {
        this.militarRepository = militarRepository;
        this.organizacaoMilitarRepository = organizacaoMilitarRepository;
    }

    @GetMapping
    public String listar(Model model) {
        var militares = militarRepository.findAll().stream().sorted(java.util.Comparator.comparingInt((Militar m)->-m.getPostoGraduacao().ordinal()).thenComparing(Militar::getNomeCompleto,String.CASE_INSENSITIVE_ORDER)).toList();
        model.addAttribute("militares", militares);
        model.addAttribute("organizacoesMilitares", militares.stream().map(Militar::getOrganizacaoMilitar).filter(java.util.Objects::nonNull).distinct().sorted(java.util.Comparator.comparing(br.mil.controlemunicao.entity.OrganizacaoMilitar::getNome,String.CASE_INSENSITIVE_ORDER)).toList());
        return "militar/lista-accordion";
    }

    @GetMapping("/novo")
    public String novo(Model model) { model.addAttribute("militar", new Militar()); carregarOpcoes(model); return "militar/form"; }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) { model.addAttribute("militar", militarRepository.findById(id).orElseThrow()); carregarOpcoes(model); return "militar/form"; }

    @PostMapping
    @Transactional
    public String salvar(@ModelAttribute Militar militar, Model model, RedirectAttributes redirectAttributes) {
        if (militar.getNomeCompleto() == null || militar.getNomeCompleto().isBlank()
                || militar.getIdentidade() == null || militar.getIdentidade().isBlank()
                || militar.getPostoGraduacao() == null || militar.getOrganizacaoMilitar() == null
                || militar.getOrganizacaoMilitar().getId() == null) {
            return formularioComErro(militar, model, "Preencha todos os campos obrigatórios.");
        }

        Long id = militar.getId();
        if (militarRepository.existsByIdentidadeAndIdNot(militar.getIdentidade().trim(), id == null ? -1L : id)) {
            return formularioComErro(militar, model, "Já existe um militar cadastrado com esta identidade.");
        }

        var organizacao = organizacaoMilitarRepository.findById(militar.getOrganizacaoMilitar().getId()).orElse(null);
        if (organizacao == null) {
            return formularioComErro(militar, model, "Organização Militar não encontrada.");
        }

        Militar registro = id == null ? new Militar() : militarRepository.findById(id).orElse(null);
        if (registro == null) {
            return formularioComErro(militar, model, "Militar não encontrado.");
        }
        registro.setNomeCompleto(militar.getNomeCompleto().trim());
        registro.setIdentidade(militar.getIdentidade().trim());
        registro.setPostoGraduacao(militar.getPostoGraduacao());
        registro.setFuncao(militar.getFuncao());
        registro.setOrganizacaoMilitar(organizacao);
        registro.setSecao(militar.getSecao());
        registro.setTelefone(militar.getTelefone());
        registro.setEmail(militar.getEmail());
        registro.setAtivo(militar.isAtivo());
        militarRepository.save(registro);
        redirectAttributes.addFlashAttribute("successMessage", "Militar salvo com sucesso.");
        return "redirect:/militares";
    }

    private String formularioComErro(Militar militar, Model model, String mensagem) {
        model.addAttribute("militar", militar);
        model.addAttribute("errorMessage", mensagem);
        carregarOpcoes(model);
        return "militar/form";
    }

    private void carregarOpcoes(Model model) { model.addAttribute("organizacoes", organizacaoMilitarRepository.findAll()); model.addAttribute("postos", PostoGraduacao.values()); model.addAttribute("funcoes", FuncaoMilitar.values()); }
}
