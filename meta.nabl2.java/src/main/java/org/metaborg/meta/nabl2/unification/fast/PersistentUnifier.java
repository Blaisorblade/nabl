package org.metaborg.meta.nabl2.unification.fast;

import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import org.metaborg.meta.nabl2.terms.IListTerm;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermVar;
import org.metaborg.meta.nabl2.terms.ListTerms;
import org.metaborg.meta.nabl2.terms.Terms;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.meta.nabl2.util.tuples.ImmutableTuple2;
import org.metaborg.meta.nabl2.util.tuples.Set2;
import org.metaborg.meta.nabl2.util.tuples.Tuple2;
import org.metaborg.util.Ref;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import io.usethesource.capsule.Map;

public abstract class PersistentUnifier implements IUnifier {

    protected abstract boolean allowRecursive();

    protected abstract Map<ITermVar, ITermVar> reps();

    protected abstract Map<ITermVar, ITerm> terms();

    ///////////////////////////////////////////
    // size()
    ///////////////////////////////////////////

    @Override public int size() {
        return reps().size();
    }

    ///////////////////////////////////////////
    // varSet()
    ///////////////////////////////////////////

    @Override public Set<ITermVar> varSet() {
        return reps().keySet();
    }

    ///////////////////////////////////////////
    // toString()
    ///////////////////////////////////////////

    @Override public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for(ITermVar var : terms().keySet()) {
            sb.append(first ? " " : ", ");
            first = false;
            sb.append(var);
            sb.append(" |-> ");
            sb.append(terms().get(var));
        }
        for(ITermVar var : reps().keySet()) {
            sb.append(first ? " " : ", ");
            first = false;
            sb.append(var);
            sb.append(" |-> ");
            sb.append(findRep(var));
        }
        sb.append(first ? "}" : " }");
        return sb.toString();
    }

    ///////////////////////////////////////////
    // find(ITerm)
    ///////////////////////////////////////////

    @Override public ITerm find(ITerm term) {
        return M.var(var -> {
            final ITermVar rep = findRep(var);
            return terms().getOrDefault(rep, rep);
        }).match(term).orElse(term);
    }

    protected abstract ITermVar findRep(ITermVar var);

    protected static ITermVar findRep(ITermVar var, Map.Transient<ITermVar, ITermVar> reps) {
        if(!reps.containsKey(var)) {
            return var;
        } else {
            ITermVar rep = findRep(reps.get(var), reps);
            reps.__put(var, rep);
            return rep;
        }
    }

    ///////////////////////////////////////////
    // areEqual(ITerm, ITerm)
    ///////////////////////////////////////////

    @Override public boolean areEqual(final ITerm left, final ITerm right) {
        return equalTerms(left, right, Sets.newHashSet(), Maps.newHashMap());
    }

    private boolean equalTerms(final ITerm left, final ITerm right, final Set<Set2<ITermVar>> stack,
            final java.util.Map<Set2<ITermVar>, Boolean> visited) {
        // @formatter:off
        return left.match(Terms.<Boolean>cases(
            applLeft -> M.cases(
                M.appl(applRight -> applLeft.getOp().equals(applRight.getOp()) &&
                                    applLeft.getArity() == applRight.getArity() &&
                                    equals(applLeft.getArgs(), applRight.getArgs(), stack, visited)),
                M.var(varRight -> equalVarTerm(varRight, applLeft, stack, visited))
            ).match(right).orElse(false),
            listLeft -> M.cases(
                M.list(listRight -> listLeft.match(ListTerms.cases(
                    consLeft -> M.cases(
                        M.cons(consRight -> {
                            return equalTerms(consLeft.getHead(), consRight.getHead(), stack, visited) && 
                            equalTerms(consLeft.getTail(), consRight.getTail(), stack, visited);
                        }),
                        M.var(varRight -> equalVarTerm(varRight, consLeft, stack, visited))
                    ).match(listRight).orElse(false),
                    nilLeft -> M.cases(
                        M.nil(nilRight -> true),
                        M.var(varRight -> equalVarTerm(varRight, nilLeft, stack, visited))
                    ).match(listRight).orElse(false),
                    varLeft -> M.cases(
                        M.var(varRight -> equalVars(varLeft, varRight, stack, visited)),
                        M.term(termRight -> equalVarTerm(varLeft, termRight, stack, visited))
                    ).match(listRight).orElse(false)
                ))),
                M.var(varRight -> equalVarTerm(varRight, listLeft, stack, visited))
            ).match(right).orElse(false),
            stringLeft -> M.cases(
                M.string(stringRight -> stringLeft.getValue().equals(stringRight.getValue())),
                M.var(varRight -> equalVarTerm(varRight, stringLeft, stack, visited))
            ).match(right).orElse(false),
            integerLeft -> M.cases(
                M.integer(integerRight -> integerLeft.getValue() == integerRight.getValue()),
                M.var(varRight -> equalVarTerm(varRight, integerLeft, stack, visited))
            ).match(right).orElse(false),
            blobLeft -> M.cases(
                M.blob(blobRight -> blobLeft.getValue().equals(blobRight.getValue())),
                M.var(varRight -> equalVarTerm(varRight, blobLeft, stack, visited))
            ).match(right).orElse(false),
            varLeft -> M.cases(
                // match var before term, or term will always match
                M.var(varRight -> equalVars(varLeft, varRight, stack, visited)),
                M.term(termRight -> equalVarTerm(varLeft, termRight, stack, visited))
            ).match(right).orElse(false)
        ));
        // @formatter:on
    }

    private boolean equalVarTerm(final ITermVar var, final ITerm term, final Set<Set2<ITermVar>> stack,
            final java.util.Map<Set2<ITermVar>, Boolean> visited) {
        final ITermVar rep = findRep(var);
        if(terms().containsKey(rep)) {
            return equalTerms(terms().get(rep), term, stack, visited);
        }
        return false;
    }

    private boolean equalVars(final ITermVar left, final ITermVar right, final Set<Set2<ITermVar>> stack,
            final java.util.Map<Set2<ITermVar>, Boolean> visited) {
        final ITermVar leftRep = findRep(left);
        final ITermVar rightRep = findRep(right);
        if(leftRep.equals(rightRep)) {
            return true;
        }
        final Set2<ITermVar> pair = Set2.of(leftRep, rightRep);
        final boolean equal;
        if(!visited.containsKey(pair)) {
            stack.add(pair);
            visited.put(pair, false);
            final ITerm leftTerm = terms().get(leftRep);
            final ITerm rightTerm = terms().get(rightRep);
            equal = (leftTerm != null && rightTerm != null) ? equalTerms(leftTerm, rightTerm, stack, visited) : false;
            visited.put(pair, equal);
            stack.remove(pair);
        } else if(stack.contains(pair)) {
            equal = allowRecursive();
        } else {
            equal = visited.get(pair);
        }
        return equal;
    }

    private boolean equals(final Iterable<ITerm> lefts, final Iterable<ITerm> rights, final Set<Set2<ITermVar>> stack,
            final java.util.Map<Set2<ITermVar>, Boolean> visited) {
        Iterator<ITerm> itLeft = lefts.iterator();
        Iterator<ITerm> itRight = rights.iterator();
        while(itLeft.hasNext()) {
            if(!itRight.hasNext()) {
                return false;
            }
            if(!equalTerms(itLeft.next(), itRight.next(), stack, visited)) {
                return false;
            }
        }
        if(itRight.hasNext()) {
            return false;
        }
        return true;
    }

    ///////////////////////////////////////////
    // areUnequal(ITerm, ITerm)
    ///////////////////////////////////////////

    @Override public boolean areUnequal(final ITerm left, final ITerm right) {
        return unequalTerms(left, right, Sets.newHashSet(), Maps.newHashMap());
    }

    private boolean unequalTerms(final ITerm left, final ITerm right, Set<Set2<ITermVar>> stack,
            final java.util.Map<Set2<ITermVar>, Boolean> visited) {
        // @formatter:off
        return left.match(Terms.<Boolean>cases(
            applLeft -> M.cases(
                M.appl(applRight -> !applLeft.getOp().equals(applRight.getOp()) ||
                                    applLeft.getArity() != applRight.getArity() ||
                                    unequals(applLeft.getArgs(), applRight.getArgs(), stack, visited)),
                M.var(varRight -> unequalVarTerm(varRight, applLeft, stack, visited))
            ).match(right).orElse(true),
            listLeft -> M.cases(
                M.list(listRight -> listLeft.match(ListTerms.cases(
                    consLeft -> M.cases(
                        M.cons(consRight -> {
                            return unequalTerms(consLeft.getHead(), consRight.getHead(), stack, visited) || 
                                   unequalTerms(consLeft.getTail(), consRight.getTail(), stack, visited);
                        }),
                        M.var(varRight -> unequalVarTerm(varRight, consLeft, stack, visited))
                    ).match(listRight).orElse(true),
                    nilLeft -> M.cases(
                        M.nil(nilRight -> false),
                        M.var(varRight -> unequalVarTerm(varRight, nilLeft, stack, visited))
                    ).match(listRight).orElse(true),
                    varLeft -> M.cases(
                        M.var(varRight -> unequalVars(varLeft, varRight, stack, visited)),
                        M.term(termRight -> unequalVarTerm(varLeft, termRight, stack, visited))
                    ).match(listRight).orElse(true)
                ))),
                M.var(varRight -> unequalVarTerm(varRight, listLeft, stack, visited))
            ).match(right).orElse(true),
            stringLeft -> M.cases(
                M.string(stringRight -> !stringLeft.getValue().equals(stringRight.getValue())),
                M.var(varRight -> unequalVarTerm(varRight, stringLeft, stack, visited))
            ).match(right).orElse(true),
            integerLeft -> M.cases(
                M.integer(integerRight -> integerLeft.getValue() != integerRight.getValue()),
                M.var(varRight -> unequalVarTerm(varRight, integerLeft, stack, visited))
            ).match(right).orElse(true),
            blobLeft -> M.cases(
                M.blob(blobRight -> !blobLeft.getValue().equals(blobRight.getValue())),
                M.var(varRight -> unequalVarTerm(varRight, blobLeft, stack, visited))
            ).match(right).orElse(true),
            varLeft -> M.cases(
                // match var before term, or term will always match
                M.var(varRight -> unequalVars(varLeft, varRight, stack, visited)),
                M.term(termRight -> unequalVarTerm(varLeft, termRight, stack, visited))
            ).match(right).orElse(true)
        ));
        // @formatter:on
    }

    private boolean unequalVarTerm(final ITermVar var, final ITerm term, Set<Set2<ITermVar>> stack,
            final java.util.Map<Set2<ITermVar>, Boolean> visited) {
        final ITermVar rep = findRep(var);
        if(terms().containsKey(rep)) {
            return unequalTerms(terms().get(rep), term, stack, visited);
        }
        return false;
    }

    private boolean unequalVars(final ITermVar left, final ITermVar right, Set<Set2<ITermVar>> stack,
            final java.util.Map<Set2<ITermVar>, Boolean> visited) {
        final ITermVar leftRep = findRep(left);
        final ITermVar rightRep = findRep(right);
        if(leftRep.equals(rightRep)) {
            return false;
        }
        final Set2<ITermVar> pair = Set2.of(leftRep, rightRep);
        final boolean unequal;
        if(!visited.containsKey(pair)) {
            stack.add(pair);
            visited.put(pair, false);
            final ITerm leftTerm = terms().get(leftRep);
            final ITerm rightTerm = terms().get(rightRep);
            unequal =
                    (leftTerm != null && rightTerm != null) ? unequalTerms(leftTerm, rightTerm, stack, visited) : false;
            visited.put(pair, unequal);
            stack.remove(pair);
        } else if(stack.contains(pair)) {
            unequal = false;
        } else {
            unequal = visited.get(pair);
        }
        return unequal;
    }

    private boolean unequals(final Iterable<ITerm> lefts, final Iterable<ITerm> rights, Set<Set2<ITermVar>> stack,
            final java.util.Map<Set2<ITermVar>, Boolean> visited) {
        Iterator<ITerm> itLeft = lefts.iterator();
        Iterator<ITerm> itRight = rights.iterator();
        while(itLeft.hasNext()) {
            if(!itRight.hasNext()) {
                return true;
            }
            if(unequalTerms(itLeft.next(), itRight.next(), stack, visited)) {
                return true;
            }
        }
        if(itRight.hasNext()) {
            return true;
        }
        return false;
    }

    ///////////////////////////////////////////
    // isCyclic(ITerm)
    ///////////////////////////////////////////

    @Override public boolean isCyclic(final ITerm term) {
        return isCyclic(term.getVars().elementSet(), Sets.newHashSet(), Maps.newHashMap());
    }

    protected boolean isCyclic(final Set<ITermVar> vars) {
        return isCyclic(vars, Sets.newHashSet(), Maps.newHashMap());
    }

    private boolean isCyclic(final Set<ITermVar> vars, final Set<ITermVar> stack,
            final java.util.Map<ITermVar, Boolean> visited) {
        return vars.stream().anyMatch(var -> isCyclic(var, stack, visited));
    }

    private boolean isCyclic(final ITermVar var, final Set<ITermVar> stack,
            final java.util.Map<ITermVar, Boolean> visited) {
        final boolean cyclic;
        final ITermVar rep = findRep(var);
        if(!visited.containsKey(rep)) {
            stack.add(rep);
            visited.put(rep, false);
            final ITerm term = terms().get(rep);
            cyclic = term != null ? isCyclic(term.getVars().elementSet(), stack, visited) : false;
            visited.put(rep, cyclic);
            stack.remove(rep);
        } else if(stack.contains(rep)) {
            cyclic = true;
        } else {
            cyclic = visited.get(rep);
        }
        return cyclic;
    }

    ///////////////////////////////////////////
    // isGround(ITerm)
    ///////////////////////////////////////////

    @Override public boolean isGround(final ITerm term) {
        return isGround(term.getVars().elementSet(), Sets.newHashSet(), Maps.newHashMap());
    }

    private boolean isGround(final Set<ITermVar> vars, final Set<ITermVar> stack,
            final java.util.Map<ITermVar, Boolean> visited) {
        return vars.stream().anyMatch(var -> isGround(var, stack, visited));
    }

    private boolean isGround(final ITermVar var, final Set<ITermVar> stack,
            final java.util.Map<ITermVar, Boolean> visited) {
        final boolean ground;
        final ITermVar rep = findRep(var);
        if(!visited.containsKey(rep)) {
            stack.add(rep);
            visited.put(rep, false);
            final ITerm term = terms().get(rep);
            ground = term != null ? isGround(term.getVars().elementSet(), stack, visited) : false;
            visited.put(rep, ground);
            stack.remove(rep);
        } else if(stack.contains(rep)) {
            ground = false;
        } else {
            ground = visited.get(rep);
        }
        return ground;
    }

    ///////////////////////////////////////////
    // getVars(ITerm)
    ///////////////////////////////////////////

    @Override public Set<ITermVar> getVars(final ITerm term) {
        final Set<ITermVar> vars = Sets.newHashSet();
        getVars(term.getVars().elementSet(), Lists.newLinkedList(), Sets.newHashSet(), vars);
        return vars;
    }

    private void getVars(final Set<ITermVar> tryVars, final LinkedList<ITermVar> stack, final Set<ITermVar> visited,
            Set<ITermVar> vars) {
        tryVars.stream().forEach(var -> getVars(var, stack, visited, vars));
    }

    private void getVars(final ITermVar var, final LinkedList<ITermVar> stack, final Set<ITermVar> visited,
            Set<ITermVar> vars) {
        final ITermVar rep = findRep(var);
        if(!visited.contains(rep)) {
            visited.add(rep);
            stack.push(rep);
            final ITerm term = terms().get(rep);
            if(term != null) {
                getVars(term.getVars().elementSet(), stack, visited, vars);
            } else {
                vars.add(rep);
            }
            stack.pop();
        } else {
            final int index = stack.indexOf(rep); // linear
            if(index >= 0) {
                vars.addAll(stack.subList(0, index + 1));
            }
        }
    }

    ///////////////////////////////////////////
    // size(ITerm)
    ///////////////////////////////////////////

    @Override public TermSize size(final ITerm term) {
        return size(term, Sets.newHashSet(), Maps.newHashMap());
    }

    private TermSize size(final ITerm term, final Set<ITermVar> stack,
            final java.util.Map<ITermVar, TermSize> visited) {
        return term.match(Terms.<TermSize>cases(
        // @formatter:off
            appl -> TermSize.ONE.add(sizes(appl.getArgs(), stack, visited)),
            list -> size(list, stack, visited),
            string -> TermSize.ONE,
            integer -> TermSize.ONE,
            blob -> TermSize.ONE,
            var -> size(var, stack, visited)
            // @formatter:on
        ));
    }

    private TermSize size(IListTerm list, final Set<ITermVar> stack, final java.util.Map<ITermVar, TermSize> visited) {
        final Ref<TermSize> size = new Ref<>(TermSize.ZERO);
        while(list != null) {
            list = list.match(ListTerms.cases(
            // @formatter:off
                cons -> {
                    size.set(size.get().add(TermSize.ONE).add(size(cons.getHead(), stack, visited)));
                    return cons.getTail();
                },
                nil -> {
                    size.set(size.get().add(TermSize.ONE));
                    return null;
                },
                var -> {
                    size.set(size.get().add(size(var, stack, visited)));
                    return null;
                }
                // @formatter:on
            ));
        }
        return size.get();
    }

    private TermSize size(final ITermVar var, final Set<ITermVar> stack,
            final java.util.Map<ITermVar, TermSize> visited) {
        final ITermVar rep = findRep(var);
        final TermSize size;
        if(!visited.containsKey(rep)) {
            stack.add(var);
            visited.put(rep, TermSize.ZERO);
            final ITerm term = terms().get(rep);
            size = term != null ? size(term, stack, visited) : TermSize.ZERO;
            visited.put(rep, size);
            stack.remove(rep);
            return size;
        } else if(stack.contains(rep)) {
            size = TermSize.INF;
        } else {
            size = visited.get(rep);
        }
        return size;
    }

    private TermSize sizes(final Iterable<ITerm> terms, final Set<ITermVar> stack,
            final java.util.Map<ITermVar, TermSize> visited) {
        TermSize size = TermSize.ZERO;
        for(ITerm term : terms) {
            size = size.add(size(term, stack, visited));
        }
        return size;
    }

    ///////////////////////////////////////////
    // Immutable
    ///////////////////////////////////////////

    public static class Immutable extends PersistentUnifier implements IUnifier.Immutable {

        private final boolean allowRecursive;

        private final Ref<Map.Immutable<ITermVar, ITermVar>> reps;
        private final Map.Immutable<ITermVar, Integer> ranks;
        private final Map.Immutable<ITermVar, ITerm> terms;

        private Immutable(boolean allowRecursive, final Map.Immutable<ITermVar, ITermVar> reps,
                final Map.Immutable<ITermVar, Integer> ranks, final Map.Immutable<ITermVar, ITerm> terms) {
            this.allowRecursive = allowRecursive;
            this.reps = new Ref<>(reps);
            this.ranks = ranks;
            this.terms = terms;
        }

        @Override protected boolean allowRecursive() {
            return allowRecursive;
        }

        @Override protected Map<ITermVar, ITermVar> reps() {
            return reps.get();
        }

        @Override protected Map<ITermVar, ITerm> terms() {
            return terms;
        }

        public Tuple2<Set<ITermVar>, IUnifier> unify(ITerm left, ITerm right) throws UnificationException {
            final PersistentUnifier.Transient unifier = melt();
            final Set<ITermVar> vars = unifier.unify(left, right);
            return ImmutableTuple2.of(vars, unifier.freeze());
        }

        @Override protected ITermVar findRep(ITermVar var) {
            final Map.Transient<ITermVar, ITermVar> reps = this.reps.get().asTransient();
            final ITermVar rep = findRep(var, reps);
            this.reps.set(reps.freeze());
            return rep;
        }

        @Override public PersistentUnifier.Transient melt() {
            return new PersistentUnifier.Transient(allowRecursive, reps.get().asTransient(), ranks.asTransient(),
                    terms.asTransient());
        }

        public static IUnifier.Immutable of(boolean allowRecursive) {
            return new PersistentUnifier.Immutable(allowRecursive, Map.Immutable.of(), Map.Immutable.of(),
                    Map.Immutable.of());
        }

    }

    ///////////////////////////////////////////
    // Transient
    ///////////////////////////////////////////

    public static class Transient extends PersistentUnifier implements IUnifier.Transient {

        private final boolean allowRecursive;

        private final Map.Transient<ITermVar, ITermVar> reps;
        private final Map.Transient<ITermVar, Integer> ranks;
        private final Map.Transient<ITermVar, ITerm> terms;

        private Transient(boolean allowRecursive, final Map.Transient<ITermVar, ITermVar> reps,
                final Map.Transient<ITermVar, Integer> ranks, final Map.Transient<ITermVar, ITerm> terms) {
            this.allowRecursive = allowRecursive;
            this.reps = reps;
            this.ranks = ranks;
            this.terms = terms;
        }

        @Override protected boolean allowRecursive() {
            return allowRecursive;
        }

        @Override protected Map<ITermVar, ITermVar> reps() {
            return reps;
        }

        @Override protected Map<ITermVar, ITerm> terms() {
            return terms;
        }

        @Override public ITermVar findRep(ITermVar var) {
            return findRep(var, reps);
        }

        @Override public PersistentUnifier.Immutable freeze() {
            return new PersistentUnifier.Immutable(allowRecursive, reps.freeze(), ranks.freeze(), terms.freeze());
        }

        public static IUnifier.Transient of(boolean allowRecursive) {
            return new PersistentUnifier.Transient(allowRecursive, Map.Transient.of(), Map.Transient.of(),
                    Map.Transient.of());
        }

        ///////////////////////////////////////////
        // unify(ITerm, ITerm)
        ///////////////////////////////////////////

        @Override public Set<ITermVar> unify(final ITerm left, final ITerm right) throws UnificationException {
            final Set<ITermVar> result = Sets.newHashSet();
            final Deque<Tuple2<ITerm, ITerm>> worklist = Lists.newLinkedList();
            worklist.push(ImmutableTuple2.of(left, right));
            while(!worklist.isEmpty()) {
                final Tuple2<ITerm, ITerm> work = worklist.pop();
                if(!unifyTerms(work._1(), work._2(), worklist, result)) {
                    throw new UnificationException(left, right);
                }
            }
            if(!allowRecursive && isCyclic(result)) {
                throw new UnificationException(left, right);
            }
            return result;
        }

        private boolean unifyTerms(final ITerm left, final ITerm right, final Deque<Tuple2<ITerm, ITerm>> worklist,
                Set<ITermVar> result) {
            // @formatter:off
            return left.match(Terms.cases(
                applLeft -> M.cases(
                    M.appl(applRight -> applLeft.getOp().equals(applRight.getOp()) &&
                                        applLeft.getArity() == applRight.getArity() &&
                                        unifys(applLeft.getArgs(), applRight.getArgs(), worklist)),
                    M.var(varRight -> unifyVarTerm(varRight, applLeft, worklist, result))
                ).match(right).orElse(false),
                listLeft -> M.cases(
                    M.list(listRight -> listLeft.match(ListTerms.cases(
                        consLeft -> M.cases(
                            M.cons(consRight -> {
                                worklist.push(ImmutableTuple2.of(consLeft.getHead(), consRight.getHead()));
                                worklist.push(ImmutableTuple2.of(consLeft.getTail(), consRight.getTail()));
                                return true;
                            }),
                            M.var(varRight -> unifyVarTerm(varRight, consLeft, worklist, result))
                        ).match(listRight).orElse(false),
                        nilLeft -> M.cases(
                            M.nil(nilRight -> true),
                            M.var(varRight -> unifyVarTerm(varRight, nilLeft, worklist, result))
                        ).match(listRight).orElse(false),
                        varLeft -> M.cases(
                            M.var(varRight -> unifyVars(varLeft, varRight, worklist, result)),
                            M.term(termRight -> unifyVarTerm(varLeft, termRight, worklist, result))
                        ).match(listRight).orElse(false)
                    ))),
                    M.var(varRight -> unifyVarTerm(varRight, listLeft, worklist, result))
                ).match(right).orElse(false),
                stringLeft -> M.cases(
                    M.string(stringRight -> stringLeft.getValue().equals(stringRight.getValue())),
                    M.var(varRight -> unifyVarTerm(varRight, stringLeft, worklist, result))
                ).match(right).orElse(false),
                integerLeft -> M.cases(
                    M.integer(integerRight -> integerLeft.getValue() == integerRight.getValue()),
                    M.var(varRight -> unifyVarTerm(varRight, integerLeft, worklist, result))
                ).match(right).orElse(false),
                blobLeft -> M.cases(
                    M.blob(blobRight -> blobLeft.getValue().equals(blobRight.getValue())),
                    M.var(varRight -> unifyVarTerm(varRight, blobLeft, worklist, result))
                ).match(right).orElse(false),
                varLeft -> M.cases(
                    // match var before term, or term will always match
                    M.var(varRight -> unifyVars(varLeft, varRight, worklist, result)),
                    M.term(termRight -> unifyVarTerm(varLeft, termRight, worklist, result))
                ).match(right).orElse(false)
            ));
            // @formatter:on
        }

        private boolean unifyVarTerm(final ITermVar var, final ITerm term, final Deque<Tuple2<ITerm, ITerm>> worklist,
                Set<ITermVar> result) {
            final ITermVar rep = findRep(var);
            if(terms.containsKey(rep)) {
                worklist.push(ImmutableTuple2.of(terms.get(rep), term));
            } else {
                terms.put(var, term);
                result.add(var);
            }
            return true;
        }

        private boolean unifyVars(final ITermVar left, final ITermVar right, final Deque<Tuple2<ITerm, ITerm>> worklist,
                Set<ITermVar> result) {
            final ITermVar leftRep = findRep(left);
            final ITermVar rightRep = findRep(right);
            if(leftRep.equals(rightRep)) {
                return true;
            }
            final int leftRank = ranks.getOrDefault(leftRep, 1);
            final int rightRank = ranks.getOrDefault(rightRep, 1);
            final boolean swap = leftRank > rightRank;
            final ITermVar var = swap ? rightRep : leftRep; // the eliminated variable
            final ITermVar with = swap ? leftRep : rightRep; // the new representative
            ranks.put(with, leftRank + rightRank);
            reps.put(var, with);
            result.add(var);
            final ITerm term = terms.__remove(var); // term for the eliminated var
            if(term != null) {
                worklist.push(ImmutableTuple2.of(term, terms.getOrDefault(with, with)));
            }
            return true;
        }

        private boolean unifys(final Iterable<ITerm> lefts, final Iterable<ITerm> rights,
                final Deque<Tuple2<ITerm, ITerm>> worklist) {
            Iterator<ITerm> itLeft = lefts.iterator();
            Iterator<ITerm> itRight = rights.iterator();
            while(itLeft.hasNext()) {
                if(!itRight.hasNext()) {
                    return false;
                }
                worklist.push(ImmutableTuple2.of(itLeft.next(), itRight.next()));
            }
            if(itRight.hasNext()) {
                return false;
            }
            return true;
        }

    }

}