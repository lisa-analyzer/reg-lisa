package it.unipr.frontend.reg;

import org.antlr.v4.runtime.ParserRuleContext;

public class Antlr4Utils {

	static int getLine(ParserRuleContext ctx) {
		return ctx.getStart().getLine();
	}

	static int getCol(ParserRuleContext ctx) {
		return ctx.getStop().getCharPositionInLine();
	}

}
