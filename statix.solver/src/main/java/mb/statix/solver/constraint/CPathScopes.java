package mb.statix.solver.constraint;

import java.util.Optional;

import javax.annotation.Nullable;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
import mb.statix.solver.IConstraint;

public class CPathScopes implements IConstraint {

    private final ITerm pathTerm;
    private final ITerm scopesTerm;

    private final @Nullable IConstraint cause;

    public CPathScopes(ITerm pathTerm, ITerm scopesTerm) {
        this(pathTerm, scopesTerm, null);
    }

    public CPathScopes(ITerm pathTerm, ITerm scopesTerm, @Nullable IConstraint cause) {
        this.pathTerm = pathTerm;
        this.scopesTerm = scopesTerm;
        this.cause = cause;
    }

    public ITerm pathTerm() {
        return pathTerm;
    }

    public ITerm scopesTerm() {
        return scopesTerm;
    }

    @Override public Optional<IConstraint> cause() {
        return Optional.ofNullable(cause);
    }

    @Override public CPathScopes withCause(@Nullable IConstraint cause) {
        return new CPathScopes(pathTerm, scopesTerm, cause);
    }

    @Override public <R> R match(Cases<R> cases) {
        return cases.casePathScopes(this);
    }

    @Override public <R, E extends Throwable> R matchOrThrow(CheckedCases<R, E> cases) throws E {
        return cases.casePathScopes(this);
    }

    @Override public CPathScopes apply(ISubstitution.Immutable subst) {
        return new CPathScopes(subst.apply(pathTerm), subst.apply(scopesTerm), cause);
    }

    @Override public String toString(TermFormatter termToString) {
        final StringBuilder sb = new StringBuilder();
        sb.append("scopes(");
        sb.append(termToString.format(pathTerm));
        sb.append(", ");
        sb.append(termToString.format(scopesTerm));
        sb.append(")");
        return sb.toString();
    }

    @Override public String toString() {
        return toString(ITerm::toString);
    }

}