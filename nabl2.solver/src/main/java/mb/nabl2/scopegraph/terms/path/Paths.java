package mb.nabl2.scopegraph.terms.path;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.metaborg.util.iterators.Iterables2;

import com.google.common.collect.Lists;

import mb.nabl2.scopegraph.ILabel;
import mb.nabl2.scopegraph.IOccurrence;
import mb.nabl2.scopegraph.IScope;
import mb.nabl2.scopegraph.path.IDeclPath;
import mb.nabl2.scopegraph.path.IResolutionPath;
import mb.nabl2.scopegraph.path.IScopePath;
import mb.nabl2.scopegraph.path.IStep;
import mb.nabl2.scopegraph.terms.Label;
import mb.nabl2.scopegraph.terms.Occurrence;
import mb.nabl2.scopegraph.terms.Scope;
import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.ITerm;

public final class Paths {

    public static final String PATH_SEPERATOR = " ";

    // --------------------------------

    public static <S extends IScope, L extends ILabel, O extends IOccurrence> IStep<S, L, O> direct(S source, L label,
            S target) {
        return ImmutableEStep.of(source, label, target);
    }

    public static <S extends IScope, L extends ILabel, O extends IOccurrence> IStep<S, L, O> named(S source, L label,
            IResolutionPath<S, L, O> importPath, S target) {
        return ImmutableNStep.of(source, label, importPath, target);
    }

    public static <S extends IScope, L extends ILabel, O extends IOccurrence> IScopePath<S, L, O> empty(S scope) {
        return ImmutableEmptyScopePath.of(scope);
    }

    public static <S extends IScope, L extends ILabel, O extends IOccurrence> IDeclPath<S, L, O>
            decl(IScopePath<S, L, O> path, O decl) {
        return ImmutableDeclPath.of(path, decl);
    }

    public static <S extends IScope, L extends ILabel, O extends IOccurrence> Optional<IScopePath<S, L, O>>
            append(IScopePath<S, L, O> left, IScopePath<S, L, O> right) {
        return Optional.ofNullable(ImmutableComposedScopePath.of(left, right));
    }

    public static <S extends IScope, L extends ILabel, O extends IOccurrence> Optional<IDeclPath<S, L, O>>
            append(IScopePath<S, L, O> left, IDeclPath<S, L, O> right) {
        return Optional.ofNullable(ImmutableComposedScopePath.of(left, right.getPath()))
                .map(p -> ImmutableDeclPath.of(p, right.getDeclaration()));
    }


    public static <S extends IScope, L extends ILabel, O extends IOccurrence> Optional<IResolutionPath<S, L, O>>
            resolve(O reference, IScopePath<S, L, O> path, O declaration) {
        return Optional.ofNullable(ImmutableResolutionPath.of(reference, path, declaration));
    }

    public static <S extends IScope, L extends ILabel, O extends IOccurrence> Optional<IResolutionPath<S, L, O>>
            resolve(O reference, IDeclPath<S, L, O> path) {
        return Optional.ofNullable(ImmutableResolutionPath.of(reference, path.getPath(), path.getDeclaration()));
    }

    // -------------------------------------------

    public static IListTerm toTerm(IResolutionPath<Scope, Label, Occurrence> path) {
        ITerm dstep = B.newAppl("D", path.getPath().getTarget(), path.getDeclaration());
        return B.newListTail(toTerms(path.getPath()), B.newList(dstep));
    }

    public static IListTerm toTerm(IScopePath<Scope, Label, Occurrence> path) {
        return B.newList(toTerms(path));
    }

    private static List<ITerm> toTerms(IScopePath<Scope, Label, Occurrence> path) {
        List<ITerm> steps = Lists.newArrayList();
        for(IStep<Scope, Label, Occurrence> step : path) {
            steps.add(step.match(IStep.ICases.of(
                // @formatter:off
                (source, label, target) -> B.newAppl("E", source, label),
                (source, label, importPath, target) -> B.newAppl("N", source, label, importPath.getReference(), toTerm(importPath))
                // @formatter:on
            )));
        }
        return steps;
    }

    // -------------------------------------------


    public static List<Occurrence> declPathsToDecls(Iterable<IDeclPath<Scope, Label, Occurrence>> paths) {
        return Iterables2.stream(paths).map(IDeclPath::getDeclaration).collect(Collectors.toList());
    }

    public static List<Occurrence> resolutionPathsToDecls(Iterable<IResolutionPath<Scope, Label, Occurrence>> paths) {
        return Iterables2.stream(paths).map(IResolutionPath::getDeclaration).collect(Collectors.toList());
    }

}