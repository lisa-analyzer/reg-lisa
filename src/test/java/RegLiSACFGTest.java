import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.conf.LiSAConfiguration;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class RegLiSACFGTest extends RegLiSAAnalysisExecutor {
    @SuppressWarnings("SameParameterValue")
    private static CronConfiguration createConfiguration(String subDir, boolean generateCfg) {
        CronConfiguration conf = new CronConfiguration();
        conf.testDir = "cfg";
        conf.testSubDir = subDir;
        conf.programFile = "example.reg";
        conf.serializeResults = true;
        conf.serializeInputs = true;
        conf.jsonOutput = true;
        if (generateCfg)
            conf.analysisGraphs = LiSAConfiguration.GraphType.DOT;
        return conf;
    }

    @Test
    public void testCFG01() throws AnalysisSetupException, IOException {
        CronConfiguration conf = createConfiguration("cfg-01", false);
        perform(conf);
    }

    @Test
    public void testCFG02() throws AnalysisSetupException, IOException {
        CronConfiguration conf = createConfiguration("cfg-02", false);
        perform(conf);
    }

    @Test
    public void testCFG03() throws AnalysisSetupException, IOException {
        CronConfiguration conf = createConfiguration("cfg-03", false);
        perform(conf);
    }

    @Test
    public void testCFG04() throws AnalysisSetupException, IOException {
        CronConfiguration conf = createConfiguration("cfg-04", false);
        perform(conf);
    }

    @Test
    public void testCFG05() throws AnalysisSetupException, IOException {
        CronConfiguration conf = createConfiguration("cfg-05", false);
        perform(conf);
    }

    @Test
    public void testCFG06() throws AnalysisSetupException, IOException {
        CronConfiguration conf = createConfiguration("cfg-06", false);
        perform(conf);
    }

    @Test
    public void testCFG07() throws AnalysisSetupException, IOException {
        CronConfiguration conf = createConfiguration("cfg-07", false);
        perform(conf);
    }

    @Test
    public void testCFG08() throws AnalysisSetupException, IOException {
        CronConfiguration conf = createConfiguration("cfg-08", false);
        perform(conf);
    }

    @Test
    public void testCFG09() throws AnalysisSetupException, IOException {
        CronConfiguration conf = createConfiguration("cfg-09", false);
        perform(conf);
    }

    @Test
    public void testCFG10() throws AnalysisSetupException, IOException {
        CronConfiguration conf = createConfiguration("cfg-10", false);
        perform(conf);
    }

    @Test
    public void testCFG11() throws AnalysisSetupException, IOException {
        CronConfiguration conf = createConfiguration("cfg-11", false);
        perform(conf);
    }

    @Test
    public void testCFG12() throws AnalysisSetupException, IOException {
        CronConfiguration conf = createConfiguration("cfg-12", false);
        perform(conf);
    }

    @Test
    public void testCFG13() throws AnalysisSetupException, IOException {
        CronConfiguration conf = createConfiguration("cfg-13", false);
        perform(conf);
    }

    @Test
    public void testCFG14() throws AnalysisSetupException {
        CronConfiguration conf = createConfiguration("cfg-14", false);
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> perform(conf));
        String expectedMessage = "Variable x not declared";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    public void testCFG15() throws AnalysisSetupException, IOException {
        CronConfiguration conf = createConfiguration("cfg-15", false);
        perform(conf);
    }
}
