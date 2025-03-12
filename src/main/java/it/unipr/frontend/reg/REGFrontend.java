package it.unipr.frontend.reg;

import it.unipr.frontend.reg.types.REGTypeSystem;
import it.unipr.reg.antlr.RegLexer;
import it.unipr.reg.antlr.RegParser;
import it.unipr.reg.antlr.RegParserBaseVisitor;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.*;
import it.unive.lisa.program.cfg.statement.literal.Int32Literal;
import it.unive.lisa.program.type.BoolType;
import it.unive.lisa.program.type.Int32Type;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static it.unipr.frontend.reg.Antlr4Utils.getCol;
import static it.unipr.frontend.reg.Antlr4Utils.getLine;

public class REGFrontend extends RegParserBaseVisitor<Object> {

    private final String file;

    private final Program program;

    private CFG currentCFG;

    private CodeMemberDescriptor descriptor;

    private static final Logger log = LogManager.getLogger(REGFrontend.class);


    public static Program processFile(String file) throws IOException {
        return new REGFrontend(file).work(null);
    }

    public static Program processText(String text) throws IOException {
        try (InputStream is = new ByteArrayInputStream(text.getBytes())) {
            return new REGFrontend("in-memory.reg").work(is);
        } catch (IOException e) {
            throw new IOException("Exception while parsing the input text", e);
        }
    }


    public REGFrontend(String file) {
        this.file = file;
        program = new Program(new REGFeatures(), new REGTypeSystem());
        // unita di codice, per noi è il programma
        ClassUnit unit = new ClassUnit(new SourceCodeLocation(file, 0, 0), program, "program", false);
        // descrittore del codice, sarebbe la signature delle funzioni, ma
        // noi non abbiamo funzioni, quindi è il programma
        CodeMemberDescriptor cfgDesc = new CodeMemberDescriptor(new SourceCodeLocation(file, 0, 0), unit, false, "function", new Parameter[]{});
        // inizializzo il CFG
        currentCFG = new CFG(cfgDesc);
    }

    private Program work(InputStream inputStream) throws IOException {

        try {
            log.info("Reading file... " + file);
            RegLexer lexer;
            if (inputStream == null) try (InputStream stream = new FileInputStream(file)) {
                lexer = new RegLexer(CharStreams.fromStream(stream, StandardCharsets.UTF_8));
            }
            else lexer = new RegLexer(CharStreams.fromStream(inputStream, StandardCharsets.UTF_8));

            RegParser parser = new RegParser(new CommonTokenStream(lexer));

            Program p = visitProgram(parser.program());

            p.getTypes().registerType(BoolType.INSTANCE);
            p.getTypes().registerType(Int32Type.INSTANCE);

            return p;
        } catch (IOException e) {
            log.fatal("Unable to open " + file, e);
            throw new IOException("Unable to open " + file, e);
        }
    }

    @Override
    public Program visitProgram(RegParser.ProgramContext ctx) {
        System.out.println("Starting program execution");
        for (int i = 0; i < ctx.e().size(); i++) {
            // ogni espressione è una coppia di statement, sarà da implementare
            // il fatto che vanno linkati da un edge sequenziale
            // quindi tenere traccia dell'ultimo statement di ogni coppia
            // e collegarlo al primo statement della coppia successiva
            visit(ctx.e(i));
            //Pair<Statement, Statement> pair = (Pair<Statement, Statement>) visit(ctx.e(i));
            // non così
            //SequentialEdge se = new SequentialEdge(pair.getLeft(), pair.getRight());
            //currentCFG.addEdge(se);
        }
        System.out.println("Program execution finished");
        return program;
    }

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
        System.out.println("Assigning " + target + " to " + exp);
        Assignment assign = new Assignment(currentCFG, loc, target, exp);
        currentCFG.addNode(assign);
        System.out.println(assign);
        return Pair.of(assign, assign);
    }

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
        System.out.println("Creating binary expression " + left + " " + ctx.op.getText() + " " + right);
        switch (ctx.op.getText()) {
            case "+":
                return new Int32Literal(currentCFG, loc, ((Int32Literal) left).getValue() + ((Int32Literal) right).getValue());
            case "-":
                return new Int32Literal(currentCFG, loc, ((Int32Literal) left).getValue() - ((Int32Literal) right).getValue());
            default:
                throw new UnsupportedOperationException("Unsupported operator " + ctx.op.getText());
        }
    }

    @Override
    public Object visitTimes(RegParser.TimesContext ctx) {
        SourceCodeLocation loc = new SourceCodeLocation(file, getLine(ctx), getCol(ctx));
        Expression left = (Expression) visit(ctx.a(0));
        Expression right = (Expression) visit(ctx.a(1));
        return new Int32Literal(currentCFG, loc, ((Int32Literal) left).getValue() * ((Int32Literal) right).getValue());
    }

    @Override
    public Expression visitA_par(RegParser.A_parContext ctx) {
        return (Expression) visit(ctx.a());
    }

    @Override
    public Expression visitE_par(RegParser.E_parContext ctx) {
        return (Expression) visit(ctx.e());
    }
}