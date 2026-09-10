package br.mil.controlemunicao.repository;
import br.mil.controlemunicao.entity.ItemDevolucao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.List;
public interface ItemDevolucaoRepository extends JpaRepository<ItemDevolucao, Long> {
    @EntityGraph(attributePaths={"devolucao","itemMovimentacao","itemMovimentacao.movimentacao","itemMovimentacao.loteMunicao","itemMovimentacao.loteMunicao.municao","itemMovimentacao.loteMunicao.municao.tipoMunicao"}) List<ItemDevolucao> findByDevolucaoId(Long devolucaoId);
    @EntityGraph(attributePaths={"devolucao","itemMovimentacao","itemMovimentacao.movimentacao","itemMovimentacao.loteMunicao","itemMovimentacao.loteMunicao.municao","itemMovimentacao.loteMunicao.municao.tipoMunicao"})
    List<ItemDevolucao> findByItemMovimentacaoMovimentacaoId(Long movimentacaoId);
}
