package br.mil.controlemunicao.service;

import br.mil.controlemunicao.entity.*;
import br.mil.controlemunicao.repository.*;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;

@Service
public class RelatorioAnualPdfService {
    private final MovimentacaoRepository movimentacoes; private final ItemMovimentacaoRepository itens;
    private final DevolucaoRepository devolucoes; private final ItemDevolucaoRepository itensDevolucao;
    public RelatorioAnualPdfService(MovimentacaoRepository m, ItemMovimentacaoRepository i, DevolucaoRepository d, ItemDevolucaoRepository id) { movimentacoes=m; itens=i; devolucoes=d; itensDevolucao=id; }

    @Transactional(readOnly=true)
    public byte[] gerar(int ano) {
        if (ano<2000 || ano>2100) throw new IllegalArgumentException("Ano inválido.");
        var lista=movimentacoes.findByDataSolicitacaoBetween(LocalDate.of(ano,1,1),LocalDate.of(ano,12,31));
        lista.sort(Comparator.comparing(this::om).thenComparing(Movimentacao::getDataSolicitacao,Comparator.nullsLast(Comparator.naturalOrder())));
        try(ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            Document doc=new Document(PageSize.A4,30,30,35,35); PdfWriter.getInstance(doc,out); doc.open();
            Font titulo=FontFactory.getFont(FontFactory.HELVETICA_BOLD,16), grupo=FontFactory.getFont(FontFactory.HELVETICA_BOLD,12), normal=FontFactory.getFont(FontFactory.HELVETICA,9);
            doc.add(new Paragraph("Relatório anual do fluxo de movimentações - "+ano,titulo)); String atual=null;
            for(Movimentacao m:lista) { if(!om(m).equals(atual)){doc.add(new Paragraph("\nOrganização Solicitante: "+om(m),grupo));atual=om(m);} doc.add(new Paragraph("EB: "+texto(m.getEb()),grupo)); doc.add(new Paragraph("DIEx: "+m.getDiex()+" | Data: "+data(m.getDataSolicitacao())+" | Status: "+m.getStatus(),normal));
                doc.add(new Paragraph("Responsáveis: Retirada: "+militar(m.getMilitarPaiolRetirada())+" | Recebimento: "+militar(m.getMilitarPaiolRecebimento())+" | Oficial OM detentora: "+militar(m.getOficialMunicaoOmDetentora()),normal)); doc.add(new Paragraph("Escolta: "+militares(m.getEscoltas())+" | Motoristas: "+militares(m.getMotoristas()),normal));
                PdfPTable t=new PdfPTable(new float[]{1.4f,1.2f,1.2f,1.2f,.8f,.8f,.8f});t.setWidthPercentage(100);cab(t,"Tipo",normal);cab(t,"Calibre",normal);cab(t,"Lote",normal);cab(t,"Virola",normal);cab(t,"Solicitada",normal);cab(t,"Entregue",normal);cab(t,"Devolvida",normal);
                for(ItemMovimentacao i:itens.findByMovimentacaoId(m.getId())) {var l=i.getLoteMunicao();cel(t,l.getMunicao().getTipoMunicao().getNome(),normal);cel(t,l.getMunicao().getCalibre(),normal);cel(t,l.getLote(),normal);cel(t,texto(l.getVirola()),normal);cel(t,i.getQuantidadeSolicitada(),normal);cel(t,i.getQuantidadeEntregue(),normal);cel(t,devolvida(m.getId(),i.getId()),normal);} doc.add(t);
            }
            if(lista.isEmpty())doc.add(new Paragraph("Nenhuma movimentação encontrada.",normal)); doc.close(); return out.toByteArray();
        } catch(Exception e){throw new IllegalStateException("Não foi possível gerar o relatório PDF.",e);}
    }
    private String militar(Militar m){return m==null?"Não informado":m.getPostoGraduacao().getDescricao()+" — "+m.getNomeCompleto()+" — "+(m.getFuncao()==null?"Função não informada":m.getFuncao().getDescricao())+" — "+m.getOrganizacaoMilitar().getSigla();}
    private String militares(java.util.List<Militar> lista){return lista==null||lista.isEmpty()?"Não informados":lista.stream().map(this::militar).collect(java.util.stream.Collectors.joining(", "));}
    private int devolvida(Long movimentoId,Long itemId){return devolucoes.findByMovimentacaoId(movimentoId).flatMap(d->itensDevolucao.findByDevolucaoId(d.getId()).stream().filter(i->i.getItemMovimentacao().getId().equals(itemId)).findFirst()).map(ItemDevolucao::getQuantidadeDevolvida).orElse(0);}
    private void cab(PdfPTable t,String v,Font f){PdfPCell c=new PdfPCell(new Phrase(v,FontFactory.getFont(FontFactory.HELVETICA_BOLD,8)));c.setPadding(4);t.addCell(c);}private void cel(PdfPTable t,Object v,Font f){PdfPCell c=new PdfPCell(new Phrase(texto(v),f));c.setPadding(4);t.addCell(c);}private String om(Movimentacao m){return m.getOmSolicitante()==null?"Não informada":m.getOmSolicitante().getSigla()+" - "+m.getOmSolicitante().getNome();}private String data(LocalDate d){return d==null?"-":d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));}private String texto(Object o){return o==null||o.toString().isBlank()?"-":o.toString();}
}
