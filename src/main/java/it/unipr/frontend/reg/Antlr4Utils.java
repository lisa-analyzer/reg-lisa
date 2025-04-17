package it.unipr.frontend.reg;

import org.antlr.v4.runtime.ParserRuleContext;

/**
 * Utility class for ANTLR4.
 * The utility is used to get the line and column of a parser rule context.
 * 
 * @author <a href="mailto:francesco.marastoni_02@studenti.univr.it">Francesco Marastoni</a>
 * @author <a href="mailto:amos.loverde@studenti.univr.it">Amos Lo Verde</a>
 */
public class Antlr4Utils {
	
	/**
	 * Yields the line number of {@code ctx}. 
	 * @param ctx the context
	 * @return the line number of {@code ctx}
	 */
    static int getLine(ParserRuleContext ctx) {
        return ctx.getStart().getLine();
    }

	/**
	 * Yields the column number of {@code ctx}. 
	 * @param ctx the context
	 * @return the column number of {@code ctx}
	 */
    static int getCol(ParserRuleContext ctx) {
        return ctx.getStop().getCharPositionInLine();
    }

}
