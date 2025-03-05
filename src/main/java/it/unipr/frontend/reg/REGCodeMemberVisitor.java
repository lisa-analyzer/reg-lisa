package it.unipr.frontend.reg;

import it.unipr.reg.antlr.RegParserBaseVisitor;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.VariableTableEntry;
import it.unive.lisa.program.cfg.controlFlow.ControlFlowStructure;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.edge.SequentialEdge;
import it.unive.lisa.program.cfg.statement.Ret;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.util.datastructures.graph.code.NodeList;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.util.*;

public class REGCodeMemberVisitor extends RegParserBaseVisitor<Object> {
    private final String file;

    private final NodeList<CFG, Statement, Edge> list;

    private final Collection<Statement> entrypoints;

    private final Collection<ControlFlowStructure> cfs;

    private final Map<String, Pair<VariableRef, Annotations>> visibleIds;

    private final CFG cfg;

    private final CodeMemberDescriptor descriptor;


    public REGCodeMemberVisitor(
            String file,
            CodeMemberDescriptor descriptor
    ) {
        this.file = file;
        this.descriptor = descriptor;
        list = new NodeList<>(new SequentialEdge());
        entrypoints = new HashSet<>();
        cfs = new LinkedList<>();
        cfg = new CFG(descriptor, entrypoints, list);

        visibleIds = new HashMap<>();
        for (VariableTableEntry par : descriptor.getVariables())
            visibleIds.put(par.getName(), Pair.of(par.createReference(cfg), par.getAnnotations()));

    }
}
