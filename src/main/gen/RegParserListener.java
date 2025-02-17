// Generated from /Users/francescomarastoni/Desktop/Java.nosync/reg-lisa/src/main/antlr/RegParser.g4 by ANTLR 4.13.2

    package it.unipr.reg.antlr;

import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link RegParser}.
 */
public interface RegParserListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link RegParser#program}.
	 * @param ctx the parse tree
	 */
	void enterProgram(RegParser.ProgramContext ctx);
	/**
	 * Exit a parse tree produced by {@link RegParser#program}.
	 * @param ctx the parse tree
	 */
	void exitProgram(RegParser.ProgramContext ctx);
	/**
	 * Enter a parse tree produced by {@link RegParser#e}.
	 * @param ctx the parse tree
	 */
	void enterE(RegParser.EContext ctx);
	/**
	 * Exit a parse tree produced by {@link RegParser#e}.
	 * @param ctx the parse tree
	 */
	void exitE(RegParser.EContext ctx);
	/**
	 * Enter a parse tree produced by {@link RegParser#a}.
	 * @param ctx the parse tree
	 */
	void enterA(RegParser.AContext ctx);
	/**
	 * Exit a parse tree produced by {@link RegParser#a}.
	 * @param ctx the parse tree
	 */
	void exitA(RegParser.AContext ctx);
	/**
	 * Enter a parse tree produced by {@link RegParser#b}.
	 * @param ctx the parse tree
	 */
	void enterB(RegParser.BContext ctx);
	/**
	 * Exit a parse tree produced by {@link RegParser#b}.
	 * @param ctx the parse tree
	 */
	void exitB(RegParser.BContext ctx);
}