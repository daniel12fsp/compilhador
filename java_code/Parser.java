

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

class NDesig {
        String nome;
        boolean vec;
        public NDesig(String n, boolean v) {
                nome = n;
                vec = v;
        }
        public String getNome() {
                return nome;
        }
        public boolean vetor() {
                return vec;
        }
}

class NTipo {
        String tipo;
        boolean vec;
        public NTipo(String t, boolean v) {
                tipo = t;
                vec = v;
        }
        public String getTipo() {
                return tipo;
        }
        public boolean vetor() {
                return vec;
        }
}



public class Parser {
	public static final int _EOF = 0;
	public static final int _id = 1;
	public static final int _strConst = 2;
	public static final int _num = 3;
	public static final int maxT = 52;
	public static final int _option = 53;

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

			if (la.kind == 53) {
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
		ofuncAtual = null; 
		ts = new Tab(this);
		ts.abrirEscopo("Global");
		
		Expect(4);
		Expect(1);
		while (la.kind == 5 || la.kind == 12 || la.kind == 16) {
			if (la.kind == 5) {
				declaracao_var();
			} else if (la.kind == 12) {
				declaracao_const();
			} else {
				bloco_procedimento();
			}
		}
		bloco_principal();
		objCode.setDataSize(ts.escopoAtual.nVars);
		ts.fecharEscopo(); 
		
	}

	void declaracao_var() {
		ArrayList<String> nomeVariaveis 
		    = new ArrayList();
		NTipo ntipo;
		
		Expect(5);
		Expect(1);
		nomeVariaveis.add(t.val); 
		while (la.kind == 6) {
			Get();
			Expect(1);
			nomeVariaveis.add(t.val); 
		}
		Expect(7);
		ntipo = tipo();
		Struct st = getTipo(ntipo.getTipo(),
		    ntipo.vetor());
		if (st == ts.semTipo)
		    erro("A variÃ¡vel nÃ£o pode ter tipo desconhecido");
		     
		Iterator iter = nomeVariaveis.iterator();
		                                        while (iter.hasNext()) {
		    ts.inserir(Obj.Var, iter.next().toString(), st);
		}
		                                        
		Expect(8);
	}

	void declaracao_const() {
		Obj obj;
		
		Expect(12);
		Expect(1);
		obj = ts.buscar("inteiro");
		obj = ts.inserir(Obj.Const, t.val, obj.tipo); 
		
		Expect(13);
		Expect(3);
		obj.val = Integer.parseInt(t.val); 
		Expect(8);
	}

	void bloco_procedimento() {
		NTipo ntipo; 
		String nome;
		boolean tipoInformado = false;
		
		Expect(16);
		Expect(1);
		nome = t.val; 
		ofuncAtual = ts.inserir(Obj.Func,
		nome,
		getTipo("void", false));
		ts.abrirEscopo("Func " + nome); 
		
		Expect(14);
		if (la.kind == 1) {
			parametros();
		}
		Expect(15);
		ofuncAtual.end = objCode.getPC();
		objCode.put(objCode.enter);
		objCode.put(ofuncAtual.nPars);
		int pcvars = objCode.getPC();
		objCode.put(0);
		
		if (la.kind == 7) {
			Get();
			ntipo = tipo();
		}
		Expect(17);
		instrucoes();
		Expect(18);
		ofuncAtual.locais = ts.escopoAtual.locais;
		ts.fecharEscopo();
		
	}

	void bloco_principal() {
		ofuncAtual = ts.inserir(Obj.Func,
		"main", getTipo("void", false));
		ts.abrirEscopo("Func main");
		
		Expect(17);
		objCode.setMainPC(); 
		instrucoes();
		Expect(18);
		ofuncAtual.locais = ts.escopoAtual.locais;
		ts.fecharEscopo();
		
	}

	NTipo  tipo() {
		NTipo  ntipo;
		boolean vet = false; 
		String tip;
		
		Expect(9);
		tip = t.val; 
		if (la.kind == 10) {
			Get();
			Expect(11);
			vet = true; 
		}
		ntipo = new NTipo(tip, vet); 
		return ntipo;
	}

	void designador() {
		Expect(1);
		if (la.kind == 10) {
			Get();
			expressao();
			Expect(11);
		}
	}

	void expressao() {
		termo();
		while (la.kind == 47 || la.kind == 48) {
			if (la.kind == 47) {
				Get();
			} else {
				Get();
			}
			termo();
		}
	}

	void parametros() {
		NTipo ntipo; Struct st; 
		Expect(1);
		String nome = t.val; 
		Expect(7);
		ntipo = tipo();
		st = getTipo(ntipo.getTipo(),
		ntipo.vetor());
		ts.inserir(Obj.Var, nome, st); 
		ofuncAtual.nPars++; 
		
		while (la.kind == 6) {
			Get();
			Expect(1);
			nome = t.val; 
			Expect(7);
			ntipo = tipo();
			st = getTipo(ntipo.getTipo(),
			ntipo.vetor());
			ts.inserir(Obj.Var, nome, st); 
			ofuncAtual.nPars++; 
			
		}
	}

	void parametros_passados() {
		Expect(14);
		if (StartOf(1)) {
			expressao();
			while (la.kind == 6) {
				Get();
				expressao();
			}
		}
		Expect(15);
	}

	void instrucoes() {
		while (StartOf(2)) {
			comando();
		}
	}

	void retorno() {
		Expect(19);
		if (StartOf(1)) {
			expressao();
		}
		Expect(8);
	}

	void comando() {
		switch (la.kind) {
		case 5: {
			declaracao_var();
			break;
		}
		case 23: case 28: case 36: {
			repeticao_def();
			break;
		}
		case 37: case 41: {
			condicional_def();
			break;
		}
		case 45: case 46: {
			io_def();
			break;
		}
		case 1: {
			designador();
			if (la.kind == 13) {
				assinalamento();
			} else if (la.kind == 14) {
				parametros_passados();
			} else SynErr(53);
			Expect(8);
			break;
		}
		case 19: {
			retorno();
			break;
		}
		default: SynErr(54); break;
		}
	}

	void repeticao_def() {
		if (la.kind == 23) {
			para_def();
		} else if (la.kind == 28) {
			enquanto_def();
		} else if (la.kind == 36) {
			repita_def();
		} else SynErr(55);
	}

	void condicional_def() {
		if (la.kind == 37) {
			se_def();
		} else if (la.kind == 41) {
			caso_def();
		} else SynErr(56);
	}

	void io_def() {
		if (la.kind == 46) {
			escreva_def();
		} else if (la.kind == 45) {
			leia_def();
		} else SynErr(57);
	}

	void assinalamento() {
		Expect(13);
		expressao();
	}

	void aloc_vetor() {
		Expect(20);
		Expect(9);
		if (la.kind == 10) {
			Get();
			expressao();
			Expect(11);
		} else if (la.kind == 21) {
			Get();
			Expect(3);
			while (la.kind == 6) {
				Get();
				Expect(3);
			}
			Expect(22);
		} else SynErr(58);
	}

	void para_def() {
		Expect(23);
		Expect(1);
		Expect(13);
		expressao();
		Expect(24);
		expressao();
		if (la.kind == 25) {
			Get();
			expressao();
		}
		Expect(26);
		instrucoes();
		Expect(27);
		if (la.kind == 8) {
			Get();
		}
	}

	void enquanto_def() {
		Expect(28);
		condicao();
		Expect(26);
		instrucoes();
		Expect(29);
		if (la.kind == 8) {
			Get();
		}
	}

	void repita_def() {
		Expect(36);
		instrucoes();
		Expect(24);
		condicao();
		Expect(8);
	}

	void condicao() {
		expressao();
		operador_relacional();
		expressao();
	}

	void operador_relacional() {
		switch (la.kind) {
		case 30: {
			Get();
			break;
		}
		case 31: {
			Get();
			break;
		}
		case 32: {
			Get();
			break;
		}
		case 33: {
			Get();
			break;
		}
		case 34: {
			Get();
			break;
		}
		case 35: {
			Get();
			break;
		}
		default: SynErr(59); break;
		}
	}

	void se_def() {
		Expect(37);
		condicao();
		Expect(38);
		instrucoes();
		if (la.kind == 39) {
			Get();
			instrucoes();
		}
		Expect(40);
		if (la.kind == 8) {
			Get();
		}
	}

	void caso_def() {
		Expect(41);
		designador();
		caso_seja_def();
		while (la.kind == 44) {
			caso_seja_def();
		}
		Expect(42);
		Expect(7);
		instrucoes();
		Expect(43);
	}

	void caso_seja_def() {
		Expect(44);
		expressao();
		Expect(26);
		instrucoes();
	}

	void escreva_def() {
		Expect(46);
		Expect(14);
		if (la.kind == 2) {
			Get();
		} else if (la.kind == 1) {
			designador();
			if (la.kind == 14) {
				parametros_passados();
			}
		} else SynErr(60);
		while (la.kind == 6) {
			Get();
			if (la.kind == 2) {
				Get();
			} else if (la.kind == 1) {
				designador();
				if (la.kind == 14) {
					parametros_passados();
				}
			} else SynErr(61);
		}
		Expect(15);
		Expect(8);
	}

	void leia_def() {
		Expect(45);
		Expect(14);
		designador();
		Expect(15);
		Expect(8);
	}

	void termo() {
		fator();
		while (la.kind == 49 || la.kind == 50 || la.kind == 51) {
			if (la.kind == 49) {
				Get();
			} else if (la.kind == 50) {
				Get();
			} else {
				Get();
			}
			fator();
		}
	}

	void fator() {
		if (la.kind == 3) {
			Get();
		} else if (la.kind == 1) {
			designador();
			if (la.kind == 14) {
				parametros_passados();
			}
		} else if (la.kind == 14) {
			Get();
			expressao();
			Expect(15);
		} else if (la.kind == 20) {
			aloc_vetor();
		} else SynErr(62);
	}



	public void Parse() {
		la = new Token();
		la.val = "";		
		Get();
		MicroPortugol();
		Expect(0);

	}

	private static final boolean[][] set = {
		{T,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x},
		{x,T,x,T, x,x,x,x, x,x,x,x, x,x,T,x, x,x,x,x, T,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x},
		{x,T,x,x, x,T,x,x, x,x,x,x, x,x,x,x, x,x,x,T, x,x,x,T, x,x,x,x, T,x,x,x, x,x,x,x, T,T,x,x, x,T,x,x, x,T,T,x, x,x,x,x, x,x}

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
			case 5: s = "\"variavel\" expected"; break;
			case 6: s = "\",\" expected"; break;
			case 7: s = "\":\" expected"; break;
			case 8: s = "\";\" expected"; break;
			case 9: s = "\"inteiro\" expected"; break;
			case 10: s = "\"[\" expected"; break;
			case 11: s = "\"]\" expected"; break;
			case 12: s = "\"constante\" expected"; break;
			case 13: s = "\"=\" expected"; break;
			case 14: s = "\"(\" expected"; break;
			case 15: s = "\")\" expected"; break;
			case 16: s = "\"procedimento\" expected"; break;
			case 17: s = "\"inicio\" expected"; break;
			case 18: s = "\"fim\" expected"; break;
			case 19: s = "\"retorne\" expected"; break;
			case 20: s = "\"novo\" expected"; break;
			case 21: s = "\"{\" expected"; break;
			case 22: s = "\"}\" expected"; break;
			case 23: s = "\"para\" expected"; break;
			case 24: s = "\"ate\" expected"; break;
			case 25: s = "\"passo\" expected"; break;
			case 26: s = "\"faca\" expected"; break;
			case 27: s = "\"fimpara\" expected"; break;
			case 28: s = "\"enquanto\" expected"; break;
			case 29: s = "\"fimenquanto\" expected"; break;
			case 30: s = "\"!=\" expected"; break;
			case 31: s = "\">\" expected"; break;
			case 32: s = "\"<\" expected"; break;
			case 33: s = "\">=\" expected"; break;
			case 34: s = "\"<=\" expected"; break;
			case 35: s = "\"==\" expected"; break;
			case 36: s = "\"repita\" expected"; break;
			case 37: s = "\"se\" expected"; break;
			case 38: s = "\"entao\" expected"; break;
			case 39: s = "\"senao\" expected"; break;
			case 40: s = "\"fimse\" expected"; break;
			case 41: s = "\"caso\" expected"; break;
			case 42: s = "\"outrocaso\" expected"; break;
			case 43: s = "\"fimcaso\" expected"; break;
			case 44: s = "\"seja\" expected"; break;
			case 45: s = "\"leia\" expected"; break;
			case 46: s = "\"escreva\" expected"; break;
			case 47: s = "\"+\" expected"; break;
			case 48: s = "\"-\" expected"; break;
			case 49: s = "\"*\" expected"; break;
			case 50: s = "\"/\" expected"; break;
			case 51: s = "\"%\" expected"; break;
			case 52: s = "??? expected"; break;
			case 53: s = "invalid comando"; break;
			case 54: s = "invalid comando"; break;
			case 55: s = "invalid repeticao_def"; break;
			case 56: s = "invalid condicional_def"; break;
			case 57: s = "invalid io_def"; break;
			case 58: s = "invalid aloc_vetor"; break;
			case 59: s = "invalid operador_relacional"; break;
			case 60: s = "invalid escreva_def"; break;
			case 61: s = "invalid escreva_def"; break;
			case 62: s = "invalid fator"; break;
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
