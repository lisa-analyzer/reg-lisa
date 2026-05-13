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
import java.io.IOException;
import org.junit.Test;

public class SymbolicTest extends RegLiSAAnalysisExecutor {

	private static CronConfiguration signConf(String subDir, boolean generateCfg) {
		CronConfiguration conf = new CronConfiguration();
		conf.testDir = "symbolic/" + subDir;
		conf.programFile = "example.reg";
		conf.analysis = new SimpleAbstractDomain<>(new MonolithicHeap(), CombinationDomain.forSign(),
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
	
	private static CronConfiguration intvConf(String subDir, boolean generateCfg) {
		CronConfiguration conf = new CronConfiguration();
		conf.testDir = "symbolic/" + subDir;
		conf.programFile = "example.reg";
		conf.analysis = new SimpleAbstractDomain<>(new MonolithicHeap(), CombinationDomain.forInterval(),
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
	public void testSimpleNN() throws AnalysisSetupException, IOException {
		CronConfiguration conf = intvConf("simpleNN", false);
		perform(conf);
	}
	
	@Test
	public void testNN() throws AnalysisSetupException, IOException {
		CronConfiguration conf = intvConf("NN", true);
		perform(conf);
	}

	@Test
	public void testSymbolic1() throws AnalysisSetupException, IOException {
		CronConfiguration conf = signConf("test1", false);
		perform(conf);
	}

	@Test
	public void testSymbolic2() throws AnalysisSetupException, IOException {
		CronConfiguration conf = signConf("test2", false);
		perform(conf);
	}

	@Test
	public void testSymbolic3() throws AnalysisSetupException, IOException {
		CronConfiguration conf = signConf("test3", false);
		perform(conf);
	}

	@Test
	public void testSymbolic4() throws AnalysisSetupException, IOException {
		CronConfiguration conf = signConf("test4", false);
		perform(conf);
	}

	@Test
	public void testSymbolic5() throws AnalysisSetupException, IOException {
		CronConfiguration conf = signConf("test5", false);
		perform(conf);
	}

	@Test
	public void testSymbolic6() throws AnalysisSetupException, IOException {
		CronConfiguration conf = signConf("test6", false);
		perform(conf);
	}

	@Test
	public void testSymbolic7() throws AnalysisSetupException, IOException {
		CronConfiguration conf = signConf("test7", false);
		perform(conf);
	}

	@Test
	public void testSymbolic8() throws AnalysisSetupException, IOException {
		CronConfiguration conf = signConf("test8", false);
		perform(conf);
	}

	@Test
	public void testSymbolic9() throws AnalysisSetupException, IOException {
		CronConfiguration conf = signConf("test9", false);
		perform(conf);
	}

	@Test
	public void testSymbolic10() throws AnalysisSetupException, IOException {
		CronConfiguration conf = signConf("test10", false);
		perform(conf);
	}

	@Test
	public void testSymbolic11() throws AnalysisSetupException, IOException {
		CronConfiguration conf = signConf("test11", false);
		perform(conf);
	}

	@Test
	public void testSymbolic12() throws AnalysisSetupException, IOException {
		CronConfiguration conf = signConf("test12", false);
		perform(conf);
	}

	@Test
	public void testSymbolic13() throws AnalysisSetupException, IOException {
		CronConfiguration conf = signConf("test13", false);
		perform(conf);
	}

	@Test
	public void testSymbolic14() throws AnalysisSetupException, IOException {
		CronConfiguration conf = signConf("test14", false);
		perform(conf);
	}

	@Test
	public void testSymbolic15() throws AnalysisSetupException, IOException {
		CronConfiguration conf = signConf("test15", false);
		perform(conf);
	}

	@Test
	public void testSymbolic16() throws AnalysisSetupException, IOException {
		CronConfiguration conf = signConf("test16", false);
		perform(conf);
	}

	@Test
	public void testSymbolic17() throws AnalysisSetupException, IOException {
		CronConfiguration conf = signConf("test17", false);
		perform(conf);
	}

	@Test
	public void testSymbolic18() throws AnalysisSetupException, IOException {
		CronConfiguration conf = signConf("test18", false);
		perform(conf);
	}
}
