package it.unipr.frontend.reg.types;

import it.unive.lisa.program.type.BoolType;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.program.type.StringType;
import it.unive.lisa.type.BooleanType;
import it.unive.lisa.type.NumericType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;

public class REGTypeSystem extends TypeSystem {

    @Override
    public BooleanType getBooleanType() {
        return BoolType.INSTANCE;
    }

    @Override
    public StringType getStringType() {
        return StringType.INSTANCE;
    }

    @Override
    public NumericType getIntegerType() {
        return Int32Type.INSTANCE;
    }

    @Override
    public boolean canBeReferenced(
            Type type) {
        return type.isInMemoryType();
    }

}