package br.com.opensig.poker.client.visao.form;

import java.util.Date;
import java.util.Map;

import br.com.opensig.core.client.OpenSigCore;
import br.com.opensig.core.client.controlador.comando.AComando;
import br.com.opensig.core.client.controlador.comando.IComando;
import br.com.opensig.core.client.controlador.comando.form.ComandoSalvar;
import br.com.opensig.core.client.controlador.comando.form.ComandoSalvarFinal;
import br.com.opensig.core.client.controlador.filtro.ECompara;
import br.com.opensig.core.client.controlador.filtro.FiltroBinario;
import br.com.opensig.core.client.controlador.filtro.FiltroObjeto;
import br.com.opensig.core.client.controlador.parametro.GrupoParametro;
import br.com.opensig.core.client.controlador.parametro.IParametro;
import br.com.opensig.core.client.controlador.parametro.ParametroFormula;
import br.com.opensig.core.client.controlador.parametro.ParametroObjeto;
import br.com.opensig.core.client.servico.CoreProxy;
import br.com.opensig.core.client.visao.abstrato.AFormulario;
import br.com.opensig.core.shared.modelo.EComando;
import br.com.opensig.core.shared.modelo.Sql;
import br.com.opensig.core.shared.modelo.sistema.SisFuncao;
import br.com.opensig.poker.client.servico.PokerProxy;
import br.com.opensig.poker.shared.modelo.PokerCash;
import br.com.opensig.poker.shared.modelo.PokerCliente;
import br.com.opensig.poker.shared.modelo.PokerForma;
import br.com.opensig.poker.shared.modelo.PokerJackpot;
import br.com.opensig.poker.shared.modelo.PokerReceber;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.gwtext.client.data.ArrayReader;
import com.gwtext.client.data.BooleanFieldDef;
import com.gwtext.client.data.FieldDef;
import com.gwtext.client.data.IntegerFieldDef;
import com.gwtext.client.data.Record;
import com.gwtext.client.data.RecordDef;
import com.gwtext.client.data.Store;
import com.gwtext.client.data.StringFieldDef;
import com.gwtext.client.widgets.MessageBox;
import com.gwtext.client.widgets.form.ComboBox;
import com.gwtext.client.widgets.form.Field;
import com.gwtext.client.widgets.form.Hidden;
import com.gwtext.client.widgets.form.MultiFieldPanel;
import com.gwtext.client.widgets.form.NumberField;
import com.gwtext.client.widgets.form.event.ComboBoxListenerAdapter;

public class FormularioReceber extends AFormulario<PokerReceber> {

	private Hidden hdnCod;
	private Hidden hdnForma;
	private Hidden hdnCash;
	private ComboBox cmbForma;
	private ComboBox cmbCash;
	private ComboBox cmbCliente;
	private NumberField txtValor;
	private Date dtCadastro;
	private Date dtRealizado;
	private boolean recebido;
	private boolean jackpot;

	public FormularioReceber(SisFuncao funcao) {
		super(new PokerReceber(), funcao);
		inicializar();
	}

	public void inicializar() {
		hdnCod = new Hidden("pokerReceberId", "0");
		add(hdnCod);

		hdnForma = new Hidden("pokerForma.pokerFormaId", "0");
		add(hdnForma);

		hdnCash = new Hidden("pokerCash.pokerCashId", "0");
		add(hdnCash);

		txtValor = new NumberField(OpenSigCore.i18n.txtTotal(), "pokerReceberValor", 100, 0);
		txtValor.setAllowBlank(false);
		txtValor.setAllowNegative(false);
		txtValor.setDecimalPrecision(2);

		MultiFieldPanel linha1 = new MultiFieldPanel();
		linha1.setBorder(false);
		linha1.addToRow(getForma(), 150);
		linha1.addToRow(getCash(), 150);
		linha1.addToRow(getCliente(), 220);
		linha1.addToRow(txtValor, 120);
		add(linha1);

		super.inicializar();
	}

	public boolean setDados() {
		boolean retorno = true;
		classe.setPokerReceberId(Integer.valueOf(hdnCod.getValueAsString()));
		if (!hdnForma.getValueAsString().equals("0")) {
			PokerForma forma = new PokerForma(Integer.valueOf(hdnForma.getValueAsString()));
			classe.setPokerForma(forma);
		}
		if (!hdnCash.getValueAsString().equals("0")) {
			PokerCash cash = new PokerCash(Integer.valueOf(hdnCash.getValueAsString()));
			classe.setPokerCash(cash);
		}
		classe.setPokerReceberDescricao(cmbCliente.getRawValue());
		if (txtValor.getValue() != null) {
			classe.setPokerReceberValor(txtValor.getValue().intValue());
		}
		classe.setPokerReceberCadastrado(dtCadastro);
		if (recebido) {
			classe.setPokerReceberRealizado(new Date());
		}
		classe.setPokerReceberAtivo(recebido);
		return retorno;
	}

	public void limparDados() {
		getForm().reset();
	}

	public void mostrarDados() {
		Record rec = lista.getPanel().getSelectionModel().getSelected();
		if (rec != null) {
			getForm().loadRecord(rec);
			dtCadastro = rec.getAsDate("pokerReceberCadastrado");
			dtRealizado = rec.getAsDate("pokerReceberRealizado");
			recebido = rec.getAsBoolean("pokerReceberAtivo");
		} else {
			dtCadastro = new Date();
		}
		cmbForma.focus(true);

		if (duplicar) {
			hdnCod.setValue("0");
			hdnForma.setValue("0");
			cmbForma.setRawValue("");
			duplicar = false;
		}
	}

	public IComando AntesDaAcao(IComando comando) {
		if (comando instanceof ComandoSalvar && jackpot) {
			AComando<PokerReceber> salvarJackpot = new AComando<PokerReceber>(new ComandoSalvarFinal()) {
				public void execute(final Map contexto) {
					PokerReceber receber = (PokerReceber) contexto.get("resultado");
					FiltroObjeto fo = new FiltroObjeto("pokerForma", ECompara.IGUAL, classe.getPokerForma());
					ParametroFormula pf = new ParametroFormula("pokerJackpotTotal", receber.getPokerReceberValor());
					ParametroObjeto po = new ParametroObjeto("pokerReceber", receber);
					GrupoParametro gp = new GrupoParametro(new IParametro[] { pf, po });

					PokerProxy proxy = new PokerProxy();
					Sql sql = new Sql(new PokerJackpot(), EComando.ATUALIZAR, fo, gp);
					proxy.executar(new Sql[] { sql }, new AsyncCallback<Integer[]>() {
						public void onFailure(Throwable caught) {
							MessageBox.alert(OpenSigCore.i18n.txtSalvar(), OpenSigCore.i18n.errSalvar());
						}

						public void onSuccess(Integer[] result) {
							comando.execute(contexto);
						}
					});
				}
			};
			comando.setProximo(salvarJackpot);
		}
		return comando;
	}

	public void gerarListas() {
	}

	private ComboBox getForma() {
		FieldDef[] campos = new FieldDef[] { new IntegerFieldDef("pokerFormaId"), new StringFieldDef("pokerFormaNome"), new BooleanFieldDef("pokerFormaRealizado"),
				new BooleanFieldDef("pokerFormaJackpot") };
		CoreProxy<PokerForma> proxy = new CoreProxy<PokerForma>(new PokerForma());
		Store store = new Store(proxy, new ArrayReader(new RecordDef(campos)), false);

		cmbForma = new ComboBox(OpenSigCore.i18n.txtTipo(), "pokerForma.pokerFormaNome", 130);
		cmbForma.setAllowBlank(false);
		cmbForma.setStore(store);
		cmbForma.setTriggerAction(ComboBox.ALL);
		cmbForma.setMode(ComboBox.REMOTE);
		cmbForma.setMinChars(1);
		cmbForma.setDisplayField("pokerFormaNome");
		cmbForma.setValueField("pokerFormaId");
		cmbForma.setForceSelection(true);
		cmbForma.setEditable(false);
		cmbForma.setListWidth(130);
		cmbForma.addListener(new ComboBoxListenerAdapter() {
			public void onSelect(ComboBox comboBox, Record record, int index) {
				hdnForma.setValue(comboBox.getValue());
				jackpot = record.getAsBoolean("pokerFormaJackpot");
				if (record.getAsBoolean("pokerFormaRealizado")) {
					recebido = true;
					dtRealizado = new Date();
				} else {
					recebido = false;
					dtRealizado = null;
				}
			}

			public void onBlur(Field field) {
				if (cmbForma.getRawValue().equals("")) {
					hdnForma.setValue("0");
				}
			}
		});

		return cmbForma;
	}

	private ComboBox getCash() {
		FieldDef[] campos = new FieldDef[] { new IntegerFieldDef("pokerCashId"), new StringFieldDef("pokerCashMesa") };
		FiltroBinario fb = new FiltroBinario("pokerCashFechado", ECompara.IGUAL, 0);
		CoreProxy<PokerCash> proxy = new CoreProxy<PokerCash>(new PokerCash(), fb);
		Store store = new Store(proxy, new ArrayReader(new RecordDef(campos)), false);

		cmbCash = new ComboBox(OpenSigCore.i18n.txtCash(), "pokerCash.pokerCashMesa", 130);
		cmbCash.setAllowBlank(false);
		cmbCash.setStore(store);
		cmbCash.setTriggerAction(ComboBox.ALL);
		cmbCash.setMode(ComboBox.REMOTE);
		cmbCash.setDisplayField("pokerCashMesa");
		cmbCash.setValueField("pokerCashId");
		cmbCash.setTpl("<div class=\"x-combo-list-item\"><b>" + OpenSigCore.i18n.txtCod() + " {pokerCashId}</b> - <i>" + OpenSigCore.i18n.txtMesa() + " {pokerCashMesa}</i></div>");
		cmbCash.setForceSelection(true);
		cmbCash.setEditable(false);
		cmbCash.setListWidth(200);
		cmbCash.addListener(new ComboBoxListenerAdapter() {
			public void onSelect(ComboBox comboBox, Record record, int index) {
				hdnCash.setValue(comboBox.getValue());
			}

			public void onBlur(Field field) {
				if (cmbCash.getRawValue().equals("")) {
					hdnCash.setValue("0");
				}
			}
		});

		return cmbCash;
	}

	private ComboBox getCliente() {
		FieldDef[] campos = new FieldDef[] { new IntegerFieldDef("pokerClienteId"), new IntegerFieldDef("pokerClienteCodigo"), new StringFieldDef("pokerClienteNome") };
		FiltroBinario fb = new FiltroBinario("pokerClienteAtivo", ECompara.IGUAL, 1);
		CoreProxy<PokerCliente> proxy = new CoreProxy<PokerCliente>(new PokerCliente(), fb);
		Store store = new Store(proxy, new ArrayReader(new RecordDef(campos)));

		cmbCliente = new ComboBox(OpenSigCore.i18n.txtDescricao(), "pokerReceberDescricao", 180);
		cmbCliente.setAllowBlank(false);
		cmbCliente.setStore(store);
		cmbCliente.setTriggerAction(ComboBox.ALL);
		cmbCliente.setMode(ComboBox.REMOTE);
		cmbCliente.setDisplayField("pokerClienteNome");
		cmbCliente.setValueField("pokerClienteId");
		cmbCliente.setForceSelection(false);
		cmbCliente.setEditable(true);
		cmbCliente.setMinChars(1);
		cmbCliente.setTypeAhead(true);
		cmbCliente.setListWidth(200);

		return cmbCliente;
	}

	public Hidden getHdnCod() {
		return hdnCod;
	}

	public void setHdnCod(Hidden hdnCod) {
		this.hdnCod = hdnCod;
	}

	public Hidden getHdnForma() {
		return hdnForma;
	}

	public void setHdnForma(Hidden hdnForma) {
		this.hdnForma = hdnForma;
	}

	public Hidden getHdnCash() {
		return hdnCash;
	}

	public void setHdnCash(Hidden hdnCash) {
		this.hdnCash = hdnCash;
	}

	public ComboBox getCmbForma() {
		return cmbForma;
	}

	public void setCmbForma(ComboBox cmbForma) {
		this.cmbForma = cmbForma;
	}

	public ComboBox getCmbCash() {
		return cmbCash;
	}

	public void setCmbCash(ComboBox cmbCash) {
		this.cmbCash = cmbCash;
	}

	public NumberField getTxtValor() {
		return txtValor;
	}

	public void setTxtValor(NumberField txtValor) {
		this.txtValor = txtValor;
	}

	public Date getDtCadastro() {
		return dtCadastro;
	}

	public void setDtCadastro(Date dtCadastro) {
		this.dtCadastro = dtCadastro;
	}

	public Date getDtRealizado() {
		return dtRealizado;
	}

	public void setDtRealizado(Date dtRealizado) {
		this.dtRealizado = dtRealizado;
	}

	public boolean isRecebido() {
		return recebido;
	}

	public void setRecebido(boolean recebido) {
		this.recebido = recebido;
	}

	public boolean isJackpot() {
		return jackpot;
	}

	public void setJackpot(boolean jackpot) {
		this.jackpot = jackpot;
	}

	public ComboBox getCmbCliente() {
		return cmbCliente;
	}

	public void setCmbCliente(ComboBox cmbCliente) {
		this.cmbCliente = cmbCliente;
	}
}
