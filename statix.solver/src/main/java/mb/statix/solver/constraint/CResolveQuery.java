package mb.statix.solver.constraint;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.metaborg.util.functions.Predicate2;

import com.google.common.collect.ImmutableSet;

import mb.nabl2.scopegraph.terms.Scope;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.PersistentUnifier;
import mb.statix.scopegraph.path.IResolutionPath;
import mb.statix.scopegraph.reference.DataEquiv;
import mb.statix.scopegraph.reference.DataWF;
import mb.statix.scopegraph.reference.NameResolution;
import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.solver.Completeness;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IDebugContext;
import mb.statix.solver.NullDebugContext;
import mb.statix.solver.Result;
import mb.statix.solver.State;
import mb.statix.solver.query.IQueryFilter;
import mb.statix.solver.query.IQueryMin;
import mb.statix.spec.Type;
import mb.statix.spoofax.StatixTerms;

public class CResolveQuery implements IConstraint {

    private final Optional<ITerm> relation;
    private final IQueryFilter filter;
    private final IQueryMin min;
    private final ITerm scopeTerm;
    private final ITerm resultTerm;

    public CResolveQuery(Optional<ITerm> relation, IQueryFilter filter, IQueryMin min, ITerm scopeTerm,
            ITerm resultTerm) {
        this.relation = relation;
        this.filter = filter;
        this.min = min;
        this.scopeTerm = scopeTerm;
        this.resultTerm = resultTerm;
    }

    @Override public IConstraint apply(ISubstitution.Immutable subst) {
        return new CResolveQuery(relation, filter.apply(subst), min.apply(subst), subst.apply(scopeTerm),
                subst.apply(resultTerm));
    }

    @Override public Optional<Result> solve(State state, Completeness completeness, IDebugContext debug)
            throws InterruptedException {
        final Type type;
        if(relation.isPresent()) {
            type = state.spec().relations().get(relation.get());
            if(type == null) {
                debug.error("Ignoring query for unknown relation {}", relation.get());
                return Optional.of(Result.of(state, ImmutableSet.of(new CFalse())));
            }
        } else {
            type = StatixTerms.SCOPE_REL_TYPE;
        }

        final IUnifier.Immutable unifier = state.unifier();
        if(!unifier.isGround(scopeTerm)) {
            return Optional.empty();
        }
        final Scope scope = Scope.matcher().match(scopeTerm, unifier)
                .orElseThrow(() -> new IllegalArgumentException("Expected scope, got " + unifier.toString(scopeTerm)));

        try {
            final IDebugContext subDebug = new NullDebugContext();
            final Predicate2<ITerm, ITerm> isComplete = (s, l) -> {
                if(completeness.isComplete(s, l, state)) {
                    subDebug.info("{} complete in {}", s, l);
                    return true;
                } else {
                    subDebug.info("{} incomplete in {}", s, l);
                    return false;
                }
            };
            // @formatter:off
            final NameResolution<ITerm, ITerm, ITerm> nameResolution = NameResolution.<ITerm, ITerm, ITerm>builder()
                    .withLabelWF(filter.getLabelWF(state, completeness, subDebug))
                    .withDataWF(filter(type, filter.getDataWF(state, completeness, subDebug), subDebug))
                    .withLabelOrder(min.getLabelOrder(state, completeness, subDebug))
                    .withDataEquiv(filter(type, min.getDataEquiv(state, completeness, subDebug), subDebug))
                    .withEdgeComplete(isComplete)
                    .withDataComplete(isComplete)
                    .build(state.scopeGraph(), relation);
            // @formatter:on
            final Set<IResolutionPath<ITerm, ITerm, ITerm>> paths = nameResolution.resolve(scope);
            final List<ITerm> pathTerms;
            if(relation.isPresent()) {
                pathTerms = paths.stream().map(p -> B.newTuple(B.newBlob(p.getPath()), B.newTuple(p.getDatum())))
                        .collect(Collectors.toList());
            } else {
                pathTerms = paths.stream().map(p -> B.newBlob(p.getPath())).collect(Collectors.toList());
            }
            final IConstraint C = new CEqual(B.newList(pathTerms), resultTerm);
            return Optional.of(Result.of(state, ImmutableSet.of(C)));
        } catch(ResolutionException e) {
            debug.info("Query resolution delayed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private DataWF<ITerm> filter(Type type, DataWF<ITerm> filter, IDebugContext debug) {
        return new DataWF<ITerm>() {
            public boolean wf(List<ITerm> datum) throws ResolutionException, InterruptedException {
                return filter.wf(filter(type, datum, debug));
            }
        };
    }

    private DataEquiv<ITerm> filter(Type type, DataEquiv<ITerm> filter, IDebugContext debug) {
        return new DataEquiv<ITerm>() {

            public boolean eq(List<ITerm> d1, List<ITerm> d2) throws ResolutionException, InterruptedException {
                return filter.eq(filter(type, d1, debug), filter(type, d2, debug));
            }

            public boolean alwaysTrue() throws InterruptedException {
                return filter.alwaysTrue();
            }

        };
    }

    private List<ITerm> filter(Type type, List<ITerm> datum, IDebugContext debug) throws ResolutionException {
        if(datum.size() != type.getArity()) {
            debug.error("Ignoring {}-ary data for {}-ary relation {}", datum.size(), type.getArity(), relation);
            throw new ResolutionException("Wrong data arity.");
        }
        return datum.stream().limit(type.getInputArity()).collect(Collectors.toList());
    }

    @Override public String toString(IUnifier unifier) {
        final StringBuilder sb = new StringBuilder();
        sb.append("query ");
        sb.append(relation);
        sb.append(" ");
        sb.append(filter.toString(unifier));
        sb.append(" ");
        sb.append(min.toString(unifier));
        sb.append(" in ");
        sb.append(unifier.toString(scopeTerm));
        sb.append(" |-> ");
        sb.append(unifier.toString(resultTerm));
        return sb.toString();
    }

    @Override public String toString() {
        return toString(PersistentUnifier.Immutable.of());
    }

}