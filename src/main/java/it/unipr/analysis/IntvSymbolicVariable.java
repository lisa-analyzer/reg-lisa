package it.unipr.analysis;

import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.type.Type;

/**
 * A {@link SymbolicVariable} marker produced when an {@code inputIntv()}
 * expression is evaluated. Evaluators check {@code instanceof
 * IntvSymbolicVariable} to return the appropriate abstract value:
 * <ul>
 * <li>sign analysis → {@code TOP};</li>
 * <li>interval analysis → {@code [0.0, 1.0]}.</li>
 * </ul>
 *
 * @author <a href="mailto:vincenzoarceri.92@gmail.com">Vincenzo Arceri</a>
 */
public class IntvSymbolicVariable extends SymbolicVariable {

	public IntvSymbolicVariable(Type staticType, String name, CodeLocation location) {
		super(staticType, name, location);
	}
}
