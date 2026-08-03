package mdtnh;

import arc.Events;
import mindustry.Vars;
import mindustry.game.EventType.Trigger;
import mindustry.gen.Building;
import mindustry.gen.Groups;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import mdtnh.MdtEnergyBlock.EnergyRole;
import mdtnh.MdtEnergyBlock.MdtEnergyBuild;

/**
 * Deterministic, deliberately simplified discrete electrical network solver.
 */
public final class MdtEnergySystem {
    private static final double ticksPerSecond = 60d;
    private static final float epsilon = 0.0001f;

    private static long lastProcessedSecond = Long.MIN_VALUE;
    private static boolean installed;

    private MdtEnergySystem() {
    }

    public static void install() {
        if (installed) return;
        installed = true;

        Events.run(Trigger.update, () -> {
            if (!Vars.state.isPlaying()) {
                lastProcessedSecond = Long.MIN_VALUE;
                return;
            }

            long currentSecond = (long) (Vars.state.tick / ticksPerSecond);

            // Loading a map or rewinding game state must not replay old seconds.
            if (lastProcessedSecond == Long.MIN_VALUE || currentSecond < lastProcessedSecond) {
                lastProcessedSecond = currentSecond;
                return;
            }

            while (lastProcessedSecond < currentSecond) {
                lastProcessedSecond++;
                stepOneSecond();
            }
        });
    }

    /** A connection exists only between allied MDT blocks when at least one side is a wire. */
    public static boolean canConnect(MdtEnergyBuild first, MdtEnergyBuild second) {
        return first != null
            && second != null
            && first.team == second.team
            && (first.isWire() || second.isWire());
    }

    public static void stepOneSecond() {
        List<MdtEnergyBuild> all = collectBuilds();
        if (all.isEmpty()) return;

        for (MdtEnergyBuild build : all) {
            build.inputA = 0;
            build.outputA = 0;
            build.currentA = 0;
            build.unmetConsumptionJ = 0f;
        }

        Set<MdtEnergyBuild> visited = Collections.newSetFromMap(new IdentityHashMap<>());

        for (MdtEnergyBuild root : all) {
            if (visited.contains(root)) continue;

            List<MdtEnergyBuild> component = collectComponent(root, visited);
            applyAutomaticEnergyChange(component);
            routePackets(component);
        }
    }

    private static List<MdtEnergyBuild> collectBuilds() {
        List<MdtEnergyBuild> result = new ArrayList<>();

        for (Building build : Groups.build) {
            if (build instanceof MdtEnergyBuild) {
                result.add((MdtEnergyBuild) build);
            }
        }

        result.sort(Comparator.comparingInt(Building::pos));
        return result;
    }

    private static List<MdtEnergyBuild> collectComponent(
        MdtEnergyBuild root,
        Set<MdtEnergyBuild> visited
    ) {
        List<MdtEnergyBuild> result = new ArrayList<>();
        ArrayDeque<MdtEnergyBuild> queue = new ArrayDeque<>();

        visited.add(root);
        queue.add(root);

        while (!queue.isEmpty()) {
            MdtEnergyBuild current = queue.removeFirst();
            result.add(current);

            for (int direction = 0; direction < 4; direction++) {
                Building raw = current.tile.nearbyBuild(direction);
                if (!(raw instanceof MdtEnergyBuild)) continue;

                MdtEnergyBuild nearby = (MdtEnergyBuild) raw;
                if (!visited.contains(nearby) && canConnect(current, nearby)) {
                    visited.add(nearby);
                    queue.addLast(nearby);
                }
            }
        }

        result.sort(Comparator.comparingInt(Building::pos));
        return result;
    }

    private static void applyAutomaticEnergyChange(List<MdtEnergyBuild> component) {
        for (MdtEnergyBuild build : component) {
            MdtEnergyBlock type = build.energyBlock();
            if (type.isWire()) continue;

            // Generators and consumers are intentionally modeled as batteries whose
            // energy changes automatically once per simulated second.
            build.energyJ = Math.min(type.capacityJ, build.energyJ + type.generationJPerSecond);

            float consumed = Math.min(build.energyJ, type.consumptionJPerSecond);
            build.energyJ -= consumed;
            build.unmetConsumptionJ = Math.max(0f, type.consumptionJPerSecond - consumed);
        }
    }

    private static void routePackets(List<MdtEnergyBuild> component) {
        List<MdtEnergyBuild> sources = new ArrayList<>();
        List<MdtEnergyBuild> sinks = new ArrayList<>();

        for (MdtEnergyBuild build : component) {
            MdtEnergyBlock type = build.energyBlock();
            if (type.isWire()) continue;

            if (type.maxOutputA > 0
                && (type.role == EnergyRole.generator || type.role == EnergyRole.battery)) {
                sources.add(build);
            }

            if (type.maxInputA > 0
                && (type.role == EnergyRole.consumer || type.role == EnergyRole.battery)) {
                sinks.add(build);
            }
        }

        sources.sort((first, second) -> {
            int firstPriority = first.energyBlock().role == EnergyRole.generator ? 0 : 1;
            int secondPriority = second.energyBlock().role == EnergyRole.generator ? 0 : 1;
            if (firstPriority != secondPriority) return Integer.compare(firstPriority, secondPriority);

            int socCompare = Float.compare(second.soc(), first.soc());
            return socCompare != 0 ? socCompare : Integer.compare(first.pos(), second.pos());
        });

        sinks.sort((first, second) -> {
            int firstPriority = first.energyBlock().role == EnergyRole.consumer ? 0 : 1;
            int secondPriority = second.energyBlock().role == EnergyRole.consumer ? 0 : 1;
            if (firstPriority != secondPriority) return Integer.compare(firstPriority, secondPriority);

            int socCompare = Float.compare(first.soc(), second.soc());
            return socCompare != 0 ? socCompare : Integer.compare(first.pos(), second.pos());
        });

        for (MdtEnergyBuild sink : sinks) {
            boolean moved;

            do {
                moved = false;
                if (!canReceive(sink)) break;

                for (MdtEnergyBuild source : sources) {
                    if (!canSupply(source, sink)) continue;
                    if (transferOneAmp(source, sink)) {
                        moved = true;
                        break;
                    }
                }
            } while (moved);
        }
    }

    private static boolean canSupply(MdtEnergyBuild source, MdtEnergyBuild sink) {
        MdtEnergyBlock sourceType = source.energyBlock();

        if (source == sink
            || source.outputA >= sourceType.maxOutputA
            || source.energyJ + epsilon < sourceType.voltageV) {
            return false;
        }

        if (sourceType.role == EnergyRole.generator) return true;

        // Batteries are backup sources for consumers only. They never charge one
        // another, which prevents packet oscillation between adjacent batteries.
        return sourceType.role == EnergyRole.battery
            && sink.energyBlock().role == EnergyRole.consumer;
    }

    private static boolean canReceive(MdtEnergyBuild sink) {
        MdtEnergyBlock sinkType = sink.energyBlock();
        return sink.inputA < sinkType.maxInputA
            && sink.energyJ < sinkType.capacityJ - epsilon;
    }

    private static boolean transferOneAmp(MdtEnergyBuild source, MdtEnergyBuild sink) {
        Path path = findPath(source, sink);
        if (path == null) return false;

        MdtEnergyBlock sourceType = source.energyBlock();
        MdtEnergyBlock sinkType = sink.energyBlock();

        float arrivalVoltage = sourceType.voltageV - path.lossV;
        if (arrivalVoltage <= epsilon) return false;

        // Safe automatic scheduler: an over-voltage packet is rejected. A later
        // machine/controller can deliberately bypass this check to implement explosions.
        if (arrivalVoltage > sinkType.voltageV + epsilon) return false;

        // Packets are indivisible; do not send one if the receiver cannot hold it all.
        if (sink.energyJ + arrivalVoltage > sinkType.capacityJ + epsilon) return false;

        source.energyJ = Math.max(0f, source.energyJ - sourceType.voltageV);
        sink.energyJ = Math.min(sinkType.capacityJ, sink.energyJ + arrivalVoltage);
        source.outputA++;
        sink.inputA++;

        for (MdtEnergyBuild wire : path.wires) {
            wire.currentA++;
        }

        return true;
    }

    private static Path findPath(MdtEnergyBuild source, MdtEnergyBuild target) {
        Map<MdtEnergyBuild, Float> distance = new HashMap<>();
        Map<MdtEnergyBuild, MdtEnergyBuild> previous = new HashMap<>();
        Set<MdtEnergyBuild> settled = new HashSet<>();
        PriorityQueue<PathState> queue = new PriorityQueue<>(
            Comparator.comparingDouble((PathState state) -> state.distance)
                .thenComparingInt(state -> state.build.pos())
        );

        distance.put(source, 0f);
        queue.add(new PathState(source, 0f));

        while (!queue.isEmpty()) {
            PathState state = queue.poll();
            MdtEnergyBuild current = state.build;

            if (!settled.add(current)) continue;
            if (current == target) break;

            // Devices are endpoints, not pass-through conductors.
            if (current != source && !current.isWire()) continue;

            for (int direction = 0; direction < 4; direction++) {
                Building raw = current.tile.nearbyBuild(direction);
                if (!(raw instanceof MdtEnergyBuild)) continue;

                MdtEnergyBuild nearby = (MdtEnergyBuild) raw;
                if (!canConnect(current, nearby)) continue;
                if (!nearby.isWire() && nearby != target) continue;

                if (nearby.isWire()) {
                    MdtEnergyBlock wireType = nearby.energyBlock();
                    if (nearby.currentA >= wireType.maxWireCurrentA) continue;
                }

                float addedLoss = nearby.isWire() ? nearby.energyBlock().wireLossV : 0f;
                float newDistance = state.distance + addedLoss;
                Float oldDistance = distance.get(nearby);

                if (oldDistance == null || newDistance + epsilon < oldDistance) {
                    distance.put(nearby, newDistance);
                    previous.put(nearby, current);
                    queue.add(new PathState(nearby, newDistance));
                }
            }
        }

        Float totalLoss = distance.get(target);
        if (totalLoss == null) return null;

        List<MdtEnergyBuild> wires = new ArrayList<>();
        MdtEnergyBuild cursor = target;

        while (cursor != source) {
            if (cursor.isWire()) wires.add(cursor);
            cursor = previous.get(cursor);
            if (cursor == null) return null;
        }

        Collections.reverse(wires);
        return new Path(totalLoss, wires);
    }

    private static final class PathState {
        final MdtEnergyBuild build;
        final float distance;

        PathState(MdtEnergyBuild build, float distance) {
            this.build = build;
            this.distance = distance;
        }
    }

    private static final class Path {
        final float lossV;
        final List<MdtEnergyBuild> wires;

        Path(float lossV, List<MdtEnergyBuild> wires) {
            this.lossV = lossV;
            this.wires = wires;
        }
    }
}
