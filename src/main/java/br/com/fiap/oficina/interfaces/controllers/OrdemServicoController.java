package br.com.fiap.oficina.interfaces.controllers;

import br.com.fiap.oficina.application.services.OrdemServicoService;
import br.com.fiap.oficina.interfaces.dtos.AdicionarPecaOSDTO;
import br.com.fiap.oficina.interfaces.dtos.AdicionarServicoOSDTO;
import br.com.fiap.oficina.interfaces.dtos.AtualizarStatusOSDTO;
import br.com.fiap.oficina.interfaces.dtos.OrdemServicoRequestDTO;
import br.com.fiap.oficina.interfaces.dtos.OrdemServicoResponseDTO;
import br.com.fiap.oficina.interfaces.dtos.StatusOrdemServicoResponseDTO;
import br.com.fiap.oficina.interfaces.dtos.TempoMedioExecucaoDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ordens-servico")
public class OrdemServicoController {

    private final OrdemServicoService ordemServicoService;

    public OrdemServicoController(
            OrdemServicoService ordemServicoService) {

        this.ordemServicoService = ordemServicoService;
    }

    @GetMapping("/relatorios/tempo-medio-execucao")
    public ResponseEntity<TempoMedioExecucaoDTO> obterTempoMedioExecucao() {

        return ResponseEntity.ok(
                ordemServicoService.obterTempoMedioExecucao()
        );
    }

    @PostMapping
    public ResponseEntity<OrdemServicoResponseDTO> criar(
            @Valid @RequestBody OrdemServicoRequestDTO request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ordemServicoService.criar(request));
    }

    @GetMapping
    public ResponseEntity<List<OrdemServicoResponseDTO>> listarTodas() {

        return ResponseEntity.ok(
                ordemServicoService.listarTodas()
        );
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<StatusOrdemServicoResponseDTO> consultarStatus(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                ordemServicoService.consultarStatus(id)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrdemServicoResponseDTO> buscarPorId(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                ordemServicoService.buscarPorId(id)
        );
    }

    @GetMapping("/veiculo/{veiculoId}")
    public ResponseEntity<List<OrdemServicoResponseDTO>> buscarPorVeiculo(
            @PathVariable Long veiculoId) {

        return ResponseEntity.ok(
                ordemServicoService.buscarPorVeiculo(veiculoId)
        );
    }

    @PostMapping("/{id}/servicos")
    public ResponseEntity<OrdemServicoResponseDTO> adicionarServico(
            @PathVariable Long id,
            @Valid @RequestBody AdicionarServicoOSDTO request) {

        return ResponseEntity.ok(
                ordemServicoService.adicionarServico(id, request)
        );
    }

    @PostMapping("/{id}/pecas")
    public ResponseEntity<OrdemServicoResponseDTO> adicionarPeca(
            @PathVariable Long id,
            @Valid @RequestBody AdicionarPecaOSDTO request) {

        return ResponseEntity.ok(
                ordemServicoService.adicionarPeca(id, request)
        );
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<OrdemServicoResponseDTO> atualizarStatus(
            @PathVariable Long id,
            @Valid @RequestBody AtualizarStatusOSDTO request) {

        return ResponseEntity.ok(
                ordemServicoService.atualizarStatus(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(
            @PathVariable Long id) {

        ordemServicoService.excluir(id);

        return ResponseEntity.noContent().build();
    }
}