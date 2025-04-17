package it.unipr.frontend.reg;

import it.unive.lisa.program.language.LanguageFeatures;
import it.unive.lisa.program.language.hierarchytraversal.HierarcyTraversalStrategy;
import it.unive.lisa.program.language.hierarchytraversal.SingleInheritanceTraversalStrategy;
import it.unive.lisa.program.language.parameterassignment.ParameterAssigningStrategy;
import it.unive.lisa.program.language.parameterassignment.PythonLikeAssigningStrategy;
import it.unive.lisa.program.language.resolution.JavaLikeMatchingStrategy;
import it.unive.lisa.program.language.resolution.ParameterMatchingStrategy;
import it.unive.lisa.program.language.validation.BaseValidationLogic;
import it.unive.lisa.program.language.validation.ProgramValidationLogic;

/**
 * The language features of the RegLiSA language.
 * This class is used to define the strategies used by the language to resolve and validate programs.
 * 
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 * @author <a href="mailto:francesco.marastoni_02@studenti.univr.it">Francesco Marastoni</a>
 * @author <a href="mailto:amos.loverde@studenti.univr.it">Amos Lo Verde</a>
 */
public class RegLiSAFeatures extends LanguageFeatures {

    @Override
    public ParameterMatchingStrategy getMatchingStrategy() {
        return JavaLikeMatchingStrategy.INSTANCE;
    }

    @Override
    public HierarcyTraversalStrategy getTraversalStrategy() {
        return SingleInheritanceTraversalStrategy.INSTANCE;
    }

    @Override
    public ParameterAssigningStrategy getAssigningStrategy() {
        return PythonLikeAssigningStrategy.INSTANCE;
    }

    @Override
    public ProgramValidationLogic getProgramValidationLogic() {
        return new BaseValidationLogic();
    }
}