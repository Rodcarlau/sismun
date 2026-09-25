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
    private final RegistroEntregaEfetivaRepository registrosEfetivos;
    public RelatorioAnualPdfService(MovimentacaoRepository m, ItemMovimentacaoRepository i, DevolucaoRepository d, ItemDevolucaoRepository id, RegistroEntregaEfetivaRepository re) { movimentacoes=m; itens=i; devolucoes=d; itensDevolucao=id; registrosEfetivos=re; }

    @Transactional(readOnly=true)
    public byte[] gerar(int ano) {
        if (ano<2000 || ano>2100) throw new IllegalArgumentException("Ano inválido.");
        var lista=movimentacoes.findByDataSolicitacaoBetween(LocalDate.of(ano,1,1),LocalDate.of(ano,12,31));
        lista.sort(Comparator.comparing(this::om).thenComparing(Movimentacao::getDataSolicitacao,Comparator.nullsLast(Comparator.naturalOrder())));
        try(ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            Document doc=new Document(PageSize.A4,30,30,35,35); PdfWriter.getInstance(doc,out); doc.open();
            Font titulo=FontFactory.getFont(FontFactory.HELVETICA_BOLD,16), grupo=FontFactory.getFont(FontFactory.HELVETICA_BOLD,12), normal=FontFactory.getFont(FontFactory.HELVETICA,9);
            doc.add(new Paragraph("Relatório anual do fluxo de movimentações - "+ano,titulo));
            doc.add(new Paragraph("Gerado em: "+data(LocalDate.now()),normal)); String atual=null;
            for(Movimentacao m:lista) { if(!om(m).equals(atual)){doc.add(new Paragraph("\nOrganização Solicitante: "+om(m),grupo));atual=om(m);} doc.add(new Paragraph("EB: "+texto(m.getEb()),grupo)); doc.add(new Paragraph("DIEx: "+m.getDiex()+" | Data da Solicitação: "+data(m.getDataSolicitacao())+" | Status: "+m.getStatus(),normal));
                doc.add(new Paragraph("Motivo da Movimentação: "+texto(m.getMotivoRetirada()),normal));
                if (m.getObservacao() != null && !m.getObservacao().isBlank()) doc.add(new Paragraph("Observação: "+m.getObservacao(),normal));
                PdfPTable t=new PdfPTable(new float[]{1.2f,1.1f,1.1f,1.1f,.7f,.7f,.7f,.7f,.7f});t.setWidthPercentage(100);cab(t,"Tipo",normal);cab(t,"Lote/Virola",normal);cab(t,"Solicitada",normal);cab(t,"Reservada",normal);cab(t,"Efetiva",normal);cab(t,"Consumida",normal);cab(t,"Devolvida",normal);cab(t,"Saldo",normal);cab(t,"Divergência",normal);
                for(ItemMovimentacao i:itens.findByMovimentacaoId(m.getId())) {var l=i.getLoteMunicao();int efetiva=efetiva(i), consumida=consumida(m.getId(),i.getId()), devolvida=devolvida(m.getId(),i.getId());cel(t,l.getMunicao().getTipoMunicao().getNome(),normal);cel(t,l.getLote()+" / "+texto(l.getVirola()),normal);cel(t,i.getQuantidadeSolicitada(),normal);cel(t,reservada(i),normal);cel(t,efetiva,normal);cel(t,consumida,normal);cel(t,devolvida,normal);cel(t,efetiva-consumida-devolvida,normal);cel(t,divergencia(i),normal);} doc.add(t);
            }
            if(lista.isEmpty())doc.add(new Paragraph("Nenhuma movimentação encontrada.",normal)); doc.close(); return out.toByteArray();
        } catch(Exception e){throw new IllegalStateException("Não foi possível gerar o relatório PDF.",e);}
    }
    private int devolvida(Long movimentoId,Long itemId){return devolucoes.findByMovimentacaoId(movimentoId).flatMap(d->itensDevolucao.findByDevolucaoId(d.getId()).stream().filter(i->i.getItemMovimentacao().getId().equals(itemId)).findFirst()).map(ItemDevolucao::getQuantidadeDevolvida).orElse(0);}
    private int consumida(Long movimentoId,Long itemId){return devolucoes.findByMovimentacaoId(movimentoId).flatMap(d->itensDevolucao.findByDevolucaoId(d.getId()).stream().filter(i->i.getItemMovimentacao().getId().equals(itemId)).findFirst()).map(ItemDevolucao::getQuantidadeConsumida).orElse(0);}
    private int efetiva(ItemMovimentacao item){var r=registrosEfetivos.findByItemMovimentacaoIdOrderById(item.getId());return r.isEmpty()?(item.getQuantidadeEntregue()==null?0:item.getQuantidadeEntregue()):r.stream().mapToInt(x->x.getQuantidadeEfetiva()==null?0:x.getQuantidadeEfetiva()).sum();}
    private int reservada(ItemMovimentacao item){var r=registrosEfetivos.findByItemMovimentacaoIdOrderById(item.getId());return r.isEmpty()?(item.getQuantidadeReservada()==null?0:item.getQuantidadeReservada()):r.get(0).getQuantidadeReservada();}
    private String divergencia(ItemMovimentacao item){var r=registrosEfetivos.findByItemMovimentacaoIdOrderById(item.getId());return r.stream().filter(x->!java.util.Objects.equals(x.getLoteMunicao().getId(),item.getLoteMunicao().getId())||!java.util.Objects.equals(x.getQuantidadeEfetiva(),item.getQuantidadeSeparada())).map(x->texto(x.getJustificativa())).collect(java.util.stream.Collectors.joining(" | "));}
    private void cab(PdfPTable t,String v,Font f){PdfPCell c=new PdfPCell(new Phrase(v,FontFactory.getFont(FontFactory.HELVETICA_BOLD,8)));c.setPadding(4);t.addCell(c);}private void cel(PdfPTable t,Object v,Font f){PdfPCell c=new PdfPCell(new Phrase(texto(v),f));c.setPadding(4);t.addCell(c);}private String om(Movimentacao m){return m.getOmSolicitante()==null?"Não informada":m.getOmSolicitante().getSigla()+" - "+m.getOmSolicitante().getNome();}private String data(LocalDate d){return d==null?"-":d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));}private String texto(Object o){return o==null||o.toString().isBlank()?"-":o.toString();}
}
