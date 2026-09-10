package br.mil.controlemunicao.repository;

import br.mil.controlemunicao.entity.Devolucao;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface DevolucaoRepository extends JpaRepository<Devolucao, Long> {
    @Override @EntityGraph(attributePaths={"movimentacao","movimentacao.omSolicitante"}) java.util.List<Devolucao> findAll();
    Optional<Devolucao> findByMovimentacaoId(Long movimentacaoId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Devolucao d where d.id = :id")
    Optional<Devolucao> findByIdForUpdate(@Param("id") Long id);
}
