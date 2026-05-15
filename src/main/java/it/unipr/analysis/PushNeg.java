package it.unipr.analysis;

import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.type.Type;

/**
 * A {@link PushAny} marker that signals a negative-constrained input
 * ({@code inputNeg()}). The symbolic domain will record {@code x_sym < 0} in
 * the path condition when this expression is assigned to a variable.
 *
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 */
public class PushNeg extends PushAny {

	public PushNeg(Type type, CodeLocation location) {
		super(type, location);
	}
}
