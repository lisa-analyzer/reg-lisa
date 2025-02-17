// Generated from /Users/francescomarastoni/Desktop/Java.nosync/reg-lisa/src/main/antlr/RegParser.g4 by ANTLR 4.13.2

    package it.unipr.reg.antlr;

import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link RegParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface RegParserVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link RegParser#program}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProgram(RegParser.ProgramContext ctx);
	/**
	 * Visit a parse tree produced by {@link RegParser#e}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitE(RegParser.EContext ctx);
	/**
	 * Visit a parse tree produced by {@link RegParser#a}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitA(RegParser.AContext ctx);
	/**
	 * Visit a parse tree produced by {@link RegParser#b}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitB(RegParser.BContext ctx);
}