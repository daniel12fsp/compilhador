

public class Parser {
	public static final int _EOF = 0;
	public static final int _id = 1;
	public static final int _strConst = 2;
	public static final int _num = 3;
	public static final int maxT = 49;
	public static final int _option = 50;

	static final boolean T = true;
	static final boolean x = false;
	static final int minErrDist = 2;

	public Token t;    // last recognized token
	public Token la;   // lookahead token
	int errDist = minErrDist;
	
	public Scanner scanner;
	public Errors errors;

	private Tab ts;
	private Obj ofuncAtual;

	private Code objCode;

	private Struct getTipo(String tipo, boolean vec) { 
		Obj o = ts.buscar(tipo);
		Struct st = o.tipo;
		if (vec) st = new Struct(Struct.Vetor, o.tipo);
		return st;	
	}

	public void erro(String msg) {
		errors.SemErr(t.line, t.col, msg);
	}



	public Parser(Scanner scanner) {
		this.scanner = scanner;
		errors = new Errors();
	}

	void SynErr (int n) {
		if (errDist >= minErrDist) errors.SynErr(la.line, la.col, n);
		errDist = 0;
	}

	public void SemErr (String msg) {
		if (errDist >= minErrDist) errors.SemErr(t.line, t.col, msg);
		errDist = 0;
	}
	
	void Get () {
		for (;;) {
			t = la;
			la = scanner.Scan();
			if (la.kind <= maxT) {
				++errDist;
				break;
			}

			if (la.kind == 50) {
				ts.dump(); 
			}
			la = t;
		}
	}
	
	void Expect (int n) {
		if (la.kind==n) Get(); else { SynErr(n); }
	}
	
	boolean StartOf (int s) {
		return set[s][la.kind];
	}
	
	void ExpectWeak (int n, int follow) {
		if (la.kind == n) Get();
		else {
			SynErr(n);
			while (!StartOf(follow)) Get();
		}
	}
	
	boolean WeakSeparator (int n, int syFol, int repFol) {
		int kind = la.kind;
		if (kind == n) { Get(); return true; }
		else if (StartOf(repFol)) return false;
		else {
			SynErr(n);
			while (!(set[syFol][kind] || set[repFol][kind] || set[0][kind])) {
				Get();
				kind = la.kind;
			}
			return StartOf(syFol);
		}
	}
	
	void MicroPortugol() {
		Expect(4);
		Expect(1);
		Linhas();
	}

	void Linhas() {
		Comando();
		while (StartOf(1)) {
			Comando();
		}
	}

	void Comando() {
		switch (la.kind) {
		case 5: {
			Bloco_def();
			break;
		}
		case 7: {
			Procedimento_def();
			break;
		}
		case 13: {
			Variavel_def();
			break;
		}
		case 16: case 21: case 23: {
			Repeticao_def();
			break;
		}
		case 29: {
			Se_def();
			break;
		}
		case 33: {
			Caso_def();
			break;
		}
		case 37: {
			Escreva_def();
			break;
		}
		case 38: {
			Leia_def();
			break;
		}
		case 39: {
			Constante_def();
			break;
		}
		case 1: {
			Designador();
			break;
		}
		default: SynErr(50); break;
		}
	}

	void Bloco_def() {
		Expect(5);
		Linhas();
		Expect(6);
	}

	void Procedimento_def() {
		Expect(7);
		Expect(1);
		Expect(8);
		if (la.kind == 1) {
			Get();
			Tipo();
			while (la.kind == 9) {
				Get();
				Expect(1);
				Tipo();
			}
		}
		Expect(10);
		if (la.kind == 14) {
			Tipo();
		}
		Expect(5);
		Linhas();
		if (la.kind == 11) {
			Get();
			Exp();
			Expect(12);
		}
		Expect(6);
	}

	void Variavel_def() {
		Expect(13);
		Expect(1);
		while (la.kind == 9) {
			Get();
			Expect(1);
		}
		Tipo();
		Expect(12);
	}

	void Repeticao_def() {
		if (la.kind == 16) {
			Para_def();
		} else if (la.kind == 21) {
			Enquanto_def();
		} else if (la.kind == 23) {
			Repita_def();
		} else SynErr(51);
	}

	void Se_def() {
		Expect(29);
		Condicao_def();
		Expect(30);
		Linhas();
		if (la.kind == 31) {
			Get();
			Linhas();
		}
		Expect(32);
	}

	void Caso_def() {
		Expect(33);
		Valor();
		Caso_seja_def();
		while (la.kind == 36) {
			Caso_seja_def();
		}
		Expect(34);
		Linhas();
		Expect(35);
	}

	void Escreva_def() {
		Expect(37);
		Expect(8);
		Expect(2);
		while (la.kind == 9) {
			Get();
			Expect(2);
		}
		Expect(10);
		Expect(12);
	}

	void Leia_def() {
		Expect(38);
		Expect(8);
		Expect(2);
		while (la.kind == 9) {
			Get();
			Expect(2);
		}
		Expect(10);
		Expect(12);
	}

	void Constante_def() {
		Expect(39);
		Expect(1);
		Expect(17);
		Valor();
		Expect(12);
	}

	void Designador() {
		Expect(1);
		if (la.kind == 8 || la.kind == 17 || la.kind == 25) {
			X();
		}
	}

	void Tipo() {
		Expect(14);
		Expect(15);
	}

	void Exp() {
		Termo();
		while (la.kind == 1 || la.kind == 3) {
			Termo();
		}
	}

	void Para_def() {
		Expect(16);
		Expect(1);
		Expect(17);
		Exp();
		Expect(18);
		Exp();
		Expect(19);
		Linhas();
		Expect(20);
	}

	void Enquanto_def() {
		Expect(21);
		Condicao_def();
		Expect(19);
		Linhas();
		Expect(22);
	}

	void Repita_def() {
		Expect(23);
		Linhas();
		Expect(18);
		Condicao_def();
		Expect(12);
	}

	void Condicao_def() {
		Exp();
		Op_relacional();
		Exp();
	}

	void Novo_def() {
		Expect(24);
		Expect(15);
		if (la.kind == 25) {
			Get();
			Exp();
			Expect(26);
		} else if (la.kind == 27) {
			Get();
			Exp();
			while (la.kind == 9) {
				Get();
				Exp();
			}
			Expect(28);
		} else SynErr(52);
	}

	void Valor() {
		if (la.kind == 3) {
			Get();
		} else if (la.kind == 1) {
			Designador();
		} else SynErr(53);
	}

	void Caso_seja_def() {
		Expect(36);
		Expect(3);
		Expect(19);
		Linhas();
	}

	void Op_relacional() {
		if (la.kind == 40) {
			Get();
		} else if (la.kind == 41) {
			Get();
		} else if (la.kind == 42) {
			Get();
		} else if (la.kind == 43) {
			Get();
		} else if (la.kind == 44) {
			Get();
		} else SynErr(54);
	}

	void Fator() {
		Valor();
		if (la.kind == 45 || la.kind == 46) {
			if (la.kind == 45) {
				Get();
			} else {
				Get();
			}
			Valor();
		}
	}

	void Termo() {
		Fator();
		if (la.kind == 47 || la.kind == 48) {
			if (la.kind == 47) {
				Get();
			} else {
				Get();
			}
			Fator();
		}
	}

	void X() {
		if (la.kind == 25) {
			Vetor_def();
		} else if (la.kind == 8) {
			Chamada_parametros();
		} else if (la.kind == 17) {
			Assinalamento_def();
		} else SynErr(55);
	}

	void Vetor_def() {
		Expect(25);
		if (la.kind == 1 || la.kind == 3) {
			Exp();
		}
		Expect(26);
	}

	void Chamada_parametros() {
		Expect(8);
		Exp();
		while (la.kind == 9) {
			Get();
			Exp();
		}
		Expect(10);
		Expect(12);
	}

	void Assinalamento_def() {
		Expect(17);
		if (la.kind == 1) {
			Designador();
		} else if (la.kind == 24) {
			Novo_def();
		} else SynErr(56);
		Expect(12);
	}



	public void Parse() {
		la = new Token();
		la.val = "";		
		Get();
		MicroPortugol();
		Expect(0);

	}

	private static final boolean[][] set = {
		{T,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x},
		{x,T,x,x, x,T,x,T, x,x,x,x, x,T,x,x, T,x,x,x, x,T,x,T, x,x,x,x, x,T,x,x, x,T,x,x, x,T,T,T, x,x,x,x, x,x,x,x, x,x,x}

	};
} // end Parser


class Errors {
	public int count = 0;                                    // number of errors detected
	public java.io.PrintStream errorStream = System.out;     // error messages go to this stream
	public String errMsgFormat = "-- line {0} col {1}: {2}"; // 0=line, 1=column, 2=text
	
	protected void printMsg(int line, int column, String msg) {
		StringBuffer b = new StringBuffer(errMsgFormat);
		int pos = b.indexOf("{0}");
		if (pos >= 0) { b.delete(pos, pos+3); b.insert(pos, line); }
		pos = b.indexOf("{1}");
		if (pos >= 0) { b.delete(pos, pos+3); b.insert(pos, column); }
		pos = b.indexOf("{2}");
		if (pos >= 0) b.replace(pos, pos+3, msg);
		errorStream.println(b.toString());
	}
	
	public void SynErr (int line, int col, int n) {
		String s;
		switch (n) {
			case 0: s = "EOF expected"; break;
			case 1: s = "id expected"; break;
			case 2: s = "strConst expected"; break;
			case 3: s = "num expected"; break;
			case 4: s = "\"algoritmo\" expected"; break;
			case 5: s = "\"inicio\" expected"; break;
			case 6: s = "\"fim\" expected"; break;
			case 7: s = "\"procedimento\" expected"; break;
			case 8: s = "\"(\" expected"; break;
			case 9: s = "\",\" expected"; break;
			case 10: s = "\")\" expected"; break;
			case 11: s = "\"retorne\" expected"; break;
			case 12: s = "\";\" expected"; break;
			case 13: s = "\"variavel\" expected"; break;
			case 14: s = "\":\" expected"; break;
			case 15: s = "\"inteiro\" expected"; break;
			case 16: s = "\"para\" expected"; break;
			case 17: s = "\"=\" expected"; break;
			case 18: s = "\"ate\" expected"; break;
			case 19: s = "\"faca\" expected"; break;
			case 20: s = "\"fimpara\" expected"; break;
			case 21: s = "\"enquanto\" expected"; break;
			case 22: s = "\"fimenquanto\" expected"; break;
			case 23: s = "\"repita\" expected"; break;
			case 24: s = "\"novo\" expected"; break;
			case 25: s = "\"[\" expected"; break;
			case 26: s = "\"]\" expected"; break;
			case 27: s = "\"{\" expected"; break;
			case 28: s = "\"}\" expected"; break;
			case 29: s = "\"se\" expected"; break;
			case 30: s = "\"entao\" expected"; break;
			case 31: s = "\"senao\" expected"; break;
			case 32: s = "\"fimse\" expected"; break;
			case 33: s = "\"caso\" expected"; break;
			case 34: s = "\"outrocaso\" expected"; break;
			case 35: s = "\"fimcaso\" expected"; break;
			case 36: s = "\"seja\" expected"; break;
			case 37: s = "\"escreva\" expected"; break;
			case 38: s = "\"leia\" expected"; break;
			case 39: s = "\"constante\" expected"; break;
			case 40: s = "\"==\" expected"; break;
			case 41: s = "\"!=\" expected"; break;
			case 42: s = "\">\" expected"; break;
			case 43: s = "\">=\" expected"; break;
			case 44: s = "\"<\" expected"; break;
			case 45: s = "\"*\" expected"; break;
			case 46: s = "\"/\" expected"; break;
			case 47: s = "\"+\" expected"; break;
			case 48: s = "\"-\" expected"; break;
			case 49: s = "??? expected"; break;
			case 50: s = "invalid Comando"; break;
			case 51: s = "invalid Repeticao_def"; break;
			case 52: s = "invalid Novo_def"; break;
			case 53: s = "invalid Valor"; break;
			case 54: s = "invalid Op_relacional"; break;
			case 55: s = "invalid X"; break;
			case 56: s = "invalid Assinalamento_def"; break;
			default: s = "error " + n; break;
		}
		printMsg(line, col, s);
		count++;
	}

	public void SemErr (int line, int col, String s) {	
		printMsg(line, col, s);
		count++;
	}
	
	public void SemErr (String s) {
		errorStream.println(s);
		count++;
	}
	
	public void Warning (int line, int col, String s) {	
		printMsg(line, col, s);
	}
	
	public void Warning (String s) {
		errorStream.println(s);
	}
} // Errors


class FatalError extends RuntimeException {
	public static final long serialVersionUID = 1L;
	public FatalError(String s) { super(s); }
}
