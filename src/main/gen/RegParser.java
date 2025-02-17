// Generated from /Users/francescomarastoni/Desktop/Java.nosync/reg-lisa/src/main/antlr/RegParser.g4 by ANTLR 4.13.2

    package it.unipr.reg.antlr;

import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue", "this-escape"})
public class RegParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.13.2", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		SEQ=1, NON_DET_CHOICE=2, COND=3, SHKIP=4, NUM=5, ID=6, ASSIGN=7, PLUS=8, 
		MINUS=9, TIMES=10, EQ=11, LEQ=12, LE=13, TRUE=14, FALSE=15, AND=16, NOT=17, 
		LPAR=18, RPAR=19, WS=20;
	public static final int
		RULE_program = 0, RULE_e = 1, RULE_a = 2, RULE_b = 3;
	private static String[] makeRuleNames() {
		return new String[] {
			"program", "e", "a", "b"
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

	@Override
	public String getGrammarFileName() { return "RegParser.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public RegParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ProgramContext extends ParserRuleContext {
		public List<EContext> e() {
			return getRuleContexts(EContext.class);
		}
		public EContext e(int i) {
			return getRuleContext(EContext.class,i);
		}
		public TerminalNode EOF() { return getToken(RegParser.EOF, 0); }
		public List<TerminalNode> SEQ() { return getTokens(RegParser.SEQ); }
		public TerminalNode SEQ(int i) {
			return getToken(RegParser.SEQ, i);
		}
		public List<TerminalNode> NON_DET_CHOICE() { return getTokens(RegParser.NON_DET_CHOICE); }
		public TerminalNode NON_DET_CHOICE(int i) {
			return getToken(RegParser.NON_DET_CHOICE, i);
		}
		public ProgramContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_program; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RegParserListener ) ((RegParserListener)listener).enterProgram(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RegParserListener ) ((RegParserListener)listener).exitProgram(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RegParserVisitor ) return ((RegParserVisitor<? extends T>)visitor).visitProgram(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ProgramContext program() throws RecognitionException {
		ProgramContext _localctx = new ProgramContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_program);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(8);
			e(0);
			setState(13);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==SEQ || _la==NON_DET_CHOICE) {
				{
				{
				setState(9);
				_la = _input.LA(1);
				if ( !(_la==SEQ || _la==NON_DET_CHOICE) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(10);
				e(0);
				}
				}
				setState(15);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(16);
			match(EOF);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class EContext extends ParserRuleContext {
		public TerminalNode LPAR() { return getToken(RegParser.LPAR, 0); }
		public List<EContext> e() {
			return getRuleContexts(EContext.class);
		}
		public EContext e(int i) {
			return getRuleContext(EContext.class,i);
		}
		public TerminalNode RPAR() { return getToken(RegParser.RPAR, 0); }
		public TerminalNode SHKIP() { return getToken(RegParser.SHKIP, 0); }
		public TerminalNode ID() { return getToken(RegParser.ID, 0); }
		public TerminalNode ASSIGN() { return getToken(RegParser.ASSIGN, 0); }
		public AContext a() {
			return getRuleContext(AContext.class,0);
		}
		public BContext b() {
			return getRuleContext(BContext.class,0);
		}
		public TerminalNode COND() { return getToken(RegParser.COND, 0); }
		public TerminalNode SEQ() { return getToken(RegParser.SEQ, 0); }
		public TerminalNode NON_DET_CHOICE() { return getToken(RegParser.NON_DET_CHOICE, 0); }
		public TerminalNode TIMES() { return getToken(RegParser.TIMES, 0); }
		public EContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_e; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RegParserListener ) ((RegParserListener)listener).enterE(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RegParserListener ) ((RegParserListener)listener).exitE(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RegParserVisitor ) return ((RegParserVisitor<? extends T>)visitor).visitE(this);
			else return visitor.visitChildren(this);
		}
	}

	public final EContext e() throws RecognitionException {
		return e(0);
	}

	private EContext e(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		EContext _localctx = new EContext(_ctx, _parentState);
		EContext _prevctx = _localctx;
		int _startState = 2;
		enterRecursionRule(_localctx, 2, RULE_e, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(30);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,1,_ctx) ) {
			case 1:
				{
				setState(19);
				match(LPAR);
				setState(20);
				e(0);
				setState(21);
				match(RPAR);
				}
				break;
			case 2:
				{
				setState(23);
				match(SHKIP);
				}
				break;
			case 3:
				{
				setState(24);
				match(ID);
				setState(25);
				match(ASSIGN);
				setState(26);
				a(0);
				}
				break;
			case 4:
				{
				setState(27);
				b(0);
				setState(28);
				match(COND);
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(39);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,3,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(37);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,2,_ctx) ) {
					case 1:
						{
						_localctx = new EContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_e);
						setState(32);
						if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
						setState(33);
						_la = _input.LA(1);
						if ( !(_la==SEQ || _la==NON_DET_CHOICE) ) {
						_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(34);
						e(3);
						}
						break;
					case 2:
						{
						_localctx = new EContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_e);
						setState(35);
						if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
						setState(36);
						match(TIMES);
						}
						break;
					}
					} 
				}
				setState(41);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,3,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class AContext extends ParserRuleContext {
		public TerminalNode LPAR() { return getToken(RegParser.LPAR, 0); }
		public List<AContext> a() {
			return getRuleContexts(AContext.class);
		}
		public AContext a(int i) {
			return getRuleContext(AContext.class,i);
		}
		public TerminalNode RPAR() { return getToken(RegParser.RPAR, 0); }
		public TerminalNode ID() { return getToken(RegParser.ID, 0); }
		public TerminalNode NUM() { return getToken(RegParser.NUM, 0); }
		public TerminalNode PLUS() { return getToken(RegParser.PLUS, 0); }
		public TerminalNode MINUS() { return getToken(RegParser.MINUS, 0); }
		public TerminalNode TIMES() { return getToken(RegParser.TIMES, 0); }
		public AContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_a; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RegParserListener ) ((RegParserListener)listener).enterA(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RegParserListener ) ((RegParserListener)listener).exitA(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RegParserVisitor ) return ((RegParserVisitor<? extends T>)visitor).visitA(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AContext a() throws RecognitionException {
		return a(0);
	}

	private AContext a(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		AContext _localctx = new AContext(_ctx, _parentState);
		AContext _prevctx = _localctx;
		int _startState = 4;
		enterRecursionRule(_localctx, 4, RULE_a, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(49);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case LPAR:
				{
				setState(43);
				match(LPAR);
				setState(44);
				a(0);
				setState(45);
				match(RPAR);
				}
				break;
			case ID:
				{
				setState(47);
				match(ID);
				}
				break;
			case NUM:
				{
				setState(48);
				match(NUM);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			_ctx.stop = _input.LT(-1);
			setState(62);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,6,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(60);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,5,_ctx) ) {
					case 1:
						{
						_localctx = new AContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_a);
						setState(51);
						if (!(precpred(_ctx, 3))) throw new FailedPredicateException(this, "precpred(_ctx, 3)");
						setState(52);
						match(PLUS);
						setState(53);
						a(4);
						}
						break;
					case 2:
						{
						_localctx = new AContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_a);
						setState(54);
						if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
						setState(55);
						match(MINUS);
						setState(56);
						a(3);
						}
						break;
					case 3:
						{
						_localctx = new AContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_a);
						setState(57);
						if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
						setState(58);
						match(TIMES);
						setState(59);
						a(2);
						}
						break;
					}
					} 
				}
				setState(64);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,6,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class BContext extends ParserRuleContext {
		public TerminalNode LPAR() { return getToken(RegParser.LPAR, 0); }
		public List<BContext> b() {
			return getRuleContexts(BContext.class);
		}
		public BContext b(int i) {
			return getRuleContext(BContext.class,i);
		}
		public TerminalNode RPAR() { return getToken(RegParser.RPAR, 0); }
		public TerminalNode TRUE() { return getToken(RegParser.TRUE, 0); }
		public TerminalNode FALSE() { return getToken(RegParser.FALSE, 0); }
		public List<AContext> a() {
			return getRuleContexts(AContext.class);
		}
		public AContext a(int i) {
			return getRuleContext(AContext.class,i);
		}
		public TerminalNode EQ() { return getToken(RegParser.EQ, 0); }
		public TerminalNode LEQ() { return getToken(RegParser.LEQ, 0); }
		public TerminalNode LE() { return getToken(RegParser.LE, 0); }
		public TerminalNode NOT() { return getToken(RegParser.NOT, 0); }
		public TerminalNode AND() { return getToken(RegParser.AND, 0); }
		public BContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_b; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RegParserListener ) ((RegParserListener)listener).enterB(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RegParserListener ) ((RegParserListener)listener).exitB(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RegParserVisitor ) return ((RegParserVisitor<? extends T>)visitor).visitB(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BContext b() throws RecognitionException {
		return b(0);
	}

	private BContext b(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		BContext _localctx = new BContext(_ctx, _parentState);
		BContext _prevctx = _localctx;
		int _startState = 6;
		enterRecursionRule(_localctx, 6, RULE_b, _p);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(86);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,7,_ctx) ) {
			case 1:
				{
				setState(66);
				match(LPAR);
				setState(67);
				b(0);
				setState(68);
				match(RPAR);
				}
				break;
			case 2:
				{
				setState(70);
				match(TRUE);
				}
				break;
			case 3:
				{
				setState(71);
				match(FALSE);
				}
				break;
			case 4:
				{
				setState(72);
				a(0);
				setState(73);
				match(EQ);
				setState(74);
				a(0);
				}
				break;
			case 5:
				{
				setState(76);
				a(0);
				setState(77);
				match(LEQ);
				setState(78);
				a(0);
				}
				break;
			case 6:
				{
				setState(80);
				a(0);
				setState(81);
				match(LE);
				setState(82);
				a(0);
				}
				break;
			case 7:
				{
				setState(84);
				match(NOT);
				setState(85);
				b(1);
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(93);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,8,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					{
					_localctx = new BContext(_parentctx, _parentState);
					pushNewRecursionContext(_localctx, _startState, RULE_b);
					setState(88);
					if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
					setState(89);
					match(AND);
					setState(90);
					b(3);
					}
					} 
				}
				setState(95);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,8,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
		switch (ruleIndex) {
		case 1:
			return e_sempred((EContext)_localctx, predIndex);
		case 2:
			return a_sempred((AContext)_localctx, predIndex);
		case 3:
			return b_sempred((BContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean e_sempred(EContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0:
			return precpred(_ctx, 2);
		case 1:
			return precpred(_ctx, 1);
		}
		return true;
	}
	private boolean a_sempred(AContext _localctx, int predIndex) {
		switch (predIndex) {
		case 2:
			return precpred(_ctx, 3);
		case 3:
			return precpred(_ctx, 2);
		case 4:
			return precpred(_ctx, 1);
		}
		return true;
	}
	private boolean b_sempred(BContext _localctx, int predIndex) {
		switch (predIndex) {
		case 5:
			return precpred(_ctx, 2);
		}
		return true;
	}

	public static final String _serializedATN =
		"\u0004\u0001\u0014a\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002"+
		"\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0001\u0000\u0001\u0000\u0001"+
		"\u0000\u0005\u0000\f\b\u0000\n\u0000\f\u0000\u000f\t\u0000\u0001\u0000"+
		"\u0001\u0000\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0003\u0001\u001f\b\u0001\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0005\u0001&\b\u0001\n\u0001\f\u0001)\t\u0001"+
		"\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002"+
		"\u0001\u0002\u0003\u00022\b\u0002\u0001\u0002\u0001\u0002\u0001\u0002"+
		"\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002"+
		"\u0005\u0002=\b\u0002\n\u0002\f\u0002@\t\u0002\u0001\u0003\u0001\u0003"+
		"\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003"+
		"\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003"+
		"\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003"+
		"\u0001\u0003\u0003\u0003W\b\u0003\u0001\u0003\u0001\u0003\u0001\u0003"+
		"\u0005\u0003\\\b\u0003\n\u0003\f\u0003_\t\u0003\u0001\u0003\u0000\u0003"+
		"\u0002\u0004\u0006\u0004\u0000\u0002\u0004\u0006\u0000\u0001\u0001\u0000"+
		"\u0001\u0002n\u0000\b\u0001\u0000\u0000\u0000\u0002\u001e\u0001\u0000"+
		"\u0000\u0000\u00041\u0001\u0000\u0000\u0000\u0006V\u0001\u0000\u0000\u0000"+
		"\b\r\u0003\u0002\u0001\u0000\t\n\u0007\u0000\u0000\u0000\n\f\u0003\u0002"+
		"\u0001\u0000\u000b\t\u0001\u0000\u0000\u0000\f\u000f\u0001\u0000\u0000"+
		"\u0000\r\u000b\u0001\u0000\u0000\u0000\r\u000e\u0001\u0000\u0000\u0000"+
		"\u000e\u0010\u0001\u0000\u0000\u0000\u000f\r\u0001\u0000\u0000\u0000\u0010"+
		"\u0011\u0005\u0000\u0000\u0001\u0011\u0001\u0001\u0000\u0000\u0000\u0012"+
		"\u0013\u0006\u0001\uffff\uffff\u0000\u0013\u0014\u0005\u0012\u0000\u0000"+
		"\u0014\u0015\u0003\u0002\u0001\u0000\u0015\u0016\u0005\u0013\u0000\u0000"+
		"\u0016\u001f\u0001\u0000\u0000\u0000\u0017\u001f\u0005\u0004\u0000\u0000"+
		"\u0018\u0019\u0005\u0006\u0000\u0000\u0019\u001a\u0005\u0007\u0000\u0000"+
		"\u001a\u001f\u0003\u0004\u0002\u0000\u001b\u001c\u0003\u0006\u0003\u0000"+
		"\u001c\u001d\u0005\u0003\u0000\u0000\u001d\u001f\u0001\u0000\u0000\u0000"+
		"\u001e\u0012\u0001\u0000\u0000\u0000\u001e\u0017\u0001\u0000\u0000\u0000"+
		"\u001e\u0018\u0001\u0000\u0000\u0000\u001e\u001b\u0001\u0000\u0000\u0000"+
		"\u001f\'\u0001\u0000\u0000\u0000 !\n\u0002\u0000\u0000!\"\u0007\u0000"+
		"\u0000\u0000\"&\u0003\u0002\u0001\u0003#$\n\u0001\u0000\u0000$&\u0005"+
		"\n\u0000\u0000% \u0001\u0000\u0000\u0000%#\u0001\u0000\u0000\u0000&)\u0001"+
		"\u0000\u0000\u0000\'%\u0001\u0000\u0000\u0000\'(\u0001\u0000\u0000\u0000"+
		"(\u0003\u0001\u0000\u0000\u0000)\'\u0001\u0000\u0000\u0000*+\u0006\u0002"+
		"\uffff\uffff\u0000+,\u0005\u0012\u0000\u0000,-\u0003\u0004\u0002\u0000"+
		"-.\u0005\u0013\u0000\u0000.2\u0001\u0000\u0000\u0000/2\u0005\u0006\u0000"+
		"\u000002\u0005\u0005\u0000\u00001*\u0001\u0000\u0000\u00001/\u0001\u0000"+
		"\u0000\u000010\u0001\u0000\u0000\u00002>\u0001\u0000\u0000\u000034\n\u0003"+
		"\u0000\u000045\u0005\b\u0000\u00005=\u0003\u0004\u0002\u000467\n\u0002"+
		"\u0000\u000078\u0005\t\u0000\u00008=\u0003\u0004\u0002\u00039:\n\u0001"+
		"\u0000\u0000:;\u0005\n\u0000\u0000;=\u0003\u0004\u0002\u0002<3\u0001\u0000"+
		"\u0000\u0000<6\u0001\u0000\u0000\u0000<9\u0001\u0000\u0000\u0000=@\u0001"+
		"\u0000\u0000\u0000><\u0001\u0000\u0000\u0000>?\u0001\u0000\u0000\u0000"+
		"?\u0005\u0001\u0000\u0000\u0000@>\u0001\u0000\u0000\u0000AB\u0006\u0003"+
		"\uffff\uffff\u0000BC\u0005\u0012\u0000\u0000CD\u0003\u0006\u0003\u0000"+
		"DE\u0005\u0013\u0000\u0000EW\u0001\u0000\u0000\u0000FW\u0005\u000e\u0000"+
		"\u0000GW\u0005\u000f\u0000\u0000HI\u0003\u0004\u0002\u0000IJ\u0005\u000b"+
		"\u0000\u0000JK\u0003\u0004\u0002\u0000KW\u0001\u0000\u0000\u0000LM\u0003"+
		"\u0004\u0002\u0000MN\u0005\f\u0000\u0000NO\u0003\u0004\u0002\u0000OW\u0001"+
		"\u0000\u0000\u0000PQ\u0003\u0004\u0002\u0000QR\u0005\r\u0000\u0000RS\u0003"+
		"\u0004\u0002\u0000SW\u0001\u0000\u0000\u0000TU\u0005\u0011\u0000\u0000"+
		"UW\u0003\u0006\u0003\u0001VA\u0001\u0000\u0000\u0000VF\u0001\u0000\u0000"+
		"\u0000VG\u0001\u0000\u0000\u0000VH\u0001\u0000\u0000\u0000VL\u0001\u0000"+
		"\u0000\u0000VP\u0001\u0000\u0000\u0000VT\u0001\u0000\u0000\u0000W]\u0001"+
		"\u0000\u0000\u0000XY\n\u0002\u0000\u0000YZ\u0005\u0010\u0000\u0000Z\\"+
		"\u0003\u0006\u0003\u0003[X\u0001\u0000\u0000\u0000\\_\u0001\u0000\u0000"+
		"\u0000][\u0001\u0000\u0000\u0000]^\u0001\u0000\u0000\u0000^\u0007\u0001"+
		"\u0000\u0000\u0000_]\u0001\u0000\u0000\u0000\t\r\u001e%\'1<>V]";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}