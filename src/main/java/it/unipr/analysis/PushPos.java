package it.unipr.analysis;

import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.type.Type;

/**
 * A {@link PushAny} marker that signals a positive-constrained input
 * ({@code inputPos()}). The symbolic domain will record {@code x_sym > 0} in
 * the path condition when this expression is assigned to a variable.
 *
 * @author <a href="mailto:vincenzoarceri.92@gmail.com">Vincenzo Arceri</a>
 */
public class PushPos extends PushAny {

	public PushPos(Type type, CodeLocation location) {
		super(type, location);
	}
}
