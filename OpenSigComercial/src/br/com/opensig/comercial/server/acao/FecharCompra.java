package br.com.opensig.comercial.server.acao;

import java.util.Date;

import javax.persistence.EntityManager;

import br.com.opensig.comercial.client.servico.ComercialException;
import br.com.opensig.comercial.server.ComercialServiceImpl;
import br.com.opensig.comercial.shared.modelo.ComCompra;
import br.com.opensig.comercial.shared.modelo.ComCompraProduto;
import br.com.opensig.core.client.controlador.filtro.ECompara;
import br.com.opensig.core.client.controlador.filtro.EJuncao;
import br.com.opensig.core.client.controlador.filtro.FiltroNumero;
import br.com.opensig.core.client.controlador.filtro.FiltroObjeto;
import br.com.opensig.core.client.controlador.filtro.GrupoFiltro;
import br.com.opensig.core.client.controlador.filtro.IFiltro;
import br.com.opensig.core.client.controlador.parametro.GrupoParametro;
import br.com.opensig.core.client.controlador.parametro.IParametro;
import br.com.opensig.core.client.controlador.parametro.ParametroBinario;
import br.com.opensig.core.client.controlador.parametro.ParametroData;
import br.com.opensig.core.client.controlador.parametro.ParametroFormula;
import br.com.opensig.core.client.controlador.parametro.ParametroNumero;
import br.com.opensig.core.client.padroes.Chain;
import br.com.opensig.core.client.servico.OpenSigException;
import br.com.opensig.core.server.Conexao;
import br.com.opensig.core.server.CoreServiceImpl;
import br.com.opensig.core.server.UtilServer;
import br.com.opensig.core.shared.modelo.Autenticacao;
import br.com.opensig.core.shared.modelo.EComando;
import br.com.opensig.core.shared.modelo.Sql;
import br.com.opensig.produto.shared.modelo.ProdEstoque;
import br.com.opensig.produto.shared.modelo.ProdEstoqueGrade;
import br.com.opensig.produto.shared.modelo.ProdGrade;
import br.com.opensig.produto.shared.modelo.ProdProduto;

public class FecharCompra extends Chain {

	private CoreServiceImpl servico;
	private ComercialServiceImpl impl;
	private ComCompra compra;

	public FecharCompra(Chain next, CoreServiceImpl servico, ComCompra compra, Autenticacao auth) throws OpenSigException {
		super(null);
		this.servico = servico;
		this.impl = new ComercialServiceImpl();

		// seleciona a compra
		FiltroNumero fn = new FiltroNumero("comCompraId", ECompara.IGUAL, compra.getId());
		this.compra = (ComCompra) servico.selecionar(compra, fn, false);
		
		// atualiza compra
		AtualizarCompra atuComp = new AtualizarCompra(next);
		// atualiza os produros
		AtualizarProduto atuProd = new AtualizarProduto(atuComp);
		// atauliza estoque
		AtualizarEstoque atuEst = new AtualizarEstoque(atuProd);
		// seleciona os produtos
		if (!auth.getConf().get("estoque.ativo").equalsIgnoreCase("ignorar")) {
			this.setNext(atuEst);
		} else {
			this.setNext(atuProd);
		}
	}

	@Override
	public void execute() throws OpenSigException {
		if (next != null) {
			next.execute();
		}
	}

	private class AtualizarEstoque extends Chain {

		public AtualizarEstoque(Chain next) throws OpenSigException {
			super(next);
		}

		@Override
		public void execute() throws OpenSigException {
			EntityManager em = null;

			try {
				// recupera uma instância do gerenciador de entidades
				FiltroObjeto fo1 = new FiltroObjeto("empEmpresa", ECompara.IGUAL, compra.getEmpEmpresa());
				em = Conexao.EMFS.get(new ProdEstoque().getPu()).createEntityManager();
				em.getTransaction().begin();

				for (ComCompraProduto cp : compra.getComCompraProdutos()) {
					// fatorando a quantida no estoque
					double qtd = cp.getComCompraProdutoQuantidade();
					if (cp.getProdEmbalagem().getProdEmbalagemId() != cp.getProdProduto().getProdEmbalagem().getProdEmbalagemId()) {
						qtd *= impl.getQtdEmbalagem(cp.getProdEmbalagem().getProdEmbalagemId());
						qtd /= impl.getQtdEmbalagem(cp.getProdProduto().getProdEmbalagem().getProdEmbalagemId());
					}
					// formando os parametros
					ParametroFormula pf = new ParametroFormula("prodEstoqueQuantidade", qtd);
					// formando o filtro
					FiltroObjeto fo2 = new FiltroObjeto("prodProduto", ECompara.IGUAL, cp.getProdProduto());
					GrupoFiltro gf = new GrupoFiltro(EJuncao.E, new IFiltro[] { fo1, fo2 });
					// formando o sql
					Sql sql = new Sql(new ProdEstoque(), EComando.ATUALIZAR, gf, pf);
					servico.executar(em, sql);
					
					// remove estoque da grade caso o produto tenha
					if (cp.getProdProduto().getProdGrades() != null) {
						for (ProdGrade grade : cp.getProdProduto().getProdGrades()) {
							if (grade.getProdGradeBarra().equals(cp.getComCompraProdutoBarra())) {
								// formando os parametros
								ParametroFormula pn2 = new ParametroFormula("prodEstoqueGradeQuantidade", qtd);
								// formando o filtro
								FiltroObjeto fo3 = new FiltroObjeto("prodGrade", ECompara.IGUAL, grade);
								GrupoFiltro gf1 = new GrupoFiltro(EJuncao.E, new IFiltro[] { fo1, fo3 });
								// formando o sql
								Sql sql1 = new Sql(new ProdEstoqueGrade(), EComando.ATUALIZAR, gf1, pn2);
								servico.executar(em, sql1);
								break;
							}
						}
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

				UtilServer.LOG.error("Erro ao atualizar estoque.", ex);
				throw new ComercialException(ex.getMessage());
			} finally {
				if (em != null) {
					em.close();
				}
			}
		}
	}

	private class AtualizarProduto extends Chain {

		public AtualizarProduto(Chain next) throws OpenSigException {
			super(next);
		}

		@Override
		public void execute() throws OpenSigException {
			EntityManager em = null;

			try {
				// recupera uma instância do gerenciador de entidades
				ProdProduto prod = new ProdProduto();
				em = Conexao.EMFS.get(prod.getPu()).createEntityManager();
				em.getTransaction().begin();

				for (ComCompraProduto cp : compra.getComCompraProdutos()) {
					// fatorando pela embalagem
					double custo = cp.getComCompraProdutoValor();
					double preco = cp.getComCompraProdutoPreco();
					if (cp.getProdEmbalagem().getProdEmbalagemId() != cp.getProdProduto().getProdEmbalagem().getProdEmbalagemId()) {
						custo /= impl.getQtdEmbalagem(cp.getProdEmbalagem().getProdEmbalagemId());
						custo *= impl.getQtdEmbalagem(cp.getProdProduto().getProdEmbalagem().getProdEmbalagemId());
						preco /= impl.getQtdEmbalagem(cp.getProdEmbalagem().getProdEmbalagemId());
						preco *= impl.getQtdEmbalagem(cp.getProdProduto().getProdEmbalagem().getProdEmbalagemId());
					}
					// formando os parametros
					ParametroNumero pn1 = new ParametroNumero("prodProdutoCusto", custo);
					ParametroNumero pn2 = new ParametroNumero("prodProdutoPreco", preco);
					ParametroData pd = new ParametroData("prodProdutoAlterado", new Date());
					GrupoParametro gp = new GrupoParametro(new IParametro[] { pn1, pn2, pd });
					// formando o filtro
					FiltroNumero fn = new FiltroNumero("prodProdutoId", ECompara.IGUAL, cp.getProdProduto().getId());
					// formando o sql
					Sql sql = new Sql(prod, EComando.ATUALIZAR, fn, gp);
					servico.executar(em, sql);
				}

				if (next != null) {
					next.execute();
				}
				em.getTransaction().commit();
			} catch (Exception ex) {
				if (em != null && em.getTransaction().isActive()) {
					em.getTransaction().rollback();
				}

				UtilServer.LOG.error("Erro ao atualizar produto.", ex);
				throw new ComercialException(ex.getMessage());
			} finally {
				if (em != null) {
					em.close();
				}
			}
		}

	}

	private class AtualizarCompra extends Chain {

		public AtualizarCompra(Chain next) throws OpenSigException {
			super(next);
		}

		@Override
		public void execute() throws OpenSigException {
			// atualiza o status para fechada
			FiltroNumero fn = new FiltroNumero("comCompraId", ECompara.IGUAL, compra.getId());
			ParametroBinario pb = new ParametroBinario("comCompraFechada", 1);
			Sql sql = new Sql(compra, EComando.ATUALIZAR, fn, pb);
			servico.executar(new Sql[] { sql });

			if (next != null) {
				next.execute();
			}
		}
	}
}
