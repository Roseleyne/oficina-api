package br.com.fiap.oficina.infrastructure.repositories;

import br.com.fiap.oficina.domain.ordemservico.OrdemServico;
import br.com.fiap.oficina.domain.ordemservico.StatusOrdemServico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface OrdemServicoRepository
        extends JpaRepository<OrdemServico, Long> {

    List<OrdemServico> findByVeiculoId(Long veiculoId);

    List<OrdemServico> findByStatusIn(Collection<StatusOrdemServico> status);
}
