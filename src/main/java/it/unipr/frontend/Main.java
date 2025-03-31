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

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        String file = args[0];
        System.out.println("File: " + file);

        Program program = RegLiSAFrontend.processFile(args[0]);
        LiSAConfiguration conf = new LiSAConfiguration();
        conf.workdir = "reglisa-outputs";
        conf.analysisGraphs = LiSAConfiguration.GraphType.DOT;

        conf.abstractState = new SimpleAbstractState<>(
                new MonolithicHeap(),
                new ValueEnvironment<>(new Interval()),
                new TypeEnvironment<>(new InferredTypes())
        );

        conf.interproceduralAnalysis = new ModularWorstCaseAnalysis<>();
        conf.callGraph = new RTACallGraph();
        conf.serializeResults = true;
        conf.optimize = false;
        conf.jsonOutput = true;

        LiSA lisa = new LiSA(conf);
        lisa.run(program);
    }

}
