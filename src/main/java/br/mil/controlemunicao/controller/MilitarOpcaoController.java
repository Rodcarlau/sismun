package br.mil.controlemunicao.controller;

import br.mil.controlemunicao.dto.MilitarOpcaoDto;
import br.mil.controlemunicao.entity.FuncaoMilitar;
import br.mil.controlemunicao.repository.MilitarRepository;
import br.mil.controlemunicao.repository.PaiolRepository;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/militares")
public class MilitarOpcaoController {
    private static final Set<FuncaoMilitar> OFICIAIS=Set.of(FuncaoMilitar.OFICIAL_MUNICAO,FuncaoMilitar.SUBSTITUTO_OFICIAL_MUNICAO);
    private static final Set<FuncaoMilitar> ESCOLTA=Set.of(FuncaoMilitar.ESCOLTA,FuncaoMilitar.CHEFE_VIATURA,FuncaoMilitar.OFICIAL_MUNICAO,FuncaoMilitar.SUBSTITUTO_OFICIAL_MUNICAO);
    private final MilitarRepository militares; private final PaiolRepository paiois;
    public MilitarOpcaoController(MilitarRepository militares,PaiolRepository paiois){this.militares=militares;this.paiois=paiois;}
    @GetMapping("/paiol/{id}") public List<MilitarOpcaoDto> paiol(@PathVariable Long id){var p=paiois.findById(id).orElseThrow();return dto(militares.findByAtivoTrueAndOrganizacaoMilitarIdOrderByNomeCompleto(p.getOrganizacaoMilitar().getId()));}
    @GetMapping("/motoristas") public List<MilitarOpcaoDto> motoristas(){return dto(militares.findByAtivoTrueAndFuncaoOrderByNomeCompleto(FuncaoMilitar.MOTORISTA));}
    @GetMapping("/oficiais-municao/organizacao/{id}") public List<MilitarOpcaoDto> oficiais(@PathVariable Long id){return dto(militares.findByAtivoTrueAndOrganizacaoMilitarIdAndFuncaoInOrderByNomeCompleto(id,OFICIAIS));}
    @GetMapping("/escolta") public List<MilitarOpcaoDto> escolta(@RequestParam Long organizacaoDetentoraId){return dto(militares.findByAtivoTrueAndFuncaoInOrderByNomeCompleto(ESCOLTA).stream().filter(m->!OFICIAIS.contains(m.getFuncao())||m.getOrganizacaoMilitar().getId().equals(organizacaoDetentoraId)).toList());}
    private List<MilitarOpcaoDto> dto(List<br.mil.controlemunicao.entity.Militar> lista){return lista.stream().map(MilitarOpcaoDto::de).toList();}
}
