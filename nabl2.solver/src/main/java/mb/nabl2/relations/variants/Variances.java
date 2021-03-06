package mb.nabl2.relations.variants;

import static mb.nabl2.terms.matching.TermMatch.M;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import mb.nabl2.relations.terms.RelationName;
import mb.nabl2.terms.matching.TermMatch.IMatcher;

public class Variances {

    @Value.Immutable
    @Serial.Version(value = 42L)
    public static abstract class Invariant implements IVariance {

        @Override public <T> T match(Cases<T> cases) {
            return cases.caseInvariant();
        }

    }

    @Value.Immutable
    @Serial.Version(value = 42L)
    public static abstract class Covariant implements IVariance {

        @Value.Parameter public abstract RelationName getRelation();

        @Override public <T> T match(Cases<T> cases) {
            return cases.caseCovariant(getRelation());
        }

    }

    @Value.Immutable
    @Serial.Version(value = 42L)
    public static abstract class Contravariant implements IVariance {

        @Value.Parameter public abstract RelationName getRelation();

        @Override public <T> T match(Cases<T> cases) {
            return cases.caseContravariant(getRelation());
        }

    }

    public static IMatcher<IVariance> matcher() {
        return M.cases(
            // @formatter:off
            M.appl0("Invar", t -> ImmutableInvariant.of()),
            M.appl1("Covar", RelationName.matcher(), (t,r) -> ImmutableCovariant.of(r)),
            M.appl1("Contravar", RelationName.matcher(), (t,r) -> ImmutableContravariant.of(r))
            // @formatter:on
        );
    }

}