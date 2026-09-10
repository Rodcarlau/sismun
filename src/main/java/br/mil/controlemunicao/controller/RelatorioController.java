package br.mil.controlemunicao.controller;
import br.mil.controlemunicao.service.RelatorioAnualPdfService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/relatorios")
public class RelatorioController {private final RelatorioAnualPdfService service;public RelatorioController(RelatorioAnualPdfService s){service=s;}@GetMapping(value="/movimentacoes.pdf",produces=MediaType.APPLICATION_PDF_VALUE)public ResponseEntity<byte[]> movimentacoes(@RequestParam int ano){return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"inline; filename=movimentacoes-"+ano+".pdf").contentType(MediaType.APPLICATION_PDF).body(service.gerar(ano));}}
