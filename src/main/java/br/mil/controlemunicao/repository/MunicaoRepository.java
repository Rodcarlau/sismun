package br.mil.controlemunicao.repository;

import br.mil.controlemunicao.entity.Municao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.List;
import java.util.Optional;

public interface MunicaoRepository extends JpaRepository<Municao, Long> {
    @Override @EntityGraph(attributePaths={"tipoMunicao"}) List<Municao> findAll();
    @Override @EntityGraph(attributePaths={"tipoMunicao"}) Optional<Municao> findById(Long id);
}
