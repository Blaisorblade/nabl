package mb.statix.solver.constraint;

import java.util.Optional;

import javax.annotation.Nullable;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
import mb.statix.solver.IConstraint;

public class CEqual implements IConstraint {

    private final ITerm term1;
    private final ITerm term2;

    private final @Nullable IConstraint cause;

    public CEqual(ITerm term1, ITerm term2) {
        this(term1, term2, null);
    }

    public CEqual(ITerm term1, ITerm term2, @Nullable IConstraint cause) {
        this.term1 = term1;
        this.term2 = term2;
        this.cause = cause;
    }

    public ITerm term1() {
        return term1;
    }

    public ITerm term2() {
        return term2;
    }

    @Override public Optional<IConstraint> cause() {
        return Optional.ofNullable(cause);
    }

    @Override public CEqual withCause(@Nullable IConstraint cause) {
        return new CEqual(term1, term2, cause);
    }

    @Override public <R> R match(Cases<R> cases) {
        return cases.caseEqual(this);
    }

    @Override public <R, E extends Throwable> R matchOrThrow(CheckedCases<R, E> cases) throws E {
        return cases.caseEqual(this);
    }

    @Override public CEqual apply(ISubstitution.Immutable subst) {
        return new CEqual(subst.apply(term1), subst.apply(term2), cause);
    }

    @Override public String toString(TermFormatter termToString) {
        final StringBuilder sb = new StringBuilder();
        sb.append(termToString.format(term1));
        sb.append(" == ");
        sb.append(termToString.format(term2));
        return sb.toString();
    }

    @Override public String toString() {
        return toString(ITerm::toString);
    }

}