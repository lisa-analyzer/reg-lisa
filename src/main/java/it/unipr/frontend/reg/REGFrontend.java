package it.unipr.frontend.reg;

import it.unipr.frontend.reg.types.REGTypeSystem;
import it.unipr.reg.antlr.RegLexer;
import it.unipr.reg.antlr.RegParser;
import it.unipr.reg.antlr.RegParserBaseVisitor;
import it.unive.lisa.program.*;
import it.unive.lisa.program.type.BoolType;
import it.unive.lisa.program.type.Int32Type;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class REGFrontend extends RegParserBaseVisitor<Object> {

    private final String file;

    private final Program program;

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
            visit(ctx.e(i));
        }
        System.out.println("Program execution finished");
        return program;
    }
}