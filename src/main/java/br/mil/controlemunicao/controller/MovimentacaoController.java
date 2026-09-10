package br.mil.controlemunicao.controller;

import br.mil.controlemunicao.entity.Movimentacao;
import br.mil.controlemunicao.entity.StatusMovimentacao;
import br.mil.controlemunicao.entity.ItemDevolucao;
import br.mil.controlemunicao.entity.Usuario;
import br.mil.controlemunicao.repository.MovimentacaoRepository;
import br.mil.controlemunicao.repository.MilitarRepository;
import br.mil.controlemunicao.repository.OrganizacaoMilitarRepository;
import br.mil.controlemunicao.repository.PaiolRepository;
import br.mil.controlemunicao.repository.TipoMunicaoRepository;
import br.mil.controlemunicao.repository.ViaturaRepository;
import br.mil.controlemunicao.repository.EstoquePaiolRepository;
import br.mil.controlemunicao.service.MovimentacaoService;
import br.mil.controlemunicao.exception.BusinessException;
import br.mil.controlemunicao.dto.ItemMovimentacaoView;
import br.mil.controlemunicao.repository.ReservaEstoqueRepository;
import br.mil.controlemunicao.repository.ItemDevolucaoRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.*;
import java.io.IOException;
import java.time.Year;
import java.util.List;
import org.springframework.security.core.Authentication;
import br.mil.controlemunicao.repository.UsuarioRepository;

@Controller
@RequestMapping("/movimentacoes")
public class MovimentacaoController {

    private final MovimentacaoRepository movimentacaoRepository;
    private final MovimentacaoService movimentacaoService;
    private final OrganizacaoMilitarRepository organizacaoMilitarRepository;
    private final PaiolRepository paiolRepository;
    private final MilitarRepository militarRepository;
    private final TipoMunicaoRepository tipoMunicaoRepository;
    private final ViaturaRepository viaturaRepository;
    private final EstoquePaiolRepository estoquePaiolRepository;
    private final ReservaEstoqueRepository reservaEstoqueRepository;
    private final ItemDevolucaoRepository itemDevolucaoRepository;
    private final UsuarioRepository usuarioRepository;

    public MovimentacaoController(MovimentacaoRepository movimentacaoRepository,
                                  MovimentacaoService movimentacaoService,
                                  OrganizacaoMilitarRepository organizacaoMilitarRepository,
                                  PaiolRepository paiolRepository,
                                  MilitarRepository militarRepository, TipoMunicaoRepository tipoMunicaoRepository,
                                  ViaturaRepository viaturaRepository, EstoquePaiolRepository estoquePaiolRepository, ReservaEstoqueRepository reservaEstoqueRepository, ItemDevolucaoRepository itemDevolucaoRepository, UsuarioRepository usuarioRepository) {
        this.movimentacaoRepository = movimentacaoRepository;
        this.movimentacaoService = movimentacaoService;
        this.organizacaoMilitarRepository = organizacaoMilitarRepository;
        this.paiolRepository = paiolRepository;
        this.militarRepository = militarRepository;
        this.tipoMunicaoRepository = tipoMunicaoRepository;
        this.viaturaRepository = viaturaRepository;
        this.estoquePaiolRepository = estoquePaiolRepository;
        this.reservaEstoqueRepository = reservaEstoqueRepository;
        this.itemDevolucaoRepository = itemDevolucaoRepository;
        this.usuarioRepository=usuarioRepository;
    }

    @GetMapping
    public String listar(Model model) {
        var movimentacoes = movimentacaoRepository.findAll().stream()
                .sorted(java.util.Comparator.comparing(Movimentacao::getDataSolicitacao,
                                java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder()))
                        .thenComparing(Movimentacao::getId, java.util.Comparator.reverseOrder()))
                .toList();
        var consumoPorMovimentacao = new java.util.HashMap<Long, Integer>();
        for (Movimentacao movimentacao : movimentacoes) {
            var itensConsumo = itemDevolucaoRepository.findByItemMovimentacaoMovimentacaoId(movimentacao.getId());
            if (itensConsumo.stream().anyMatch(ItemDevolucao::isConsumoProcessado)) {
                consumoPorMovimentacao.put(movimentacao.getId(), itensConsumo.stream()
                        .filter(ItemDevolucao::isConsumoProcessado)
                        .mapToInt(item -> item.getQuantidadeConsumida() == null ? 0 : item.getQuantidadeConsumida())
                        .sum());
            }
        }
        model.addAttribute("movimentacoes", movimentacoes);
        model.addAttribute("consumoPorMovimentacao", consumoPorMovimentacao);
        model.addAttribute("statusList", StatusMovimentacao.values());
        return "movimentacao/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        Movimentacao movimentacao = new Movimentacao(); movimentacao.setStatus(StatusMovimentacao.SOLICITADA);
        model.addAttribute("movimentacao", movimentacao);
        carregarOpcoes(model);
        return "movimentacao/form";
    }

    @PostMapping
    public String salvar(@Valid @ModelAttribute("movimentacao") Movimentacao movimentacao, BindingResult result,
                         @RequestParam(required = false) List<Long> itemId, @RequestParam(required = false) List<Long> estoqueId, @RequestParam(required = false) List<Integer> quantidadeItem, @RequestParam(value = "arquivo", required = false) MultipartFile arquivo,
                         RedirectAttributes redirectAttributes, Model model, Authentication auth) {
        if (result.hasErrors()) {
            carregarOpcoes(model);
            return "movimentacao/form";
        }

        try {
            if (movimentacao.getId() != null && (arquivo == null || arquivo.isEmpty())) {
                Movimentacao atual = movimentacaoService.buscarPorId(movimentacao.getId());
                movimentacao.setAnexoNome(atual.getAnexoNome()); movimentacao.setAnexoTipo(atual.getAnexoTipo()); movimentacao.setAnexoConteudo(atual.getAnexoConteudo());
            } else if (arquivo != null && !arquivo.isEmpty()) {
                movimentacao.setAnexoNome(arquivo.getOriginalFilename()); movimentacao.setAnexoTipo(arquivo.getContentType()); movimentacao.setAnexoConteudo(arquivo.getBytes());
            }
        } catch (IOException ex) { throw new IllegalArgumentException("Não foi possível armazenar o anexo.", ex); }
        try {
            if (movimentacao.getId() == null) {
                movimentacaoService.criarSolicitacaoComItens(movimentacao, estoqueId, quantidadeItem, usuarioRepository.findByUsername(auth.getName()).orElse(null));
                redirectAttributes.addFlashAttribute("successMessage", "Solicitação salva e estoque reservado com sucesso.");
            } else {
                movimentacaoService.atualizarItens(movimentacao.getId(), movimentacao, itemId, estoqueId, quantidadeItem, usuarioRepository.findByUsername(auth.getName()).orElse(null));
                redirectAttributes.addFlashAttribute("successMessage", "Solicitação, itens e reservas atualizados com sucesso.");
            }
        } catch (BusinessException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            carregarOpcoes(model);
            return "movimentacao/form";
        }
        return "redirect:/movimentacoes";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) { Movimentacao m=movimentacaoService.buscarPorId(id); model.addAttribute("movimentacao",m); model.addAttribute("itensEdicao",movimentacaoService.itens(id).stream().map(i->{var r=reservaEstoqueRepository.findByItemMovimentacaoId(i.getId()).orElseThrow();return new ItemMovimentacaoView(i,r.getEstoque().getId(),i.getLoteMunicao().getMunicao().getId());}).toList()); carregarOpcoes(model); return "movimentacao/form"; }

    @GetMapping("/{id}")
    public String detalhes(@PathVariable Long id, Model model) {
        Movimentacao movimentacao=movimentacaoService.buscarPorId(id);
        var prestacoes=itemDevolucaoRepository.findByItemMovimentacaoMovimentacaoId(id);
        model.addAttribute("movimentacao", movimentacao); model.addAttribute("camposPendentesAceite", movimentacaoService.camposPendentesParaAceite(movimentacao)); model.addAttribute("itens", movimentacaoService.itens(id));
        model.addAttribute("consumidoPorItem",prestacoes.stream().collect(java.util.stream.Collectors.toMap(d->d.getItemMovimentacao().getId(),ItemDevolucao::getQuantidadeConsumida)));
        model.addAttribute("devolvidoPorItem",prestacoes.stream().collect(java.util.stream.Collectors.toMap(d->d.getItemMovimentacao().getId(),ItemDevolucao::getQuantidadeDevolvida)));
        model.addAttribute("origemPorItem",reservaEstoqueRepository.findByItemMovimentacaoMovimentacaoId(id).stream().collect(java.util.stream.Collectors.toMap(r->r.getItemMovimentacao().getId(),r->r.getEstoque().getPaiol().getNome())));
        return "movimentacao/detalhes";
    }

    @PostMapping("/{id}/acao")
    public String acao(@PathVariable Long id, @RequestParam String acao, @RequestParam(required=false) List<Long> itemEntregaId, @RequestParam(required=false) List<Integer> quantidadeEntregue,
                       @RequestParam(required = false) String justificativa, RedirectAttributes redirectAttributes, Authentication auth) {
        Usuario usuario=usuarioRepository.findByUsername(auth.getName()).orElse(null);
        switch (acao) {
            case "autorizar" -> movimentacaoService.autorizar(id, usuario);
            case "separar" -> movimentacaoService.confirmarSeparacao(id, usuario);
            case "despachar" -> movimentacaoService.despachar(id, usuario);
            case "entregar" -> movimentacaoService.confirmarEntregaItens(id, itemEntregaId, quantidadeEntregue, justificativa, usuario);
            case "cancelar" -> movimentacaoService.cancelar(id, usuario);
            default -> throw new IllegalArgumentException("Ação inválida.");
        }
        redirectAttributes.addFlashAttribute("successMessage", "Etapa processada com sucesso."); return "redirect:/movimentacoes/" + id;
    }

    @GetMapping("/{id}/anexo")
    public ResponseEntity<byte[]> baixarAnexo(@PathVariable Long id) { Movimentacao m = movimentacaoService.buscarPorId(id); if (m.getAnexoConteudo() == null) return ResponseEntity.notFound().build(); return ResponseEntity.ok().contentType(MediaType.parseMediaType(m.getAnexoTipo() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : m.getAnexoTipo())).header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + m.getAnexoNome() + "\"").body(m.getAnexoConteudo()); }

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Long id, RedirectAttributes redirectAttributes, Authentication auth) {
        movimentacaoService.excluir(id, usuarioRepository.findByUsername(auth.getName()).orElse(null));
        redirectAttributes.addFlashAttribute("successMessage", "Movimentação excluída com sucesso.");
        return "redirect:/movimentacoes";
    }

    private void carregarOpcoes(Model model) {
        model.addAttribute("statusList", StatusMovimentacao.values());
        model.addAttribute("anoAtual", Year.now().getValue());
        model.addAttribute("organizacoes", organizacaoMilitarRepository.findAll());
        model.addAttribute("paiols", paiolRepository.findAll());
        model.addAttribute("militares", militarRepository.findAll());
        model.addAttribute("tiposMunicao", tipoMunicaoRepository.findAll());
        model.addAttribute("viaturas", viaturaRepository.findAll());
        var estoques = estoquePaiolRepository.findAll().stream().filter(e -> e.getLoteMunicao().isAtivo() && e.getLoteMunicao().getMunicao().isAtivo() && !e.getLoteMunicao().getDataValidade().isBefore(java.time.LocalDate.now())).toList();
        model.addAttribute("estoques", estoques);
        model.addAttribute("municoes", estoques.stream().map(e -> e.getLoteMunicao().getMunicao()).distinct().toList());
    }
}
