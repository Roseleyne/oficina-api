package br.com.fiap.oficina.infrastructure.repositories;

import br.com.fiap.oficina.domain.veiculo.Veiculo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VeiculoRepository extends JpaRepository<Veiculo, Long> {

    boolean existsByPlaca(String placa);

    Optional<Veiculo> findByPlaca(String placa);

    List<Veiculo> findByClienteId(Long clienteId);
}