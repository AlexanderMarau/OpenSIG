package br.com.opensig.fiscal.server.sped.contribuicao.blocoF;

import br.com.opensig.fiscal.server.sped.Bean;

public class DadosF001 extends Bean {

	private int ind_mov;

	public DadosF001() {
		reg = "F001";
	}

	public int getInd_mov() {
		return ind_mov;
	}

	public void setInd_mov(int ind_mov) {
		this.ind_mov = ind_mov;
	}

}
