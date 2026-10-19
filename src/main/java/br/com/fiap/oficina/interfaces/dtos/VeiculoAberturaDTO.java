package br.com.fiap.oficina.interfaces.dtos;

import br.com.fiap.oficina.interfaces.validation.PlacaVeiculo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Dados do veiculo na abertura completa da OS (o cliente vem no mesmo payload).
 */
public record VeiculoAberturaDTO(

        @NotBlank(message = "Placa é obrigatória")
        @PlacaVeiculo(message = "Placa de veículo inválida (deve seguir o formato tradicional ABC1234 ou Mercosul ABC1D23)")
        String placa,

        @NotBlank(message = "Marca é obrigatória")
        @Size(max = 50, message = "Marca deve ter no máximo 50 caracteres")
        String marca,

        @NotBlank(message = "Modelo é obrigatório")
        @Size(max = 50, message = "Modelo deve ter no máximo 50 caracteres")
        String modelo,

        @NotNull(message = "Ano é obrigatório")
        Integer ano

) {
}
