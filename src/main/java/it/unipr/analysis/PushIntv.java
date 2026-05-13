package it.unipr.analysis;

import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.type.Type;

/**
 * A {@link PushAny} marker that signals an interval-constrained input
 * ({@code inputIntv()}). The symbolic domain will produce an
 * {@link IntvSymbolicVariable} when this expression is evaluated, so that
 * downstream evaluators can return {@code [0, 1]} for interval analysis and
 * {@code TOP} for sign analysis.
 *
 * @author <a href="mailto:vincenzoarceri.92@gmail.com">Vincenzo Arceri</a>
 */
public class PushIntv extends PushAny {

	public PushIntv(Type type, CodeLocation location) {
		super(type, location);
	}
}
