package br.com.opensig.fiscal.server.sped.blocoC;

import java.util.List;

import br.com.opensig.comercial.shared.modelo.ComEcfNotaProduto;
import br.com.opensig.fiscal.server.sped.ARegistro;
import br.com.opensig.produto.shared.modelo.ProdTributacao;

public class RegistroC390 extends ARegistro<DadosC390, List<ComEcfNotaProduto>> {

	@Override
	protected DadosC390 getDados(List<ComEcfNotaProduto> dados) throws Exception {
		DadosC390 d = new DadosC390();
		for (ComEcfNotaProduto np : dados) {
			ProdTributacao pt = np.getProdProduto().getProdTributacao();
			d.setCst_icms(auth.getConf().get("nfe.crt").equals("1") ? pt.getProdTributacaoCson() : pt.getProdTributacaoCst());
			d.setCfop(pt.getProdTributacaoCfop());
			d.setAliq_icms(np.getComEcfNotaProdutoIcms());
			d.setVl_opr(somarDoubles(d.getVl_opr(), np.getComEcfNotaProdutoLiquido()));
		}
		if (d.getVl_opr() != null) {
			d.setVl_bc_icms(d.getVl_opr());
			d.setVl_icms(d.getVl_opr() * d.getAliq_icms() / 100);
		}
		d.setVl_red_bc(0.00);
		d.setCod_obs("");
		return d;
	}

}
