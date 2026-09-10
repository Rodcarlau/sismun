package br.mil.controlemunicao.controller;

import br.mil.controlemunicao.entity.Usuario;
import br.mil.controlemunicao.repository.PerfilRepository;
import br.mil.controlemunicao.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioController(UsuarioRepository usuarioRepository, PerfilRepository perfilRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("usuarios", usuarioRepository.findAll());
        return "usuario/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) { model.addAttribute("usuario", new Usuario()); model.addAttribute("perfis", perfilRepository.findAll()); return "usuario/form"; }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) { Usuario usuario = usuarioRepository.findById(id).orElseThrow(); usuario.setSenha(""); model.addAttribute("usuario", usuario); model.addAttribute("perfis", perfilRepository.findAll()); return "usuario/form"; }

    @PostMapping
    public String salvar(@ModelAttribute Usuario usuario, RedirectAttributes redirectAttributes) {
        if (usuario.getId() != null) {
            Usuario atual = usuarioRepository.findById(usuario.getId()).orElseThrow();
            if (usuario.getSenha() == null || usuario.getSenha().isBlank()) usuario.setSenha(atual.getSenha()); else usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
        } else {
            if (usuario.getSenha() == null || usuario.getSenha().isBlank()) throw new IllegalArgumentException("Senha é obrigatória.");
            usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
        }
        usuarioRepository.save(usuario); redirectAttributes.addFlashAttribute("successMessage", "Usuário salvo com sucesso."); return "redirect:/usuarios";
    }

    @PostMapping("/{id}/status")
    public String alterarStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) { Usuario usuario = usuarioRepository.findById(id).orElseThrow(); usuario.setAtivo(!usuario.isAtivo()); usuarioRepository.save(usuario); redirectAttributes.addFlashAttribute("successMessage", "Status do usuário atualizado."); return "redirect:/usuarios"; }
}
