// Generated from /Users/francescomarastoni/Desktop/Java.nosync/reg-lisa/src/main/antlr/RegLexer.g4 by ANTLR 4.13.2

    package it.unipr.reg.antlr;

import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue", "this-escape"})
public class RegLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.13.2", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		SEQ=1, NON_DET_CHOICE=2, COND=3, SHKIP=4, NUM=5, ID=6, ASSIGN=7, PLUS=8, 
		MINUS=9, TIMES=10, EQ=11, LEQ=12, LE=13, TRUE=14, FALSE=15, AND=16, NOT=17, 
		LPAR=18, RPAR=19, WS=20;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"SEQ", "NON_DET_CHOICE", "COND", "SHKIP", "NUM", "ID", "ASSIGN", "PLUS", 
			"MINUS", "TIMES", "EQ", "LEQ", "LE", "TRUE", "FALSE", "AND", "NOT", "LPAR", 
			"RPAR", "WS"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "';'", "'|'", "'?'", "'skip'", null, null, "':='", "'+'", "'-'", 
			"'*'", "'='", "'<='", "'<'", "'tt'", "'ff'", "'&'", "'!'", "'('", "')'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "SEQ", "NON_DET_CHOICE", "COND", "SHKIP", "NUM", "ID", "ASSIGN", 
			"PLUS", "MINUS", "TIMES", "EQ", "LEQ", "LE", "TRUE", "FALSE", "AND", 
			"NOT", "LPAR", "RPAR", "WS"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}


	public RegLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "RegLexer.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getChannelNames() { return channelNames; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\u0004\u0000\u0014`\u0006\uffff\uffff\u0002\u0000\u0007\u0000\u0002\u0001"+
		"\u0007\u0001\u0002\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004"+
		"\u0007\u0004\u0002\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007"+
		"\u0007\u0007\u0002\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002\u000b"+
		"\u0007\u000b\u0002\f\u0007\f\u0002\r\u0007\r\u0002\u000e\u0007\u000e\u0002"+
		"\u000f\u0007\u000f\u0002\u0010\u0007\u0010\u0002\u0011\u0007\u0011\u0002"+
		"\u0012\u0007\u0012\u0002\u0013\u0007\u0013\u0001\u0000\u0001\u0000\u0001"+
		"\u0001\u0001\u0001\u0001\u0002\u0001\u0002\u0001\u0003\u0001\u0003\u0001"+
		"\u0003\u0001\u0003\u0001\u0003\u0001\u0004\u0004\u00046\b\u0004\u000b"+
		"\u0004\f\u00047\u0001\u0005\u0004\u0005;\b\u0005\u000b\u0005\f\u0005<"+
		"\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0007\u0001\u0007\u0001\b\u0001"+
		"\b\u0001\t\u0001\t\u0001\n\u0001\n\u0001\u000b\u0001\u000b\u0001\u000b"+
		"\u0001\f\u0001\f\u0001\r\u0001\r\u0001\r\u0001\u000e\u0001\u000e\u0001"+
		"\u000e\u0001\u000f\u0001\u000f\u0001\u0010\u0001\u0010\u0001\u0011\u0001"+
		"\u0011\u0001\u0012\u0001\u0012\u0001\u0013\u0001\u0013\u0001\u0013\u0001"+
		"\u0013\u0000\u0000\u0014\u0001\u0001\u0003\u0002\u0005\u0003\u0007\u0004"+
		"\t\u0005\u000b\u0006\r\u0007\u000f\b\u0011\t\u0013\n\u0015\u000b\u0017"+
		"\f\u0019\r\u001b\u000e\u001d\u000f\u001f\u0010!\u0011#\u0012%\u0013\'"+
		"\u0014\u0001\u0000\u0003\u0001\u000009\u0003\u000009AZaz\u0003\u0000\t"+
		"\n\r\r  a\u0000\u0001\u0001\u0000\u0000\u0000\u0000\u0003\u0001\u0000"+
		"\u0000\u0000\u0000\u0005\u0001\u0000\u0000\u0000\u0000\u0007\u0001\u0000"+
		"\u0000\u0000\u0000\t\u0001\u0000\u0000\u0000\u0000\u000b\u0001\u0000\u0000"+
		"\u0000\u0000\r\u0001\u0000\u0000\u0000\u0000\u000f\u0001\u0000\u0000\u0000"+
		"\u0000\u0011\u0001\u0000\u0000\u0000\u0000\u0013\u0001\u0000\u0000\u0000"+
		"\u0000\u0015\u0001\u0000\u0000\u0000\u0000\u0017\u0001\u0000\u0000\u0000"+
		"\u0000\u0019\u0001\u0000\u0000\u0000\u0000\u001b\u0001\u0000\u0000\u0000"+
		"\u0000\u001d\u0001\u0000\u0000\u0000\u0000\u001f\u0001\u0000\u0000\u0000"+
		"\u0000!\u0001\u0000\u0000\u0000\u0000#\u0001\u0000\u0000\u0000\u0000%"+
		"\u0001\u0000\u0000\u0000\u0000\'\u0001\u0000\u0000\u0000\u0001)\u0001"+
		"\u0000\u0000\u0000\u0003+\u0001\u0000\u0000\u0000\u0005-\u0001\u0000\u0000"+
		"\u0000\u0007/\u0001\u0000\u0000\u0000\t5\u0001\u0000\u0000\u0000\u000b"+
		":\u0001\u0000\u0000\u0000\r>\u0001\u0000\u0000\u0000\u000fA\u0001\u0000"+
		"\u0000\u0000\u0011C\u0001\u0000\u0000\u0000\u0013E\u0001\u0000\u0000\u0000"+
		"\u0015G\u0001\u0000\u0000\u0000\u0017I\u0001\u0000\u0000\u0000\u0019L"+
		"\u0001\u0000\u0000\u0000\u001bN\u0001\u0000\u0000\u0000\u001dQ\u0001\u0000"+
		"\u0000\u0000\u001fT\u0001\u0000\u0000\u0000!V\u0001\u0000\u0000\u0000"+
		"#X\u0001\u0000\u0000\u0000%Z\u0001\u0000\u0000\u0000\'\\\u0001\u0000\u0000"+
		"\u0000)*\u0005;\u0000\u0000*\u0002\u0001\u0000\u0000\u0000+,\u0005|\u0000"+
		"\u0000,\u0004\u0001\u0000\u0000\u0000-.\u0005?\u0000\u0000.\u0006\u0001"+
		"\u0000\u0000\u0000/0\u0005s\u0000\u000001\u0005k\u0000\u000012\u0005i"+
		"\u0000\u000023\u0005p\u0000\u00003\b\u0001\u0000\u0000\u000046\u0007\u0000"+
		"\u0000\u000054\u0001\u0000\u0000\u000067\u0001\u0000\u0000\u000075\u0001"+
		"\u0000\u0000\u000078\u0001\u0000\u0000\u00008\n\u0001\u0000\u0000\u0000"+
		"9;\u0007\u0001\u0000\u0000:9\u0001\u0000\u0000\u0000;<\u0001\u0000\u0000"+
		"\u0000<:\u0001\u0000\u0000\u0000<=\u0001\u0000\u0000\u0000=\f\u0001\u0000"+
		"\u0000\u0000>?\u0005:\u0000\u0000?@\u0005=\u0000\u0000@\u000e\u0001\u0000"+
		"\u0000\u0000AB\u0005+\u0000\u0000B\u0010\u0001\u0000\u0000\u0000CD\u0005"+
		"-\u0000\u0000D\u0012\u0001\u0000\u0000\u0000EF\u0005*\u0000\u0000F\u0014"+
		"\u0001\u0000\u0000\u0000GH\u0005=\u0000\u0000H\u0016\u0001\u0000\u0000"+
		"\u0000IJ\u0005<\u0000\u0000JK\u0005=\u0000\u0000K\u0018\u0001\u0000\u0000"+
		"\u0000LM\u0005<\u0000\u0000M\u001a\u0001\u0000\u0000\u0000NO\u0005t\u0000"+
		"\u0000OP\u0005t\u0000\u0000P\u001c\u0001\u0000\u0000\u0000QR\u0005f\u0000"+
		"\u0000RS\u0005f\u0000\u0000S\u001e\u0001\u0000\u0000\u0000TU\u0005&\u0000"+
		"\u0000U \u0001\u0000\u0000\u0000VW\u0005!\u0000\u0000W\"\u0001\u0000\u0000"+
		"\u0000XY\u0005(\u0000\u0000Y$\u0001\u0000\u0000\u0000Z[\u0005)\u0000\u0000"+
		"[&\u0001\u0000\u0000\u0000\\]\u0007\u0002\u0000\u0000]^\u0001\u0000\u0000"+
		"\u0000^_\u0006\u0013\u0000\u0000_(\u0001\u0000\u0000\u0000\u0003\u0000"+
		"7<\u0001\u0006\u0000\u0000";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}