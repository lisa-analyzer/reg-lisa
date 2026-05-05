import java.io.IOException;

import org.junit.Test;

import it.unipr.analysis.CombinationDomain;
import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.analysis.SimpleAbstractDomain;
import it.unive.lisa.analysis.heap.MonolithicHeap;
import it.unive.lisa.analysis.types.InferredTypes;
import it.unive.lisa.interprocedural.ModularWorstCaseAnalysis;
import it.unive.lisa.interprocedural.callgraph.RTACallGraph;
import it.unive.lisa.outputs.HtmlResults;
import it.unive.lisa.outputs.JSONReportDumper;
import it.unive.lisa.outputs.JSONResults;

public class SymbolicTest extends RegLiSAAnalysisExecutor {

	private static CronConfiguration createConfiguration(String subDir, boolean generateCfg) {
		CronConfiguration conf = new CronConfiguration();
		conf.testDir = "symbolic/" + subDir;
		conf.programFile = "example.reg";
		conf.analysis = new SimpleAbstractDomain<>(new MonolithicHeap(), new CombinationDomain(),
				new InferredTypes());
		
		conf.outputs.add(new JSONReportDumper());
		conf.outputs.add(new JSONResults<>());
		conf.useWideningPoints = false;
		conf.compareWithOptimization = false;
		if (generateCfg)
			conf.outputs.add(new HtmlResults<>(true));
		conf.callGraph = new RTACallGraph();
		conf.interproceduralAnalysis = new ModularWorstCaseAnalysis<>();
		return conf;
	}

	@Test
	public void testNN() throws AnalysisSetupException, IOException {
		CronConfiguration conf = createConfiguration("NN", false);
		perform(conf);
	}

	@Test
	public void testSymbolic1() throws AnalysisSetupException, IOException {
		CronConfiguration conf = createConfiguration("test1", false);
		perform(conf);
	}

	@Test
	public void testSymbolic2() throws AnalysisSetupException, IOException {
		CronConfiguration conf = createConfiguration("test2", false);
		perform(conf);
	}

	@Test
	public void testSymbolic3() throws AnalysisSetupException, IOException {
		CronConfiguration conf = createConfiguration("test3", false);
		perform(conf);
	}

	@Test
	public void testSymbolic4() throws AnalysisSetupException, IOException {
		CronConfiguration conf = createConfiguration("test4", false);
		perform(conf);
	}

	@Test
	public void testSymbolic5() throws AnalysisSetupException, IOException {
		CronConfiguration conf = createConfiguration("test5", false);
		perform(conf);
	}

	@Test
	public void testSymbolic6() throws AnalysisSetupException, IOException {
		CronConfiguration conf = createConfiguration("test6", false);
		perform(conf);
	}

	@Test
	public void testSymbolic7() throws AnalysisSetupException, IOException {
		CronConfiguration conf = createConfiguration("test7", false);
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

	@Test
	public void testSymbolic10() throws AnalysisSetupException, IOException {
		CronConfiguration conf = createConfiguration("test10", false);
		conf.forceUpdate = true;
		perform(conf);
	}

	@Test
	public void testSymbolic11() throws AnalysisSetupException, IOException {
		CronConfiguration conf = createConfiguration("test11", false);
		perform(conf);
	}

	@Test
	public void testSymbolic12() throws AnalysisSetupException, IOException {
		CronConfiguration conf = createConfiguration("test12", false);
		perform(conf);
	}
}
