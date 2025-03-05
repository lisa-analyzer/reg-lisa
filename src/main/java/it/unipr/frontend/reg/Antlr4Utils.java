package it.unipr.frontend.reg;

import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Antlr4Utils {
    private static final Logger log = LogManager.getLogger(Antlr4Utils.class);

    static int getLine(ParserRuleContext ctx) {
        return ctx.getStart().getLine();
    }

    static int getCol(ParserRuleContext ctx) {
        return ctx.getStop().getCharPositionInLine();
    }

}
