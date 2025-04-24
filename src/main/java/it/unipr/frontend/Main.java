package it.unipr.frontend;

import it.unipr.frontend.reg.RegLiSAFrontend;
import it.unive.lisa.LiSA;
import it.unive.lisa.analysis.SimpleAbstractState;
import it.unive.lisa.analysis.heap.MonolithicHeap;
import it.unive.lisa.analysis.nonrelational.value.TypeEnvironment;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.numeric.Interval;
import it.unive.lisa.analysis.types.InferredTypes;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.interprocedural.ModularWorstCaseAnalysis;
import it.unive.lisa.interprocedural.callgraph.RTACallGraph;
import it.unive.lisa.program.Program;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main {
    private static final Logger log = LogManager.getLogger(Main.class);

    public static void main(String[] args) {
        Options options = getOptions();

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        formatter.setWidth(120);
        formatter.setLeftPadding(4);
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);

            if (cmd.hasOption("h")) {
                formatter.printHelp("java -jar reg-lisa-all.jar [-a] [-f <file>] [-g <type>] [-o <dir>] [-h] [-v]", options);
                return;
            }

            if (cmd.hasOption("v")) {
                System.out.println("RegLiSA version 1.0.0");
                return;
            }

            String file = cmd.getOptionValue("file", "reglisa-testcases/runtime.reg");
            boolean analysis = cmd.hasOption("analysis");
            String graph = cmd.getOptionValue("graph", "HTML").toUpperCase();
            String outputDir = cmd.getOptionValue("output", "reglisa-outputs");

            log.info("Running RegLiSA with the following options:");
            log.info("\tFile: {}", file);
            log.info("\tAnalysis: {}", analysis ? "enabled" : "disabled");
            log.info("\tGraph type: {}", graph);
            log.info("\tOutput dir: {}", outputDir);

            LiSAConfiguration conf = new LiSAConfiguration();

            conf.workdir = outputDir;
            conf.serializeResults = true;
            conf.serializeInputs = true;
            conf.jsonOutput = true;
            conf.analysisGraphs = graph.equals("DOT")
                    ? LiSAConfiguration.GraphType.DOT
                    : LiSAConfiguration.GraphType.HTML;

            if (analysis) {
                conf.abstractState = new SimpleAbstractState<>(
                        new MonolithicHeap(),
                        new ValueEnvironment<>(new Interval()),
                        new TypeEnvironment<>(new InferredTypes())
                );

                // by disabling useWideningPoints
                // the widening is applied in all points of the program
                conf.useWideningPoints = false;
                conf.optimize = false;
                conf.interproceduralAnalysis = new ModularWorstCaseAnalysis<>();
                conf.callGraph = new RTACallGraph();
            } else
                log.info("Analysis is disabled. Creating only the control flow graph.");

            LiSA lisa = new LiSA(conf);
            Program program = RegLiSAFrontend.processFile(file);
            lisa.run(program);
        } catch (ParseException e) {
            System.err.println("Parsing failed: " + e.getMessage());
            formatter.printHelp("java -jar reg-lisa-all.jar [-a] [-f <file>] [-g <type>] [-o <dir>] [-h] [-v]", options);
        } catch (Exception e) {
            log.error("Unexpected error: {}", e.getMessage());
        } finally {
            log.info("Execution finished.");
        }
    }

    private static Options getOptions() {
        Options options = new Options();

        options.addOption("f", "file", true, "Input .reg file (default: reglisa-testcases/runtime.reg)");
        options.addOption("a", "analysis", false, "Enable analysis (default: disabled)");
        options.addOption("g", "graph", true, "Graph type: DOT or HTML (default: HTML)");
        options.addOption("o", "output", true, "Output directory (default: reglisa-outputs)");
        options.addOption("h", "help", false, "Print help");
        options.addOption("v", "version", false, "Print version");
        return options;
    }
}
