package org.metaborg.meta.nabl2.constraints.nameresolution;

import org.metaborg.meta.nabl2.constraints.Constraints;
import org.metaborg.meta.nabl2.constraints.messages.MessageInfo;
import org.metaborg.meta.nabl2.scopegraph.terms.Label;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.Terms.IMatcher;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.meta.nabl2.terms.generic.TB;
import org.metaborg.meta.nabl2.unification.ISubstitution;

public final class NameResolutionConstraints {

    private static final String C_RESOLVE = "CResolve";
    private static final String C_ASSOC = "CAssoc";
    private static final String C_DECL_PROPERTY = "CDeclProperty";

    public static IMatcher<INameResolutionConstraint> matcher() {
        return M.<INameResolutionConstraint>cases(
            // @formatter:off
            M.appl3(C_RESOLVE, M.term(), M.term(), MessageInfo.matcher(),
                    (c, ref, decl, origin) -> {
                        return ImmutableCResolve.of(ref, decl, origin);
                    }),
            M.appl4(C_ASSOC, M.term(), Label.matcher(), M.term(), MessageInfo.matcher(),
                    (c, decl, label, scope, origin) -> {
                        return ImmutableCAssoc.of(decl, label, scope, origin);
                    }),
            M.appl5(C_DECL_PROPERTY, M.term(), M.term(), M.term(), Constraints.priorityMatcher(), MessageInfo.matcher(),
                    (c, decl, key, value, prio, origin) -> {
                        return ImmutableCDeclProperty.of(decl, key, value, prio, origin);
                    })
            // @formatter:on
        );
    }

    public static ITerm build(INameResolutionConstraint constraint) {
        return constraint.match(INameResolutionConstraint.Cases.<ITerm>of(
            // @formatter:off
            res -> TB.newAppl(C_RESOLVE, res.getReference(), res.getDeclaration(),
                              MessageInfo.build(res.getMessageInfo())),
            assoc -> TB.newAppl(C_ASSOC, assoc.getDeclaration(), assoc.getLabel(), assoc.getScope(),
                                MessageInfo.build(assoc.getMessageInfo())),
            prop ->TB.newAppl(C_DECL_PROPERTY, prop.getDeclaration(), prop.getKey(), prop.getValue(),
                              Constraints.buildPriority(prop.getPriority()), MessageInfo.build(prop.getMessageInfo()))
            // @formatter:on
        ));
    }

    public static INameResolutionConstraint substitute(INameResolutionConstraint constraint, ISubstitution.Immutable unifier) {
        return constraint.match(INameResolutionConstraint.Cases.<INameResolutionConstraint>of(
            // @formatter:off
            res -> ImmutableCResolve.of(
                        unifier.find(res.getReference()),
                        unifier.find(res.getDeclaration()),
                        res.getMessageInfo().apply(unifier::find)),
            assoc -> ImmutableCAssoc.of(
                        unifier.find(assoc.getDeclaration()),
                        assoc.getLabel(),
                        unifier.find(assoc.getScope()),
                        assoc.getMessageInfo().apply(unifier::find)),
            prop -> ImmutableCDeclProperty.of(
                        unifier.find(prop.getDeclaration()),
                        prop.getKey(),
                        unifier.find(prop.getValue()),
                        prop.getPriority(),
                        prop.getMessageInfo().apply(unifier::find))
            // @formatter:on
        ));
    }

}