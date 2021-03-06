package mb.statix.solver.store;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import mb.nabl2.terms.ITermVar;
import mb.statix.scopegraph.reference.CriticalEdge;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IConstraintStore;
import mb.statix.solver.log.IDebugContext;

public class BaseConstraintStore implements IConstraintStore {

    private final Set<IConstraint> active;
    private final Set<IConstraint> stuckBecauseStuck;
    private final Multimap<ITermVar, IConstraint> stuckOnVar;
    private final Multimap<CriticalEdge, IConstraint> stuckOnEdge;

    public BaseConstraintStore(Iterable<? extends IConstraint> constraints, IDebugContext debug) {
        this.active = Sets.newConcurrentHashSet();
        this.stuckBecauseStuck = Sets.newHashSet();
        this.stuckOnVar = HashMultimap.create();
        this.stuckOnEdge = HashMultimap.create();
        addAll(constraints);
    }

    @Override public int activeSize() {
        return active.size();
    }

    @Override public int delayedSize() {
        return stuckBecauseStuck.size() + stuckOnVar.size() + stuckOnEdge.size();
    }

    @Override public void addAll(Iterable<? extends IConstraint> constraints) {
        for(IConstraint constraint : constraints) {
            active.add(constraint);
        }
    }

    @Override public void activateStray() {
        addAll(stuckBecauseStuck);
        stuckBecauseStuck.clear();
    }

    @Override public void activateFromVars(Iterable<? extends ITermVar> vars, IDebugContext debug) {
        for(ITermVar var : vars) {
            final Collection<IConstraint> activated = stuckOnVar.removeAll(var);
            stuckOnVar.values().removeAll(activated);
            debug.info("activating {}", activated);
            addAll(activated);
        }
    }

    @Override public void activateFromEdges(Iterable<? extends CriticalEdge> edges, IDebugContext debug) {
        for(CriticalEdge edge : edges) {
            final Collection<IConstraint> activated = stuckOnEdge.removeAll(edge);
            stuckOnEdge.values().removeAll(activated);
            debug.info("activating {}", activated);
            addAll(activated);
        }
    }

    @Override public Iterable<Entry> active(IDebugContext debug) {
        return new Iterable<IConstraintStore.Entry>() {
            @Override public Iterator<Entry> iterator() {
                final Iterator<IConstraint> it = active.iterator();
                return new Iterator<IConstraintStore.Entry>() {

                    @Override public boolean hasNext() {
                        return it.hasNext();
                    }

                    @Override public Entry next() {
                        final IConstraint c = it.next();
                        return new Entry() {

                            @Override public void remove() {
                                it.remove();
                            }

                            @Override public void delay(Delay d) {
                                it.remove();
                                if(!d.vars().isEmpty()) {
                                    debug.info("delayed {} on vars {}", c, d.vars());
                                    for(ITermVar var : d.vars()) {
                                        stuckOnVar.put(var, c);
                                    }
                                } else if(!d.criticalEdges().isEmpty()) {
                                    debug.info("delayed {} on critical edges {}", c, d.criticalEdges());
                                    for(CriticalEdge edge : d.criticalEdges()) {
                                        stuckOnEdge.put(edge, c);
                                    }
                                } else {
                                    debug.warn("delayed for no apparent reason ");
                                    stuckBecauseStuck.add(c);
                                }
                            }

                            @Override public IConstraint constraint() {
                                return c;
                            }

                        };
                    }

                };
            }
        };
    }

    @Override public Map<IConstraint, Delay> delayed() {
        Builder<IConstraint, Delay> delayed = ImmutableMap.builder();
        stuckBecauseStuck.stream().forEach(c -> delayed.put(c, Delay.of()));
        stuckOnVar.entries().stream().forEach(e -> delayed.put(e.getValue(), Delay.ofVar(e.getKey())));
        stuckOnEdge.entries().stream().forEach(e -> delayed.put(e.getValue(), Delay.ofCriticalEdge(e.getKey())));
        return delayed.build();
    }

}