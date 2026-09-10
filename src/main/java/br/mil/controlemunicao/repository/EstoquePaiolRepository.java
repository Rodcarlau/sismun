package br.mil.controlemunicao.repository;

import br.mil.controlemunicao.entity.EstoquePaiol;
import br.mil.controlemunicao.entity.LoteMunicao;
import br.mil.controlemunicao.entity.Paiol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface EstoquePaiolRepository extends JpaRepository<EstoquePaiol, Long> {
    @Override @EntityGraph(attributePaths={"paiol","paiol.organizacaoMilitar","paiol.omDetentoraMunicao","loteMunicao","loteMunicao.organizacaoProprietaria","loteMunicao.municao","loteMunicao.municao.tipoMunicao"}) List<EstoquePaiol> findAll();
    @Override @EntityGraph(attributePaths={"paiol","loteMunicao","loteMunicao.municao","loteMunicao.municao.tipoMunicao"}) Optional<EstoquePaiol> findById(Long id);
    Optional<EstoquePaiol> findByPaiolAndLoteMunicao(Paiol paiol, LoteMunicao loteMunicao);
    List<EstoquePaiol> findByPaiol(Paiol paiol);
    List<EstoquePaiol> findByLoteMunicao(LoteMunicao loteMunicao);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from EstoquePaiol e where e.id = :id")
    Optional<EstoquePaiol> findByIdForUpdate(@Param("id") Long id);

    Optional<EstoquePaiol> findByPaiolIdAndLoteMunicaoId(Long paiolId, Long loteId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from EstoquePaiol e where e.paiol.id = :paiolId and e.loteMunicao.id = :loteId")
    Optional<EstoquePaiol> findDestinoForUpdate(@Param("paiolId") Long paiolId, @Param("loteId") Long loteId);
}
