package br.com.fiap.oficina.interfaces.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Abertura completa de OS: cliente + veiculo + servicos + pecas em uma unica chamada.
 */
public record AberturaOrdemServicoRequestDTO(

        @NotNull(message = "Cliente é obrigatório")
        @Valid
        ClienteRequestDTO cliente,

        @NotNull(message = "Veículo é obrigatório")
        @Valid
        VeiculoAberturaDTO veiculo,

        @NotBlank(message = "Descrição do problema é obrigatória")
        @Size(max = 500, message = "Descrição do problema deve ter no máximo 500 caracteres")
        String descricaoProblema,

        List<@Valid AdicionarServicoOSDTO> servicos,

        List<@Valid AdicionarPecaOSDTO> pecas

) {

    public AberturaOrdemServicoRequestDTO {
        servicos = servicos == null ? List.of() : servicos;
        pecas = pecas == null ? List.of() : pecas;
    }
}
