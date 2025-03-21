import java.io.IOException;

import org.junit.Test;

import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.conf.LiSAConfiguration;

public class RegLiSACFGTest extends RegLiSAAnalysisExecutor {


	private static CronConfiguration createConfiguration(String testDir, String subDir, String programFile,
			boolean generateCfg) {

		CronConfiguration conf = new CronConfiguration();
		conf.testDir = testDir;
		conf.testSubDir = subDir;
		conf.programFile = programFile;
		conf.serializeResults = true;
		conf.serializeInputs = true;
		conf.jsonOutput = true;
		if (generateCfg) {
			conf.analysisGraphs = LiSAConfiguration.GraphType.DOT;
		}
	
		return conf;
	}
	
	@Test
	public void testCFG01() throws AnalysisSetupException, IOException {
		CronConfiguration conf = createConfiguration("cfg", "cfg-01", "example.reg", false);
		perform(conf);
	}
}
