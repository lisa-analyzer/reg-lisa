import it.unipr.analysis.SymbolicAbstractDomain;
import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.analysis.SimpleAbstractState;
import it.unive.lisa.analysis.heap.MonolithicHeap;
import it.unive.lisa.analysis.nonrelational.value.TypeEnvironment;
import it.unive.lisa.analysis.types.InferredTypes;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.interprocedural.ModularWorstCaseAnalysis;
import it.unive.lisa.interprocedural.callgraph.RTACallGraph;
import java.io.IOException;
import org.junit.Test;

public class SymbolicTest extends RegLiSAAnalysisExecutor {

	private static CronConfiguration createConfiguration(String subDir, boolean generateCfg) {
		CronConfiguration conf = new CronConfiguration();
		conf.testDir = "symbolic";
		conf.testSubDir = subDir;
		conf.programFile = "example.reg";
		conf.serializeInputs = false;
		conf.jsonOutput = true;
		conf.abstractState = new SimpleAbstractState<>(new MonolithicHeap(), new SymbolicAbstractDomain(),
				new TypeEnvironment<>(new InferredTypes()));
		if (generateCfg)
			conf.analysisGraphs = LiSAConfiguration.GraphType.HTML_WITH_SUBNODES;
		conf.serializeResults = true;
		conf.callGraph = new RTACallGraph();
		conf.interproceduralAnalysis = new ModularWorstCaseAnalysis<>();
		return conf;
	}

	@Test
	public void testSymbolic() throws AnalysisSetupException, IOException {
		CronConfiguration conf = createConfiguration("test", true);
		perform(conf);
	}
	
	@Test
	public void testSymbolic1() throws AnalysisSetupException, IOException {
		CronConfiguration conf = createConfiguration("test1", true);
		perform(conf);
	}
	
	@Test
	public void testSymbolic2() throws AnalysisSetupException, IOException {
		CronConfiguration conf = createConfiguration("test2", true);
		perform(conf);
	}
	
	@Test
	public void testSymbolic3() throws AnalysisSetupException, IOException {
		CronConfiguration conf = createConfiguration("test3", true);
		perform(conf);
	}
	
	@Test
	public void testSymbolic4() throws AnalysisSetupException, IOException {
		CronConfiguration conf = createConfiguration("test4", true);
		perform(conf);
	}
	
	@Test
	public void testSymbolic5() throws AnalysisSetupException, IOException {
		CronConfiguration conf = createConfiguration("test5", true);
		perform(conf);
	}
	
	@Test
	public void testSymbolic7() throws AnalysisSetupException, IOException {
		CronConfiguration conf = createConfiguration("test7", true);
		perform(conf);
	}
	
	@Test
	public void testSymbolic8() throws AnalysisSetupException, IOException {
		CronConfiguration conf = createConfiguration("test8", true);
		perform(conf);
	}
	
	@Test
	public void testSymbolic9() throws AnalysisSetupException, IOException {
		CronConfiguration conf = createConfiguration("test9", true);
		perform(conf);
	}
}
