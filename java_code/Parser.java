

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
	private Operand op;

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

	public int toInteger(String str){
		return Integer.parseInt(str);
	}

	public void debug(String str){
		System.out.println(str);
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
				objCode.setDataSize(ts.escopoAtual.nVars);
			}
		}
		bloco_principal();
		ts.fecharEscopo(); 
	}

	void declaracao_var() {
		ArrayList<String> nomeVariaveis 
		    = new ArrayList();
		NTipo ntipo;
		
		Expect(5);
		debug("variavel"); 
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
		debug(ntipo.getTipo());
		if (st == ts.semTipo)
		    erro("A variÃ¡vel nÃ£o pode ter tipo desconhecido");
		     
		Iterator iter = nomeVariaveis.iterator();
		debug("npars++");
		//ofuncAtual.nPars  = nomeVariaveis.size();
		while (iter.hasNext()) {
		    ts.inserir(Obj.Var, iter.next().toString(), st);
		}
		
		Expect(8);
	}

	void declaracao_const() {
		Obj obj;
		
		Expect(12);
		debug("const"); 
		Expect(1);
		obj = ts.buscar("inteiro");
		obj = ts.inserir(Obj.Const, t.val, obj.tipo); 
		
		Expect(13);
		Expect(3);
		obj.val = toInteger(t.val); 
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
		if (la.kind == 7) {
			Get();
			ntipo = tipo();
		}
		Expect(17);
		ofuncAtual.end = objCode.getPC();
		               objCode.put(objCode.enter);
		objCode.put(ofuncAtual.nPars);
		               int pcvars = objCode.getPC();
		               objCode.put(0);
		            
		instrucoes();
		objCode.put(pcvars, ts.escopoAtual.nVars);
		             if (ofuncAtual.tipo == ts.semTipo) {
		                  objCode.put(objCode.exit);
		                  objCode.put(objCode.return_);
		             } else {
		                  // fim da funcao alcancado
		                  // sem instrucao return
		                  objCode.put(objCode.trap);
		                  objCode.put(1);
		             }						
		
		Expect(18);
		debug("fim"); 
		ofuncAtual.locais = ts.escopoAtual.locais;
		ts.fecharEscopo();
		
	}

	void bloco_principal() {
		ofuncAtual = ts.inserir(Obj.Func,
		"main", getTipo("void", false));
		ts.abrirEscopo("Func main");
		objCode = new Code();
		objCode.setMainPC();
		
		Expect(17);
		ofuncAtual.end = objCode.getPC();
		              objCode.put(objCode.enter);
		              objCode.put(ofuncAtual.nPars);
		              int pcvars = objCode.getPC();
		              objCode.put(0);
		           
		instrucoes();
		objCode.put(pcvars, ts.escopoAtual.nVars);
		Expect(18);
		ofuncAtual.locais = ts.escopoAtual.locais;
		ts.fecharEscopo();
		objCode.put(objCode.exit);
		objCode.put(objCode.return_);
		
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
			debug(tip + " eh Vetor");
		}
		ntipo = new NTipo(tip, vet); 
		return ntipo;
	}

	void parametros() {
		NTipo ntipo; 
		Expect(1);
		Expect(7);
		ntipo = tipo();
		while (la.kind == 6) {
			Get();
			Expect(1);
			Expect(7);
			ntipo = tipo();
		}
	}

	void parametros_passados(Operand op) {
		Operand passados; 
		Expect(14);
		if(op.cat != Operand.Func){
		erro("nao eh uma funcao ");
		op.obj = ts.semObj;
		}
		int preais = 0;
		int pdecl = op.obj.nPars;
		Obj fp = op.obj.locais;
		
		if (StartOf(1)) {
			passados = expressao();
			objCode.load(passados);
			preais++;
			if( fp != null){
			if(!passados.tipo.assinalavelPara(fp.tipo))
			erro("Tipo de paramentro incompativel");
			fp = fp.prox;
			}
			
			while (la.kind == 6) {
				Get();
				passados = expressao();
				objCode.load(passados);
				preais++;
				if (fp != null){
				if(!passados.tipo.assinalavelPara(fp.tipo))
				erro("Tipo de paramentro incompativel");
				}											
				
			}
		}
		if (preais > pdecl)
		erro("quantidade de paramentros passados maior do que os declarados");
		else if(preais < pdecl)
		erro("quantidade de paramentros passados menor do que os declarados");
		
		Expect(15);
	}

	Operand  expressao() {
		Operand  op;
		Operand op2; int operador; op = null; 
		op = termo();
		if(op.tipo != ts.tipoInt)
		erro("Operando de tipo nao-inteiro");
		
		while (la.kind == 44 || la.kind == 45) {
			if (la.kind == 44) {
				Get();
				operador = objCode.add; 
			} else {
				Get();
				operador = objCode.sub; 
			}
			objCode.load(op); 
			op2 = termo();
			objCode.load(op2); 
			if(op.tipo != ts.tipoInt || op2.tipo != ts.tipoInt)
			erro("Operando de tipo nao-inteiro");
			objCode.put(operador);
			
		}
		return op;
	}

	void instrucoes() {
		while (StartOf(2)) {
			comando();
			debug("comando");
		}
	}

	void retorno() {
		Expect(19);
		if (StartOf(1)) {
			op = expressao();
		}
		Expect(8);
	}

	void comando() {
		Operand op = null; 
		switch (la.kind) {
		case 5: {
			declaracao_var();
			debug("dec_var");
			objCode.setDataSize(ts.escopoAtual.nVars);
			break;
		}
		case 20: case 25: case 33: {
			repeticao_def();
			debug("rep");
			break;
		}
		case 34: case 38: {
			condicional_def();
			debug("cond");
			break;
		}
		case 42: case 43: {
			io_def();
			debug("io");
			break;
		}
		case 1: {
			op = designador();
			debug("desig");
			if (la.kind == 13) {
				assinalamento(op);
				debug("assina");
			} else if (la.kind == 14) {
				parametros_passados(op);
				debug("param");
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
		if (la.kind == 20) {
			para_def();
		} else if (la.kind == 25) {
			enquanto_def();
		} else if (la.kind == 33) {
			repita_def();
		} else SynErr(55);
	}

	void condicional_def() {
		if (la.kind == 34) {
			se_def();
		} else if (la.kind == 38) {
			caso_def();
		} else SynErr(56);
	}

	void io_def() {
		if (la.kind == 43) {
			escreva_def();
		} else if (la.kind == 42) {
			leia_def();
		} else SynErr(57);
	}

	Operand  designador() {
		Operand  op;
		String nome; Operand indice = null; 
		Expect(1);
		nome = t.val;
		op  = new Operand(ts.buscar(nome));
		
		if (la.kind == 10) {
			Get();
			objCode.load(op); 
			indice = expressao();
			if(op.tipo.cat == Struct.Vetor){
			if(indice.tipo.cat != Struct.Int)
				erro("O indice deve ser um valor inteiro");
			objCode.load(indice);
			op.cat = Operand.Elem;
			op.tipo =  op.tipo.tipoElemento;
			}else{
				erro("A variavel "+ nome + " nao eh um vetor");
			}
					
			Expect(11);
			debug("]");
		}
		return op;
	}

	void assinalamento(Operand op1) {
		Operand op2 = null; 
		Expect(13);
		op2 = expressao();
		if(op2.tipo.assinalavelPara(op1.tipo))
		objCode.assign(op1, op2);
		else
		erro("tipos incompativeis");
		
	}

	void para_def() {
		Expect(20);
		Expect(1);
		Expect(13);
		op = expressao();
		Expect(21);
		op = expressao();
		if (la.kind == 22) {
			Get();
			op = expressao();
		}
		Expect(23);
		instrucoes();
		Expect(24);
		if (la.kind == 8) {
			Get();
		}
	}

	void enquanto_def() {
		Expect(25);
		condicao();
		Expect(23);
		instrucoes();
		Expect(26);
		if (la.kind == 8) {
			Get();
		}
	}

	void repita_def() {
		Expect(33);
		instrucoes();
		Expect(21);
		condicao();
		Expect(8);
	}

	void condicao() {
		op = expressao();
		operador_relacional();
		op = expressao();
	}

	void operador_relacional() {
		switch (la.kind) {
		case 27: {
			Get();
			break;
		}
		case 28: {
			Get();
			break;
		}
		case 29: {
			Get();
			break;
		}
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
		default: SynErr(58); break;
		}
	}

	void se_def() {
		Expect(34);
		condicao();
		Expect(35);
		instrucoes();
		if (la.kind == 36) {
			Get();
			instrucoes();
		}
		Expect(37);
		if (la.kind == 8) {
			Get();
		}
	}

	void caso_def() {
		Expect(38);
		op = designador();
		caso_seja_def();
		while (la.kind == 41) {
			caso_seja_def();
		}
		Expect(39);
		Expect(7);
		instrucoes();
		Expect(40);
	}

	void caso_seja_def() {
		Expect(41);
		op = expressao();
		Expect(23);
		instrucoes();
	}

	void escreva_def() {
		Expect(43);
		debug("escreva");
		Expect(14);
		if (la.kind == 2) {
			Get();
		} else if (la.kind == 1) {
			op = designador();
			if (la.kind == 14) {
				parametros_passados(op);
			}
		} else SynErr(59);
		while (la.kind == 6) {
			Get();
			if (la.kind == 2) {
				Get();
			} else if (la.kind == 1) {
				op = designador();
				if (la.kind == 14) {
					parametros_passados(op);
				}
			} else SynErr(60);
		}
		Expect(15);
		Expect(8);
	}

	void leia_def() {
		Expect(42);
		debug("leia");
		Expect(14);
		op = designador();
		Expect(15);
		Expect(8);
	}

	Operand  termo() {
		Operand  op;
		Operand op2; int operador; op = null;
		op = fator();
		while (la.kind == 46 || la.kind == 47 || la.kind == 48) {
			if (la.kind == 46) {
				Get();
				operador = objCode.mul; 
			} else if (la.kind == 47) {
				Get();
				operador = objCode.div; 
			} else {
				Get();
				operador = objCode.rem; 
			}
			objCode.load(op); 
			op2 = fator();
			objCode.load(op2); 
			if(op.tipo != ts.tipoInt || op2.tipo != ts.tipoInt)
			erro("Operando de tipo nao-inteiro");
			objCode.put(operador);
			
		}
		return op;
	}

	Operand  fator() {
		Operand  op;
		op=null; 
		if (la.kind == 3) {
			Get();
			op = new Operand(toInteger(t.val)); 
		} else if (la.kind == 1) {
			op = designador();
			if (la.kind == 14) {
				parametros_passados(op);
				if (op.tipo == ts.semTipo)
				erro("funÃ§Ã£o de tipo void usada em expressÃ£o");
				if (op.obj == ts.objTamVetor)
				objCode.put(objCode.arraylength);
				else {
				objCode.put(objCode.call);
				objCode.put2(op.end);
				}
				op.cat = Operand.Stack;
											
			}
		} else if (la.kind == 14) {
			Get();
			op = expressao();
			Expect(15);
		} else if (la.kind == 49) {
			op = aloc_vetor();
		} else SynErr(61);
		return op;
	}

	Operand  aloc_vetor() {
		Operand  op;
		Expect(49);
		op = null; List<Integer> lista = null; NTipo ntipo; 
		Expect(9);
		Obj obj = ts.buscar("inteiro"); 
		if (la.kind == 10) {
			Get();
			op = expressao();
			if( op.tipo != ts.tipoInt)
			erro("A quantidade de elementos do vetor deve ser uma variavel de tipo inteira");
			
			Expect(11);
		} else if (la.kind == 50) {
			Get();
			Expect(3);
			lista.add(toInteger(t.val)); 
			while (la.kind == 6) {
				Get();
				Expect(3);
				lista.add(toInteger(t.val)); 
			}
			Expect(51);
			op = new Operand(lista.size()); 
		} else SynErr(62);
		op.tipo = new Struct(Struct.Vetor, obj.tipo); 
		return op;
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
		{x,T,x,T, x,x,x,x, x,x,x,x, x,x,T,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,T,x,x, x,x},
		{x,T,x,x, x,T,x,x, x,x,x,x, x,x,x,x, x,x,x,T, T,x,x,x, x,T,x,x, x,x,x,x, x,T,T,x, x,x,T,x, x,x,T,T, x,x,x,x, x,x,x,x, x,x}

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
			case 20: s = "\"para\" expected"; break;
			case 21: s = "\"ate\" expected"; break;
			case 22: s = "\"passo\" expected"; break;
			case 23: s = "\"faca\" expected"; break;
			case 24: s = "\"fimpara\" expected"; break;
			case 25: s = "\"enquanto\" expected"; break;
			case 26: s = "\"fimenquanto\" expected"; break;
			case 27: s = "\"!=\" expected"; break;
			case 28: s = "\">\" expected"; break;
			case 29: s = "\"<\" expected"; break;
			case 30: s = "\">=\" expected"; break;
			case 31: s = "\"<=\" expected"; break;
			case 32: s = "\"==\" expected"; break;
			case 33: s = "\"repita\" expected"; break;
			case 34: s = "\"se\" expected"; break;
			case 35: s = "\"entao\" expected"; break;
			case 36: s = "\"senao\" expected"; break;
			case 37: s = "\"fimse\" expected"; break;
			case 38: s = "\"caso\" expected"; break;
			case 39: s = "\"outrocaso\" expected"; break;
			case 40: s = "\"fimcaso\" expected"; break;
			case 41: s = "\"seja\" expected"; break;
			case 42: s = "\"leia\" expected"; break;
			case 43: s = "\"escreva\" expected"; break;
			case 44: s = "\"+\" expected"; break;
			case 45: s = "\"-\" expected"; break;
			case 46: s = "\"*\" expected"; break;
			case 47: s = "\"/\" expected"; break;
			case 48: s = "\"%\" expected"; break;
			case 49: s = "\"novo\" expected"; break;
			case 50: s = "\"{\" expected"; break;
			case 51: s = "\"}\" expected"; break;
			case 52: s = "??? expected"; break;
			case 53: s = "invalid comando"; break;
			case 54: s = "invalid comando"; break;
			case 55: s = "invalid repeticao_def"; break;
			case 56: s = "invalid condicional_def"; break;
			case 57: s = "invalid io_def"; break;
			case 58: s = "invalid operador_relacional"; break;
			case 59: s = "invalid escreva_def"; break;
			case 60: s = "invalid escreva_def"; break;
			case 61: s = "invalid fator"; break;
			case 62: s = "invalid aloc_vetor"; break;
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
