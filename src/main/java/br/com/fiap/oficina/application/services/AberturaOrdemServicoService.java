package br.com.fiap.oficina.application.services;

import br.com.fiap.oficina.domain.cliente.Cliente;
import br.com.fiap.oficina.domain.ordemservico.StatusOrdemServico;
import br.com.fiap.oficina.domain.veiculo.Veiculo;
import br.com.fiap.oficina.infrastructure.repositories.ClienteRepository;
import br.com.fiap.oficina.infrastructure.repositories.VeiculoRepository;
import br.com.fiap.oficina.interfaces.dtos.AberturaOrdemServicoRequestDTO;
import br.com.fiap.oficina.interfaces.dtos.AdicionarPecaOSDTO;
import br.com.fiap.oficina.interfaces.dtos.AdicionarServicoOSDTO;
import br.com.fiap.oficina.interfaces.dtos.AtualizarStatusOSDTO;
import br.com.fiap.oficina.interfaces.dtos.ClienteRequestDTO;
import br.com.fiap.oficina.interfaces.dtos.OrdemServicoRequestDTO;
import br.com.fiap.oficina.interfaces.dtos.OrdemServicoResponseDTO;
import br.com.fiap.oficina.interfaces.dtos.VeiculoAberturaDTO;
import br.com.fiap.oficina.interfaces.exceptions.VeiculoDuplicadoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: abertura completa de Ordem de Servico.
 *
 * Recebe cliente, veiculo, servicos e pecas em uma unica requisicao:
 * - Cliente e reaproveitado pelo CPF/CNPJ (ou criado).
 * - Veiculo e reaproveitado pela placa (ou criado); se a placa pertencer
 *   a outro cliente, a abertura e recusada (409).
 * - Com servicos/pecas informados, a OS passa por EM_DIAGNOSTICO (itens
 *   adicionados, estoque reservado) e fica AGUARDANDO_APROVACAO do orcamento.
 * - Sem itens, a OS permanece RECEBIDA.
 * Tudo em uma unica transacao: qualquer erro desfaz a abertura inteira.
 */
@Service
public class AberturaOrdemServicoService {

    private final ClienteRepository clienteRepository;
    private final VeiculoRepository veiculoRepository;
    private final OrdemServicoService ordemServicoService;

    public AberturaOrdemServicoService(
            ClienteRepository clienteRepository,
            VeiculoRepository veiculoRepository,
            OrdemServicoService ordemServicoService) {

        this.clienteRepository = clienteRepository;
        this.veiculoRepository = veiculoRepository;
        this.ordemServicoService = ordemServicoService;
    }

    @Transactional
    public OrdemServicoResponseDTO abrir(AberturaOrdemServicoRequestDTO request) {

        Cliente cliente = obterOuCriarCliente(request.cliente());
        Veiculo veiculo = obterOuCriarVeiculo(request.veiculo(), cliente);

        OrdemServicoResponseDTO criada = ordemServicoService.criar(
                new OrdemServicoRequestDTO(veiculo.getId(), request.descricaoProblema())
        );
        Long ordemServicoId = criada.id();

        boolean possuiItens = !request.servicos().isEmpty() || !request.pecas().isEmpty();

        if (possuiItens) {
            ordemServicoService.atualizarStatus(ordemServicoId,
                    new AtualizarStatusOSDTO(StatusOrdemServico.EM_DIAGNOSTICO));

            for (AdicionarServicoOSDTO servico : request.servicos()) {
                ordemServicoService.adicionarServico(ordemServicoId, servico);
            }

            for (AdicionarPecaOSDTO peca : request.pecas()) {
                ordemServicoService.adicionarPeca(ordemServicoId, peca);
            }

            ordemServicoService.atualizarStatus(ordemServicoId,
                    new AtualizarStatusOSDTO(StatusOrdemServico.AGUARDANDO_APROVACAO));
        }

        return ordemServicoService.buscarPorId(ordemServicoId);
    }

    private Cliente obterOuCriarCliente(ClienteRequestDTO dados) {

        return clienteRepository.findByCpfCnpj(dados.cpfCnpj())
                .orElseGet(() -> {
                    Cliente novo = new Cliente();
                    novo.setNome(dados.nome());
                    novo.setCpfCnpj(dados.cpfCnpj());
                    novo.setEmail(dados.email());
                    novo.setTelefone(dados.telefone());
                    return clienteRepository.save(novo);
                });
    }

    private Veiculo obterOuCriarVeiculo(VeiculoAberturaDTO dados, Cliente cliente) {

        String placa = dados.placa().trim().toUpperCase();

        return veiculoRepository.findByPlaca(placa)
                .map(existente -> {
                    if (!existente.getCliente().getId().equals(cliente.getId())) {
                        throw new VeiculoDuplicadoException(
                                "Veículo já cadastrado para outro cliente"
                        );
                    }
                    return existente;
                })
                .orElseGet(() -> {
                    Veiculo novo = new Veiculo();
                    novo.setPlaca(placa);
                    novo.setMarca(dados.marca());
                    novo.setModelo(dados.modelo());
                    novo.setAno(dados.ano());
                    novo.setCliente(cliente);
                    return veiculoRepository.save(novo);
                });
    }
}
