package it.unipr.frontend.reg;

import it.unipr.reg.antlr.RegLexer;
import it.unipr.reg.antlr.RegParser;
import it.unipr.reg.antlr.RegParserBaseVisitor;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.VariableTableEntry;
import it.unive.lisa.program.cfg.edge.FalseEdge;
import it.unive.lisa.program.cfg.edge.SequentialEdge;
import it.unive.lisa.program.cfg.edge.TrueEdge;
import it.unive.lisa.program.cfg.statement.*;
import it.unive.lisa.program.cfg.statement.comparison.Equal;
import it.unive.lisa.program.cfg.statement.comparison.LessOrEqual;
import it.unive.lisa.program.cfg.statement.comparison.LessThan;
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

import static it.unipr.frontend.reg.Antlr4Utils.getCol;
import static it.unipr.frontend.reg.Antlr4Utils.getLine;

public class RegLiSAFrontend extends RegParserBaseVisitor<Object> {

    private final String file;

    private final Program program;

    private CFG currentCFG;

    private CodeMemberDescriptor descriptor;

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
        CodeMemberDescriptor cfgDesc = new CodeMemberDescriptor(new SourceCodeLocation(file, 0, 0), unit, false, "function", new Parameter[]{});
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

            return visitProgram(parser.program());
        } catch (IOException e) {
            throw new IOException("Unable to open " + file, e);
        }
    }

    @Override
    public Program visitProgram(RegParser.ProgramContext ctx) {
        return (Program) visit(ctx.expr());
    }

    @Override
    @SuppressWarnings("unchecked")
    public Program visitSeq(RegParser.SeqContext ctx) {
        Pair<Statement, Statement> firstPair = (Pair<Statement, Statement>) visit(ctx.e(0));

        Statement firstEntry = firstPair.getLeft();
        Statement firstExit = firstPair.getRight();

        // aggiungo il primo nodo al CFG
        currentCFG.addNode(firstEntry);
        currentCFG.getEntrypoints().add(firstEntry);

        // last è l'exit del primo statement
        Statement last = firstExit;

        for (int i = 1; i < ctx.e().size(); i++) {
            Pair<Statement, Statement> pair = (Pair<Statement, Statement>) visit(ctx.e(i));
            Statement entry = pair.getLeft();
            Statement exit = pair.getRight();

            // exit precedente -> entry corrente
            currentCFG.addNode(entry);
            currentCFG.addEdge(new SequentialEdge(last, entry));

            // last è l'exit corrente
            last = exit;
        }

        // TODO: 100, 100 hardcodati?
        Ret eof = new Ret(currentCFG, new SourceCodeLocation(file, 100, 100));
        currentCFG.addNode(eof);
        currentCFG.addEdge(new SequentialEdge(last, eof));
        currentCFG.simplify();

        return program;
    }

    /*
    * ESPRESSIONI
    * */

    @Override
    public Pair<Statement, Statement> visitNoop(RegParser.NoopContext ctx) {
        // funziona, ed è giusto
        // loc è dove mi trovo nel codice
        SourceCodeLocation loc = new SourceCodeLocation(file, getLine(ctx), getCol(ctx));
        // NoOp è gia una classe in LiSA e quindi uso quello per creare l'espressione
        NoOp noop = new NoOp(currentCFG, loc);
        currentCFG.addNode(noop);
        // vedi il paper, il NoOp non fa una mazza
        return Pair.of(noop, noop);
    }

    @Override
    public Pair<Statement, Statement> visitAssign(RegParser.AssignContext ctx) {
        // anche qua LiSA ha gia una classe per l'assign, quindi è simile al noop
        // soltanto che dobbiamo espandere l'assegnamento per avere l'espressione

        // grammatica# e: ID ASSIGN a
        SourceCodeLocation loc = new SourceCodeLocation(file, getLine(ctx), getCol(ctx));
        // target è la variabile a sinistra dell'uguale
        VariableRef target = new VariableRef(currentCFG, loc, ctx.ID().getText());
        // exp è l'espressione a destra dell'uguale
        Expression exp = (Expression) visit(ctx.a());
        Assignment assign = new Assignment(currentCFG, loc, target, exp);
        currentCFG.addNode(assign);
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

        // ritorno l'inizio e la fine
        return Pair.of(init, end);
    }

    @Override
    public Expression visitE_par(RegParser.E_parContext ctx) {
        return (Expression) visit(ctx.expr());
    }


    /*
    *  ESPRESSIONI ARITMETICHE
    * */

    @Override
    public Expression visitNum(RegParser.NumContext ctx) {
        SourceCodeLocation loc = new SourceCodeLocation(file, getLine(ctx), getCol(ctx));
        return new Int32Literal(currentCFG, loc, Integer.parseInt(ctx.NUM().getText()));
    }

    @Override
    public Expression visitPlus_minus(RegParser.Plus_minusContext ctx) {
        SourceCodeLocation loc = new SourceCodeLocation(file, getLine(ctx), getCol(ctx));
        Expression left = (Expression) visit(ctx.a(0));
        Expression right = (Expression) visit(ctx.a(1));
        //System.out.println("Creating binary expression " + left + " " + ctx.op.getText() + " " + right);
        switch (ctx.op.getText()) {
            case "+":
                return new Addition(currentCFG, loc, left, right);
            case "-":
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
        System.out.println("Created variable " + var);
        return var;
    }

    @Override
    public Expression visitTimes(RegParser.TimesContext ctx) {
        SourceCodeLocation loc = new SourceCodeLocation(file, getLine(ctx), getCol(ctx));
        Expression left = (Expression) visit(ctx.a(0));
        Expression right = (Expression) visit(ctx.a(1));
        return new Multiplication(currentCFG, loc, left, right);
    }

    @Override
    public Expression visitA_par(RegParser.A_parContext ctx) {
        return (Expression) visit(ctx.a());
    }


    /*
    *  ESPRESSIONI BOOLEANE
    * */

    @Override
    public Expression visitB_par(RegParser.B_parContext ctx) {
        return (Expression) visit(ctx.b());
    }


    @Override
    public Expression visitEq_leq_le(RegParser.Eq_leq_leContext ctx) {
        SourceCodeLocation loc = new SourceCodeLocation(file, getLine(ctx), getCol(ctx));
        Expression left = (Expression) visit(ctx.a(0));
        Expression right = (Expression) visit(ctx.a(1));
        switch (ctx.op.getText()) {
            case "=":
                return new Equal(currentCFG, loc, left, right);
            case "<=":
                return new LessOrEqual(currentCFG, loc, left, right);
            case "<":
                return new LessThan(currentCFG, loc, left, right);
            default:
                throw new UnsupportedOperationException("Unsupported operator " + ctx.op.getText());
        }
    }

    // TODO
    @Override
    public Pair<Statement, Statement> visitCond(RegParser.CondContext ctx) {
        SourceCodeLocation loc = new SourceCodeLocation(file, getLine(ctx), getCol(ctx));

        // nodo iniziale skip
        NoOp init = new NoOp(currentCFG, loc);
        currentCFG.addNode(init);



        return Pair.of(init, init);
    }

    // TODO
    @Override
    public Pair<Statement, Statement> visitKleene(RegParser.KleeneContext ctx) {
        SourceCodeLocation loc = new SourceCodeLocation(file, getLine(ctx), getCol(ctx));

        // Nodo iniziale della condizione
        Expression cond = (Expression) visit(ctx.b());
        currentCFG.addNode(cond);


        // Si crea il corpo del ciclo
        Statement last = cond;

        for (int i = 0; i < ctx.e().size(); i++) {
            Pair<Statement, Statement> pair = (Pair<Statement, Statement>) visit(ctx.e(i));
            System.out.println(ctx.e(i).getText());
            Statement entry = pair.getLeft();
            Statement exit = pair.getRight();

            // exit precedente -> entry corrente
            currentCFG.addNode(entry);
            currentCFG.addEdge(new SequentialEdge(last, entry));

            // last è l'exit corrente
            last = exit;
        }

        // Si collega l'ultima espressione con la condizione
        currentCFG.addEdge(new SequentialEdge(last, cond));


        // Nodo della condizione negata
        SourceCodeLocation newloc = new SourceCodeLocation(file, getLine(ctx), getCol(ctx));
        Expression negcond = new Not(currentCFG, newloc, cond);
        currentCFG.addNode(negcond);
        currentCFG.addEdge(new SequentialEdge(cond, negcond));


        return Pair.of(cond, negcond);
    }


    @Override
    public Expression visitTrue(RegParser.TrueContext ctx) {
        SourceCodeLocation loc = new SourceCodeLocation(file, getLine(ctx), getCol(ctx));
        return new TrueLiteral(currentCFG, loc);
    }

    @Override
    public Expression visitFalse(RegParser.FalseContext ctx) {
        SourceCodeLocation loc = new SourceCodeLocation(file, getLine(ctx), getCol(ctx));
        return new TrueLiteral(currentCFG, loc);
    }

    @Override
    public Expression visitAnd(RegParser.AndContext ctx) {
        SourceCodeLocation loc = new SourceCodeLocation(file, getLine(ctx), getCol(ctx));
        Expression left = (Expression) visit(ctx.b(0));
        Expression right = (Expression) visit(ctx.b(1));
        return new And(currentCFG, loc, left, right);
    }

    @Override
    public Expression visitNot(RegParser.NotContext ctx) {
        SourceCodeLocation loc = new SourceCodeLocation(file, getLine(ctx), getCol(ctx));
        Expression exp = (Expression) visit(ctx.b());
        return new Not(currentCFG, loc, exp);
    }



}