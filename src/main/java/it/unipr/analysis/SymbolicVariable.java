package it.unipr.analysis;

import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Type;

public class SymbolicVariable extends Variable {

	protected SymbolicVariable(
			Type staticType,
			String name,
			CodeLocation location) {
		super(staticType, name, new Annotations(), location);
	}

	@Override
	public boolean canBeScoped() {
		return true;
	}

	@Override
	public String toString() {
		return super.toString() + "_sym";
	}
}
