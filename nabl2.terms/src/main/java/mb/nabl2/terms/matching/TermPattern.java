package mb.nabl2.terms.matching;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.ListTerms;
import mb.nabl2.terms.Terms;
import mb.nabl2.terms.build.TermBuild;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.unification.IUnifier;

public class TermPattern {

    public static P P = new P();

    public static class P {

        public final Pattern EMPTY_TUPLE = new ApplPattern(Terms.TUPLE_OP, ImmutableList.of());
        public final Pattern EMPTY_LIST = new NilPattern();

        public Pattern newAppl(String op, Pattern... args) {
            return newAppl(op, Arrays.asList(args));
        }

        public Pattern newAppl(String op, Iterable<? extends Pattern> args) {
            if(op.equals("")) {
                throw new IllegalArgumentException();
            }
            return new ApplPattern(op, args);
        }

        public Pattern newTuple(Pattern... args) {
            return newTuple(Arrays.asList(args));
        }

        public Pattern newTuple(Iterable<? extends Pattern> args) {
            final List<Pattern> argList = ImmutableList.copyOf(args);
            if(argList.size() == 1) {
                return argList.get(0);
            } else {
                return new ApplPattern(Terms.TUPLE_OP, argList);
            }
        }

        public Pattern newList(Iterable<? extends Pattern> args) {
            return newListTail(args, EMPTY_LIST);
        }

        public Pattern newListTail(Iterable<? extends Pattern> args, Pattern tail) {
            Pattern list = tail;
            for(Pattern elem : ImmutableList.copyOf(args).reverse()) {
                list = newCons(elem, list);
            }
            return list;
        }

        public Pattern newCons(Pattern head, Pattern tail) {
            return new ConsPattern(head, tail);
        }

        public Pattern newNil() {
            return new NilPattern();
        }

        public Pattern newString(String value) {
            return new StringPattern(value);
        }

        public Pattern newInt(int value) {
            return new IntPattern(value);
        }

        public Pattern newWld() {
            return new PatternVar();
        }

        public Pattern newVar(String name) {
            return new PatternVar(name);
        }

        public Pattern newVar(ITermVar var) {
            return new PatternVar(var);
        }

        public Pattern newAs(String name, Pattern pattern) {
            return new PatternAs(name, pattern);
        }

        public Pattern newAs(ITermVar var, Pattern pattern) {
            return new PatternAs(var, pattern);
        }

        public Pattern newAs(Pattern pattern) {
            return new PatternAs(pattern);
        }

        public Pattern fromTerm(ITerm term) {
            // @formatter:off
            return term.match(Terms.cases(
                appl -> new ApplPattern(appl.getOp(),
                        appl.getArgs().stream().map(this::fromTerm).collect(Collectors.toList())),
                list -> list.match(ListTerms.cases(
                    cons -> new ConsPattern(fromTerm(cons.getHead()), fromTerm(cons.getTail())),
                    nil -> new NilPattern(),
                    var -> new PatternVar(var)
                )),
                string -> new StringPattern(string.getValue()),
                integer -> new IntPattern(integer.getValue()),
                blob -> {
                    throw new IllegalArgumentException("Cannot create blob patterns.");
                },
                var -> new PatternVar(var)
            ));
            // @formatter:on
        }

        public Optional<ISubstitution.Immutable> match(final Iterable<Pattern> patterns, final Iterable<ITerm> terms) {
            return TermPattern.P.newTuple(patterns).match(TermBuild.B.newTuple(terms));
        }

        public MaybeNotInstantiated<Optional<ISubstitution.Immutable>> match(final Iterable<Pattern> patterns,
                final Iterable<ITerm> terms, IUnifier unifier) {
            return TermPattern.P.newTuple(patterns).match(TermBuild.B.newTuple(terms), unifier);
        }

    }

}