import java.io.IOException;
import java.nio.file.Path;

import it.unipr.frontend.reg.RegLiSAFrontend;
import it.unive.lisa.program.Program;
import it.unive.lisa.util.testing.AnalysisTestExecutor;
import it.unive.lisa.util.testing.TestConfiguration;

public  class RegLiSAAnalysisExecutor extends AnalysisTestExecutor {

	protected static final String EXPECTED_RESULTS_DIR = "reglisa-testcases";
	protected static final String ACTUAL_RESULTS_DIR = "reglisa-outputs";
	
	
	public RegLiSAAnalysisExecutor() {
		super(EXPECTED_RESULTS_DIR, ACTUAL_RESULTS_DIR);
	}
	
	
	@Override
	public Program readProgram(TestConfiguration conf, Path target) {
		try {
			return RegLiSAFrontend.processFile(target.toString());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}	
}