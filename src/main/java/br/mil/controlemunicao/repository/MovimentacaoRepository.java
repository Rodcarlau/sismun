package br.mil.controlemunicao.repository;

import br.mil.controlemunicao.entity.Movimentacao;
import br.mil.controlemunicao.entity.StatusMovimentacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

import java.time.LocalDate;
import java.util.List;

public interface MovimentacaoRepository extends JpaRepository<Movimentacao, Long> {
    @Override @EntityGraph(attributePaths={"omSolicitante","oficialMunicaoSolicitante","paiolOrigem","paiolOrigem.organizacaoMilitar","paiolDestino","paiolDestino.organizacaoMilitar","militarPaiolRetirada","militarPaiolRetirada.organizacaoMilitar","militarPaiolRecebimento","militarPaiolRecebimento.organizacaoMilitar","oficialMunicaoOmDetentora","oficialMunicaoOmDetentora.organizacaoMilitar"}) Optional<Movimentacao> findById(Long id);
    @Override @EntityGraph(attributePaths={"omSolicitante","tipoMunicao","itens","itens.loteMunicao","itens.loteMunicao.municao","itens.loteMunicao.municao.tipoMunicao"}) List<Movimentacao> findAll();
    List<Movimentacao> findByStatus(StatusMovimentacao status);
    List<Movimentacao> findByDataSolicitacaoBetween(LocalDate inicio, LocalDate fim);
    List<Movimentacao> findByDiexContainingIgnoreCase(String diex);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select m from Movimentacao m where m.id=:id") Optional<Movimentacao> findByIdForUpdate(@Param("id") Long id);
}
