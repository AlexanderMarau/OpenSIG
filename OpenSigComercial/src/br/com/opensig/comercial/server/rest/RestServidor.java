package br.com.opensig.comercial.server.rest;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;

import org.apache.log4j.Logger;

import br.com.opensig.comercial.server.MyIcms;
import br.com.opensig.comercial.shared.modelo.ComEcfDocumento;
import br.com.opensig.comercial.shared.modelo.ComEcfNota;
import br.com.opensig.comercial.shared.modelo.ComEcfNotaProduto;
import br.com.opensig.comercial.shared.modelo.ComEcfVenda;
import br.com.opensig.comercial.shared.modelo.ComEcfVendaProduto;
import br.com.opensig.comercial.shared.modelo.ComEcfZ;
import br.com.opensig.comercial.shared.modelo.ComEcfZTotais;
import br.com.opensig.comercial.shared.modelo.ComNatureza;
import br.com.opensig.comercial.shared.modelo.ComTroca;
import br.com.opensig.comercial.shared.modelo.ComTrocaProduto;
import br.com.opensig.comercial.shared.modelo.ComVenda;
import br.com.opensig.comercial.shared.modelo.ComVendaProduto;
import br.com.opensig.comercial.shared.rest.SisCliente;
import br.com.opensig.core.client.controlador.filtro.ECompara;
import br.com.opensig.core.client.controlador.filtro.EJuncao;
import br.com.opensig.core.client.controlador.filtro.FiltroBinario;
import br.com.opensig.core.client.controlador.filtro.FiltroData;
import br.com.opensig.core.client.controlador.filtro.FiltroNumero;
import br.com.opensig.core.client.controlador.filtro.FiltroObjeto;
import br.com.opensig.core.client.controlador.filtro.FiltroTexto;
import br.com.opensig.core.client.controlador.filtro.GrupoFiltro;
import br.com.opensig.core.client.controlador.filtro.IFiltro;
import br.com.opensig.core.client.controlador.parametro.ParametroData;
import br.com.opensig.core.client.controlador.parametro.ParametroFormula;
import br.com.opensig.core.client.controlador.parametro.ParametroObjeto;
import br.com.opensig.core.client.servico.CoreException;
import br.com.opensig.core.client.servico.OpenSigException;
import br.com.opensig.core.server.UtilServer;
import br.com.opensig.core.shared.modelo.EComando;
import br.com.opensig.core.shared.modelo.EDirecao;
import br.com.opensig.core.shared.modelo.Lista;
import br.com.opensig.core.shared.modelo.Sql;
import br.com.opensig.empresa.shared.modelo.EmpCliente;
import br.com.opensig.empresa.shared.modelo.EmpContato;
import br.com.opensig.empresa.shared.modelo.EmpContatoTipo;
import br.com.opensig.empresa.shared.modelo.EmpEndereco;
import br.com.opensig.empresa.shared.modelo.EmpEnderecoTipo;
import br.com.opensig.empresa.shared.modelo.EmpEntidade;
import br.com.opensig.empresa.shared.modelo.EmpMunicipio;
import br.com.opensig.financeiro.shared.modelo.FinForma;
import br.com.opensig.financeiro.shared.modelo.FinReceber;
import br.com.opensig.financeiro.shared.modelo.FinRecebimento;
import br.com.opensig.fiscal.shared.modelo.ENotaStatus;
import br.com.opensig.fiscal.shared.modelo.FisNotaSaida;
import br.com.opensig.fiscal.shared.modelo.FisNotaStatus;
import br.com.opensig.nfe.TNFe;
import br.com.opensig.nfe.TNFe.InfNFe.Det.Prod;
import br.com.opensig.permissao.shared.modelo.SisConfiguracao;
import br.com.opensig.produto.shared.modelo.ProdEmbalagem;
import br.com.opensig.produto.shared.modelo.ProdEstoque;
import br.com.opensig.produto.shared.modelo.ProdEstoqueGrade;
import br.com.opensig.produto.shared.modelo.ProdProduto;

/**
 * Classe que representa a comunicao do Cliente para o Servidor via Rest
 * 
 * @author Pedro H. Lira
 */
@Provider
@Path("/server")
public class RestServidor extends ARest {

	private Map<String, String> conf;

	/**
	 * Construtor padrao.
	 */
	public RestServidor() {
		super();
		log = Logger.getLogger(RestServidor.class);
	}

	@GET
	@Produces(MediaType.TEXT_HTML)
	@Override
	public String ajuda() throws RestException {
		return super.ajuda();
	}

	/**
	 * Metodo que cadastra na base do server as notas de consumidor emitidas
	 * pelos sistemas em modo client.
	 * 
	 * @param ecfNota
	 *            um objeto do tipo Nota.
	 * @throws RestException
	 *             em caso de nao conseguir acessar a informacao.
	 */
	@Path("/nota")
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	public void setNota(ComEcfNota ecfNota) throws RestException {
		autorizar();

		try {
			// valida se ja existe
			FiltroObjeto fo = new FiltroObjeto("empEmpresa", ECompara.IGUAL, ecf.getEmpEmpresa());
			FiltroTexto ft = new FiltroTexto("comEcfNotaSerie", ECompara.IGUAL, ecfNota.getComEcfNotaSerie());
			FiltroTexto ft1 = new FiltroTexto("comEcfNotaSubserie", ECompara.IGUAL, ecfNota.getComEcfNotaSubserie());
			FiltroNumero fn = new FiltroNumero("comEcfNotaNumero", ECompara.IGUAL, ecfNota.getComEcfNotaNumero());
			GrupoFiltro gf = new GrupoFiltro(EJuncao.E, new IFiltro[] { fo, ft, ft1, fn });
			ComEcfNota aux = (ComEcfNota) service.selecionar(ecfNota, gf, false);

			if (aux == null) {
				// identifica o cliente
				conf = getConfig();
				EmpCliente cliente = getCliente(ecfNota.getSisCliente());

				// transforma os descontos em porcentagens
				double desc = ecfNota.getComEcfNotaDesconto() > 0 ? ecfNota.getComEcfNotaDesconto() / ecfNota.getComEcfNotaBruto() * 100 : 0.00;
				FinReceber receber = null;
				if (!ecfNota.getComEcfNotaCancelada()) {
					// salva o receber
					receber = new FinReceber();
					receber.setEmpEmpresa(ecf.getEmpEmpresa());
					receber.setEmpEntidade(cliente.getEmpEntidade());
					receber.setFinReceberCadastro(ecfNota.getComEcfNotaData());
					receber.setFinReceberCategoria(conf.get("categoria.ecf"));
					receber.setFinReceberNfe(ecfNota.getComEcfNotaNumero());
					receber.setFinReceberValor(ecfNota.getComEcfNotaLiquido());
					receber.setFinReceberObservacao("NFC emitido pelo ECF.");
					receber = (FinReceber) service.salvar(receber);

					// salva o recebimento
					FinRecebimento recebimento = new FinRecebimento();
					recebimento.setFinReceber(receber);
					recebimento.setFinForma(new FinForma(1));
					recebimento.setFinConta(null);
					recebimento.setFinRecebimentoCadastro(ecfNota.getComEcfNotaData());
					recebimento.setFinRecebimentoConciliado(ecfNota.getComEcfNotaData());
					recebimento.setFinRecebimentoDocumento("Serie:" + ecfNota.getComEcfNotaSerie() + " Sub:" + ecfNota.getComEcfNotaSubserie() + " N:" + ecfNota.getComEcfNotaNumero());
					recebimento.setFinRecebimentoObservacao("NFC emitido pelo ECF.");
					recebimento.setFinRecebimentoParcela("01/01");
					recebimento.setFinRecebimentoRealizado(ecfNota.getComEcfNotaData());
					recebimento.setFinRecebimentoStatus("REALIZADO");
					recebimento.setFinRecebimentoValor(ecfNota.getComEcfNotaLiquido());
					recebimento.setFinRecebimentoVencimento(ecfNota.getComEcfNotaData());
					service.salvar(recebimento);
				}

				// salva a nota
				List<ComEcfNotaProduto> nps = ecfNota.getComEcfNotaProdutos();
				ecfNota.setId(0);
				ecfNota.setComEcfNotaDesconto(desc);
				ecfNota.setEmpEmpresa(ecf.getEmpEmpresa());
				ecfNota.setEmpCliente(cliente);
				ecfNota.setComEcfNotaProdutos(null);
				ecfNota.setFinReceber(receber);
				ecfNota = (ComEcfNota) service.salvar(ecfNota);

				// salva os produtos vendidos
				List<Sql> sqls = new ArrayList<Sql>();
				for (ComEcfNotaProduto np : nps) {
					np.setId(0);
					np.setComEcfNotaProdutoDesconto(desc);
					np.setComEcfNota(ecfNota);
					getEstoque(sqls, np.getComEcfNotaProdutoQuantidade(), np.getProdEmbalagem(), np.getProdProduto(), np.getComEcfNotaProdutoBarra());
					sqls.add(getAtualizaProduto(np.getProdProduto()));
					service.salvar(np);
				}

				if (!ecfNota.getComEcfNotaCancelada()) {
					// atualiza o estoque
					service.executar(sqls.toArray(new Sql[] {}));
				}
			}
		} catch (Exception ex) {
			log.error("Erro ao salvar a nota.", ex);
			throw new RestException(ex.getMessage());
		}
	}

	/**
	 * Metodo que cadastra na base do server as nfe emitidas pelos sistemas em
	 * modo client.
	 * 
	 * @param ecfNfe
	 *            um objeto do tipo NFe.
	 * @throws RestException
	 *             em caso de nao conseguir acessar a informacao.
	 */
	@Path("/nfe")
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	public void setNfe(FisNotaSaida ecfNfe) throws RestException {
		autorizar();
		try {
			FiltroTexto ft = new FiltroTexto("fisNotaSaidaChave", ECompara.IGUAL, ecfNfe.getFisNotaSaidaChave());
			FisNotaSaida aux = (FisNotaSaida) service.selecionar(ecfNfe, ft, false);

			if (aux == null) {
				// identifica o status
				conf = getConfig();
				ENotaStatus status = ENotaStatus.valueOf(ecfNfe.getEcfNotaEletronicaStatus());
				FisNotaStatus ns = new FisNotaStatus(status);

				// salva a nfe
				ecfNfe.setId(0);
				ecfNfe.setEmpEmpresa(ecf.getEmpEmpresa());
				ecfNfe.setFisNotaStatus(ns);
				ecfNfe.setFisNotaSaidaCadastro(ecfNfe.getFisNotaSaidaData());
				ecfNfe.setFisNotaSaidaErro("");
				ecfNfe = (FisNotaSaida) service.salvar(ecfNfe);

				if (status == ENotaStatus.AUTORIZADO || status == ENotaStatus.CANCELADO) {
					// faz o parse do xml para objeto
					String xml = ecfNfe.getFisNotaSaidaXml();
					int I = xml.indexOf("<infNFe");
					int F = xml.indexOf("</NFe>") + 6;
					xml = "<NFe xmlns=\"http://www.portalfiscal.inf.br/nfe\">" + xml.substring(I, F);
					TNFe nfe = UtilServer.xmlToObj(xml, "br.com.opensig.nfe");

					// identifica o cliente
					String doc = nfe.getInfNFe().getDest().getCPF();
					if (doc == null || doc.equals("")) {
						doc = nfe.getInfNFe().getDest().getCNPJ();
					}
					String nome = nfe.getInfNFe().getDest().getXNome();
					SisCliente sisCliente = new SisCliente();
					sisCliente.setSisClienteDoc(doc);
					sisCliente.setSisClienteNome(nome);
					EmpCliente cliente = getCliente(sisCliente);

					// salva o receber
					FinReceber receber = null;
					if (status == ENotaStatus.AUTORIZADO) {
						// salva o receber
						receber = new FinReceber();
						receber.setEmpEmpresa(ecf.getEmpEmpresa());
						receber.setEmpEntidade(cliente.getEmpEntidade());
						receber.setFinReceberCadastro(ecfNfe.getFisNotaSaidaCadastro());
						receber.setFinReceberCategoria(conf.get("categoria.ecf"));
						receber.setFinReceberNfe(ecfNfe.getFisNotaSaidaNumero());
						receber.setFinReceberValor(ecfNfe.getFisNotaSaidaValor());
						receber.setFinReceberObservacao("NFe emitido pelo ECF.");
						receber = (FinReceber) service.salvar(receber);

						// salva o recebimento
						FinRecebimento recebimento = new FinRecebimento();
						recebimento.setFinReceber(receber);
						recebimento.setFinForma(new FinForma(1));
						recebimento.setFinConta(null);
						recebimento.setFinRecebimentoCadastro(ecfNfe.getFisNotaSaidaCadastro());
						recebimento.setFinRecebimentoConciliado(ecfNfe.getFisNotaSaidaCadastro());
						recebimento.setFinRecebimentoDocumento("NFe: " + ecfNfe.getFisNotaSaidaNumero());
						recebimento.setFinRecebimentoObservacao("NFe emitido pelo ECF.");
						recebimento.setFinRecebimentoParcela("01/01");
						recebimento.setFinRecebimentoRealizado(ecfNfe.getFisNotaSaidaCadastro());
						recebimento.setFinRecebimentoStatus("REALIZADO");
						recebimento.setFinRecebimentoValor(ecfNfe.getFisNotaSaidaValor());
						recebimento.setFinRecebimentoVencimento(ecfNfe.getFisNotaSaidaCadastro());
						service.salvar(recebimento);
					}

					// identifica a natureza de venda
					FiltroNumero fn = new FiltroNumero("comNaturezaCfopTrib", ECompara.IGUAL, 5102);
					ComNatureza natureza = (ComNatureza) service.selecionar(new ComNatureza(), fn, false);

					// salva a venda
					ComVenda venda = new ComVenda();
					venda.setSisUsuario(venda.getSisUsuario());
					venda.setSisVendedor(venda.getSisUsuario());
					venda.setEmpCliente(cliente);
					venda.setEmpEmpresa(ecf.getEmpEmpresa());
					venda.setComNatureza(natureza);
					venda.setFinReceber(receber);
					venda.setFisNotaSaida(ecfNfe);
					venda.setComVendaValorBruto(ecfNfe.getFisNotaSaidaValor());
					venda.setComVendaValorLiquido(ecfNfe.getFisNotaSaidaValor());
					venda.setComVendaData(ecfNfe.getFisNotaSaidaCadastro());
					venda.setComVendaObservacao("Venda importada do ECF.");
					venda.setComVendaFechada(true);
					venda.setComVendaRecebida(receber != null);
					venda.setComVendaCancelada(status == ENotaStatus.CANCELADO);
					venda.setComVendaNfe(true);
					venda = (ComVenda) service.salvar(venda, false);

					// identifica os produtos da venda
					List<Sql> sqls = new ArrayList<Sql>();
					List<ComVendaProduto> cvp = new ArrayList<ComVendaProduto>();
					for (TNFe.InfNFe.Det det : nfe.getInfNFe().getDet()) {
						// achando o produto
						IFiltro filtro;
						if (det.getProd().getCEAN() == null || det.getProd().getCEAN().equals("")) {
							filtro = new FiltroNumero("prodProdutoId", ECompara.IGUAL, det.getProd().getCProd());
						} else {
							filtro = new FiltroTexto("prodProdutoBarra", ECompara.IGUAL, det.getProd().getCEAN());
						}
						ProdProduto pp = (ProdProduto) service.selecionar(new ProdProduto(), filtro, false);

						// achando a embalagem usada na venda
						FiltroTexto ft1 = new FiltroTexto("prodEmbalagemNome", ECompara.IGUAL, det.getProd().getUCom());
						ProdEmbalagem emb = (ProdEmbalagem) service.selecionar(new ProdEmbalagem(), ft1, false);

						// monta a atualizacao do estoque
						double qtd = Double.valueOf(det.getProd().getQCom());
						getEstoque(sqls, qtd, emb, pp, "");
						sqls.add(getAtualizaProduto(pp));

						MyIcms myicms = new MyIcms(det.getImposto().getICMS());
						// ipi
						String ipi = "";
						String pIpi = "";
						try {
							ipi = det.getImposto().getIPI().getIPITrib().getCST();
							pIpi = det.getImposto().getIPI().getIPITrib().getPIPI();
							if (pIpi == null) {
								pIpi = "0.00";
							}
						} catch (Exception e) {
							ipi = "99";
							pIpi = "0.00";
						}
						// pis
						String pis = "";
						String pPis = "";
						try {
							pis = det.getImposto().getPIS().getPISAliq().getCST();
							pPis = det.getImposto().getPIS().getPISAliq().getPPIS();
						} catch (Exception e) {
							pis = "49";
							pPis = "0.00";
						}
						// cofins
						String cofins = "";
						String pCofins = "";
						try {
							cofins = det.getImposto().getCOFINS().getCOFINSAliq().getCST();
							pCofins = det.getImposto().getCOFINS().getCOFINSAliq().getPCOFINS();
						} catch (Exception e) {
							cofins = "49";
							pCofins = "0.00";
						}

						// setando o produto da venda
						ComVendaProduto vp = new ComVendaProduto();
						Prod prod = det.getProd();
						vp.setProdProduto(pp);
						vp.setProdEmbalagem(pp.getProdEmbalagem());
						vp.setComVenda(venda);
						vp.setComVendaProdutoCfop(Integer.valueOf(prod.getCFOP()));
						vp.setComVendaProdutoIcmsCst(myicms.getCst());
						vp.setComVendaProdutoIcms(Double.valueOf(myicms.getAliquota()));
						vp.setComVendaProdutoIpiCst(ipi);
						vp.setComVendaProdutoIpi(Double.valueOf(pIpi));
						vp.setComVendaProdutoPisCst(pis);
						vp.setComVendaProdutoPis(Double.valueOf(pPis));
						vp.setComVendaProdutoCofinsCst(cofins);
						vp.setComVendaProdutoCofins(Double.valueOf(pCofins));
						vp.setComVendaProdutoQuantidade(Double.valueOf(prod.getQCom()));
						vp.setComVendaProdutoBruto(Double.valueOf(prod.getVUnCom()));
						vp.setComVendaProdutoDesconto(prod.getVDesc() == null ? 0.00 : Double.valueOf(prod.getVDesc()));
						vp.setComVendaProdutoLiquido(vp.getComVendaProdutoBruto() - vp.getComVendaProdutoDesconto());
						vp.setComVendaProdutoTotalBruto(vp.getComVendaProdutoQuantidade() * vp.getComVendaProdutoBruto());
						vp.setComVendaProdutoTotalLiquido(vp.getComVendaProdutoQuantidade() * vp.getComVendaProdutoLiquido());
						vp.setComVendaProdutoBarra(pp.getProdProdutoBarra());
						vp.setComVendaProdutoOrdem(Integer.valueOf(det.getNItem()));
						cvp.add(vp);
					}
					service.salvar(cvp);

					// remove do estoque
					if (status == ENotaStatus.AUTORIZADO) {
						service.executar(sqls.toArray(new Sql[] {}));
					}
				}
			}
		} catch (Exception ex) {
			log.error("Erro ao salvar a nfe.", ex);
			throw new RestException(ex.getMessage());
		}
	}

	/**
	 * Metodo que cadastra na base do server os clientesclieados pelos sistemas
	 * em modo client.
	 * 
	 * @param cliente
	 *            um objeto do tipo SisCliente.
	 * @throws RestException
	 *             em caso de nao conseguir acessar a informacao.
	 */
	@Path("/cliente")
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	public void setCliente(SisCliente cliente) throws RestException {
		autorizar();

		try {
			getCliente(cliente);
		} catch (Exception ex) {
			log.error("Erro ao salvar o cliente.", ex);
			throw new RestException(ex.getMessage());
		}
	}

	/**
	 * Metodo que cadastra na base do server as vendas, produtos vendidos,
	 * pagamentos emitidos pelos sistemas em modo client.
	 * 
	 * @param ecfVenda
	 *            um objeto do tipo ComEcfVenda.
	 * @throws RestException
	 *             em caso de nao conseguir acessar a informacao.
	 */
	@Path("/venda")
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	public void setVenda(ComEcfVenda ecfVenda) throws RestException {
		autorizar();

		try {
			// pega o config e um cliente padrao
			conf = getConfig();
			FiltroNumero fn2 = new FiltroNumero("empClienteId", ECompara.IGUAL, conf.get("cliente.padrao"));
			EmpCliente cliente = ecfVenda.getSisCliente() != null ? getCliente(ecfVenda.getSisCliente()) : (EmpCliente) service.selecionar(new EmpCliente(), fn2, false);

			// guarda as listas
			List<ComEcfVendaProduto> vProdutos = ecfVenda.getComEcfVendaProdutos();
			List<ComTroca> trocas = ecfVenda.getComTrocas();

			// transforma os acrescimos e descontos em porcentagens
			double acresP = ecfVenda.getComEcfVendaBruto() > 0 ? ecfVenda.getComEcfVendaAcrescimo() / ecfVenda.getComEcfVendaBruto() * 100 : 0.00;
			double descP = ecfVenda.getComEcfVendaBruto() > 0 ? ecfVenda.getComEcfVendaDesconto() / ecfVenda.getComEcfVendaBruto() * 100 : 0.00;

			// salva o receber da venda se nao for cancelada
			FinReceber receber = null;
			if (!ecfVenda.getComEcfVendaCancelada()) {
				// coloca dados de cartao na obs
				StringBuilder sb = new StringBuilder("CUPOM FISCAL:: ");
				for (FinReceber rec : ecfVenda.getEcfPagamentos()) {
					if (rec.getFinReceberNfe() > 0) {
						sb.append("GNF: ").append(rec.getFinReceberNfe()).append(" - ").append("NSU: ").append(rec.getFinReceberCategoria()).append("\n");
					}
				}

				// salva o receber da venda
				receber = new FinReceber();
				receber.setEmpEmpresa(ecf.getEmpEmpresa());
				receber.setEmpEntidade(cliente.getEmpEntidade());
				receber.setFinReceberCadastro(ecfVenda.getComEcfVendaData());
				receber.setFinReceberCategoria(conf.get("categoria.ecf"));
				receber.setFinReceberNfe(ecfVenda.getComEcfVendaCcf());
				receber.setFinReceberValor(ecfVenda.getComEcfVendaLiquido());
				receber.setFinReceberObservacao(sb.toString());
				receber = (FinReceber) service.salvar(receber);

				// salva os recebimentos
				for (FinReceber rec : ecfVenda.getEcfPagamentos()) {
					int par = 0;
					for (FinRecebimento recebimento : rec.getFinRecebimentos()) {
						recebimento.setFinRecebimentoId(0);
						recebimento.setFinReceber(receber);
						recebimento.setFinForma(rec.getFinForma());
						recebimento.setFinConta(null);
						recebimento.setFinRecebimentoCadastro(rec.getFinReceberCadastro());
						recebimento.setFinRecebimentoRealizado(rec.getFinReceberCadastro());
						recebimento.setFinRecebimentoStatus("REALIZADO");
						if ("".equals(recebimento.getFinRecebimentoDocumento())) {
							recebimento.setFinRecebimentoDocumento("CCF: " + ecfVenda.getComEcfVendaCcf());
						}
						recebimento.setFinRecebimentoObservacao("CUPOM FISCAL");
						par++;
						recebimento.setFinRecebimentoParcela(UtilServer.formataNumero(par, 2, 0, false) + "/" + UtilServer.formataNumero(rec.getFinRecebimentos().size(), 2, 0, false));
						service.salvar(recebimento);
					}
				}
			} else {
				// filtro pra deletar a venda ja salva antes
				FiltroObjeto fo = new FiltroObjeto("comEcf", ECompara.IGUAL, ecf);
				FiltroNumero fn = new FiltroNumero("comEcfVendaCcf", ECompara.IGUAL, ecfVenda.getComEcfVendaCcf());
				GrupoFiltro gf = new GrupoFiltro(EJuncao.E, new IFiltro[] { fo, fn });
				ComEcfVenda vendaAux = (ComEcfVenda) service.selecionar(ecfVenda, gf, false);
				// caso ja exista deleta e remove do estoque
				if (vendaAux != null) {
					service.deletar(vendaAux);
					List<Sql> sqls = new ArrayList<Sql>();
					for (ComEcfVendaProduto vp : vProdutos) {
						if (!vp.getComEcfVendaProdutoCancelado()) {
							getEstoque(sqls, vp.getComEcfVendaProdutoQuantidade() * -1, vp.getProdEmbalagem(), vp.getProdProduto(), vp.getComEcfVendaProdutoBarra());
						}
					}
					service.executar(sqls.toArray(new Sql[] {}));
				}
			}

			// salva a venda
			ecfVenda.setId(0);
			ecfVenda.setComEcf(ecf);
			ecfVenda.setComEcfZ(null);
			ecfVenda.setEmpCliente(cliente);
			ecfVenda.setFinReceber(receber);
			ecfVenda.setComEcfVendaProdutos(null);
			ecfVenda.setComTrocas(null);
			ecfVenda.setComEcfVendaAcrescimo(acresP);
			ecfVenda.setComEcfVendaDesconto(descP);
			ecfVenda.setComEcfVendaProdutos(null);
			ecfVenda.setComEcfVendaFechada(true);
			if (ecfVenda.getComEcfVendaCancelada() == false) {
				ecfVenda.setComEcfVendaCancelada(vProdutos.isEmpty());
			}
			ecfVenda = (ComEcfVenda) service.salvar(ecfVenda);

			// salva os produtos
			if (vProdutos != null) {
				List<Sql> sqls = new ArrayList<Sql>();
				for (ComEcfVendaProduto vp : vProdutos) {
					double acresV = vp.getComEcfVendaProdutoBruto() * acresP / 100;
					double descV = vp.getComEcfVendaProdutoBruto() * descP / 100;

					vp.setId(0);
					vp.setComEcfVenda(ecfVenda);
					vp.setComEcfVendaProdutoAcrescimo(acresP);
					vp.setComEcfVendaProdutoDesconto(descP);
					vp.setComEcfVendaProdutoLiquido(vp.getComEcfVendaProdutoBruto() - descV + acresV);
					vp.setComEcfVendaProdutoTotal(vp.getComEcfVendaProdutoLiquido() * vp.getComEcfVendaProdutoQuantidade());
					if (!ecfVenda.getComEcfVendaCancelada() && !vp.getComEcfVendaProdutoCancelado()) {
						getEstoque(sqls, vp.getComEcfVendaProdutoQuantidade(), vp.getProdEmbalagem(), vp.getProdProduto(), vp.getComEcfVendaProdutoBarra());
						sqls.add(getAtualizaProduto(vp.getProdProduto()));
					}
				}
				service.salvar(vProdutos);

				// atualiza com as instrucoes SQL.
				if (!sqls.isEmpty()) {
					service.executar(sqls.toArray(new Sql[] {}));
				}
			}

			// salva as trocas
			if (trocas != null) {
				for (ComTroca troca : trocas) {
					// guarda os produtos da troca
					List<ComTrocaProduto> tps = troca.getComTrocaProdutos();

					// salva a troca
					troca.setComTrocaId(0);
					troca.setEmpEmpresa(ecf.getEmpEmpresa());
					troca.setComCompra(null);
					troca.setComEcfVenda(ecfVenda);
					troca.setComTrocaProdutos(null);
					troca = (ComTroca) service.salvar(troca);

					// salva os produtos
					for (ComTrocaProduto tp : tps) {
						tp.setId(0);
						tp.setComTroca(troca);
					}
					service.salvar(tps);
				}
			}
		} catch (Exception ex) {
			log.error("Erro ao salvar a venda.", ex);
			throw new RestException(ex.getMessage());
		}
	}

	/**
	 * Metodo que cadastra na base do server as reducoes Z, totais, vendas,
	 * produtos vendidos, pagamentos, documentos emitidos pelos sistemas em modo
	 * client.
	 * 
	 * @param ecfZ
	 *            um objeto do tipo ReducaoZ com a lista de documentos anexada.
	 * @throws RestException
	 *             em caso de nao conseguir acessar a informacao.
	 */
	@Path("/reducaoZ")
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	public void setReducaoZ(ComEcfZ ecfZ) throws RestException {
		autorizar();

		try {
			// valida se ja existe
			FiltroObjeto fo = new FiltroObjeto("comEcf", ECompara.IGUAL, ecf);
			FiltroNumero fn = new FiltroNumero("comEcfZCrz", ECompara.IGUAL, ecfZ.getComEcfZCrz());
			GrupoFiltro gf = new GrupoFiltro(EJuncao.E, new IFiltro[] { fo, fn });
			ComEcfZ aux = (ComEcfZ) service.selecionar(ecfZ, gf, false);

			if (aux == null) {
				// pega o config e um cliente padrao
				conf = getConfig();

				// guarda os totais e vendas e docs
				List<ComEcfZTotais> totais = ecfZ.getComEcfZTotais();
				List<ComEcfDocumento> docs = ecfZ.getComEcfDocumentos();

				// salva a reduzaoZ
				ecfZ.setId(0);
				ecfZ.setComEcf(ecf);
				ecfZ.setComEcfZTotais(null);
				ecfZ.setComEcfVendas(null);
				ecfZ.setComEcfDocumentos(null);
				ecfZ = (ComEcfZ) service.salvar(ecfZ);

				// salva os totais
				Map<String, ComEcfZTotais> zTotais = new HashMap<String, ComEcfZTotais>();
				for (ComEcfZTotais tot : totais) {
					if (zTotais.containsKey(tot.getComEcfZTotaisCodigo())) {
						tot.setComEcfZTotaisValor(tot.getComEcfZTotaisValor() + zTotais.remove(tot.getComEcfZTotaisCodigo()).getComEcfZTotaisValor());
					}
					tot.setId(0);
					tot.setComEcfZ(ecfZ);
					zTotais.put(tot.getComEcfZTotaisCodigo(), tot);
				}
				service.salvar(zTotais.values());

				// salva os documentos
				for (ComEcfDocumento doc : docs) {
					doc.setId(0);
					doc.setComEcf(ecf);
					doc.setComEcfZ(ecfZ);
				}
				service.salvar(docs);

				// atualiza as vendas do ecf
				Calendar cal = Calendar.getInstance();
				cal.setTime(ecfZ.getComEcfZMovimento());
				cal.add(Calendar.DAY_OF_MONTH, 1);
				Date fim = cal.getTime();

				FiltroData fd = new FiltroData("comEcfVendaData", ECompara.MAIOR_IGUAL, ecfZ.getComEcfZMovimento());
				FiltroData fd1 = new FiltroData("comEcfVendaData", ECompara.MENOR, fim);
				GrupoFiltro gf1 = new GrupoFiltro(EJuncao.E, new IFiltro[] { fo, fd, fd1 });
				ParametroObjeto po = new ParametroObjeto("comEcfZ", ecfZ);

				Sql sql = new Sql(new ComEcfVenda(), EComando.ATUALIZAR, gf1, po);
				service.executar(new Sql[] { sql });
			}
		} catch (Exception ex) {
			log.error("Erro ao salvar reduzao Z.", ex);
			throw new RestException(ex.getMessage());
		}
	}

	/**
	 * Metodo que valida o uso do sistema pela empresa.
	 * 
	 * @param aux
	 *            arquivo auxilar do ECF criptografado.
	 * @return o mesmo arquivo, mas com a data de vencimento atualziada.
	 * @throws RestException
	 *             dispara caso nao consiga.
	 */
	@Path("/validar")
	@PUT
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.TEXT_PLAIN)
	public String validar(String aux) throws RestException {
		try {
			// recuperando os valores
			Properties mapa = new Properties();
			String[] props = UtilServer.descriptar(aux).split("\n");
			for (String prop : props) {
				String[] chaveValor = prop.split("=");
				mapa.put(chaveValor[0], chaveValor[1]);
			}

			// recuperando o ultimo recebimento realizado pela empresa do ECF.
			FinRecebimento recebimento = new FinRecebimento();
			recebimento.setOrdemDirecao(EDirecao.DESC);
			FiltroTexto ft = new FiltroTexto("finReceber.empEntidade.empEntidadeDocumento1", ECompara.IGUAL, UtilServer.formataTexto(mapa.getProperty("cli.cnpj"), "##.###.###/####-##"));
			FiltroTexto ft1 = new FiltroTexto("finRecebimentoStatus", ECompara.DIFERENTE, "ABERTO");
			GrupoFiltro gf = new GrupoFiltro(EJuncao.E, new IFiltro[] { ft, ft1 });
			Lista<FinRecebimento> lista = service.selecionar(recebimento, 0, 1, gf, false);

			if (lista.getLista().size() == 1) {
				recebimento = lista.getLista().get(0);

				// setando a data de validade
				Calendar cal = Calendar.getInstance();
				cal.setTime(recebimento.getFinRecebimentoVencimento());
				cal.add(Calendar.MONTH, 1);
				cal.add(Calendar.DAY_OF_MONTH, 5);
				mapa.setProperty("out.validade", UtilServer.formataData(cal.getTime(), "dd/MM/yyyy"));

				// gerando o arquivo de retorno
				StringBuilder sb = new StringBuilder();
				for (String chave : mapa.stringPropertyNames()) {
					sb.append(chave).append("=").append(mapa.getProperty(chave)).append("\n");
				}
				return UtilServer.encriptar(sb.toString());
			} else {
				return aux;
			}
		} catch (Exception ex) {
			log.error("Erro ao validar o PDV.", ex);
			throw new RestException(ex.getMessage());
		}
	}

	/**
	 * Metodo que encontra o cliente dentro do sistema usando os dados do
	 * cliente enviado, como o CNPJ.
	 * 
	 * @param sisCliente
	 *            o obejto de cliente do OpenPDV.
	 * @return o cliente encontrado nesta base de dados do OpenSIG.
	 */
	private EmpCliente getCliente(SisCliente sisCliente) {
		try {
			// acha o cliente ou seta os dados
			String doc = sisCliente.getSisClienteDoc().replaceAll("\\D", "");
			String mask;
			String pessoa;

			if (doc.length() == 11) {
				mask = "###.###.###-##";
				pessoa = "FÍSICA";
			} else {
				mask = "##.###.###/####-##";
				pessoa = "JURÍDICA";
			}
			doc = UtilServer.formataTexto(doc, mask);
			FiltroTexto ft = new FiltroTexto("empEntidade.empEntidadeDocumento1", ECompara.IGUAL, doc);
			EmpCliente cli = (EmpCliente) service.selecionar(new EmpCliente(), ft, false);

			EmpEntidade ent = new EmpEntidade();
			EmpEndereco ende = new EmpEndereco();
			EmpContato cont1 = new EmpContato();
			EmpContato cont2 = new EmpContato();
			if (cli != null) {
				ent = cli.getEmpEntidade();
				ende = ent.getEmpEnderecos().get(0);
				cont1 = ent.getEmpContatos().get(0);
				if (ent.getEmpContatos().size() > 1) {
					cont2 = ent.getEmpContatos().get(1);
				}
			} else {
				cli = new EmpCliente();
			}

			// entidade
			ent.setEmpEntidadeNome1(sisCliente.getSisClienteNome());
			ent.setEmpEntidadeNome2("CONSUMIDOR");
			ent.setEmpEntidadeDocumento1(doc);
			ent.setEmpEntidadeDocumento2(sisCliente.getSisClienteDoc1());
			ent.setEmpEntidadeDocumento2("ISENTO");
			ent.setEmpEntidadeDocumento3("ISENTO");
			ent.setEmpEntidadePessoa(pessoa);
			ent.setEmpEntidadeAtivo(true);
			ent.setEmpEntidadeObservacao(sisCliente.getSisClienteObservacao());
			ent.setEmpEntidadeData(new Date());
			ent = (EmpEntidade) service.salvar(ent);
			// endereco
			ende.setEmpEntidade(ent);
			ende.setEmpEnderecoTipo(new EmpEnderecoTipo(Integer.valueOf(conf.get("nfe.tipoenderes"))));
			ende.setEmpMunicipio(new EmpMunicipio(sisCliente.getSisMunicipio().getSisMunicipioId()));
			ende.setEmpEnderecoLogradouro(sisCliente.getSisClienteEndereco().equals("") ? "NAO INFORMADO" : sisCliente.getSisClienteEndereco());
			ende.setEmpEnderecoNumero(sisCliente.getSisClienteNumero() + "");
			ende.setEmpEnderecoBairro(sisCliente.getSisClienteBairro().equals("") ? "NAO INFORMADO" : sisCliente.getSisClienteBairro());
			ende.setEmpEnderecoComplemento(sisCliente.getSisClienteComplemento());
			ende.setEmpEnderecoCep(sisCliente.getSisClienteCep().equals("") ? "00000-000" : sisCliente.getSisClienteCep());
			ende = (EmpEndereco) service.salvar(ende);
			// contato 1
			cont1.setEmpEntidade(ent);
			cont1.setEmpContatoTipo(new EmpContatoTipo(Integer.valueOf(conf.get("nfe.tipoconttel"))));
			cont1.setEmpContatoDescricao(sisCliente.getSisClienteTelefone().equals("") ? "(00) 0000-0000" : sisCliente.getSisClienteTelefone());
			cont1.setEmpContatoPessoa("");
			cont1 = (EmpContato) service.salvar(cont1);
			// contato 2
			cont2.setEmpEntidade(ent);
			cont2.setEmpContatoTipo(new EmpContatoTipo(Integer.valueOf(conf.get("nfe.tipocontemail"))));
			cont2.setEmpContatoDescricao(sisCliente.getSisClienteEmail().equals("") ? "N@O.TENHO" : sisCliente.getSisClienteEmail());
			cont2.setEmpContatoPessoa("");
			cont2 = (EmpContato) service.salvar(cont2);
			// cliente
			cli.setEmpEntidade(ent);
			cli = (EmpCliente) service.salvar(cli);
			return cli;
		} catch (Exception ex) {
			return new EmpCliente(1);
		}
	}

	/**
	 * Metodo que gera o SQL de atualizacao do estoque para as vendas recebidas.
	 * 
	 * @param sqls
	 *            uma lista de instrucoes.
	 * @param qtd
	 *            a quantidade de produtos vendidos.
	 * @param emb
	 *            o tipo de embalagem usada na venda.
	 * @param prod
	 *            o produto que foi vendido.
	 * @param barra
	 *            o codigo escolhido na hora da venda.
	 * @throws CoreException
	 *             dispara caso nao consiga gerar o sql de atualizacao.
	 */
	private void getEstoque(List<Sql> sqls, double qtd, ProdEmbalagem emb, ProdProduto prod, String barra) throws CoreException {
		// fatorando a quantida no estoque
		if (emb.getProdEmbalagemId() != prod.getProdEmbalagem().getProdEmbalagemId()) {
			qtd *= emb.getProdEmbalagemUnidade();
			qtd /= prod.getProdEmbalagem().getProdEmbalagemUnidade();
		}

		// atualiza o estoque
		ParametroFormula pf = new ParametroFormula("prodEstoqueQuantidade", -1 * qtd);
		FiltroObjeto fo1 = new FiltroObjeto("prodProduto", ECompara.IGUAL, prod);
		FiltroObjeto fo2 = new FiltroObjeto("empEmpresa", ECompara.IGUAL, ecf.getEmpEmpresa());
		GrupoFiltro gf = new GrupoFiltro(EJuncao.E, new IFiltro[] { fo1, fo2 });
		Sql sql = new Sql(new ProdEstoque(), EComando.ATUALIZAR, gf, pf);
		sqls.add(sql);

		// remove estoque da grade caso o produto tenha
		if (prod.getProdGrades() != null) {
			// formando os parametros e fitros
			ParametroFormula pf2 = new ParametroFormula("prodEstoqueGradeQuantidade", -1 * qtd);
			FiltroTexto ft = new FiltroTexto("prodGrade.prodGradeBarra", ECompara.IGUAL, barra);
			GrupoFiltro gf1 = new GrupoFiltro(EJuncao.E, new IFiltro[] { fo2, ft });
			// busca o item
			Sql sql1 = new Sql(new ProdEstoqueGrade(), EComando.ATUALIZAR, gf1, pf2);
			sqls.add(sql1);
		}
	}

	/**
	 * Metodo que gera o SQL de atualizacao do produto, para as vendas
	 * recebidas.
	 * 
	 * @param prod
	 *            o produto que foi vendido.
	 * @return uma instrucao de SQL no formato de objeto para ser executada.
	 * @throws CoreException
	 *             dispara caso nao consiga gerar o sql de atualizacao.
	 */
	private Sql getAtualizaProduto(ProdProduto prod) throws CoreException {
		// atualiza o produto
		ParametroData pd = new ParametroData("prodProdutoAlterado", new Date());
		FiltroNumero fn = new FiltroNumero("prodProdutoId", ECompara.IGUAL, prod.getProdProdutoId());
		return new Sql(new ProdProduto(), EComando.ATUALIZAR, fn, pd);
	}

	/**
	 * Metodo que recupera os dados de configuracoa da empresa + os padroes.
	 * 
	 * @return um mapa de chave/valor com as configuracoes completas.
	 * @throws OpenSigException
	 *             dispara caso nao consiga recuperar o config.
	 */
	private Map<String, String> getConfig() throws CoreException {
		// adicionando as configuracoes
		FiltroBinario fb = new FiltroBinario("sisConfiguracaoAtivo", ECompara.IGUAL, 1);
		FiltroObjeto fo = new FiltroObjeto("empEmpresa", ECompara.IGUAL, ecf.getEmpEmpresa());
		GrupoFiltro gf = new GrupoFiltro(EJuncao.E, new IFiltro[] { fb, fo });
		Lista<SisConfiguracao> lista = service.selecionar(new SisConfiguracao(), 0, 0, gf, false);

		Map<String, String> mapa = new HashMap<String, String>();
		for (SisConfiguracao conf : lista.getLista()) {
			mapa.put(conf.getSisConfiguracaoChave().toLowerCase(), conf.getSisConfiguracaoValor());
		}

		// lidos dos arquivos
		mapa.putAll(UtilServer.getConf());
		return mapa;
	}
}
