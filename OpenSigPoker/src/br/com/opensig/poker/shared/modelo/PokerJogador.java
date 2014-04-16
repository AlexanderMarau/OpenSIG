package br.com.opensig.poker.shared.modelo;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;

import br.com.opensig.core.client.UtilClient;
import br.com.opensig.core.shared.modelo.Dados;

/**
 * Classe que representa um jogador no sistema.
 * 
 * @author Pedro H. Lira
 */
@Entity
@Table(name = "poker_jogador")
@XmlRootElement
public class PokerJogador extends Dados implements Serializable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "poker_jogador_id")
	private int pokerJogadorId;

	@Column(name = "poker_jogador_ativo")
	private int pokerJogadorAtivo;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "poker_cash_id")
	private PokerCash pokerCash;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "poker_cliente_id")
	private PokerCliente pokerCliente;

	public PokerJogador() {
		this(0);
	}

	public PokerJogador(int pokerJogadorId) {
		super("pu_poker", "PokerJogador", "pokerJogadorId", "pokerCliente.pokerClienteNome");
		this.pokerJogadorId = pokerJogadorId;
	}

	public int getPokerJogadorId() {
		return this.pokerJogadorId;
	}

	public void setPokerJogadorId(int pokerJogadorId) {
		this.pokerJogadorId = pokerJogadorId;
	}

	public boolean getPokerJogadorAtivo() {
		return this.pokerJogadorAtivo == 0 ? false : true;
	}

	public void setPokerJogadorAtivo(boolean pokerJogadorAtivo) {
		this.pokerJogadorAtivo = pokerJogadorAtivo == false ? 0 : 1;
	}

	public PokerCash getPokerCash() {
		return pokerCash;
	}

	public void setPokerCash(PokerCash pokerCash) {
		this.pokerCash = pokerCash;
	}

	public void setPokerCliente(PokerCliente pokerCliente) {
		this.pokerCliente = pokerCliente;
	}

	public PokerCliente getPokerCliente() {
		return this.pokerCliente;
	}

	public Number getId() {
		return pokerJogadorId;
	}

	public void setId(Number id) {
		pokerJogadorId = id.intValue();
	}

	public String[] toArray() {
		return new String[] { pokerJogadorId + "", pokerCliente.getPokerClienteId() + "", pokerCliente.getPokerClienteCodigo() + "", pokerCliente.getPokerClienteNome(),
				pokerCash.getPokerCashId() + "", pokerCash.getPokerCashCodigo(), UtilClient.getDataGrid(pokerCash.getPokerCashData()), pokerCash.getPokerCashFechado() + "",
				getPokerJogadorAtivo() + "" };
	}

	public void anularDependencia() {
		pokerCliente = null;
	}
}