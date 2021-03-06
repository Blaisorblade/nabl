package mb.nabl2.spoofax.primitives;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.List;
import java.util.Optional;

import org.spoofax.interpreter.core.InterpreterException;

import mb.nabl2.scopegraph.terms.Scope;
import mb.nabl2.solver.ISolution;
import mb.nabl2.terms.ITerm;

public class SG_get_reachable_decls extends AnalysisPrimitive {

    public SG_get_reachable_decls() {
        super(SG_get_reachable_decls.class.getSimpleName());
    }

    @Override public Optional<? extends ITerm> call(ISolution solution, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        return Scope.matcher().match(term, solution.unifier()).<ITerm>flatMap(scope -> {
            return solution.nameResolution().reachable(scope).map(B::newList);
        });
    }

}