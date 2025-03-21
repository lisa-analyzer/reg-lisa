package it.unipr.frontend.reg;

import it.unipr.reg.antlr.RegLexer;
import it.unipr.reg.antlr.RegParser;
import it.unipr.reg.antlr.RegParserBaseVisitor;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.VariableTableEntry;
import it.unive.lisa.program.cfg.edge.FalseEdge;
import it.unive.lisa.program.cfg.edge.SequentialEdge;
import it.unive.lisa.program.cfg.edge.TrueEdge;
import it.unive.lisa.program.cfg.statement.*;
import it.unive.lisa.program.cfg.statement.comparison.Equal;
import it.unive.lisa.program.cfg.statement.comparison.LessOrEqual;
import it.unive.lisa.program.cfg.statement.comparison.LessThan;
import it.unive.lisa.program.cfg.statement.literal.FalseLiteral;
import it.unive.lisa.program.cfg.statement.literal.Int32Literal;
import it.unive.lisa.program.cfg.statement.literal.TrueLiteral;
import it.unive.lisa.program.cfg.statement.logic.And;
import it.unive.lisa.program.cfg.statement.logic.Not;
import it.unive.lisa.program.cfg.statement.numeric.Addition;
import it.unive.lisa.program.cfg.statement.numeric.Multiplication;
import it.unive.lisa.program.cfg.statement.numeric.Subtraction;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static it.unipr.frontend.reg.Antlr4Utils.getCol;
import static it.unipr.frontend.reg.Antlr4Utils.getLine;

public class RegLiSAFrontend extends RegParserBaseVisitor<Object> {

    private final String file;

    private final Program program;

    private final CFG currentCFG;

    private final CodeMemberDescriptor descriptor;

    private static final Logger log = LogManager.getLogger(RegLiSAFrontend.class);

    public static Program processFile(String file) throws IOException {
        return new RegLiSAFrontend(file).work();
    }

    public RegLiSAFrontend(String file) {
        this.file = file;
        this.program = new Program(new RegLiSAFeatures(), new RegLiSATypeSystem());
        // unita di codice, per noi è il programma
        ClassUnit unit = new ClassUnit(new SourceCodeLocation(file, 0, 0), program, "program", false);
        // descrittore del codice, sarebbe la signature delle funzioni, ma
        // noi non abbiamo funzioni, quindi è il programma
        CodeMemberDescriptor cfgDesc = new CodeMemberDescriptor(new SourceCodeLocation(file, 0, 0), unit, false, "function");
        // inizializzo il CFG
        this.currentCFG = new CFG(cfgDesc);
        this.descriptor = cfgDesc;
        unit.addCodeMember(currentCFG);
        program.addUnit(unit);

    }

    private Program work() throws IOException {

        try {
            RegLexer lexer;
            try (InputStream stream = new FileInputStream(file)) {
                lexer = new RegLexer(CharStreams.fromStream(stream, StandardCharsets.UTF_8));
            }

            RegParser parser = new RegParser(new CommonTokenStream(lexer));
            log.info("Parsing {}", file);

            return visitProgram(parser.program());
        } catch (IOException e) {
            throw new IOException("Unable to open " + file, e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Program visitProgram(RegParser.ProgramContext ctx) {

        // visitiamo le espressioni
        Pair<Statement, Statement> exprPair = (Pair<Statement, Statement>) visit(ctx.expr());
        Statement firstEntry = exprPair.getLeft();
        Statement lastExit = exprPair.getRight();

        currentCFG.addNode(firstEntry);
        currentCFG.getEntrypoints().add(firstEntry);

        SourceCodeLocation loc = new SourceCodeLocation(file, ctx.getStop().getLine(), ctx.getStop().getCharPositionInLine());
        Ret eof = new Ret(currentCFG, loc);
        currentCFG.addNode(eof);
        currentCFG.addEdge(new SequentialEdge(lastExit, eof));

        currentCFG.simplify();
        log.info("Nodes: {}", currentCFG.getNodes());
        log.info("Edges: {}", currentCFG.getEdges());

        return program;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Pair<Statement, Statement> visitSeq(RegParser.SeqContext ctx) {
        Pair<Statement, Statement> firstPair = (Pair<Statement, Statement>) visit(ctx.e(0));
        log.info("First pair of the program: {}", firstPair);

        Statement firstEntry = firstPair.getLeft();

        Statement last = firstPair.getRight();

        for (int i = 1; i < ctx.e().size(); i++) {
            Pair<Statement, Statement> pair = (Pair<Statement, Statement>) visit(ctx.e(i));
            log.info("Pair {}: {}", i, pair);

            Statement entry = pair.getLeft();
            Statement exit = pair.getRight();

            // uscita corrente -> entrata successiva
            currentCFG.addNode(entry);
            currentCFG.addEdge(new SequentialEdge(last, entry));

            // uscita corrente
            last = exit;
        }

        return Pair.of(firstEntry, last);
    }

    /*
     * ESPRESSIONI
     */

    @Override
    public Pair<Statement, Statement> visitNoop(RegParser.NoopContext ctx) {
        // funziona, ed è giusto
        // loc è dove mi trovo nel codice
        SourceCodeLocation loc = new SourceCodeLocation(file, getLine(ctx), getCol(ctx));
        // NoOp è gia una classe in LiSA e quindi uso quello per creare
        // l'espressione
        NoOp noop = new NoOp(currentCFG, loc);
        currentCFG.addNode(noop);
        log.info("Noop at {}", loc);
        // vedi il paper, il NoOp non fa una mazza
        return Pair.of(noop, noop);
    }

    @Override
    public Pair<Statement, Statement> visitAssign(RegParser.AssignContext ctx) {
        // anche qua LiSA ha gia una classe per l'assign, quindi è simile al
        // noop
        // soltanto che dobbiamo espandere l'assegnamento per avere
        // l'espressione

        // grammatica# e: ID ASSIGN a
        SourceCodeLocation loc = new SourceCodeLocation(file, getLine(ctx), getCol(ctx));
        // target è la variabile a sinistra dell'uguale
        VariableRef target = new VariableRef(currentCFG, loc, ctx.ID().getText());
        // exp è l'espressione a destra dell'uguale
        Expression exp = (Expression) visit(ctx.a());
        Assignment assign = new Assignment(currentCFG, loc, target, exp);
        currentCFG.addNode(assign);
        log.info("Assign {} at {}", assign, loc);
        return Pair.of(assign, assign);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Pair<Statement, Statement> visitE_ndc(RegParser.E_ndcContext ctx) {
        SourceCodeLocation loc = new SourceCodeLocation(file, getLine(ctx), getCol(ctx));

        // nodo iniziale skip
        NoOp init = new NoOp(currentCFG, loc);
        currentCFG.addNode(init);

        // visitiamo tutti e due i rami
        Pair<Statement, Statement> left = (Pair<Statement, Statement>) visit(ctx.e(0));
        Pair<Statement, Statement> right = (Pair<Statement, Statement>) visit(ctx.e(1));

        currentCFG.addEdge(new SequentialEdge(init, left.getLeft()));
        currentCFG.addEdge(new SequentialEdge(init, right.getLeft()));

        // nodo finale join
        NoOp end = new NoOp(currentCFG, new SourceCodeLocation(file, getLine(ctx) + 1, getCol(ctx)));
        currentCFG.addNode(end);

        currentCFG.addEdge(new SequentialEdge(left.getRight(), end));
        currentCFG.addEdge(new SequentialEdge(right.getRight(), end));
        log.info("NDC at {}", loc);
        // ritorno l'inizio e la fine
        return Pair.of(init, end);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Pair<Statement, Statement> visitE_par(RegParser.E_parContext ctx) {
        log.info("Visiting e_par");
        return (Pair<Statement, Statement>) visit(ctx.expr());
    }

    /*
     * ESPRESSIONI ARITMETICHE
     */

    @Override
    public Expression visitNum(RegParser.NumContext ctx) {
        SourceCodeLocation loc = new SourceCodeLocation(file, getLine(ctx), getCol(ctx));
        log.info("Num at {}", loc);
        return new Int32Literal(currentCFG, loc, Integer.parseInt(ctx.NUM().getText()));
    }

    @Override
    public Expression visitPlus_minus(RegParser.Plus_minusContext ctx) {
        SourceCodeLocation loc = new SourceCodeLocation(file, getLine(ctx), getCol(ctx));
        Expression left = (Expression) visit(ctx.a(0));
        Expression right = (Expression) visit(ctx.a(1));
        switch (ctx.op.getText()) {
            case "+":
                log.info("Addition at {}", loc);
                return new Addition(currentCFG, loc, left, right);
            case "-":
                log.info("Subtraction at {}", loc);
                return new Subtraction(currentCFG, loc, left, right);
            default:
                throw new UnsupportedOperationException("Unsupported operator " + ctx.op.getText());
        }
    }

    @Override
    public Expression visitId(RegParser.IdContext ctx) {
        SourceCodeLocation loc = new SourceCodeLocation(file, getLine(ctx), getCol(ctx));
        descriptor.addVariable(new VariableTableEntry(loc, 0, ctx.ID().getText()));
        VariableRef var = new VariableRef(currentCFG, loc, ctx.ID().getText());
        log.info("Created variable {}", var);
        return var;
    }

    @Override
    public Expression visitTimes(RegParser.TimesContext ctx) {
        SourceCodeLocation loc = new SourceCodeLocation(file, getLine(ctx), getCol(ctx));
        Expression left = (Expression) visit(ctx.a(0));
        Expression right = (Expression) visit(ctx.a(1));
        log.info("Multiplication at {}", loc);
        return new Multiplication(currentCFG, loc, left, right);
    }

    @Override
    public Expression visitA_par(RegParser.A_parContext ctx) {
        log.info("Visiting a_par");
        return (Expression) visit(ctx.a());
    }

    /*
     * ESPRESSIONI BOOLEANE
     */

    @Override
    public Expression visitB_par(RegParser.B_parContext ctx) {
        log.info("Visiting b_par");
        return (Expression) visit(ctx.b());
    }

    @Override
    public Expression visitEq_leq_le(RegParser.Eq_leq_leContext ctx) {
        SourceCodeLocation loc = new SourceCodeLocation(file, getLine(ctx), getCol(ctx));
        Expression left = (Expression) visit(ctx.a(0));
        Expression right = (Expression) visit(ctx.a(1));
        switch (ctx.op.getText()) {
            case "=":
                log.info("Equal at {}", loc);
                return new Equal(currentCFG, loc, left, right);
            case "<=":
                log.info("LessOrEqual at {}", loc);
                return new LessOrEqual(currentCFG, loc, left, right);
            case "<":
                log.info("LessThan at {}", loc);
                return new LessThan(currentCFG, loc, left, right);
            default:
                throw new UnsupportedOperationException("Unsupported operator " + ctx.op.getText());
        }
    }

    @Override
    public Pair<Statement, Statement> visitCond(RegParser.CondContext ctx) {
        SourceCodeLocation loc = new SourceCodeLocation(file, getLine(ctx), getCol(ctx));
        Expression cond = (Expression) visit(ctx.b());
        currentCFG.addNode(cond);

        if (ctx.e().isEmpty()) {
            NoOp noop = new NoOp(currentCFG, loc);
            currentCFG.addNode(noop);
            currentCFG.addEdge(new TrueEdge(cond, noop));
            Ret falseAssert = new Ret(currentCFG, new SourceCodeLocation(file, getLine(ctx) + 1, getCol(ctx)));
            currentCFG.addNode(falseAssert);
            currentCFG.addEdge(new FalseEdge(cond, falseAssert));
            log.info("Assert like Cond at {}", loc);
            return Pair.of(cond, noop);
        }

        return processConditionalBlock(ctx, cond, loc, false);
    }

    @Override
    public Pair<Statement, Statement> visitKleene(RegParser.KleeneContext ctx) {
        SourceCodeLocation loc = new SourceCodeLocation(file, getLine(ctx), getCol(ctx));
        Expression cond = (Expression) visit(ctx.b());
        currentCFG.addNode(cond);

        return processConditionalBlock(ctx, cond, loc, true);
    }

    private Pair<Statement, Statement> processConditionalBlock(RegParser.CondContext ctx, Expression cond, SourceCodeLocation loc, boolean isKleene) {
        return processBlock(ctx.e(), cond, loc, isKleene);
    }

    private Pair<Statement, Statement> processConditionalBlock(RegParser.KleeneContext ctx, Expression cond, SourceCodeLocation loc, boolean isKleene) {
        return processBlock(ctx.e(), cond, loc, isKleene);
    }

    @SuppressWarnings("unchecked")
    private Pair<Statement, Statement> processBlock(List<? extends RegParser.EContext> expressions, Expression cond, SourceCodeLocation loc, boolean isKleene) {
        Pair<Statement, Statement> firstPair = (Pair<Statement, Statement>) visit(expressions.get(0));
        currentCFG.addEdge(new TrueEdge(cond, firstPair.getLeft()));

        Pair<Statement, Statement> lastPair = firstPair;

        // for loop che visita tutte le exp e le connette sequenzialmente
        for (int i = 1; i < expressions.size(); i++) {
            Pair<Statement, Statement> pair = (Pair<Statement, Statement>) visit(expressions.get(i));
            currentCFG.addNode(pair.getLeft());
            currentCFG.addEdge(new SequentialEdge(lastPair.getRight(), pair.getLeft()));
            lastPair = pair;
        }

        NoOp end = new NoOp(currentCFG, loc);
        currentCFG.addNode(end);

        // collega l'ultimo statement al nodo end
        // in un kleene si torna alla condizione, altrimenti si va al nodo end
        if (isKleene) {
            log.info("Kleene at {}", loc);
            currentCFG.addEdge(new SequentialEdge(lastPair.getRight(), cond));
        } else {
            log.info("Cond at {}", loc);
            currentCFG.addEdge(new SequentialEdge(lastPair.getRight(), end));
        }

        currentCFG.addEdge(new FalseEdge(cond, end));

        return Pair.of(cond, end);
    }

    @Override
    public Expression visitTrue(RegParser.TrueContext ctx) {
        SourceCodeLocation loc = new SourceCodeLocation(file, getLine(ctx), getCol(ctx));
        log.info("True at {}", loc);
        return new TrueLiteral(currentCFG, loc);
    }

    @Override
    public Expression visitFalse(RegParser.FalseContext ctx) {
        SourceCodeLocation loc = new SourceCodeLocation(file, getLine(ctx), getCol(ctx));
        log.info("False at {}", loc);
        return new FalseLiteral(currentCFG, loc);
    }

    @Override
    public Expression visitAnd(RegParser.AndContext ctx) {
        SourceCodeLocation loc = new SourceCodeLocation(file, getLine(ctx), getCol(ctx));
        Expression left = (Expression) visit(ctx.b(0));
        Expression right = (Expression) visit(ctx.b(1));
        log.info("And at {}", loc);
        return new And(currentCFG, loc, left, right);
    }

    @Override
    public Expression visitNot(RegParser.NotContext ctx) {
        SourceCodeLocation loc = new SourceCodeLocation(file, getLine(ctx), getCol(ctx));
        Expression exp = (Expression) visit(ctx.b());
        log.info("Not at {}", loc);
        return new Not(currentCFG, loc, exp);
    }
}