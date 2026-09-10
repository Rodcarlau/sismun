package br.mil.controlemunicao.repository;

import br.mil.controlemunicao.entity.LoteMunicao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.List;
import java.util.Optional;

public interface LoteMunicaoRepository extends JpaRepository<LoteMunicao, Long> {
    @Override @EntityGraph(attributePaths={"municao","municao.tipoMunicao","organizacaoProprietaria"}) List<LoteMunicao> findAll();
    @Override @EntityGraph(attributePaths={"municao","municao.tipoMunicao","organizacaoProprietaria"}) Optional<LoteMunicao> findById(Long id);
    Page<LoteMunicao> findByAtivoTrue(Pageable pageable);
}
