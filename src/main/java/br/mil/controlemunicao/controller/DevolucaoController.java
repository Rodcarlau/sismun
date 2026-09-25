package br.mil.controlemunicao.controller;

import br.mil.controlemunicao.repository.DevolucaoRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.*;
import br.mil.controlemunicao.entity.*;
import br.mil.controlemunicao.repository.MovimentacaoRepository;
import br.mil.controlemunicao.repository.ItemDevolucaoRepository;
import br.mil.controlemunicao.service.DevolucaoService;
import br.mil.controlemunicao.dto.DevolucaoForm;
import java.io.IOException;
import java.util.List;
import org.springframework.security.core.Authentication;
import br.mil.controlemunicao.repository.UsuarioRepository;

@Controller
@RequestMapping("/devolucoes")
public class DevolucaoController {

    private final DevolucaoRepository devolucaoRepository;
    private final MovimentacaoRepository movimentacaoRepository;
    private final ItemDevolucaoRepository itemDevolucaoRepository;
    private final DevolucaoService devolucaoService;
    private final UsuarioRepository usuarioRepository;
    private final br.mil.controlemunicao.repository.PaiolRepository paiolRepository;
    private final br.mil.controlemunicao.repository.ReservaEstoqueRepository reservaEstoqueRepository;
    private final br.mil.controlemunicao.repository.RegistroEntregaEfetivaRepository registroEntregaRepository;

    public DevolucaoController(DevolucaoRepository devolucaoRepository, MovimentacaoRepository movimentacaoRepository, ItemDevolucaoRepository itemDevolucaoRepository, DevolucaoService devolucaoService, UsuarioRepository usuarioRepository, br.mil.controlemunicao.repository.PaiolRepository paiolRepository, br.mil.controlemunicao.repository.ReservaEstoqueRepository reservaEstoqueRepository, br.mil.controlemunicao.repository.RegistroEntregaEfetivaRepository registroEntregaRepository) {
        this.devolucaoRepository = devolucaoRepository;
        this.movimentacaoRepository = movimentacaoRepository;
        this.itemDevolucaoRepository = itemDevolucaoRepository; this.devolucaoService = devolucaoService;
        this.usuarioRepository=usuarioRepository;
        this.paiolRepository=paiolRepository;
        this.reservaEstoqueRepository=reservaEstoqueRepository;
        this.registroEntregaRepository=registroEntregaRepository;
    }

    @GetMapping
    public String listar(Model model) {
        var devolucoes = devolucaoRepository.findAll();
        var itensPorDevolucao = devolucoes.stream().collect(java.util.stream.Collectors.toMap(Devolucao::getId, d -> itemDevolucaoRepository.findByDevolucaoId(d.getId())));
        var paiolOrigemPorItem = new java.util.HashMap<Long, String>();
        itensPorDevolucao.values().stream().flatMap(java.util.Collection::stream).forEach(item ->
                reservaEstoqueRepository.findByItemMovimentacaoId(item.getItemMovimentacao().getId()).ifPresent(reserva ->
                        paiolOrigemPorItem.put(item.getItemMovimentacao().getId(), reserva.getEstoque().getPaiol().getNome())));
        model.addAttribute("devolucoes", devolucoes);
        model.addAttribute("itensPorDevolucao", itensPorDevolucao);
        model.addAttribute("paiolOrigemPorItem", paiolOrigemPorItem);
        var efetivoPorItem = itensPorDevolucao.values().stream().flatMap(java.util.Collection::stream)
                .collect(java.util.stream.Collectors.toMap(i -> i.getItemMovimentacao().getId(),
                        i -> registroEntregaRepository.findByItemMovimentacaoIdOrderById(i.getItemMovimentacao().getId()).stream()
                                .mapToInt(r -> r.getQuantidadeEfetiva() == null ? 0 : r.getQuantidadeEfetiva()).sum(),
                        (primeiro, segundo) -> primeiro));
        model.addAttribute("efetivoPorItem", efetivoPorItem);
        model.addAttribute("paiols", paiolRepository.findAll());
        return "devolucao/lista-abas";
    }

    @PostMapping("/{id}/consumo")
    public String consumo(@PathVariable Long id, @RequestParam List<Long> itemDevolucaoId, @RequestParam List<Integer> quantidadeConsumida,
                            RedirectAttributes redirectAttributes, Authentication auth) {
        devolucaoService.registrarConsumo(id, itemDevolucaoId, quantidadeConsumida, usuarioRepository.findByUsername(auth.getName()).orElse(null));
        redirectAttributes.addFlashAttribute("successMessage", "Consumo registrado com sucesso."); return "redirect:/devolucoes";
    }

    @PostMapping("/{id}/devolucao")
    public String devolucao(@PathVariable Long id, @RequestParam(required=false) List<Long> itemDevolucaoId, @RequestParam(required=false) List<Integer> quantidadeDevolvida, @RequestParam(required=false) List<Long> paiolDestinoId,
                            @RequestParam(required=false) String observacao, @RequestParam(value="arquivo", required=false) MultipartFile arquivo, RedirectAttributes redirectAttributes, Authentication auth) throws IOException {
        devolucaoService.registrarDevolucao(id, itemDevolucaoId, quantidadeDevolvida, paiolDestinoId,
                arquivo == null ? null : arquivo.getBytes(), arquivo == null ? null : arquivo.getOriginalFilename(), arquivo == null ? null : arquivo.getContentType(),
                observacao, usuarioRepository.findByUsername(auth.getName()).orElse(null));
        redirectAttributes.addFlashAttribute("successMessage", "Devolução registrada com sucesso."); return "redirect:/devolucoes";
    }

    @GetMapping("/{id}/anexo")
    public ResponseEntity<byte[]> baixarAnexo(@PathVariable Long id) { Devolucao d = devolucaoRepository.findById(id).orElseThrow(); if (d.getAnexoConteudo() == null) return ResponseEntity.notFound().build(); return ResponseEntity.ok().contentType(MediaType.parseMediaType(d.getAnexoTipo() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : d.getAnexoTipo())).header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + d.getAnexoNome() + "\"").body(d.getAnexoConteudo()); }
}
