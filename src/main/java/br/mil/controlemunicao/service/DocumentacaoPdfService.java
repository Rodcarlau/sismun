package br.mil.controlemunicao.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
public class DocumentacaoPdfService {
    private final DocumentacaoInventarioService inventario;

    public DocumentacaoPdfService(DocumentacaoInventarioService inventario) { this.inventario = inventario; }
    private static final Map<String, String> TITULOS = Map.of(
        "completa", "Documentação completa", "funcional", "Manual funcional",
        "tecnica", "Documentação técnica", "fluxogramas", "Fluxogramas",
        "banco", "Modelo de dados", "apresentacao", "Apresentação");

    public byte[] gerar(String tipo) {
        String titulo = TITULOS.get(tipo);
        if (titulo == null) throw new IllegalArgumentException("Documento não disponível");
        try {
            ByteArrayOutputStream saida = new ByteArrayOutputStream();
            Document pdf = new Document(PageSize.A4, 44, 44, 48, 48);
            PdfWriter.getInstance(pdf, saida);
            pdf.open();
            pdf.add(new Paragraph("SisMun - Sistema de Controle de Munição", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18)));
            pdf.add(new Paragraph(titulo, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14)));
            pdf.add(new Paragraph("Versão 0.0.1-SNAPSHOT | Gerado em " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));
            pdf.add(new Paragraph("Esta descrição corresponde ao código em execução na data de geração.\n "));
            if (!tipo.equals("banco") && !tipo.equals("tecnica")) {
                adicionar(pdf, "Fluxo operacional", "Solicitação e reserva; autorização; separação; despacho; confirmação da entrega; consumo; devolução quando houver saldo; finalização.");
                adicionar(pdf, "Estoque", "O saldo disponível é a quantidade atual menos a quantidade reservada. A reserva valida disponibilidade sob bloqueio. A entrega baixa quantidade do paiol de origem e libera a diferença da reserva. Quando há paiol de destino, ocorre transferência.");
                adicionar(pdf, "Estados", "SOLICITADA -> AUTORIZADA -> SEPARADA -> EM_TRANSPORTE -> ENTREGUE -> DEVOLUCAO_PENDENTE -> FINALIZADA. O cancelamento é permitido antes da entrega.");
            }
            if (!tipo.equals("banco") && !tipo.equals("fluxogramas")) {
                adicionar(pdf, "Rastreabilidade", "ReservaEstoque e MovimentacaoEstoque registram reserva, baixa e devolução. RegistroEntregaEfetiva conserva lote e quantidade informados na entrega. Devolucao e ItemDevolucao controlam consumo e retorno.");
                adicionar(pdf, "Implementação", "MovimentacaoController -> MovimentacaoService -> ReservaEstoqueService / EstoqueService / DevolucaoService -> repositórios JPA -> banco de dados.");
            }
            if (tipo.equals("banco") || tipo.equals("tecnica") || tipo.equals("completa"))
                adicionar(pdf, "Modelo de dados", "Movimentacao possui itens; ItemMovimentacao referencia LoteMunicao. ReservaEstoque associa item e EstoquePaiol. EstoquePaiol associa Paiol e LoteMunicao. Devolucao associa Movimentacao e ItemDevolucao.");
            if (tipo.equals("banco") || tipo.equals("tecnica") || tipo.equals("completa")) {
                for (DocumentacaoInventarioService.Relacao r : inventario.relacoes())
                    pdf.add(new Paragraph(r.origem() + "." + r.campo() + " -> " + r.destino() + " [" + r.tipo() + "]"));
            }
            if (tipo.equals("tecnica") || tipo.equals("completa")) {
                pdf.newPage();
                adicionar(pdf, "Endpoints registrados", "Inventário extraído do Spring MVC no momento da geração.");
                for (DocumentacaoInventarioService.Rota r : inventario.rotas())
                    pdf.add(new Paragraph(r.verbo() + " " + r.caminho() + " | " + r.controller() + "." + r.metodoJava()));
            }
            pdf.close();
            return saida.toByteArray();
        } catch (DocumentException e) { throw new IllegalStateException("Falha ao gerar documentação PDF", e); }
    }

    private void adicionar(Document pdf, String titulo, String conteudo) throws DocumentException {
        pdf.add(new Paragraph(titulo, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
        pdf.add(new Paragraph(conteudo + "\n "));
    }
}
