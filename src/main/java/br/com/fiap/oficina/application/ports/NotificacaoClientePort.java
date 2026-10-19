package br.com.fiap.oficina.application.ports;

import br.com.fiap.oficina.domain.ordemservico.OrdemServicoStatusAlteradoEvent;

/**
 * Porta de saida (Arquitetura Hexagonal): notifica o cliente sobre a
 * situacao da sua Ordem de Servico. A implementacao (e-mail, SMS, push...)
 * fica na camada de infraestrutura.
 */
public interface NotificacaoClientePort {

    void notificarMudancaDeStatus(OrdemServicoStatusAlteradoEvent evento);
}
