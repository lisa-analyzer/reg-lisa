package it.unipr.frontend.reg;

import it.unipr.reg.antlr.RegParser;
import it.unipr.reg.antlr.RegParserBaseVisitor;

import java.util.HashMap;

public class REGCodeMemberVisitor extends RegParserBaseVisitor<Object> {
    HashMap<String, Integer> vars = new HashMap<>();

    @Override
    public Object visitProgram(RegParser.ProgramContext ctx) {
        System.out.println("Starting program execution");
        for (int i = 0; i < ctx.e().size(); i++) {
            System.out.println("Visiting expression " + i);
            visit(ctx.e(i));
        }
        System.out.println("Program execution finished");
        System.out.println("Variables: " + vars);
        return null;
    }

    @Override
    public Object visitNoop(RegParser.NoopContext ctx) {
        System.out.println("Encountered a NOOP operation");
        System.out.println("LINE: " + ctx.getStart().getLine());
        return null;
    }

    @Override
    public Object visitOp(RegParser.OpContext ctx) {
        System.out.println("Visiting an operation");
        System.out.println("LINE: " + ctx.getStart().getLine());
        if (ctx.SEQ() != null) {
            System.out.println("Executing sequential operation");
            visit(ctx.e(0));
            visit(ctx.e(1));
        } else {
            System.out.println("Executing random choice operation");
            visit(Math.random() < 0.5 ? ctx.e(0) : ctx.e(1));
        }
        return null;
    }

    @Override
    public Object visitE_par(RegParser.E_parContext ctx) {
        System.out.println("Visiting parenthesized expression");
        System.out.println("LINE: " + ctx.getStart().getLine());
        return visit(ctx.e());
    }

    @Override
    public Object visitCond(RegParser.CondContext ctx) {
        System.out.println("Evaluating condition");
        if (! (Boolean) visit(ctx.b())) {
            System.out.println("Condition is false");
            System.exit(0);
        }
        return null;
    }

    @Override
    public Object visitAssign(RegParser.AssignContext ctx) {
        System.out.println("LINE: " + ctx.getStart().getLine());
        String varName = ctx.ID().getText();
        int value = (Integer) visit(ctx.a());
        vars.put(varName, value);
        System.out.println("Variable " + varName + " assigned to " + value);
        return null;
    }

    @Override
    public Object visitKleene(RegParser.KleeneContext ctx) {
        System.out.println("LINE: " + ctx.getStart().getLine());
        System.out.println("Visiting Kleene star expression");
        return visit(ctx.e());
    }

    @Override
    public Object visitA_par(RegParser.A_parContext ctx) {
        System.out.println("Visiting arithmetic parenthesized expression");
        return visit(ctx.a());
    }

    @Override
    public Object visitNum(RegParser.NumContext ctx) {
        int num = Integer.parseInt(ctx.NUM().getText());
        System.out.println("Parsed number: " + num);
        return num;
    }

    @Override
    public Object visitPlus_minus_times(RegParser.Plus_minus_timesContext ctx) {
        int left = (Integer) visit(ctx.a(0));
        int right = (Integer) visit(ctx.a(1));
        int result;

        if (ctx.PLUS() != null) {
            result = left + right;
            System.out.println("Adding: " + left + " + " + right + " = " + result);
        } else if (ctx.MINUS() != null) {
            result = left - right;
            System.out.println("Subtracting: " + left + " - " + right + " = " + result);
        } else {
            result = left * right;
            System.out.println("Multiplying: " + left + " * " + right + " = " + result);
        }
        return result;
    }

    @Override
    public Object visitId(RegParser.IdContext ctx) {
        String varName = ctx.ID().getText();
        Integer value = vars.get(varName);
        System.out.println("Accessing variable " + varName + " with value " + value);
        return value;
    }

    @Override
    public Object visitNot(RegParser.NotContext ctx) {
        boolean value = !(Boolean) visit(ctx.b());
        System.out.println("Negating boolean expression: result = " + value);
        return value;
    }

    @Override
    public Object visitB_par(RegParser.B_parContext ctx) {
        System.out.println("Visiting boolean parenthesized expression");
        return visit(ctx.b());
    }

    @Override
    public Object visitAnd(RegParser.AndContext ctx) {
        boolean left = (Boolean) visit(ctx.b(0));
        boolean right = (Boolean) visit(ctx.b(1));
        boolean result = left && right;
        System.out.println("AND operation: " + left + " && " + right + " = " + result);
        return result;
    }

    @Override
    public Object visitEq_leq_le(RegParser.Eq_leq_leContext ctx) {
        System.out.println("[LOG] LINE: " + ctx.getStart().getLine());
        System.out.println("[LOG] VARS: " + vars);
        int left = (Integer) visit(ctx.a(0));
        int right = (Integer) visit(ctx.a(1));
        boolean result;

        if (ctx.EQ() != null) {
            result = left == right;
            System.out.println("Equality check: " + left + " == " + right + " = " + result);
        } else if (ctx.LEQ() != null) {
            result = left <= right;
            System.out.println("Less than or equal check: " + left + " <= " + right + " = " + result);
        } else {
            result = left < right;
            System.out.println("Less than check: " + left + " < " + right + " = " + result);
        }
        return result;
    }

    @Override
    public Object visitTrue(RegParser.TrueContext ctx) {
        System.out.println("Boolean literal: true");
        return true;
    }

    @Override
    public Object visitFalse(RegParser.FalseContext ctx) {
        System.out.println("Boolean literal: false");
        return false;
    }
}
