package br.com.opensig.produto.client.visao.lista;

import java.util.Map.Entry;

import br.com.opensig.core.client.OpenSigCore;
import br.com.opensig.core.client.UtilClient;
import br.com.opensig.core.client.controlador.comando.FabricaComando;
import br.com.opensig.core.client.controlador.filtro.ECompara;
import br.com.opensig.core.client.controlador.filtro.FiltroObjeto;
import br.com.opensig.core.client.servico.CoreProxy;
import br.com.opensig.core.client.visao.Ponte;
import br.com.opensig.core.client.visao.abstrato.AListagem;
import br.com.opensig.core.client.visao.abstrato.IFormulario;
import br.com.opensig.core.shared.modelo.IFavorito;
import br.com.opensig.core.shared.modelo.sistema.SisFuncao;
import br.com.opensig.empresa.client.controlador.comando.ComandoFornecedor;
import br.com.opensig.empresa.shared.modelo.EmpEmpresa;
import br.com.opensig.produto.client.visao.form.FormularioProduto;
import br.com.opensig.produto.shared.modelo.ProdCofins;
import br.com.opensig.produto.shared.modelo.ProdEmbalagem;
import br.com.opensig.produto.shared.modelo.ProdIpi;
import br.com.opensig.produto.shared.modelo.ProdOrigem;
import br.com.opensig.produto.shared.modelo.ProdPis;
import br.com.opensig.produto.shared.modelo.ProdProduto;
import br.com.opensig.produto.shared.modelo.ProdTipo;
import br.com.opensig.produto.shared.modelo.ProdIcms;

import com.gwtext.client.data.ArrayReader;
import com.gwtext.client.data.BooleanFieldDef;
import com.gwtext.client.data.DateFieldDef;
import com.gwtext.client.data.FieldDef;
import com.gwtext.client.data.FloatFieldDef;
import com.gwtext.client.data.IntegerFieldDef;
import com.gwtext.client.data.RecordDef;
import com.gwtext.client.data.Store;
import com.gwtext.client.data.StringFieldDef;
import com.gwtext.client.widgets.grid.BaseColumnConfig;
import com.gwtext.client.widgets.grid.ColumnConfig;
import com.gwtext.client.widgets.grid.ColumnModel;
import com.gwtext.client.widgets.menu.Menu;
import com.gwtext.client.widgets.menu.MenuItem;
import com.gwtextux.client.widgets.grid.plugins.GridBooleanFilter;
import com.gwtextux.client.widgets.grid.plugins.GridFilter;
import com.gwtextux.client.widgets.grid.plugins.GridListFilter;

public class ListagemProduto extends AListagem<ProdProduto> {

	public ListagemProduto(IFormulario formulario) {
		super(formulario);
		inicializar();
	}

	public void inicializar() {
		// campos
		FieldDef[] fd = new FieldDef[] { new IntegerFieldDef("prodProdutoId"), new StringFieldDef("prodProdutoNcm"), new StringFieldDef("prodProdutoBarra"),
				new StringFieldDef("prodProdutoDescricao"), new StringFieldDef("prodProdutoReferencia"), new FloatFieldDef("prodProdutoCusto"), new FloatFieldDef("prodProdutoPreco"),
				new IntegerFieldDef("prodEmbalagem.prodEmbalagemId"), new StringFieldDef("prodEmbalagem.prodEmbalagemNome"), new IntegerFieldDef("prodProdutoVolume"),
				new FloatFieldDef("t1.prodEstoqueQuantidade"), new StringFieldDef("prodProdutoCategoria"), new IntegerFieldDef("empFornecedor.empFornecedorId"),
				new StringFieldDef("empFornecedor.empEntidade.empEntidadeNome1"), new IntegerFieldDef("empFabricante.empFornecedorId"),
				new StringFieldDef("empFabricante.empEntidade.empEntidadeNome1"), new IntegerFieldDef("prodIcms.prodIcmsId"), new StringFieldDef("prodIcms.prodIcmsNome"),
				new IntegerFieldDef("prodIpi.prodIpiId"), new StringFieldDef("prodIpi.prodIpiNome"), new IntegerFieldDef("prodPis.prodPisId"), new StringFieldDef("prodPis.prodPisNome"),
				new IntegerFieldDef("prodCofins.prodCofinsId"), new StringFieldDef("prodCofins.prodCofinsNome"), new IntegerFieldDef("prodTipo.prodTipoId"),
				new StringFieldDef("prodTipo.prodTipoDescricao"), new IntegerFieldDef("prodOrigem.prodOrigemId"), new StringFieldDef("prodOrigem.prodOrigemDescricao"),
				new DateFieldDef("prodProdutoCadastrado"), new DateFieldDef("prodProdutoAlterado"), new BooleanFieldDef("prodProdutoAtivo"), new StringFieldDef("prodProdutoObservacao") };
		campos = new RecordDef(fd);

		// colunas
		ColumnConfig ccId = new ColumnConfig(OpenSigCore.i18n.txtCod(), "prodProdutoId", 50, true);
		ColumnConfig ccNcm = new ColumnConfig(OpenSigCore.i18n.txtNcm(), "prodProdutoNcm", 100, true);
		ccNcm.setHidden(true);
		ColumnConfig ccBarra = new ColumnConfig(OpenSigCore.i18n.txtBarra(), "prodProdutoBarra", 100, true);
		ColumnConfig ccDescricao = new ColumnConfig(OpenSigCore.i18n.txtDescricao(), "prodProdutoDescricao", 250, true);
		ColumnConfig ccRef = new ColumnConfig(OpenSigCore.i18n.txtRef(), "prodProdutoReferencia", 75, true);
		ColumnConfig ccCusto = new ColumnConfig(OpenSigCore.i18n.txtCusto(), "prodProdutoCusto", 100, true, DINHEIRO);
		ccCusto.setHidden(true);
		ColumnConfig ccPreco = new ColumnConfig(OpenSigCore.i18n.txtPreco(), "prodProdutoPreco", 75, true, DINHEIRO);
		ColumnConfig ccVolume = new ColumnConfig(OpenSigCore.i18n.txtQtdCx(), "prodProdutoVolume", 50, true);
		ColumnConfig ccEstoque = new ColumnConfig(OpenSigCore.i18n.txtEstoque(), "t1.prodEstoqueQuantidade", 75, true, NUMERO);
		ColumnConfig ccCategoria = new ColumnConfig(OpenSigCore.i18n.txtCategoria(), "prodProdutoCategoria", 200, true);
		ColumnConfig ccCodForn = new ColumnConfig(OpenSigCore.i18n.txtCod() + " - " + OpenSigCore.i18n.txtFornecedor(), "empFornecedor.empFornecedorId", 100, true);
		ccCodForn.setHidden(true);
		ColumnConfig ccFornecedor = new ColumnConfig(OpenSigCore.i18n.txtFornecedor(), "empFornecedor.empEntidade.empEntidadeNome1", 200, true);
		ColumnConfig ccCodFabr = new ColumnConfig(OpenSigCore.i18n.txtCod() + " - " + OpenSigCore.i18n.txtFabricante(), "empFabricante.empFornecedorId", 100, true);
		ccCodFabr.setHidden(true);
		ColumnConfig ccFabricante = new ColumnConfig(OpenSigCore.i18n.txtFabricante(), "empFabricante.empEntidade.empEntidadeNome1", 200, true);
		ccFabricante.setHidden(true);
		ColumnConfig ccIcmsId = new ColumnConfig(OpenSigCore.i18n.txtCod() + " - " + OpenSigCore.i18n.txtIcms(), "prodIcms.prodIcmsId", 100, true);
		ccIcmsId.setHidden(true);
		ColumnConfig ccIcms = new ColumnConfig(OpenSigCore.i18n.txtIcms(), "prodIcms.prodIcmsNome", 100, true);
		ccIcms.setHidden(true);
		ColumnConfig ccIpiId = new ColumnConfig(OpenSigCore.i18n.txtCod() + " - " + OpenSigCore.i18n.txtIpi(), "prodIpi.prodIpiId", 100, true);
		ccIpiId.setHidden(true);
		ColumnConfig ccIpi = new ColumnConfig(OpenSigCore.i18n.txtIpi(), "prodIpi.prodIpiNome", 100, true);
		ccIpi.setHidden(true);
		ColumnConfig ccPisId = new ColumnConfig(OpenSigCore.i18n.txtCod() + " - " + OpenSigCore.i18n.txtPis(), "prodPis.prodPisId", 100, true);
		ccPisId.setHidden(true);
		ColumnConfig ccPis = new ColumnConfig(OpenSigCore.i18n.txtPis(), "prodPis.prodPisNome", 100, true);
		ccPis.setHidden(true);
		ColumnConfig ccCofinsId = new ColumnConfig(OpenSigCore.i18n.txtCod() + " - " + OpenSigCore.i18n.txtCofins(), "prodCofins.prodCofinsId", 100, true);
		ccCofinsId.setHidden(true);
		ColumnConfig ccCofins = new ColumnConfig(OpenSigCore.i18n.txtCofins(), "prodCofins.prodCofinsNome", 100, true);
		ccCofins.setHidden(true);
		ColumnConfig ccTipoId = new ColumnConfig(OpenSigCore.i18n.txtCod() + " - " + OpenSigCore.i18n.txtTipo(), "prodTipo.prodTipoId", 100, true);
		ccTipoId.setHidden(true);
		ColumnConfig ccTipoDesc = new ColumnConfig(OpenSigCore.i18n.txtTipo(), "prodTipo.prodTipoDescricao", 100, true);
		ccTipoDesc.setHidden(true);
		ColumnConfig ccOrigemId = new ColumnConfig(OpenSigCore.i18n.txtCod() + " - " + OpenSigCore.i18n.txtOrigem(), "prodOrigem.prodOrigemId", 100, true);
		ccOrigemId.setHidden(true);
		ColumnConfig ccOrigem = new ColumnConfig(OpenSigCore.i18n.txtOrigem(), "prodOrigem.prodOrigemDescricao", 100, true);
		ccOrigem.setHidden(true);
		ColumnConfig ccEmbalagemId = new ColumnConfig(OpenSigCore.i18n.txtCod() + " - " + OpenSigCore.i18n.txtEmbalagem(), "prodEmbalagem.prodEmbalagemId", 100, true);
		ccEmbalagemId.setHidden(true);
		ColumnConfig ccEmbalagem = new ColumnConfig(OpenSigCore.i18n.txtEmbalagem(), "prodEmbalagem.prodEmbalagemNome", 75, true);
		ColumnConfig ccCadastro = new ColumnConfig(OpenSigCore.i18n.txtCadastrado(), "prodProdutoCadastrado", 120, true, DATAHORA);
		ColumnConfig ccAlterado = new ColumnConfig(OpenSigCore.i18n.txtAlterado(), "prodProdutoAlterado", 120, true, DATAHORA);
		ColumnConfig ccAtivo = new ColumnConfig(OpenSigCore.i18n.txtAtivo(), "prodProdutoAtivo", 50, true, BOLEANO);
		ColumnConfig ccObservacao = new ColumnConfig(OpenSigCore.i18n.txtObservacao(), "prodProdutoObservacao", 200, true);

		if (form instanceof FormularioProduto) {
			BaseColumnConfig[] bcc = new BaseColumnConfig[] { ccId, ccNcm, ccBarra, ccDescricao, ccRef, ccCusto, ccPreco, ccEmbalagemId, ccEmbalagem, ccVolume, ccEstoque, ccCategoria, ccCodForn,
					ccFornecedor, ccCodFabr, ccFabricante, ccIcmsId, ccIcms, ccIpiId, ccIpi, ccPisId, ccPis, ccCofinsId, ccCofins, ccTipoId, ccTipoDesc, ccOrigemId, ccOrigem, ccCadastro,
					ccAlterado, ccAtivo, ccObservacao };
			modelos = new ColumnModel(bcc);
		} else {
			BaseColumnConfig[] bcc = new BaseColumnConfig[] { ccBarra, ccDescricao, ccRef, ccEmbalagem, ccVolume, ccPreco, ccEstoque, ccFornecedor, ccObservacao };
			modelos = new ColumnModel(bcc);
			barraTarefa = false;
			agrupar = false;
		}

		filtroPadrao = new FiltroObjeto("empEmpresa", ECompara.IGUAL, new EmpEmpresa(Ponte.getLogin().getEmpresaId()));
		filtroPadrao.setCampoPrefixo("t1.");
		super.inicializar();
	}

	public void setGridFiltro() {
		super.setGridFiltro();
		for (Entry<String, GridFilter> entry : filtros.entrySet()) {
			if (entry.getKey().equals("prodProdutoAtivo")) {
				((GridBooleanFilter) entry.getValue()).setValue(true);
			} else if (entry.getKey().equals("prodIcms.prodIcmsNome")) {
				// icms
				FieldDef[] fdIcms = new FieldDef[] { new IntegerFieldDef("prodIcmsId"), new StringFieldDef("prodIcmsNome") };
				CoreProxy<ProdIcms> proxy = new CoreProxy<ProdIcms>(new ProdIcms());
				Store storeIcms = new Store(proxy, new ArrayReader(new RecordDef(fdIcms)), true);

				GridListFilter fIcms = new GridListFilter("prodIcms.prodIcmsNome", storeIcms);
				fIcms.setLabelField("prodIcmsNome");
				fIcms.setLabelValue("prodIcmsNome");
				fIcms.setLoadingText(OpenSigCore.i18n.txtAguarde());
				entry.setValue(fIcms);
			} else if (entry.getKey().equals("prodIpi.prodIpiNome")) {
				// ipi
				FieldDef[] fdIpi = new FieldDef[] { new IntegerFieldDef("prodIpiId"), new StringFieldDef("prodIpiNome") };
				CoreProxy<ProdIpi> proxy = new CoreProxy<ProdIpi>(new ProdIpi());
				Store storeIpi = new Store(proxy, new ArrayReader(new RecordDef(fdIpi)), true);

				GridListFilter fIpi = new GridListFilter("prodIpi.prodIpiNome", storeIpi);
				fIpi.setLabelField("prodIpiNome");
				fIpi.setLabelValue("prodIpiNome");
				fIpi.setLoadingText(OpenSigCore.i18n.txtAguarde());
				entry.setValue(fIpi);
			} else if (entry.getKey().equals("prodPis.prodPisNome")) {
				// ipi
				FieldDef[] fdPis = new FieldDef[] { new IntegerFieldDef("prodPisId"), new StringFieldDef("prodPisNome") };
				CoreProxy<ProdPis> proxy = new CoreProxy<ProdPis>(new ProdPis());
				Store storePis = new Store(proxy, new ArrayReader(new RecordDef(fdPis)), true);

				GridListFilter fPis = new GridListFilter("prodPis.prodPisNome", storePis);
				fPis.setLabelField("prodPisNome");
				fPis.setLabelValue("prodPisNome");
				fPis.setLoadingText(OpenSigCore.i18n.txtAguarde());
				entry.setValue(fPis);
			} else if (entry.getKey().equals("prodCofins.prodCofinsNome")) {
				// ipi
				FieldDef[] fdCofins = new FieldDef[] { new IntegerFieldDef("prodCofinsId"), new StringFieldDef("prodCofinsNome") };
				CoreProxy<ProdCofins> proxy = new CoreProxy<ProdCofins>(new ProdCofins());
				Store storeCofins = new Store(proxy, new ArrayReader(new RecordDef(fdCofins)), true);

				GridListFilter fCofins = new GridListFilter("prodCofins.prodCofinsNome", storeCofins);
				fCofins.setLabelField("prodCofinsNome");
				fCofins.setLabelValue("prodCofinsNome");
				fCofins.setLoadingText(OpenSigCore.i18n.txtAguarde());
				entry.setValue(fCofins);
			} else if (entry.getKey().equals("prodTipo.prodTipoDescricao")) {
				// tipo
				FieldDef[] fdTipo = new FieldDef[] { new IntegerFieldDef("prodTipoId"), new StringFieldDef("prodTipoValor"), new StringFieldDef("prodTipoDescricao") };
				CoreProxy<ProdTipo> proxy = new CoreProxy<ProdTipo>(new ProdTipo());
				Store storeTipo = new Store(proxy, new ArrayReader(new RecordDef(fdTipo)), true);

				GridListFilter fTipo = new GridListFilter("prodTipo.prodTipoDescricao", storeTipo);
				fTipo.setLabelField("prodTipoDescricao");
				fTipo.setLabelValue("prodTipoDescricao");
				fTipo.setLoadingText(OpenSigCore.i18n.txtAguarde());
				entry.setValue(fTipo);
			} else if (entry.getKey().equals("prodOrigem.prodOrigemDescricao")) {
				// origem
				FieldDef[] fdOrigem = new FieldDef[] { new IntegerFieldDef("prodOrigemId"), new IntegerFieldDef("prodOrigemValor"), new StringFieldDef("prodOrigemDescricao") };
				CoreProxy<ProdOrigem> proxy = new CoreProxy<ProdOrigem>(new ProdOrigem());
				Store storeOrigem = new Store(proxy, new ArrayReader(new RecordDef(fdOrigem)), true);

				GridListFilter fOrigem = new GridListFilter("prodOrigem.prodOrigemDescricao", storeOrigem);
				fOrigem.setLabelField("prodOrigemDescricao");
				fOrigem.setLabelValue("prodOrigemDescricao");
				fOrigem.setLoadingText(OpenSigCore.i18n.txtAguarde());
				entry.setValue(fOrigem);
			} else if (entry.getKey().equals("prodEmbalagem.prodEmbalagemNome")) {
				// embalagem
				FieldDef[] fdEmbalagem = new FieldDef[] { new IntegerFieldDef("prodEmbalagemId"), new StringFieldDef("prodEmbalagemNome") };
				CoreProxy<ProdEmbalagem> proxy = new CoreProxy<ProdEmbalagem>(new ProdEmbalagem());
				Store storeEmbalagem = new Store(proxy, new ArrayReader(new RecordDef(fdEmbalagem)), true);

				GridListFilter fEmbalagem = new GridListFilter("prodEmbalagem.prodEmbalagemNome", storeEmbalagem);
				fEmbalagem.setLabelField("prodEmbalagemNome");
				fEmbalagem.setLabelValue("prodEmbalagemNome");
				fEmbalagem.setLoadingText(OpenSigCore.i18n.txtAguarde());
				entry.setValue(fEmbalagem);
			}
		}
	}

	public void setFavorito(IFavorito favorito) {
		filtros.get("prodProdutoAtivo").setActive(false, true);
		super.setFavorito(favorito);
	}

	@Override
	public void irPara() {
		Menu mnuContexto = new Menu();

		// fornecedor
		SisFuncao fornecedor = UtilClient.getFuncaoPermitida(ComandoFornecedor.class);
		MenuItem itemFornecedor = gerarFuncao(fornecedor, "empFornecedorId", "empFornecedor.empFornecedorId");
		if (itemFornecedor != null) {
			mnuContexto.addItem(itemFornecedor);
		}

		// compra produtos
		String strCompras = FabricaComando.getInstancia().getComandoCompleto("ComandoCompraProduto");
		SisFuncao compras = UtilClient.getFuncaoPermitida(strCompras);
		MenuItem itemCompras = gerarFuncao(compras, "prodProduto.prodProdutoId", "prodProdutoId");
		if (itemCompras != null) {
			mnuContexto.addItem(itemCompras);
		}

		// venda produtos
		String strVendas = FabricaComando.getInstancia().getComandoCompleto("ComandoVendaProduto");
		SisFuncao vendas = UtilClient.getFuncaoPermitida(strVendas);
		MenuItem itemVendas = gerarFuncao(vendas, "prodProduto.prodProdutoId", "prodProdutoId");
		if (itemVendas != null) {
			mnuContexto.addItem(itemVendas);
		}

		// venda ecf produtos
		String strEcfs = FabricaComando.getInstancia().getComandoCompleto("ComandoEcfVendaProduto");
		SisFuncao ecf = UtilClient.getFuncaoPermitida(strEcfs);
		MenuItem itemEcf = gerarFuncao(ecf, "prodProduto.prodProdutoId", "prodProdutoId");
		if (itemEcf != null) {
			mnuContexto.addItem(itemEcf);
		}

		// grade produtos
		String strGrades = FabricaComando.getInstancia().getComandoCompleto("ComandoGrade");
		SisFuncao grades = UtilClient.getFuncaoPermitida(strGrades);
		MenuItem itemGrades = gerarFuncao(grades, "prodGrade.prodProduto.prodProdutoId", "prodProdutoId");
		if (itemGrades != null) {
			mnuContexto.addItem(itemGrades);
		}

		if (mnuContexto.getItems().length > 0) {
			MenuItem mnuItem = getIrPara();
			mnuItem.setMenu(mnuContexto);

			getMenu().addSeparator();
			getMenu().addItem(mnuItem);
		}
	}
}
