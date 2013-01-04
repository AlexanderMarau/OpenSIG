package br.com.opensig.comercial.server.acao;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import br.com.opensig.comercial.client.servico.ComercialException;
import br.com.opensig.comercial.shared.modelo.ComConsumo;
import br.com.opensig.core.client.controlador.filtro.ECompara;
import br.com.opensig.core.client.controlador.filtro.FiltroNumero;
import br.com.opensig.core.client.padroes.Chain;
import br.com.opensig.core.client.servico.OpenSigException;
import br.com.opensig.core.server.Conexao;
import br.com.opensig.core.server.CoreServiceImpl;
import br.com.opensig.core.server.UtilServer;
import br.com.opensig.core.shared.modelo.Autenticacao;
import br.com.opensig.financeiro.shared.modelo.FinConta;
import br.com.opensig.financeiro.shared.modelo.FinPagamento;

public class ExcluirConsumo extends Chain {

	private CoreServiceImpl servico;
	private ComConsumo consumo;
	private Autenticacao auth;

	public ExcluirConsumo(Chain next, CoreServiceImpl servico, ComConsumo consumo, Autenticacao auth) throws OpenSigException {
		super(null);
		this.servico = servico;
		this.consumo = consumo;
		this.auth = auth;
		
		// atualiza frete
		DeletarConsumo delFrete = new DeletarConsumo(next);
		// atualiza os conta
		AtualizarConta atuConta = new AtualizarConta(delFrete);
		// seleciona os produtos
		this.next = atuConta;
	}

	public void execute() throws OpenSigException {
		FiltroNumero fn = new FiltroNumero("comConsumoId", ECompara.IGUAL, consumo.getId());
		consumo = (ComConsumo) servico.selecionar(consumo, fn, false);
		if (next != null) {
			next.execute();
		}
	}

	private class AtualizarConta extends Chain {

		public AtualizarConta(Chain next) throws OpenSigException {
			super(next);
		}

		@Override
		public void execute() throws OpenSigException {
			EntityManagerFactory emf = null;
			EntityManager em = null;

			try {
				// recupera uma instância do gerenciador de entidades
				FinConta conta = new FinConta();
				emf = Conexao.getInstancia(conta.getPu());
				em = emf.createEntityManager();
				em.getTransaction().begin();

				if (consumo.getFinPagar() != null) {
					conta = consumo.getFinPagar().getFinConta();
					double valPag = 0.00;
					for (FinPagamento pag : consumo.getFinPagar().getFinPagamentos()) {
						if (!pag.getFinPagamentoStatus().equalsIgnoreCase(auth.getConf().get("txtAberto"))) {
							valPag += pag.getFinPagamentoValor();
						}
					}

					if (valPag > 0) {
						conta.setFinContaSaldo(conta.getFinContaSaldo() + valPag);
						servico.salvar(em, conta);
					}
				}

				if (next != null) {
					next.execute();
				}
				em.getTransaction().commit();
			} catch (Exception ex) {
				if (em != null && em.getTransaction().isActive()) {
					em.getTransaction().rollback();
				}

				UtilServer.LOG.error("Erro ao atualiza a conta.", ex);
				throw new ComercialException(ex.getMessage());
			} finally {
				em.close();
				emf.close();
			}
		}
	}

	private class DeletarConsumo extends Chain {

		public DeletarConsumo(Chain next) throws OpenSigException {
			super(next);
		}
		
		@Override
		public void execute() throws OpenSigException {
			EntityManagerFactory emf = null;
			EntityManager em = null;

			try {
				// recupera uma instância do gerenciador de entidades
				emf = Conexao.getInstancia(consumo.getPu());
				em = emf.createEntityManager();
				em.getTransaction().begin();
				servico.deletar(em, consumo);
				
				if (next != null) {
					next.execute();
				}
				em.getTransaction().commit();
			} catch (Exception ex) {
				if (em != null && em.getTransaction().isActive()) {
					em.getTransaction().rollback();
				}

				throw new ComercialException(ex.getMessage());
			} finally {
				em.close();
				emf.close();
			}
		}
	}
}
