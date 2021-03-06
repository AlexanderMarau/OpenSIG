package br.com.opensig.comercial.client.controlador.comando.acao;

import java.util.Map;

import br.com.opensig.comercial.client.servico.ComercialProxy;
import br.com.opensig.comercial.shared.modelo.ComFrete;
import br.com.opensig.comercial.shared.modelo.ComNatureza;
import br.com.opensig.comercial.shared.modelo.ComVenda;
import br.com.opensig.core.client.OpenSigCore;
import br.com.opensig.core.client.UtilClient;
import br.com.opensig.core.client.controlador.comando.ComandoAcao;
import br.com.opensig.core.client.visao.ComboEntidade;
import br.com.opensig.core.client.visao.Ponte;
import br.com.opensig.empresa.shared.modelo.EmpCliente;
import br.com.opensig.empresa.shared.modelo.EmpEmpresa;
import br.com.opensig.empresa.shared.modelo.EmpTransportadora;
import br.com.opensig.financeiro.shared.modelo.FinReceber;
import br.com.opensig.fiscal.shared.modelo.FisNotaSaida;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.gwtext.client.core.EventObject;
import com.gwtext.client.core.Position;
import com.gwtext.client.data.Record;
import com.gwtext.client.widgets.Button;
import com.gwtext.client.widgets.Component;
import com.gwtext.client.widgets.MessageBox;
import com.gwtext.client.widgets.Window;
import com.gwtext.client.widgets.event.ButtonListenerAdapter;
import com.gwtext.client.widgets.form.ComboBox;
import com.gwtext.client.widgets.form.FormPanel;
import com.gwtext.client.widgets.form.MultiFieldPanel;
import com.gwtext.client.widgets.form.NumberField;
import com.gwtext.client.widgets.form.Radio;
import com.gwtext.client.widgets.form.TextArea;
import com.gwtext.client.widgets.form.TextField;
import com.gwtext.client.widgets.form.event.FormPanelListenerAdapter;
import com.gwtext.client.widgets.layout.FitLayout;
import com.gwtextux.client.widgets.window.ToastWindow;

public class ComandoGerarNfeSaida extends ComandoAcao {

	private ComVenda venda;
	private ComFrete frete;

	private Radio rdEmitente;
	private Radio rdDestinatario;
	private Radio rdTerceiro;
	private Radio rdSemFrete;
	private NumberField txtVolume;
	private NumberField txtLiquido;
	private NumberField txtBruto;
	private TextField txtPlaca;
	private TextField txtUF;
	private TextField txtRNTC;
	private TextField txtNfe;
	private NumberField txtEcf;
	private NumberField txtCoo;
	private TextField txtEspecie;
	private ComboBox cmbTransportadora;
	private TextArea txtObservacao;
	private Window wndNFe;

	/**
	 * @see ComandoAcao#execute(Map)
	 */
	public void execute(final Map contexto) {
		super.execute(contexto, new AsyncCallback() {
			public void onSuccess(Object result) {
				Record rec = LISTA.getPanel().getSelectionModel().getSelected();

				if (rec != null && rec.getAsBoolean("comVendaFechada") && !rec.getAsBoolean("comVendaNfe") && !rec.getAsBoolean("comVendaCancelada")
						&& rec.getAsInteger("empEmpresa.empEmpresaId") == Ponte.getLogin().getEmpresaId()) {
					venda = new ComVenda();
					venda.setEmpEmpresa(new EmpEmpresa(rec.getAsInteger("empEmpresa.empEmpresaId")));
					venda.setEmpCliente(new EmpCliente(rec.getAsInteger("empCliente.empClienteId")));
					venda.setFinReceber(new FinReceber(rec.getAsInteger("finReceber.finReceberId")));
					venda.setComNatureza(new ComNatureza(rec.getAsInteger("comNatureza.comNaturezaId")));
					venda.setComVendaId(rec.getAsInteger("comVendaId"));
					venda.setComVendaData(rec.getAsDate("comVendaData"));
					venda.setComVendaValorBruto(rec.getAsDouble("comVendaValorBruto"));
					venda.setComVendaValorLiquido(rec.getAsDouble("comVendaValorLiquido"));
					venda.setComVendaObservacao(rec.getAsString("comVendaObservacao") == null ? "" : rec.getAsString("comVendaObservacao"));
					abrirFrete();
				} else {
					MessageBox.alert(OpenSigCore.i18n.txtAcesso(), OpenSigCore.i18n.txtAcessoNegado());
				}
			}

			public void onFailure(Throwable caught) {
			}
		});
	}

	private AsyncCallback<FisNotaSaida> salvar = new AsyncCallback<FisNotaSaida>() {
		public void onFailure(Throwable caught) {
			MessageBox.hide();
			MessageBox.alert(OpenSigCore.i18n.txtNfe(), OpenSigCore.i18n.errSalvar());
			new ToastWindow(OpenSigCore.i18n.txtNfe(), caught.getMessage()).show();
		}

		public void onSuccess(FisNotaSaida result) {
			Record rec = LISTA.getPanel().getSelectionModel().getSelected();
			rec.set("fisNotaSaida.fisNotaSaidaId", result.getFisNotaSaidaId());
			rec.set("comVendaNfe", true);

			MessageBox.hide();
			wndNFe.close();
			new ToastWindow(OpenSigCore.i18n.txtNfe(), OpenSigCore.i18n.msgSalvarOK()).show();
		}
	};

	private void abrirFrete() {
		// janela
		wndNFe = new Window(OpenSigCore.i18n.txtNfe() + " -> " + OpenSigCore.i18n.txtFrete(), 350, 450, true, false);
		wndNFe.setLayout(new FitLayout());
		wndNFe.setIconCls("icon-nfe");
		wndNFe.setClosable(false);
		wndNFe.addListener(new FormPanelListenerAdapter() {
			public void onShow(Component component) {
				cmbTransportadora.focus(true, 10);
			}
		});

		// formulario
		FormPanel frm = new FormPanel();
		frm.setLabelAlign(Position.TOP);
		frm.setPaddings(5);
		frm.setMargins(1);

		// botoes
		setBotoes(frm);

		// campos
		setCampos(frm);

		wndNFe.add(frm);
		wndNFe.show();
	}

	private void setBotoes(final FormPanel frm) {
		Button btnCancelar = new Button();
		btnCancelar.setText(OpenSigCore.i18n.txtCancelar());
		btnCancelar.setIconCls("icon-cancelar");
		btnCancelar.addListener(new ButtonListenerAdapter() {
			public void onClick(Button button, EventObject e) {
				wndNFe.close();
			}
		});

		Button btnSalvar = new Button();
		btnSalvar.setText(OpenSigCore.i18n.txtSalvar());
		btnSalvar.setIconCls("icon-salvar");
		btnSalvar.addListener(new ButtonListenerAdapter() {
			public void onClick(Button button, EventObject e) {
				if (rdSemFrete.getValue() || frm.getForm().isValid()) {
					frete = new ComFrete();
					// tipo
					if (rdEmitente.getValue()) {
						frete.setComFreteId(0);
					} else if (rdDestinatario.getValue()) {
						frete.setComFreteId(1);
					} else if (rdTerceiro.getValue()) {
						frete.setComFreteId(2);
					} else {
						frete.setComFreteId(9);
					}

					if (!rdSemFrete.getValue()) {
						// transportadora
						frete.setEmpTransportadora(new EmpTransportadora(Integer.valueOf(cmbTransportadora.getValue())));
						// volume
						frete.setComFreteVolume(txtVolume.getValue().intValue());
						// especie
						frete.setComFreteEspecie(txtEspecie.getValueAsString());
						// peso bruto
						if (txtBruto.getValue() != null) {
							frete.setComFretePeso(txtBruto.getValue().doubleValue());
						}
						// peso liquido
						if (txtLiquido.getValue() != null) {
							frete.setComFreteCubagem(txtLiquido.getValue().doubleValue());
						}
						// coloca na obs os dados do carro
						frete.setComFretePlaca(txtPlaca.getValueAsString());
						frete.setComFreteUF(txtUF.getValueAsString());
						frete.setComFreteRNTC(txtRNTC.getValueAsString());
					}
					// seta info
					venda.setComVendaObservacao(txtObservacao.getValueAsString());

					// caso tenha cupom referenciado
					int[] cupom = null;
					if (txtEcf.getValue() != null && txtCoo.getValue() != null) {
						cupom = new int[2];
						cupom[0] = txtEcf.getValue().intValue();
						cupom[1] = txtCoo.getValue().intValue();
					}

					MessageBox.wait(OpenSigCore.i18n.txtAguarde(), OpenSigCore.i18n.txtNfe());
					ComercialProxy proxy = new ComercialProxy();
					proxy.gerarNfe(venda, frete, txtNfe.getValueAsString(), cupom, salvar);
				}
			}
		});

		frm.setTopToolbar(new Button[] { btnSalvar, btnCancelar });
	}

	private void setCampos(FormPanel frm) {
		rdEmitente = new Radio(OpenSigCore.i18n.txtOrigem(), "tipo");
		rdEmitente.setChecked(true);
		rdDestinatario = new Radio(OpenSigCore.i18n.txtDestino(), "tipo");
		rdTerceiro = new Radio(OpenSigCore.i18n.txtTerceiro(), "tipo");
		rdSemFrete = new Radio(OpenSigCore.i18n.txtSemFrete(), "tipo");

		MultiFieldPanel linha1 = new MultiFieldPanel();
		linha1.setBorder(false);
		linha1.addToRow(rdEmitente, 80);
		linha1.addToRow(rdDestinatario, 80);
		linha1.addToRow(rdTerceiro, 80);
		linha1.addToRow(rdSemFrete, 80);
		frm.add(linha1);

		MultiFieldPanel linha2 = new MultiFieldPanel();
		linha2.setBorder(false);
		linha2.addToRow(getTransportadora(), 320);
		frm.add(linha2);

		txtVolume = new NumberField(OpenSigCore.i18n.txtVolume(), "comFreteVolume", 60);
		txtVolume.setAllowBlank(false);
		txtVolume.setAllowDecimals(false);
		txtVolume.setAllowNegative(false);
		txtVolume.setMaxLength(6);

		txtEspecie = new TextField(OpenSigCore.i18n.txtEspecie(), "comFreteEspecie", 60);
		txtEspecie.setMaxLength(10);
		txtEspecie.setAllowBlank(false);

		txtBruto = new NumberField(OpenSigCore.i18n.txtBruto(), "comFreteBruto", 60);
		txtBruto.setAllowNegative(false);
		txtBruto.setMaxLength(11);
		txtBruto.setDecimalPrecision(2);

		txtLiquido = new NumberField(OpenSigCore.i18n.txtLiquido(), "comFreteLiquido", 60);
		txtLiquido.setAllowNegative(false);
		txtLiquido.setMaxLength(11);
		txtLiquido.setDecimalPrecision(2);

		MultiFieldPanel linha3 = new MultiFieldPanel();
		linha3.setBorder(false);
		linha3.addToRow(txtVolume, 80);
		linha3.addToRow(txtEspecie, 80);
		linha3.addToRow(txtBruto, 80);
		linha3.addToRow(txtLiquido, 80);
		frm.add(linha3);

		txtPlaca = new TextField(OpenSigCore.i18n.txtPlaca(), "comFretePlaca", 80);
		txtPlaca.setMaxLength(8);
		txtPlaca.setRegex("^[A-Z0-9]+$");
		
		txtUF = new TextField(OpenSigCore.i18n.txtUF(), "comFreteUF", 60);
		txtUF.setMaxLength(2);
		txtUF.setRegex("^[A-Z]{2}$");
		
		txtRNTC = new TextField(OpenSigCore.i18n.txtRNTC(), "comFreteRNTC", 120);
		txtRNTC.setMaxLength(20);
		
		MultiFieldPanel linha4 = new MultiFieldPanel();
		linha4.setBorder(false);
		linha4.addToRow(txtPlaca, 100);
		linha4.addToRow(txtUF, 80);
		linha4.addToRow(txtRNTC, 140);
		frm.add(linha4);
		
		txtNfe = new TextField(OpenSigCore.i18n.txtNfe() + " " + OpenSigCore.i18n.txtComplemento(), "comCompraNfe", 300);
		txtNfe.setRegex("^(\\d{44})$");
		frm.add(txtNfe);

		txtEcf = new NumberField(OpenSigCore.i18n.txtEcf(), "comCompraEcf", 120);
		txtEcf.setAllowNegative(false);
		txtEcf.setAllowDecimals(false);
		txtEcf.setMaxLength(3);

		txtCoo = new NumberField(OpenSigCore.i18n.txtCoo(), "comCompraCoo", 120);
		txtCoo.setAllowNegative(false);
		txtCoo.setAllowDecimals(false);
		txtCoo.setMaxLength(11);

		MultiFieldPanel linha5 = new MultiFieldPanel();
		linha5.setBorder(false);
		linha5.addToRow(txtEcf, 160);
		linha5.addToRow(txtCoo, 160);
		frm.add(linha5);

		txtObservacao = new TextArea(OpenSigCore.i18n.txtObservacao(), "comVendaObservacao");
		txtObservacao.setMaxLength(4000);
		txtObservacao.setWidth("95%");
		txtObservacao.setValue(venda.getComVendaObservacao());
		frm.add(txtObservacao);
	}

	private ComboBox getTransportadora() {
		cmbTransportadora = UtilClient.getComboEntidade(new ComboEntidade(new EmpTransportadora()));
		cmbTransportadora.setName("empTransportadora.empEntidade.empEntidadeNome1");
		cmbTransportadora.setLabel(OpenSigCore.i18n.txtTransportadora());
		cmbTransportadora.setTriggerAction(ComboBox.ALL);

		return cmbTransportadora;
	}
}
