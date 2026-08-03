package mdtnh.energy;

import arc.Events;
import mindustry.Vars;
import mindustry.game.EventType.Trigger;
import mindustry.gen.Building;
import mindustry.gen.Groups;

import java.util.*;

public final class MdtEnergySystem {
    private static final double ticksPerSecond = 60d;
    private static final float epsilon = 0.0001f;

    private static long lastProcessedSecond = Long.MIN_VALUE;
    private static boolean installed;

    private MdtEnergySystem() {}

    public static void install() {
        if (installed) return;
        installed = true;

        Events.run(Trigger.update, () -> {
            if (!Vars.state.isPlaying()) {
                lastProcessedSecond = Long.MIN_VALUE;
                return;
            }

            long currentSecond = (long) (Vars.state.tick / ticksPerSecond);
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

    /** 统一连接判定：队伍相同且至少有一方是导线 */
    public static boolean canConnect(MdtEnergyNode a, MdtEnergyNode b) {
        if (a == null || b == null) return false;
        return a.energyBuilding().team == b.energyBuilding().team
                && (a.isEnergyWire() || b.isEnergyWire());
    }

    public static void stepOneSecond() {
        List<MdtEnergyNode> all = collectNodes();
        if (all.isEmpty()) return;

        for (MdtEnergyNode node : all) {
            EnergyState state = node.energyState();
            state.inputA = 0;
            state.outputA = 0;
            state.currentA = 0;
        }

        Set<MdtEnergyNode> visited = Collections.newSetFromMap(new IdentityHashMap<>());

        for (MdtEnergyNode root : all) {
            if (visited.contains(root)) continue;
            List<MdtEnergyNode> component = collectComponent(root, visited);
            applyAutomaticEnergyChange(component);
            routePackets(component);
        }
    }

    private static List<MdtEnergyNode> collectNodes() {
        List<MdtEnergyNode> result = new ArrayList<>();
        for (Building build : Groups.build) {
            if (build instanceof MdtEnergyNode) {
                result.add((MdtEnergyNode) build);
            }
        }
        result.sort(Comparator.comparingInt(b -> b.energyBuilding().pos()));
        return result;
    }

    /**
     * 返回与节点建筑边缘实际接触的能源节点。
     *
     * Building.proximity 按整个建筑占地维护邻接关系，能够正确处理 size > 1
     * 的方块；不能使用 tile.nearbyBuild()，后者只从建筑锚点取四格邻居。
     */
    private static List<MdtEnergyNode> adjacentNodes(MdtEnergyNode node) {
        Building building = node.energyBuilding();
        List<MdtEnergyNode> result = new ArrayList<>();
        Set<MdtEnergyNode> seen = Collections.newSetFromMap(new IdentityHashMap<>());

        if (building.proximity != null) {
            for (Building raw : building.proximity) {
                if (!(raw instanceof MdtEnergyNode)) continue;

                MdtEnergyNode neighbor = (MdtEnergyNode) raw;
                if (neighbor == node || !canConnect(node, neighbor) || !seen.add(neighbor)) continue;
                result.add(neighbor);
            }
        }

        result.sort(Comparator.comparingInt(n -> n.energyBuilding().pos()));
        return result;
    }

    private static List<MdtEnergyNode> collectComponent(MdtEnergyNode root, Set<MdtEnergyNode> visited) {
        List<MdtEnergyNode> component = new ArrayList<>();
        ArrayDeque<MdtEnergyNode> queue = new ArrayDeque<>();

        visited.add(root);
        queue.add(root);

        while (!queue.isEmpty()) {
            MdtEnergyNode current = queue.removeFirst();
            component.add(current);

            for (MdtEnergyNode neighbor : adjacentNodes(current)) {
                if (visited.add(neighbor)) {
                    queue.addLast(neighbor);
                }
            }
        }

        component.sort(Comparator.comparingInt(b -> b.energyBuilding().pos()));
        return component;
    }

    private static void applyAutomaticEnergyChange(List<MdtEnergyNode> component) {
        for (MdtEnergyNode node : component) {
            EnergySpec spec = node.energySpec();
            EnergyState state = node.energyState();

            if (spec.isWire()) continue;

            // 发电机：增加能量；消费者：扣除能量（超出部分记录未满足）
            if (spec.role == EnergySpec.Role.generator) {
                // 旧系统 generationJPerSecond 需要额外存储，这里使用自定义字段
                // 为了兼容，暂时在 EnergySpec 里没有 generationJPerSecond，我们可以从原始 block 获取
                // 由于 MdtEnergyBlock 的 generationJPerSecond 并未复制到 spec，需特殊处理
                // 这里我们通过 node 的原始类型特殊处理，或者扩展 EnergySpec
                // 简便做法：判断 node 是否为 MdtEnergyBlock.MdtEnergyBuild，从中读取
                if (node instanceof MdtEnergyBlock.MdtEnergyBuild) {
                    MdtEnergyBlock.MdtEnergyBuild legacy = (MdtEnergyBlock.MdtEnergyBuild) node;
                    state.energyJ = Math.min(spec.capacityJ, state.energyJ + legacy.energyBlock().generationJPerSecond);
                }
            }

            if (spec.role == EnergySpec.Role.consumer) {
                if (node instanceof MdtEnergyBlock.MdtEnergyBuild) {
                    MdtEnergyBlock.MdtEnergyBuild legacy = (MdtEnergyBlock.MdtEnergyBuild) node;
                    float consumed = Math.min(state.energyJ, legacy.energyBlock().consumptionJPerSecond);
                    state.energyJ -= consumed;
                    // 也可记录 unmet，但先不管
                }
            }

            // 对于 RecipeCrafter 或 EnergyInputHatch，它们没有自动消耗，所以无需处理
        }
    }

    private static void routePackets(List<MdtEnergyNode> component) {
        List<MdtEnergyNode> sources = new ArrayList<>();
        List<MdtEnergyNode> sinks = new ArrayList<>();

        for (MdtEnergyNode node : component) {
            EnergySpec spec = node.energySpec();
            if (spec.isWire()) continue;

            if (spec.maxOutputA > 0 && (spec.role == EnergySpec.Role.generator || spec.role == EnergySpec.Role.battery)) {
                sources.add(node);
            }
            if (spec.maxInputA > 0 && (spec.role == EnergySpec.Role.consumer || spec.role == EnergySpec.Role.battery)) {
                sinks.add(node);
            }
        }

        sources.sort((a, b) -> {
            EnergySpec sa = a.energySpec();
            EnergySpec sb = b.energySpec();
            int priA = sa.role == EnergySpec.Role.generator ? 0 : 1;
            int priB = sb.role == EnergySpec.Role.generator ? 0 : 1;
            if (priA != priB) return Integer.compare(priA, priB);
            int socComp = Float.compare(b.energyState().energyJ / Math.max(1, sb.capacityJ),
                    a.energyState().energyJ / Math.max(1, sa.capacityJ));
            return socComp != 0 ? socComp : Integer.compare(a.energyBuilding().pos(), b.energyBuilding().pos());
        });

        sinks.sort((a, b) -> {
            EnergySpec sa = a.energySpec();
            EnergySpec sb = b.energySpec();
            int priA = sa.role == EnergySpec.Role.consumer ? 0 : 1;
            int priB = sb.role == EnergySpec.Role.consumer ? 0 : 1;
            if (priA != priB) return Integer.compare(priA, priB);
            int socComp = Float.compare(a.energyState().energyJ / Math.max(1, sa.capacityJ),
                    b.energyState().energyJ / Math.max(1, sb.capacityJ));
            return socComp != 0 ? socComp : Integer.compare(a.energyBuilding().pos(), b.energyBuilding().pos());
        });

        for (MdtEnergyNode sink : sinks) {
            boolean moved;
            do {
                moved = false;
                if (!canReceive(sink)) break;
                for (MdtEnergyNode source : sources) {
                    if (!canSupply(source, sink)) continue;
                    if (transferOneAmp(source, sink)) {
                        moved = true;
                        break;
                    }
                }
            } while (moved);
        }
    }

    private static boolean canSupply(MdtEnergyNode source, MdtEnergyNode sink) {
        EnergySpec srcSpec = source.energySpec();
        EnergyState srcState = source.energyState();

        if (source == sink || srcState.outputA >= srcSpec.maxOutputA || srcState.energyJ + epsilon < srcSpec.voltageV)
            return false;

        if (srcSpec.role == EnergySpec.Role.generator) return true;

        return srcSpec.role == EnergySpec.Role.battery && sink.energySpec().role == EnergySpec.Role.consumer;
    }

    private static boolean canReceive(MdtEnergyNode sink) {
        EnergySpec spec = sink.energySpec();
        EnergyState state = sink.energyState();
        return state.inputA < spec.maxInputA && state.energyJ < spec.capacityJ - epsilon;
    }

    private static boolean transferOneAmp(MdtEnergyNode source, MdtEnergyNode sink) {
        Path path = findPath(source, sink);
        if (path == null) return false;

        EnergySpec srcSpec = source.energySpec();
        EnergySpec sinkSpec = sink.energySpec();

        float arrivalVoltage = srcSpec.voltageV - path.lossV;
        if (arrivalVoltage <= epsilon) return false;
        if (arrivalVoltage > sinkSpec.voltageV + epsilon) return false;

        EnergyState sinkState = sink.energyState();
        if (sinkState.energyJ + arrivalVoltage > sinkSpec.capacityJ + epsilon) return false;

        source.energyState().energyJ = Math.max(0f, source.energyState().energyJ - srcSpec.voltageV);
        sinkState.energyJ = Math.min(sinkSpec.capacityJ, sinkState.energyJ + arrivalVoltage);
        source.energyState().outputA++;
        sinkState.inputA++;

        for (MdtEnergyNode wire : path.wires) {
            wire.energyState().currentA++;
        }

        return true;
    }

    private static Path findPath(MdtEnergyNode source, MdtEnergyNode target) {
        Map<MdtEnergyNode, Float> distance = new HashMap<>();
        Map<MdtEnergyNode, MdtEnergyNode> previous = new HashMap<>();
        Set<MdtEnergyNode> settled = new HashSet<>();
        PriorityQueue<PathState> queue = new PriorityQueue<>(
                Comparator.comparingDouble((PathState s) -> s.distance)
                        .thenComparingInt(s -> s.node.energyBuilding().pos())
        );

        distance.put(source, 0f);
        queue.add(new PathState(source, 0f));

        while (!queue.isEmpty()) {
            PathState state = queue.poll();
            MdtEnergyNode current = state.node;
            if (!settled.add(current)) continue;
            if (current == target) break;

            if (current != source && !current.isEnergyWire()) continue;

            for (MdtEnergyNode neighbor : adjacentNodes(current)) {
                if (!neighbor.isEnergyWire() && neighbor != target) continue;

                if (neighbor.isEnergyWire()) {
                    EnergySpec wireSpec = neighbor.energySpec();
                    if (neighbor.energyState().currentA >= wireSpec.maxWireCurrentA) continue;
                }

                float addedLoss = neighbor.isEnergyWire() ? neighbor.energySpec().wireLossV : 0f;
                float newDist = state.distance + addedLoss;
                Float oldDist = distance.get(neighbor);
                if (oldDist == null || newDist + epsilon < oldDist) {
                    distance.put(neighbor, newDist);
                    previous.put(neighbor, current);
                    queue.add(new PathState(neighbor, newDist));
                }
            }
        }

        Float totalLoss = distance.get(target);
        if (totalLoss == null) return null;

        List<MdtEnergyNode> wires = new ArrayList<>();
        MdtEnergyNode cursor = target;
        while (cursor != source) {
            if (cursor.isEnergyWire()) wires.add(cursor);
            cursor = previous.get(cursor);
            if (cursor == null) return null;
        }
        Collections.reverse(wires);
        return new Path(totalLoss, wires);
    }

    private static class PathState {
        final MdtEnergyNode node;
        final float distance;
        PathState(MdtEnergyNode node, float distance) {
            this.node = node;
            this.distance = distance;
        }
    }

    private static class Path {
        final float lossV;
        final List<MdtEnergyNode> wires;
        Path(float lossV, List<MdtEnergyNode> wires) {
            this.lossV = lossV;
            this.wires = wires;
        }
    }
}