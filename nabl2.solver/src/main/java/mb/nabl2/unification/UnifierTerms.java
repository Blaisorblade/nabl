package mb.nabl2.unification;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.List;
import java.util.stream.Collectors;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.IUnifier;

public final class UnifierTerms {

    private final IUnifier unifier;

    private UnifierTerms(IUnifier unifier) {
        this.unifier = unifier;
    }

    private ITerm build() {
        List<ITerm> entries = unifier.varSet().stream().map(this::buildVar).collect(Collectors.toList());
        return B.newAppl("Unifier", (ITerm) B.newList(entries));
    }

    private ITerm buildVar(ITermVar var) {
        return B.newTuple(var, unifier.findRecursive(var));
    }

    public static ITerm build(IUnifier unifier) {
        return new UnifierTerms(unifier).build();
    }

}