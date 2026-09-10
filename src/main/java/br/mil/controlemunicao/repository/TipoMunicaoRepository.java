package br.mil.controlemunicao.repository;

import br.mil.controlemunicao.entity.TipoMunicao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TipoMunicaoRepository extends JpaRepository<TipoMunicao, Long> {
    Optional<TipoMunicao> findByNome(String nome);
}
