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

/**
 * Frontend class for translating REG language programs into LiSA's Control Flow
 * Graph (CFG) representation.
 * This class implements the Visitor pattern through
 * ANTLR's RegParserBaseVisitor to traverse the Abstract Syntax Tree (AST).
 * <p>
 * The frontend performs the following tasks:
 * <ul>
 *   <li>Parse the REG source code using ANTLR4 lexer and parser</li>
 *   <li>Construct a LiSA Program object with corresponding ClassUnit</li>
 *   <li>Build a complete Control Flow Graph (CFG) by visiting AST nodes</li>
 *   <li>Map REG language constructs to appropriate LiSA CFG structures</li>
 *   <li>Register variables and handle expression translation</li>
 * </ul>
 * <p>
 * The CFG construction follows these principles:
 * <ul>
 *   <li>Each expression node returns a Pair<Statement, Statement> representing entry and exit points</li>
 *   <li>Control structures (conditionals, loops) are translated to appropriate branch structures</li>
 *   <li>Expressions are translated to their corresponding LiSA statement types</li>
 * </ul>
 * <p>
 * @see it.unipr.reg.antlr.RegParser
 * @see it.unipr.reg.antlr.RegLexer
 */
public class RegLiSAFrontend extends RegParserBaseVisitor<Object> {

    /**
     * The path to the source file being processed.
     */
    private final String file;

    /**
     * The LiSA Program object being constructed to represent the Reg program.
     */
    private final Program program;

    /**
     * The Control Flow Graph (CFG) being constructed for the current program.
     */
    private final CFG currentCFG;

    /**
     * The descriptor containing metadata about the code being processed,
     * including variable declarations.
     */
    private final CodeMemberDescriptor descriptor;

    /**
     * Logger instance for debugging and information output.
     */
    private static final Logger log = LogManager.getLogger(RegLiSAFrontend.class);

    // region Initialization and Setup

    /**
     * Constructs a new RegLiSAFrontend instance to process the specified source file.
     * Initializes the Program, ClassUnit, CodeMemberDescriptor, and CFG objects
     * needed to represent the Reg program in LiSA.
     *
     * @param file The path to the source file to process
     */
    public RegLiSAFrontend(String file) {
        this.file = file;
        this.program = new Program(new RegLiSAFeatures(), new RegLiSATypeSystem());

        // Create a class unit to represent the program
        ClassUnit unit = new ClassUnit(new SourceCodeLocation(file, 0, 0), program, "program", false);

        // Create a descriptor for the "function" that will contain the program's code
        // Since Reg doesn't have functions, we represent the entire program as a single function
        CodeMemberDescriptor cfgDesc = new CodeMemberDescriptor(new SourceCodeLocation(file, 0, 0), unit, false, "function");

        // Initialize the CFG for the program
        this.currentCFG = new CFG(cfgDesc);
        this.descriptor = cfgDesc;

        // Add the CFG to the descriptor and the unit to the program
        unit.addCodeMember(currentCFG);
        program.addUnit(unit);
    }

    /**
     * Static factory method to process a Reg source file and return the
     * resulting LiSA Program object.
     *
     * @param file The path to the source file to process
     * @return The constructed LiSA Program object
     * @throws IOException If an I/O error occurs while reading the file
     */
    public static Program processFile(String file) throws IOException {
        return new RegLiSAFrontend(file).work();
    }

    /**
     * Performs the actual work of parsing the input file and constructing
     * the LiSA Program object.
     * Sets up the ANTLR lexer and parser, then
     * initiates the AST traversal by visiting the root program node.
     *
     * @return The constructed LiSA Program object
     * @throws IOException If an I/O error occurs while reading the file
     */
    private Program work() throws IOException {
        try {
            // Set up the ANTLR lexer
            RegLexer lexer;
            try (InputStream stream = new FileInputStream(file)) {
                lexer = new RegLexer(CharStreams.fromStream(stream, StandardCharsets.UTF_8));
            }

            // Set up the ANTLR parser
            RegParser parser = new RegParser(new CommonTokenStream(lexer));
            log.info("Parsing {}", file);

            // Start the visitor pattern by visiting the root program node
            return visitProgram(parser.program());
        } catch (IOException e) {
            throw new IOException("Unable to open " + file, e);
        }
    }

    // endregion

    // region Program Structure Visitors

    /**
     * Visits the program context node (root of the AST), processes the main program expression,
     * and completes the CFG construction by adding a return statement and simplifying the graph.
     * <p>
     * This method serves as the entry point for the visitor pattern traversal. It:
     * <ol>
     *   <li>Visits the main expression to build the program's CFG</li>
     *   <li>Adds the first statement as the CFG entry point</li>
     *   <li>Appends a return statement at the end of the CFG</li>
     *   <li>Simplifies the CFG by removing NoOp nodes and redundant edges</li>
     * </ol>
     * <p>
     * Grammar rule:
     * <code>program: expr EOF;</code>
     *
     * @param ctx The program context from the parser
     * @return The fully constructed LiSA Program object ready for static analysis
     */
    @Override
    @SuppressWarnings("unchecked")
    public Program visitProgram(RegParser.ProgramContext ctx) {
        // Visit the main expression in the program
        // This will visit all expressions recursively
        Pair<Statement, Statement> exprPair = (Pair<Statement, Statement>) visit(ctx.expr());
        Statement firstEntry = exprPair.getLeft();
        Statement lastExit = exprPair.getRight();

        // Add the entry point to the CFG
        currentCFG.addNode(firstEntry);
        currentCFG.getEntrypoints().add(firstEntry);

        // Add a return statement at the end of the program
        SourceCodeLocation loc = new SourceCodeLocation(file, ctx.getStop().getLine(), ctx.getStop().getCharPositionInLine());
        Ret eof = new Ret(currentCFG, loc);
        currentCFG.addNode(eof);
        currentCFG.addEdge(new SequentialEdge(lastExit, eof));

        // Simplify the CFG by eliminating redundant nodes and edges. I.e., NoOp nodes
        currentCFG.simplify();
        log.info("Nodes: {}", currentCFG.getNodes());
        log.info("Edges: {}", currentCFG.getEdges());

        return program;
    }

    /**
     * Processes a sequence of expressions by visiting each one and creating sequential
     * edges between them in the CFG.
     * <p>
     * The method creates a chain of connected expressions where:
     * <ul>
     *   <li>Each expression is visited to get its entry and exit points</li>
     *   <li>The exit point of expression <code>i</code> is connected to the entry point of expression <code>i + 1</code></li>
     *   <li>The returned pair contains the entry of the first expression and exit of the last expression</li>
     * </ul>
     * <p>
     * CFG structure created:
     * <pre>
     * firstEntry → ... → firstExit → secondEntry → ... → secondExit → ... → lastEntry → ... → lastExit
     * </pre>
     * <p>
     * Grammar rule:
     * <code>expr: e (SEQ e)*;</code>
     *
     * @param ctx The sequence context from the parser
     * @return A pair where:
     * - left: the entry point statement of the first expression
     * - right: the exit point statement of the last expression
     */
    @Override
    @SuppressWarnings("unchecked")
    public Pair<Statement, Statement> visitSeq(RegParser.SeqContext ctx) {
        // Visit the first expression in the sequence
        Pair<Statement, Statement> firstPair = (Pair<Statement, Statement>) visit(ctx.e(0));
        log.info("First pair of the program: {}", firstPair);

        Statement firstEntry = firstPair.getLeft();
        Statement last = firstPair.getRight();

        // Visit each later expression and connect it to the previous one
        for (int i = 1; i < ctx.e().size(); i++) {
            Pair<Statement, Statement> pair = (Pair<Statement, Statement>) visit(ctx.e(i));
            log.info("Pair {}: {}", i, pair);

            Statement entry = pair.getLeft();
            Statement exit = pair.getRight();

            // Connect the exit of the previous expression to the entry of the current one
            // latestExit -> CurrentEntry
            currentCFG.addNode(entry);
            currentCFG.addEdge(new SequentialEdge(last, entry));

            // Update the last exit point
            last = exit;
        }

        // Return the entry of the first expression and the exit of the last one
        return Pair.of(firstEntry, last);
    }

    /**
     * Visits a parenthesized expression and delegates to the enclosed expression.
     * <p>
     * Grammar:
     * <code>e: LPAR expr RPAR</code>
     *
     * @param ctx The parenthesized expression context from the parser
     * @return The result of visiting the enclosed expression
     */
    @Override
    @SuppressWarnings("unchecked")
    public Pair<Statement, Statement> visitE_par(RegParser.E_parContext ctx) {
        log.info("Visiting e_par");
        return (Pair<Statement, Statement>) visit(ctx.expr());
    }

    // endregion

    // region Statement Visitors

    /**
     * Visits a no-operation expression (noop) and creates a corresponding NoOp node
     * in the CFG.
     * <p>
     * Grammar:
     * <code>e: NOOP</code>
     *
     * @param ctx The noop context from the parser
     * @return A pair containing the NoOp node as both entry and exit points
     */
    @Override
    public Pair<Statement, Statement> visitNoop(RegParser.NoopContext ctx) {
        // Create a source code location for the noop
        SourceCodeLocation loc = new SourceCodeLocation(file, getLine(ctx), getCol(ctx));

        // Create a NoOp statement
        NoOp noop = new NoOp(currentCFG, loc);
        currentCFG.addNode(noop);
        log.info("Noop at {}", loc);

        // Return the noop as both entry and exit points since it's a single node
        return Pair.of(noop, noop);
    }

    /**
     * Visits an assignment expression and creates a corresponding Assignment node
     * in the CFG.
     * <p>
     * Grammar:
     * <code>e: ID ASSIGN a</code>
     *
     * @param ctx The assignment context from the parser
     * @return A pair containing the Assignment node as both entry and exit points
     */
    @Override
    public Pair<Statement, Statement> visitAssign(RegParser.AssignContext ctx) {
        SourceCodeLocation loc = new SourceCodeLocation(file, getLine(ctx), getCol(ctx));

        // Create a variable reference for the target (left side of assignment)
        VariableRef target = new VariableRef(currentCFG, loc, ctx.ID().getText());

        // Visit the expression on the right side of the assignment
        Expression exp = (Expression) visit(ctx.a());

        // Create the assignment statement
        Assignment assign = new Assignment(currentCFG, loc, target, exp);
        currentCFG.addNode(assign);
        log.info("Assign {} at {}", assign, loc);

        // Return the assignment as both entry and exit points
        return Pair.of(assign, assign);
    }

    /**
     * Visits a non-deterministic choice expression (e + e) and creates the
     * corresponding structure in the CFG with branches for each choice.
     * <p>
     * Grammar:
     * <code>e: LPAR e NON_DET_CHOICE e RPAR</code>
     *
     * @param ctx The non-deterministic choice context from the parser
     * @return A pair containing the entry NoOp node and the exit NoOp node
     */
    @Override
    @SuppressWarnings("unchecked")
    public Pair<Statement, Statement> visitE_ndc(RegParser.E_ndcContext ctx) {
        SourceCodeLocation loc = new SourceCodeLocation(file, getLine(ctx), getCol(ctx));

        // Create an initial NoOp node to branch from
        NoOp init = new NoOp(currentCFG, loc);
        currentCFG.addNode(init);

        // Visit both branches of the choice
        Pair<Statement, Statement> left = (Pair<Statement, Statement>) visit(ctx.e(0));
        Pair<Statement, Statement> right = (Pair<Statement, Statement>) visit(ctx.e(1));

        // Connect the initial node to both branches
        currentCFG.addEdge(new SequentialEdge(init, left.getLeft()));
        currentCFG.addEdge(new SequentialEdge(init, right.getLeft()));

        // Create a final NoOp node to join the branches
        NoOp end = new NoOp(currentCFG, new SourceCodeLocation(file, getLine(ctx) + 1, getCol(ctx)));
        currentCFG.addNode(end);

        // Connect both branches to the final node
        currentCFG.addEdge(new SequentialEdge(left.getRight(), end));
        currentCFG.addEdge(new SequentialEdge(right.getRight(), end));
        log.info("NDC at {}", loc);

        // Return the initial and final nodes
        return Pair.of(init, end);
    }

    /**
     * Translates a conditional expression to a branch structure in the CFG.
     * <p>
     * This method handles two cases:
     * <ol>
     *   <li><b>Assertion-like condition</b>: When no 'then' branch is provided, creates a structure
     *       where the true branch leads to a NoOp and the false branch to a return
     *       (error case, so an exit point)</li>
     *   <li><b>Standard conditional</b>: Creates a branch where the true path executes the 'then'
     *       statements, and both paths eventually merge at an end node</li>
     * </ol>
     * <p>
     * CFG structure for assertion-like condition:
     * <pre>
     *       cond ─── TrueEdge ───→ NoOp
     *         │
     *         └─── FalseEdge ────→ Ret (error case)
     * </pre>
     * <p>
     * CFG structure for standard conditional:
     * <pre>
     *       cond ─── TrueEdge ───→ thenBlock ───→ end
     *         │                                    ▲
     *         └──── FalseEdge ─────────────────────┘
     * </pre>
     * <p>
     * Grammar rule:
     * <code>e: LPAR b COND (SEQ e (SEQ e)*)? RPAR</code>
     * <p>
     * Examples:
     * <ul>
     *     <li><code>(x < 10 ? ) ...</code> - assertion-like condition</li>
     *     <li><code>(x < 10 ? x := x + 1 ; ...)</code> - standard conditional</li>
     * </ul>
     *
     * @param ctx The conditional expression context from the parser
     * @return A pair where:
     * <ul>
     *     <li>left: the condition node (entry point)</li>
     *     <li>right: the NoOp or end node (exit point)</li>
     * </ul>
     * @see #visitKleene(RegParser.KleeneContext)
     * @see #processBlock(List, Expression, SourceCodeLocation, boolean)
     */
    @Override
    public Pair<Statement, Statement> visitCond(RegParser.CondContext ctx) {
        SourceCodeLocation loc = new SourceCodeLocation(file, getLine(ctx), getCol(ctx));

        // Visit the condition expression
        Expression cond = (Expression) visit(ctx.b());
        currentCFG.addNode(cond);

        // Handle the case where there's no then branch (assertion-like)
        if (ctx.e().isEmpty()) {
            NoOp noop = new NoOp(currentCFG, loc);
            currentCFG.addNode(noop);
            currentCFG.addEdge(new TrueEdge(cond, noop));

            // Create a return node for the false branch
            Ret falseAssert = new Ret(currentCFG, new SourceCodeLocation(file, getLine(ctx) + 1, getCol(ctx)));
            currentCFG.addNode(falseAssert);
            currentCFG.addEdge(new FalseEdge(cond, falseAssert));
            log.info("Assert like Cond at {}", loc);
            return Pair.of(cond, noop);
        }

        // Process the conditional block
        return processConditionalBlock(ctx, cond, loc, false);
    }

    /**
     * Visits a Kleene star expression (while loop) and creates the corresponding structure
     * in the CFG with a loop back from the end of the body to the condition.
     * <p>
     * Grammar:
     * <code>e: LPAR b COND SEQ e (SEQ e)* RPAR TIMES</code>
     *
     * @param ctx The Kleene star expression context from the parser
     * @return A pair containing the condition node as entry and the end node as exit
     */
    @Override
    public Pair<Statement, Statement> visitKleene(RegParser.KleeneContext ctx) {
        SourceCodeLocation loc = new SourceCodeLocation(file, getLine(ctx), getCol(ctx));

        // Visit the condition expression
        Expression cond = (Expression) visit(ctx.b());
        currentCFG.addNode(cond);

        // Process the loop body
        return processConditionalBlock(ctx, cond, loc, true);
    }

    // endregion

    // region Arithmetic Expression Visitors

    /**
     * Visits a numeric literal and creates a corresponding Int32Literal node.
     * <p>
     * Grammar:
     * <code>a: NUM</code>
     *
     * @param ctx The numeric literal context from the parser
     * @return The Int32Literal expression
     */
    @Override
    public Expression visitNum(RegParser.NumContext ctx) {
        SourceCodeLocation loc = new SourceCodeLocation(file, getLine(ctx), getCol(ctx));
        log.info("Num at {}", loc);
        return new Int32Literal(currentCFG, loc, Integer.parseInt(ctx.NUM().getText()));
    }

    /**
     * Visits an identifier reference and creates a corresponding VariableRef node.
     * Also adds the variable to the descriptor's variable table.
     * <p>
     * Grammar:
     * <code>a: ID</code>
     *
     * @param ctx The identifier context from the parser
     * @return The VariableRef expression
     */
    @Override
    public Expression visitId(RegParser.IdContext ctx) {
        SourceCodeLocation loc = new SourceCodeLocation(file, getLine(ctx), getCol(ctx));

        // Register the variable in the variable table
        descriptor.addVariable(new VariableTableEntry(loc, 0, ctx.ID().getText()));

        // Create the variable reference
        VariableRef var = new VariableRef(currentCFG, loc, ctx.ID().getText());
        log.info("Created variable {}", var);
        return var;
    }

    /**
     * Visits an addition or subtraction expression and creates the corresponding
     * Addition or Subtraction node.
     * <p>
     * Grammar:
     * <code>a: a op = (PLUS | MINUS) a</code>
     *
     * @param ctx The addition or subtraction context from the parser
     * @return The Addition or Subtraction expression
     */
    @Override
    public Expression visitPlus_minus(RegParser.Plus_minusContext ctx) {
        SourceCodeLocation loc = new SourceCodeLocation(file, getLine(ctx), getCol(ctx));
        Expression left = (Expression) visit(ctx.a(0));
        Expression right = (Expression) visit(ctx.a(1));

        // Determine the operator type and create the appropriate expression
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

    /**
     * Visits a multiplication expression and creates a corresponding Multiplication node.
     * There's a particular visitor for this operation to handle precedence correctly.
     * <p>
     * Grammar:
     * <code>a: a TIMES a</code>
     *
     * @param ctx The multiplication context from the parser
     * @return The Multiplication expression
     */
    @Override
    public Expression visitTimes(RegParser.TimesContext ctx) {
        SourceCodeLocation loc = new SourceCodeLocation(file, getLine(ctx), getCol(ctx));
        Expression left = (Expression) visit(ctx.a(0));
        Expression right = (Expression) visit(ctx.a(1));
        log.info("Multiplication at {}", loc);
        return new Multiplication(currentCFG, loc, left, right);
    }

    /**
     * Visits a parenthesized arithmetic expression and delegates to the enclosed expression.
     * <p>
     * Grammar:
     * <code>a: LPAR a RPAR</code>
     *
     * @param ctx The parenthesized arithmetic expression context from the parser
     * @return The result of visiting the enclosed expression
     */
    @Override
    public Expression visitA_par(RegParser.A_parContext ctx) {
        log.info("Visiting a_par");
        return (Expression) visit(ctx.a());
    }

    // endregion

    // region Boolean Expression Visitors

    /**
     * Visits a parenthesized boolean expression and delegates to the enclosed expression.
     * <p>
     * Grammar:
     * <code>b: LPAR b RPAR</code>
     *
     * @param ctx The parenthesized boolean expression context from the parser
     * @return The result of visiting the enclosed expression
     */
    @Override
    public Expression visitB_par(RegParser.B_parContext ctx) {
        log.info("Visiting b_par");
        return (Expression) visit(ctx.b());
    }

    /**
     * Visits a comparison expression (equal, less or equal, less than) and creates
     * the corresponding comparison node.
     * <p>
     * Grammar:
     * <code>b: a op = (EQ | LEQ | LE) a</code>
     *
     * @param ctx The comparison expression context from the parser
     * @return The comparison expression (Equal, LessOrEqual, or LessThan)
     */
    @Override
    public Expression visitEq_leq_le(RegParser.Eq_leq_leContext ctx) {
        SourceCodeLocation loc = new SourceCodeLocation(file, getLine(ctx), getCol(ctx));
        Expression left = (Expression) visit(ctx.a(0));
        Expression right = (Expression) visit(ctx.a(1));

        // Determine the operator type and create the appropriate expression
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

    /**
     * Visits a true literal and creates a corresponding TrueLiteral node.
     * <p>
     * Grammar:
     * <code>b: TRUE</code>
     *
     * @param ctx The true literal context from the parser
     * @return The TrueLiteral expression
     */
    @Override
    public Expression visitTrue(RegParser.TrueContext ctx) {
        SourceCodeLocation loc = new SourceCodeLocation(file, getLine(ctx), getCol(ctx));
        log.info("True at {}", loc);
        return new TrueLiteral(currentCFG, loc);
    }

    /**
     * Visits a false literal and creates a corresponding FalseLiteral node.
     * <p>
     * Grammar:
     * *     <code>b: FALSE</code>
     *
     * @param ctx The false literal context from the parser
     * @return The FalseLiteral expression
     */
    @Override
    public Expression visitFalse(RegParser.FalseContext ctx) {
        SourceCodeLocation loc = new SourceCodeLocation(file, getLine(ctx), getCol(ctx));
        log.info("False at {}", loc);
        return new FalseLiteral(currentCFG, loc);
    }

    /**
     * Visits a logical AND expression and creates a corresponding And node.
     * <p>
     * Grammar:
     * <code>b: b AND b</code>
     *
     * @param ctx The logical AND expression context from the parser
     * @return The And expression
     */
    @Override
    public Expression visitAnd(RegParser.AndContext ctx) {
        SourceCodeLocation loc = new SourceCodeLocation(file, getLine(ctx), getCol(ctx));
        Expression left = (Expression) visit(ctx.b(0));
        Expression right = (Expression) visit(ctx.b(1));
        log.info("And at {}", loc);
        return new And(currentCFG, loc, left, right);
    }

    /**
     * Visits a logical NOT expression and creates a corresponding Not node.
     * <p>
     * Grammar:
     * <code>b: NOT b</code>
     *
     * @param ctx The logical NOT expression context from the parser
     * @return The Not expression
     */
    @Override
    public Expression visitNot(RegParser.NotContext ctx) {
        SourceCodeLocation loc = new SourceCodeLocation(file, getLine(ctx), getCol(ctx));
        Expression exp = (Expression) visit(ctx.b());
        log.info("Not at {}", loc);
        return new Not(currentCFG, loc, exp);
    }

    // endregion

    // region Helper Methods

    /**
     * Helper method to process a conditional block from a Cond context.
     * Delegates to the generic processBlock method.
     *
     * @param ctx      The conditional expression context
     * @param cond     The condition expression node
     * @param loc      The source code location
     * @param isKleene True if this is a Kleene star (loop), false otherwise
     * @return A pair containing the condition node as entry and the end node as exit
     */
    @SuppressWarnings("SameParameterValue")
    private Pair<Statement, Statement> processConditionalBlock(RegParser.CondContext ctx, Expression cond, SourceCodeLocation loc, boolean isKleene) {
        return processBlock(ctx.e(), cond, loc, isKleene);
    }

    /**
     * Helper method to process a conditional block from a Kleene context.
     * Delegates to the generic processBlock method.
     *
     * @param ctx      The Kleene star expression context
     * @param cond     The condition expression node
     * @param loc      The source code location
     * @param isKleene True if this is a Kleene star (loop), false otherwise
     * @return A pair containing the condition node as entry and the end node as exit
     */
    @SuppressWarnings("SameParameterValue")
    private Pair<Statement, Statement> processConditionalBlock(RegParser.KleeneContext ctx, Expression cond, SourceCodeLocation loc, boolean isKleene) {
        return processBlock(ctx.e(), cond, loc, isKleene);
    }

    /**
     * Creates the CFG structure for a conditional block or loop body based on a list of expressions.
     * <p>
     * This helper method is used by both conditional (if-then) and Kleene star (loop) constructs to:
     * <ol>
     *   <li>Visit each expression in the block and connect them sequentially</li>
     *   <li>Connect the condition to the first statement with a TrueEdge</li>
     *   <li>Connect the last statement appropriately based on a construct type:</li>
     *     <ul>
     *       <li>For conditionals: connect to the end NoOp node</li>
     *       <li>For loops (Kleene): connect back to the condition node</li>
     *     </ul>
     *   <li>Connect the condition's false branch to the end node</li>
     * </ol>
     * <p>
     * CFG structure for conditional (isKleene = false):
     * <pre>
     *         ┌─── TrueEdge ───→ block ───→ end
     *         │                              ▲
     *       cond                             │
     *         │                              │
     *         └─── FalseEdge ────────────────┘
     * </pre>
     * <p>
     * CFG structure for loop (isKleene = true):
     * <pre>
     *         ┌─── TrueEdge ───→ block ──┐
     *         │                          │
     *       cond ←───────────────────────┘
     *         │
     *         └─── FalseEdge ───→ end
     * </pre>
     *
     * @param expressions The list of expressions in the block
     * @param cond        The condition expression node
     * @param loc         The source code location for new nodes
     * @param isKleene    True if this is a Kleene star (loop), false if it's a conditional
     * @return A pair where:
     * <ul>
     *     <li>left: the condition node (entry point of the structure)</li>
     *     <li>right: the end node (exit point of the structure)</li>
     * </ul>
     */
    @SuppressWarnings("unchecked")
    private Pair<Statement, Statement> processBlock(List<? extends RegParser.EContext> expressions, Expression cond, SourceCodeLocation loc, boolean isKleene) {
        // Visit the first expression in the block
        Pair<Statement, Statement> firstPair = (Pair<Statement, Statement>) visit(expressions.get(0));
        currentCFG.addEdge(new TrueEdge(cond, firstPair.getLeft()));

        Pair<Statement, Statement> lastPair = firstPair;

        // Visit and connect each expression in the block sequentially
        for (int i = 1; i < expressions.size(); i++) {
            Pair<Statement, Statement> pair = (Pair<Statement, Statement>) visit(expressions.get(i));
            currentCFG.addNode(pair.getLeft());
            currentCFG.addEdge(new SequentialEdge(lastPair.getRight(), pair.getLeft()));
            lastPair = pair;
        }

        // Create an end node for the block
        NoOp end = new NoOp(currentCFG, loc);
        currentCFG.addNode(end);

        // Connect the last statement to either the condition (for loops) or the end node
        if (isKleene) {
            log.info("Kleene at {}", loc);
            // For Kleene star (loops), connect back to the condition
            currentCFG.addEdge(new SequentialEdge(lastPair.getRight(), cond));
        } else {
            log.info("Cond at {}", loc);
            // For conditionals, connect to the end node
            currentCFG.addEdge(new SequentialEdge(lastPair.getRight(), end));
        }

        // Connect the false branch of the condition to the end node to create an exit point
        currentCFG.addEdge(new FalseEdge(cond, end));

        return Pair.of(cond, end);
    }

    // endregion
}