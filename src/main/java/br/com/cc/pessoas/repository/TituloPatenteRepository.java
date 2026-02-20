package br.com.cc.pessoas.repository;

import br.com.cc.pessoas.entity.TituloPatente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TituloPatenteRepository
        extends JpaRepository<TituloPatente, Long> {
}
