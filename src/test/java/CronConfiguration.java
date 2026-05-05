
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.util.testing.TestConfiguration;

/**
 * An extended {@link LiSAConfiguration} that also holds test configuration
 * keys. This configuration disables optimizations
 * ({@link LiSAConfiguration#optimize}) by default.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class CronConfiguration extends TestConfiguration {

}