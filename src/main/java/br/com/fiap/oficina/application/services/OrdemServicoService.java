package br.com.fiap.oficina.application.services;

import br.com.fiap.oficina.domain.ordemservico.OrdemServico;
import br.com.fiap.oficina.domain.ordemservico.OrdemServicoPeca;
import br.com.fiap.oficina.domain.ordemservico.OrdemServicoServico;
import br.com.fiap.oficina.domain.ordemservico.OrdemServicoStatusAlteradoEvent;
import br.com.fiap.oficina.domain.ordemservico.StatusOrdemServico;
import br.com.fiap.oficina.domain.peca.Peca;
import br.com.fiap.oficina.domain.servico.Servico;
import br.com.fiap.oficina.domain.veiculo.Veiculo;
import br.com.fiap.oficina.infrastructure.repositories.OrdemServicoPecaRepository;
import br.com.fiap.oficina.infrastructure.repositories.OrdemServicoRepository;
import br.com.fiap.oficina.infrastructure.repositories.OrdemServicoServicoRepository;
import br.com.fiap.oficina.infrastructure.repositories.PecaRepository;
import br.com.fiap.oficina.infrastructure.repositories.ServicoRepository;
import br.com.fiap.oficina.infrastructure.repositories.VeiculoRepository;
import br.com.fiap.oficina.interfaces.dtos.AdicionarPecaOSDTO;
import br.com.fiap.oficina.interfaces.dtos.AdicionarServicoOSDTO;
import br.com.fiap.oficina.interfaces.dtos.AtualizarStatusOSDTO;
import br.com.fiap.oficina.interfaces.dtos.OrdemServicoPecaResponseDTO;
import br.com.fiap.oficina.interfaces.dtos.OrdemServicoRequestDTO;
import br.com.fiap.oficina.interfaces.dtos.OrdemServicoResponseDTO;
import br.com.fiap.oficina.interfaces.dtos.OrdemServicoServicoResponseDTO;
import br.com.fiap.oficina.interfaces.dtos.StatusOrdemServicoResponseDTO;
import br.com.fiap.oficina.interfaces.dtos.TempoMedioExecucaoDTO;
import br.com.fiap.oficina.interfaces.exceptions.EstoqueInsuficienteException;
import br.com.fiap.oficina.interfaces.exceptions.OrdemServicoNaoEncontradaException;
import br.com.fiap.oficina.interfaces.exceptions.PecaNaoEncontradaException;
import br.com.fiap.oficina.interfaces.exceptions.ServicoNaoEncontradoException;
import br.com.fiap.oficina.interfaces.exceptions.StatusOrdemServicoInvalidoException;
import br.com.fiap.oficina.interfaces.exceptions.VeiculoNaoEncontradoException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Service
public class OrdemServicoService {

    private static final Comparator<OrdemServico> ORDENACAO_LISTAGEM =
            Comparator.comparingInt((OrdemServico os) -> os.getStatus().getPrioridadeListagem())
                    .thenComparing(OrdemServico::getDataAbertura,
                            Comparator.nullsLast(Comparator.naturalOrder()));

    private final OrdemServicoRepository ordemServicoRepository;
    private final VeiculoRepository veiculoRepository;
    private final ServicoRepository servicoRepository;
    private final PecaRepository pecaRepository;
    private final OrdemServicoServicoRepository ordemServicoServicoRepository;
    private final OrdemServicoPecaRepository ordemServicoPecaRepository;
    private final ApplicationEventPublisher eventPublisher;

    public OrdemServicoService(
            OrdemServicoRepository ordemServicoRepository,
            VeiculoRepository veiculoRepository,
            ServicoRepository servicoRepository,
            PecaRepository pecaRepository,
            OrdemServicoServicoRepository ordemServicoServicoRepository,
            OrdemServicoPecaRepository ordemServicoPecaRepository,
            ApplicationEventPublisher eventPublisher) {

        this.ordemServicoRepository = ordemServicoRepository;
        this.veiculoRepository = veiculoRepository;
        this.servicoRepository = servicoRepository;
        this.pecaRepository = pecaRepository;
        this.ordemServicoServicoRepository = ordemServicoServicoRepository;
        this.ordemServicoPecaRepository = ordemServicoPecaRepository;
        this.eventPublisher = eventPublisher;
    }

    public OrdemServicoResponseDTO criar(
            OrdemServicoRequestDTO request) {

        Veiculo veiculo = veiculoRepository
                .findById(request.veiculoId())
                .orElseThrow(() ->
                        new VeiculoNaoEncontradoException(
                                "Veículo não encontrado"
                        )
                );

        OrdemServico ordemServico = new OrdemServico();

        ordemServico.setVeiculo(veiculo);
        ordemServico.setDescricaoProblema(request.descricaoProblema());
        ordemServico.setStatus(StatusOrdemServico.RECEBIDA);
        ordemServico.setValorTotal(BigDecimal.ZERO);
        ordemServico.setDataAbertura(LocalDateTime.now());

        OrdemServico salva =
                ordemServicoRepository.save(ordemServico);

        eventPublisher.publishEvent(OrdemServicoStatusAlteradoEvent.de(salva));

        return converterParaResponse(salva);
    }

    /**
     * Lista as ordens de servico ativas, ordenadas por prioridade de status
     * (Em Execucao > Aprovada > Aguardando Aprovacao > Diagnostico > Recebida)
     * e, dentro do mesmo status, das mais antigas para as mais recentes.
     * OS finalizadas, entregues ou canceladas nao sao exibidas
     * (exclusao logica: os registros permanecem no banco).
     */
    public List<OrdemServicoResponseDTO> listarTodas() {

        List<StatusOrdemServico> statusExibidos = Arrays.stream(StatusOrdemServico.values())
                .filter(StatusOrdemServico::isExibidoNaListagem)
                .toList();

        return ordemServicoRepository.findByStatusIn(statusExibidos)
                .stream()
                .sorted(ORDENACAO_LISTAGEM)
                .map(this::converterParaResponse)
                .toList();
    }

    /**
     * Processa a decisao do cliente sobre o orcamento (recebida via webhook).
     * Aprovado: AGUARDANDO_APROVACAO -> APROVADA.
     * Recusado: AGUARDANDO_APROVACAO -> CANCELADA (pecas reservadas voltam ao estoque).
     * Idempotente: reenvio da mesma decisao nao altera a OS.
     */
    @Transactional
    public StatusOrdemServicoResponseDTO processarDecisaoOrcamento(
            Long ordemServicoId,
            boolean aprovado) {

        OrdemServico ordemServico =
                buscarEntidadePorId(ordemServicoId);

        StatusOrdemServico destino = aprovado
                ? StatusOrdemServico.APROVADA
                : StatusOrdemServico.CANCELADA;

        if (ordemServico.getStatus() == destino) {
            return consultarStatus(ordemServicoId);
        }

        if (ordemServico.getStatus() != StatusOrdemServico.AGUARDANDO_APROVACAO) {
            throw new StatusOrdemServicoInvalidoException(
                    "Orçamento só pode ser aprovado ou recusado quando a OS está "
                            + "AGUARDANDO_APROVACAO. Status atual: "
                            + ordemServico.getStatus()
            );
        }

        atualizarStatus(ordemServicoId, new AtualizarStatusOSDTO(destino));

        return consultarStatus(ordemServicoId);
    }

    /**
     * Consulta a situacao atual de uma Ordem de Servico.
     */
    public StatusOrdemServicoResponseDTO consultarStatus(Long id) {

        OrdemServico ordemServico =
                buscarEntidadePorId(id);

        return new StatusOrdemServicoResponseDTO(
                ordemServico.getId(),
                ordemServico.getStatus(),
                ordemServico.getStatus().getDescricao(),
                ordemServico.getDataAbertura(),
                ordemServico.getDataInicioExecucao(),
                ordemServico.getDataFinalizacao()
        );
    }

    public OrdemServicoResponseDTO buscarPorId(Long id) {

        OrdemServico ordemServico =
                buscarEntidadePorId(id);

        return converterParaResponse(ordemServico);
    }

    public List<OrdemServicoResponseDTO> buscarPorVeiculo(
            Long veiculoId) {

        if (!veiculoRepository.existsById(veiculoId)) {
            throw new VeiculoNaoEncontradoException(
                    "Veículo não encontrado"
            );
        }

        return ordemServicoRepository
                .findByVeiculoId(veiculoId)
                .stream()
                .map(this::converterParaResponse)
                .toList();
    }

    @Transactional
    public OrdemServicoResponseDTO adicionarServico(
            Long ordemServicoId,
            AdicionarServicoOSDTO request) {

        OrdemServico ordemServico =
                buscarEntidadePorId(ordemServicoId);

        if (ordemServico.getStatus() != StatusOrdemServico.EM_DIAGNOSTICO) {
            throw new StatusOrdemServicoInvalidoException(
                    "Serviços e peças só podem ser adicionados enquanto a ordem de serviço estiver em diagnóstico"
            );
        }

        Servico servico = servicoRepository
                .findById(request.servicoId())
                .orElseThrow(() ->
                        new ServicoNaoEncontradoException(
                                "Serviço não encontrado"
                        )
                );

        OrdemServicoServico item =
                new OrdemServicoServico();

        item.setOrdemServico(ordemServico);
        item.setServico(servico);
        item.setPreco(servico.getPreco());

        ordemServicoServicoRepository.save(item);

        BigDecimal novoTotal =
                ordemServico.getValorTotal()
                        .add(servico.getPreco());

        ordemServico.setValorTotal(novoTotal);

        OrdemServico atualizada =
                ordemServicoRepository.save(ordemServico);

        return converterParaResponse(atualizada);
    }

    @Transactional
    public OrdemServicoResponseDTO adicionarPeca(
            Long ordemServicoId,
            AdicionarPecaOSDTO request) {

        OrdemServico ordemServico =
                buscarEntidadePorId(ordemServicoId);

        if (ordemServico.getStatus() != StatusOrdemServico.EM_DIAGNOSTICO) {
            throw new StatusOrdemServicoInvalidoException(
                    "Serviços e peças só podem ser adicionados enquanto a ordem de serviço estiver em diagnóstico"
            );
        }

        Peca peca = pecaRepository
                .findById(request.pecaId())
                .orElseThrow(() ->
                        new PecaNaoEncontradaException(
                                "Peça não encontrada"
                        )
                );

        if (peca.getQuantidadeEstoque() < request.quantidade()) {
            throw new EstoqueInsuficienteException(
                    "Estoque insuficiente para adicionar a peça à ordem de serviço"
            );
        }

        BigDecimal quantidade =
                BigDecimal.valueOf(request.quantidade());

        BigDecimal subtotal =
                peca.getPreco().multiply(quantidade);

        OrdemServicoPeca item =
                new OrdemServicoPeca();

        item.setOrdemServico(ordemServico);
        item.setPeca(peca);
        item.setQuantidade(request.quantidade());
        item.setPrecoUnitario(peca.getPreco());
        item.setSubtotal(subtotal);

        ordemServicoPecaRepository.save(item);

        int novoEstoque =
                peca.getQuantidadeEstoque()
                        - request.quantidade();

        peca.setQuantidadeEstoque(novoEstoque);

        pecaRepository.save(peca);

        BigDecimal novoTotal =
                ordemServico.getValorTotal()
                        .add(subtotal);

        ordemServico.setValorTotal(novoTotal);

        OrdemServico atualizada =
                ordemServicoRepository.save(ordemServico);

        return converterParaResponse(atualizada);
    }

    @Transactional
    public OrdemServicoResponseDTO atualizarStatus(
            Long id,
            AtualizarStatusOSDTO request) {

        OrdemServico ordemServico =
                buscarEntidadePorId(id);

        StatusOrdemServico atual =
                ordemServico.getStatus();

        StatusOrdemServico novo =
                request.status();

        validarTransicao(atual, novo);

        ordemServico.setStatus(novo);

        if (novo == StatusOrdemServico.EM_EXECUCAO && ordemServico.getDataInicioExecucao() == null) {
            ordemServico.setDataInicioExecucao(LocalDateTime.now());
        } else if (novo == StatusOrdemServico.FINALIZADA && ordemServico.getDataFinalizacao() == null) {
            ordemServico.setDataFinalizacao(LocalDateTime.now());
        } else if (novo == StatusOrdemServico.CANCELADA) {
            List<OrdemServicoPeca> itensPeca =
                    ordemServicoPecaRepository.findByOrdemServicoId(ordemServico.getId());
            for (OrdemServicoPeca item : itensPeca) {
                Peca peca = item.getPeca();
                peca.setQuantidadeEstoque(peca.getQuantidadeEstoque() + item.getQuantidade());
                pecaRepository.save(peca);
            }
        }

        OrdemServico atualizada =
                ordemServicoRepository.save(ordemServico);

        eventPublisher.publishEvent(OrdemServicoStatusAlteradoEvent.de(atualizada));

        return converterParaResponse(atualizada);
    }

    public TempoMedioExecucaoDTO obterTempoMedioExecucao() {

        List<OrdemServico> ordensFinalizadas = ordemServicoRepository.findAll()
                .stream()
                .filter(os -> (os.getStatus() == StatusOrdemServico.FINALIZADA || os.getStatus() == StatusOrdemServico.ENTREGUE)
                        && os.getDataInicioExecucao() != null
                        && os.getDataFinalizacao() != null)
                .toList();

        if (ordensFinalizadas.isEmpty()) {
            return new TempoMedioExecucaoDTO(0, 0.0, "0 minutos");
        }

        long totalMinutos = 0;
        for (OrdemServico os : ordensFinalizadas) {
            long minutos = Duration.between(os.getDataInicioExecucao(), os.getDataFinalizacao()).toMinutes();
            totalMinutos += Math.max(0, minutos);
        }

        double mediaMinutos = (double) totalMinutos / ordensFinalizadas.size();
        long horas = (long) mediaMinutos / 60;
        long minsRestantes = Math.round(mediaMinutos % 60);

        String formatado = horas > 0
                ? String.format("%d hora(s) e %d minuto(s)", horas, minsRestantes)
                : String.format("%d minuto(s)", minsRestantes);

        return new TempoMedioExecucaoDTO(ordensFinalizadas.size(), mediaMinutos, formatado);
    }

    private void validarTransicao(
            StatusOrdemServico atual,
            StatusOrdemServico novo) {

        boolean valida = switch (atual) {

            case RECEBIDA ->
                    novo == StatusOrdemServico.EM_DIAGNOSTICO
                            || novo == StatusOrdemServico.CANCELADA;

            case EM_DIAGNOSTICO ->
                    novo == StatusOrdemServico.AGUARDANDO_APROVACAO
                            || novo == StatusOrdemServico.CANCELADA;

            case AGUARDANDO_APROVACAO ->
                    novo == StatusOrdemServico.APROVADA
                            || novo == StatusOrdemServico.CANCELADA;

            case APROVADA ->
                    novo == StatusOrdemServico.EM_EXECUCAO
                            || novo == StatusOrdemServico.CANCELADA;

            case EM_EXECUCAO ->
                    novo == StatusOrdemServico.FINALIZADA;

            case FINALIZADA ->
                    novo == StatusOrdemServico.ENTREGUE;

            case ENTREGUE, CANCELADA -> false;
        };

        if (!valida) {
            throw new StatusOrdemServicoInvalidoException(
                    "Transição de status inválida: "
                            + atual
                            + " -> "
                            + novo
            );
        }
    }

    public void excluir(Long id) {

        OrdemServico ordemServico =
                buscarEntidadePorId(id);

        ordemServicoRepository.delete(ordemServico);
    }

    private OrdemServico buscarEntidadePorId(Long id) {

        return ordemServicoRepository
                .findById(id)
                .orElseThrow(() ->
                        new OrdemServicoNaoEncontradaException(
                                "Ordem de serviço não encontrada"
                        )
                );
    }

    private OrdemServicoResponseDTO converterParaResponse(
            OrdemServico ordemServico) {

        List<OrdemServicoServicoResponseDTO> servicos =
                ordemServicoServicoRepository
                        .findByOrdemServicoId(ordemServico.getId())
                        .stream()
                        .map(item ->
                                new OrdemServicoServicoResponseDTO(
                                        item.getServico().getId(),
                                        item.getServico().getNome(),
                                        item.getPreco()
                                )
                        )
                        .toList();

        List<OrdemServicoPecaResponseDTO> pecas =
                ordemServicoPecaRepository
                        .findByOrdemServicoId(ordemServico.getId())
                        .stream()
                        .map(item ->
                                new OrdemServicoPecaResponseDTO(
                                        item.getPeca().getId(),
                                        item.getPeca().getNome(),
                                        item.getQuantidade(),
                                        item.getPrecoUnitario(),
                                        item.getSubtotal()
                                )
                        )
                        .toList();

        return new OrdemServicoResponseDTO(
                ordemServico.getId(),
                ordemServico.getVeiculo().getId(),
                ordemServico.getVeiculo().getPlaca(),
                ordemServico.getVeiculo().getCliente().getId(),
                ordemServico.getVeiculo().getCliente().getNome(),
                ordemServico.getDescricaoProblema(),
                ordemServico.getStatus(),
                servicos,
                pecas,
                ordemServico.getValorTotal(),
                ordemServico.getDataAbertura(),
                ordemServico.getDataInicioExecucao(),
                ordemServico.getDataFinalizacao()
        );
    }
}