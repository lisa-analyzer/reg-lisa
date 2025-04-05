package it.unipr.frontend.reg;

import org.antlr.v4.runtime.ParserRuleContext;

/**
 * Utility class for ANTLR4.
 * The utility is used to get the line and column of a parser rule context.
 */
public class Antlr4Utils {

    static int getLine(ParserRuleContext ctx) {
        return ctx.getStart().getLine();
    }

    static int getCol(ParserRuleContext ctx) {
        return ctx.getStop().getCharPositionInLine();
    }

}
