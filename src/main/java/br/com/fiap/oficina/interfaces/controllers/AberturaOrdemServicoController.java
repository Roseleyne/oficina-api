package br.com.fiap.oficina.interfaces.controllers;

import br.com.fiap.oficina.application.services.AberturaOrdemServicoService;
import br.com.fiap.oficina.interfaces.dtos.AberturaOrdemServicoRequestDTO;
import br.com.fiap.oficina.interfaces.dtos.OrdemServicoResponseDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ordens-servico")
public class AberturaOrdemServicoController {

    private final AberturaOrdemServicoService aberturaOrdemServicoService;

    public AberturaOrdemServicoController(
            AberturaOrdemServicoService aberturaOrdemServicoService) {

        this.aberturaOrdemServicoService = aberturaOrdemServicoService;
    }

    /**
     * Abertura completa: cliente + veiculo + servicos + pecas.
     * Retorna 201 com a OS criada (campo "id" = identificacao unica da OS).
     */
    @PostMapping("/completa")
    public ResponseEntity<OrdemServicoResponseDTO> abrir(
            @Valid @RequestBody AberturaOrdemServicoRequestDTO request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(aberturaOrdemServicoService.abrir(request));
    }
}
