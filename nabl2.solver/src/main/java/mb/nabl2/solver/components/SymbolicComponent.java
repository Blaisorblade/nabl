package mb.nabl2.solver.components;

import java.util.Optional;

import io.usethesource.capsule.Set;
import mb.nabl2.constraints.messages.IMessageInfo;
import mb.nabl2.constraints.sym.ISymbolicConstraint;
import mb.nabl2.solver.ASolver;
import mb.nabl2.solver.ISolver.SeedResult;
import mb.nabl2.solver.ISolver.SolveResult;
import mb.nabl2.solver.SolverCore;
import mb.nabl2.symbolic.ISymbolicConstraints;
import mb.nabl2.symbolic.ImmutableSymbolicConstraints;
import mb.nabl2.terms.ITerm;

public class SymbolicComponent extends ASolver {

    private final Set.Transient<ITerm> facts;
    private final Set.Transient<ITerm> goals;

    public SymbolicComponent(SolverCore core, ISymbolicConstraints initial) {
        super(core);
        this.facts = initial.getFacts().asTransient();
        this.goals = initial.getGoals().asTransient();
    }

    public SeedResult seed(ISymbolicConstraints solution, @SuppressWarnings("unused") IMessageInfo message)
            throws InterruptedException {
        facts.__insertAll(solution.getFacts());
        goals.__insertAll(solution.getGoals());
        return SeedResult.empty();
    }

    public Optional<SolveResult> solve(ISymbolicConstraint constraint) throws InterruptedException {
        constraint.match(ISymbolicConstraint.Cases.of(
            // @formatter:off
            fact -> facts.__insert(fact.getFact()),
            goal -> goals.__insert(goal.getGoal())
            // @formatter:on
        ));
        return Optional.of(SolveResult.empty());
    }

    public ISymbolicConstraints finish() {
        return ImmutableSymbolicConstraints.of(facts.freeze(), goals.freeze());
    }

}
