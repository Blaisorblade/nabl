package mb.nabl2.constraints.poly;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import org.metaborg.util.functions.Function1;

import mb.nabl2.constraints.messages.MessageInfo;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.nabl2.terms.substitution.ISubstitution;

public final class PolyConstraints {

    private static final String C_GEN = "CGen";
    private static final String C_INST = "CInst";

    public static IMatcher<IPolyConstraint> matcher() {
        return M.<IPolyConstraint>cases(
        // @formatter:off
            M.appl4(C_GEN, M.term(), M.var(), M.term(), MessageInfo.matcher(), (c, scheme, genVars, type, origin) -> {
                return ImmutableCGeneralize.of(scheme, genVars, type, origin);
            }),
            M.appl4(C_INST, M.term(), M.var(), M.term(), MessageInfo.matcher(), (c, type, instVars, scheme, origin) -> {
                return ImmutableCInstantiate.of(type, instVars, scheme, origin);
            })
            // @formatter:on
        );
    }

    public static ITerm build(IPolyConstraint constraint) {
        return constraint.match(IPolyConstraint.Cases.<ITerm>of(
        // @formatter:off
            gen -> B.newAppl(C_GEN, gen.getDeclaration(), gen.getType(), MessageInfo.build(gen.getMessageInfo())),
            inst -> B.newAppl(C_INST, inst.getType(), inst.getDeclaration(), MessageInfo.build(inst.getMessageInfo()))
            // @formatter:on
        ));
    }

    public static IPolyConstraint substitute(IPolyConstraint constraint, ISubstitution.Immutable subst) {
        // @formatter:off
        return constraint.match(IPolyConstraint.Cases.of(
            gen -> ImmutableCGeneralize.of(
                        subst.apply(gen.getDeclaration()),
                        gen.getGenVars(),
                        subst.apply(gen.getType()),
                        gen.getMessageInfo().apply(subst::apply)),
            inst -> ImmutableCInstantiate.of(
                        subst.apply(inst.getType()),
                        inst.getInstVars(),
                        subst.apply(inst.getDeclaration()),
                        inst.getMessageInfo().apply(subst::apply))
        ));
        // @formatter:on
    }

    public static IPolyConstraint transform(IPolyConstraint constraint, Function1<ITerm, ITerm> map) {
        // @formatter:off
        return constraint.match(IPolyConstraint.Cases.of(
            gen -> ImmutableCGeneralize.of(
                        map.apply(gen.getDeclaration()),
                        gen.getGenVars(),
                        map.apply(gen.getType()),
                        gen.getMessageInfo().apply(map::apply)),
            inst -> ImmutableCInstantiate.of(
                        map.apply(inst.getType()),
                        inst.getInstVars(),
                        map.apply(inst.getDeclaration()),
                        inst.getMessageInfo().apply(map::apply))
        ));
        // @formatter:on
    }

}