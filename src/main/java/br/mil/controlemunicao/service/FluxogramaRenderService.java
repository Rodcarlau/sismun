package br.mil.controlemunicao.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class FluxogramaRenderService {
    private static final int LABEL = 150, CELL_W = 190, LANE_H = 115, MARGIN = 24;
    private final FluxogramaMenuService fluxos;

    public FluxogramaRenderService(FluxogramaMenuService fluxos) { this.fluxos = fluxos; }

    public String svg(String id) {
        FluxogramaMenuService.Fluxo f = fluxos.fluxo(id);
        int colunas = f.passos().stream().mapToInt(FluxogramaMenuService.Passo::coluna).max().orElse(0) + 1;
        int largura = MARGIN * 2 + LABEL + colunas * CELL_W;
        int altura = 115 + f.raias().size() * LANE_H + 78;
        Map<String, Pos> pos = posicoes(f);
        StringBuilder s = new StringBuilder();
        s.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"").append(largura).append("\" height=\"").append(altura).append("\" viewBox=\"0 0 ").append(largura).append(' ').append(altura).append("\" role=\"img\">")
            .append("<defs><marker id=\"arrow\" viewBox=\"0 0 10 10\" refX=\"9\" refY=\"5\" markerWidth=\"7\" markerHeight=\"7\" orient=\"auto-start-reverse\"><path d=\"M 0 0 L 10 5 L 0 10 z\" fill=\"#35536e\"/></marker></defs>")
            .append("<style>text{font-family:Arial,sans-serif;fill:#17324a}.title{font-size:22px;font-weight:700}.meta{font-size:12px;fill:#587086}.lane{font-size:13px;font-weight:700}.step{font-size:13px;font-weight:700}.detail{font-size:10px;fill:#486174}.edge{stroke:#35536e;stroke-width:2;fill:none;marker-end:url(#arrow)}.label{font-size:10px;font-weight:700;paint-order:stroke;stroke:white;stroke-width:4px}</style>");
        s.append("<rect width=\"100%\" height=\"100%\" fill=\"white\"/><text x=\"").append(MARGIN).append("\" y=\"32\" class=\"title\">Fluxograma — ").append(esc(f.menu().nome())).append("</text>")
            .append("<text x=\"").append(MARGIN).append("\" y=\"54\" class=\"meta\">SisMun — Sistema de Controle de Munição | Estado atual em ").append(data()).append(" | versão 0.0.1-SNAPSHOT</text>");
        for (int i=0;i<f.raias().size();i++) {
            int y=78+i*LANE_H;
            s.append("<rect x=\"").append(MARGIN).append("\" y=\"").append(y).append("\" width=\"").append(largura-2*MARGIN).append("\" height=\"").append(LANE_H).append("\" fill=\"").append(i%2==0?"#f7f9fc":"#eef4f8").append("\" stroke=\"#b8c7d4\"/>")
             .append("<text x=\"").append(MARGIN+10).append("\" y=\"").append(y+LANE_H/2).append("\" class=\"lane\">").append(esc(f.raias().get(i))).append("</text>");
        }
        for (FluxogramaMenuService.Ligacao l:f.ligacoes()) {
            Pos a=pos.get(l.origem()), b=pos.get(l.destino()); if(a==null||b==null) continue;
            s.append("<path class=\"edge\" d=\"M ").append(a.x+70).append(' ').append(a.y).append(" L ").append(b.x-75).append(' ').append(b.y).append("\"/>");
            if(!l.rotulo().isBlank()) s.append("<text x=\"").append((a.x+b.x)/2).append("\" y=\"").append((a.y+b.y)/2-5).append("\" class=\"label\">").append(esc(l.rotulo())).append("</text>");
        }
        for (FluxogramaMenuService.Passo p:f.passos()) desenharPassoSvg(s,p,pos.get(p.id()));
        int ly=78+f.raias().size()*LANE_H+28;
        s.append("<text x=\"").append(MARGIN).append("\" y=\"").append(ly).append("\" class=\"meta\">Legenda: ▭ processo | ◇ decisão | 👤 ação humana | ⚙ processamento interno | ▱ documento | seta = sequência</text>")
            .append("<text x=\"").append(MARGIN).append("\" y=\"").append(ly+20).append("\" class=\"meta\">Fluxograma correspondente ao estado atual do sistema na data da geração.</text></svg>");
        return s.toString();
    }

    private void desenharPassoSvg(StringBuilder s, FluxogramaMenuService.Passo p, Pos q) {
        String fill=switch(p.forma()){case "decisao"->"#fff4d6";case "usuario"->"#e7f5e8";case "automatico"->"#f1eafd";case "documento"->"#fff0e5";default->"#ffffff";};
        if(p.forma().equals("decisao")) s.append("<polygon points=\"").append(q.x).append(',').append(q.y-38).append(' ').append(q.x+72).append(',').append(q.y).append(' ').append(q.x).append(',').append(q.y+38).append(' ').append(q.x-72).append(',').append(q.y).append("\" fill=\"").append(fill).append("\" stroke=\"#35536e\" stroke-width=\"2\"/>");
        else s.append("<rect x=\"").append(q.x-75).append("\" y=\"").append(q.y-38).append("\" width=\"150\" height=\"76\" rx=\"").append(p.forma().equals("usuario")?"20":"5").append("\" fill=\"").append(fill).append("\" stroke=\"#35536e\" stroke-width=\"2\"/>");
        textSvg(s,q.x,q.y-7,p.texto(),"step"); textSvg(s,q.x,q.y+15,p.detalhe(),"detail");
    }

    private void textSvg(StringBuilder s,int x,int y,String texto,String classe){
        List<String> linhas=quebrar(texto,28); int inicio=y-(linhas.size()-1)*6;
        s.append("<text x=\"").append(x).append("\" y=\"").append(inicio).append("\" class=\"").append(classe).append("\" text-anchor=\"middle\">");
        for(int i=0;i<linhas.size();i++) s.append("<tspan x=\"").append(x).append("\" dy=\"").append(i==0?0:12).append("\">").append(esc(linhas.get(i))).append("</tspan>"); s.append("</text>");
    }

    public byte[] pdf(String id) { return pdf(List.of(fluxos.fluxo(id)), false); }
    public byte[] pdfTodos() { return pdf(fluxos.menus().stream().map(m->fluxos.fluxo(m.id())).toList(), true); }

    private byte[] pdf(List<FluxogramaMenuService.Fluxo> lista, boolean capa) {
        try {
            ByteArrayOutputStream out=new ByteArrayOutputStream(); Document doc=new Document(PageSize.A3.rotate(),24,24,28,28); PdfWriter w=PdfWriter.getInstance(doc,out); doc.open();
            if(capa){doc.add(new Paragraph("SisMun",FontFactory.getFont(FontFactory.HELVETICA_BOLD,28)));doc.add(new Paragraph("Sistema de Controle de Munição\nFluxogramas do Sistema\nGerado em "+data()+"\nVersão 0.0.1-SNAPSHOT",FontFactory.getFont(FontFactory.HELVETICA,16)));doc.newPage();}
            for(int n=0;n<lista.size();n++){desenharPdf(w.getDirectContent(),lista.get(n),doc.getPageSize().getWidth(),doc.getPageSize().getHeight());if(n<lista.size()-1)doc.newPage();}
            doc.close();return out.toByteArray();
        } catch(DocumentException|java.io.IOException e){throw new IllegalStateException("Falha ao gerar fluxograma PDF",e);}
    }

    private void desenharPdf(PdfContentByte cb,FluxogramaMenuService.Fluxo f,float pw,float ph)throws java.io.IOException{
        BaseFont normal=BaseFont.createFont(BaseFont.HELVETICA,BaseFont.WINANSI,false), bold=BaseFont.createFont(BaseFont.HELVETICA_BOLD,BaseFont.WINANSI,false);
        int cols=f.passos().stream().mapToInt(FluxogramaMenuService.Passo::coluna).max().orElse(0)+1; float scale=Math.min((pw-60-LABEL)/(cols*CELL_W),(ph-125)/(float)(f.raias().size()*LANE_H)); scale=Math.min(scale,1f); float ox=30, top=ph-75;
        texto(cb,bold,18,ox,ph-36,"Fluxograma - "+f.menu().nome()); texto(cb,normal,9,ox,ph-53,"SisMun - Sistema de Controle de Municao | Gerado em "+data()+" | versao 0.0.1-SNAPSHOT");
        Map<String,Pos> pos=posicoes(f);
        for(int i=0;i<f.raias().size();i++){float y=top-(i+1)*LANE_H*scale;cb.setColorFill(i%2==0?new Color(247,249,252):new Color(238,244,248));cb.setColorStroke(new Color(184,199,212));cb.rectangle(ox,y,pw-60,LANE_H*scale);cb.fillStroke();texto(cb,bold,8,ox+5,y+LANE_H*scale/2,f.raias().get(i));}
        for(FluxogramaMenuService.Ligacao l:f.ligacoes()){Pos a=pos.get(l.origem()),b=pos.get(l.destino());if(a==null||b==null)continue;float ax=ox+(a.x-MARGIN)*scale+60, ay=top-(a.y-78)*scale;float bx=ox+(b.x-MARGIN)*scale-60,by=top-(b.y-78)*scale;cb.setColorStroke(new Color(53,83,110));cb.moveTo(ax,ay);cb.lineTo(bx,by);cb.stroke();cb.moveTo(bx,by);cb.lineTo(bx-5,by+3);cb.lineTo(bx-5,by-3);cb.closePathStroke();if(!l.rotulo().isBlank())texto(cb,bold,7,(ax+bx)/2,(ay+by)/2+3,l.rotulo());}
        for(FluxogramaMenuService.Passo p:f.passos()){Pos q=pos.get(p.id());float x=ox+(q.x-MARGIN)*scale,y=top-(q.y-78)*scale,w=145*scale,h=70*scale;cb.setColorFill(Color.WHITE);cb.setColorStroke(new Color(53,83,110));if(p.forma().equals("decisao")){cb.moveTo(x,y+h/2);cb.lineTo(x+w/2,y);cb.lineTo(x,y-h/2);cb.lineTo(x-w/2,y);cb.closePathFillStroke();}else{cb.rectangle(x-w/2,y-h/2,w,h);cb.fillStroke();}textoCentro(cb,bold,8,x,y+7,p.texto());textoCentro(cb,normal,6,x,y-9,p.detalhe());}
        texto(cb,normal,8,ox,25,"Legenda: retangulo=processo | losango=decisao | seta=sequencia. Fluxograma correspondente ao estado atual do sistema.");
    }

    private Map<String,Pos> posicoes(FluxogramaMenuService.Fluxo f){Map<String,Pos> m=new HashMap<>();for(FluxogramaMenuService.Passo p:f.passos()){int lane=f.raias().indexOf(p.raia());m.put(p.id(),new Pos(MARGIN+LABEL+p.coluna()*CELL_W+CELL_W/2,78+lane*LANE_H+LANE_H/2));}return m;}
    private record Pos(int x,int y){}
    private static List<String> quebrar(String s,int max){List<String> out=new ArrayList<>();StringBuilder l=new StringBuilder();for(String w:s.split(" ")){if(l.length()>0&&l.length()+w.length()+1>max){out.add(l.toString());l.setLength(0);}if(l.length()>0)l.append(' ');l.append(w);}if(l.length()>0)out.add(l.toString());return out;}
    private static String esc(String s){return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");}
    private static String data(){return LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));}
    private static void texto(PdfContentByte cb,BaseFont f,float size,float x,float y,String t){cb.beginText();cb.setFontAndSize(f,size);cb.setTextMatrix(x,y);cb.showText(latin(t));cb.endText();}
    private static void textoCentro(PdfContentByte cb,BaseFont f,float size,float x,float y,String t){List<String> ls=quebrar(latin(t),30);for(int i=0;i<Math.min(3,ls.size());i++){float tx=x-f.getWidthPoint(ls.get(i),size)/2;texto(cb,f,size,tx,y-i*(size+2),ls.get(i));}}
    private static String latin(String s){return s.replace('ç','c').replace('ã','a').replace('õ','o').replace('á','a').replace('é','e').replace('í','i').replace('ó','o').replace('ú','u').replace('â','a').replace('ê','e').replace('ô','o').replace('Ç','C').replace('Ã','A').replace('Õ','O');}
}
