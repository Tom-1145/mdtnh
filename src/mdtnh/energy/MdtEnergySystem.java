package mdtnh.energy;

import arc.Events;
import mindustry.Vars;
import mindustry.game.EventType.Trigger;
import mindustry.gen.Building;
import mindustry.gen.Groups;

import java.util.*;

/**
 * MDT 离散能源网络的全局结算器。
 *
 * <p>系统把时间划分为一秒，并把电流离散为持续一秒的 1A 电流包。
 * 每个模拟秒依次执行以下步骤：</p>
 *
 * <ol>
 *     <li>收集世界中全部 {@link MdtEnergyNode}；</li>
 *     <li>按实际邻接关系划分互不连通的网络；</li>
 *     <li>处理发电机和示例用电器的自动能量变化；</li>
 *     <li>在每个网络内部按优先级和最小线损路径传输 1A 电流包。</li>
 * </ol>
 *
 * <p>该系统独立于 Mindustry 原生 PowerGraph。所有能源建筑通过
 * {@link MdtEnergyNode} 接口参与结算。</p>
 */
public final class MdtEnergySystem {

    /** Mindustry 默认每秒包含的逻辑 tick 数。 */
    private static final double ticksPerSecond = 60d;

    /** 浮点比较容差，用于避免临界值因计算误差被错误拒绝。 */
    private static final float epsilon = 0.0001f;

    /** 最近一次已经完成结算的整数秒编号。 */
    private static long lastProcessedSecond = Long.MIN_VALUE;

    /** 防止事件监听器被重复注册。 */
    private static boolean installed;

    private MdtEnergySystem() {}

    /**
     * 注册全局更新监听器。
     *
     * <p>该方法应在模组内容加载后调用一次。若游戏暂停、退出地图或尚未进入游戏，
     * 秒计数会被重置，避免把菜单停留时间补算到下一局。</p>
     */
    public static void install() {
        if (installed) return;
        installed = true;

        Events.run(Trigger.update, () -> {
            if (!Vars.state.isPlaying()) {
                lastProcessedSecond = Long.MIN_VALUE;
                return;
            }

            long currentSecond = (long) (Vars.state.tick / ticksPerSecond);

            // 首次进入地图或 tick 回退时只建立时间基准，不立即执行一次结算。
            if (lastProcessedSecond == Long.MIN_VALUE || currentSecond < lastProcessedSecond) {
                lastProcessedSecond = currentSecond;
                return;
            }

            // 当单帧跨过多个整数秒时逐秒补算，保证结果不依赖帧率。
            while (lastProcessedSecond < currentSecond) {
                lastProcessedSecond++;
                stepOneSecond();
            }
        });
    }

    /**
     * 判断两个能源节点能否直接建立网络邻接边。
     *
     * <p>节点必须属于同一队伍，并且至少一端是导线。这样普通设备只能作为路径端点，
     * 电流不会直接穿过一个设备再进入另一个设备。</p>
     */
    public static boolean canConnect(MdtEnergyNode a, MdtEnergyNode b) {
        if (a == null || b == null) return false;
        return a.energyBuilding().team == b.energyBuilding().team
                && (a.isEnergyWire() || b.isEnergyWire());
    }

    /**
     * 执行一个完整模拟秒的能源结算。
     *
     * <p>输入、输出和导线电流统计在每秒开始时清零，只表示当前结算秒的结果。
     * 各连通分量独立处理，因此不同网络之间不会交换能量。</p>
     */
    public static void stepOneSecond() {
        List<MdtEnergyNode> all = collectNodes();
        if (all.isEmpty()) return;

        // 清除上一秒的电流测量值，储能量不受影响。
        for (MdtEnergyNode node : all) {
            EnergyState state = node.energyState();
            state.inputA = 0;
            state.outputA = 0;
            state.currentA = 0;
            state.lastInputVoltageV = 0f;
            state.ignoredInputA = 0;
            state.overvoltageA = 0;
        }

        // 使用对象身份集合，避免建筑类自定义 equals/hashCode 影响节点去重。
        Set<MdtEnergyNode> visited = Collections.newSetFromMap(new IdentityHashMap<>());

        for (MdtEnergyNode root : all) {
            if (visited.contains(root)) continue;

            List<MdtEnergyNode> component = collectComponent(root, visited);
            applyAutomaticEnergyChange(component);
            routePackets(component);
        }
    }

    /**
     * 扫描世界中的全部能源节点。
     *
     * <p>按建筑位置排序可以让相同地图在相同状态下获得稳定的调度顺序，
     * 降低集合遍历顺序造成的结果差异。</p>
     */
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
     * 返回与指定建筑整个占地边缘相邻的能源节点。
     *
     * <p>{@link Building#proximity} 覆盖建筑全部边缘，因此能够正确处理 2×2
     * 或更大的能源工厂。结果会去重并按位置排序，供连通分量搜索和寻路共同使用。</p>
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

    /**
     * 使用广度优先搜索收集一个完整连通网络。
     *
     * @param root 搜索起点
     * @param visited 全局已访问节点集合；本方法会把发现的节点加入其中
     * @return 与起点相连的全部能源节点
     */
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

    /**
     * 处理不依赖网络传输的自动能量变化。
     *
     * <p>通用示例发电机每秒向自身缓存加入固定能量，示例用电器每秒从自身缓存
     * 扣除固定能量。配方工厂和能源输入仓由各自建筑逻辑决定何时消耗能量，
     * 因此不会在这里额外扣除。</p>
     */
    private static void applyAutomaticEnergyChange(List<MdtEnergyNode> component) {
        for (MdtEnergyNode node : component) {
            EnergySpec spec = node.energySpec();
            EnergyState state = node.energyState();

            if (spec.isWire()) continue;

            if (spec.role == EnergySpec.Role.generator) {
                // 自动发电参数属于通用能源方块定义，其他节点类型默认不产生能量。
                if (node instanceof MdtEnergyBlock.MdtEnergyBuild) {
                    MdtEnergyBlock.MdtEnergyBuild legacy = (MdtEnergyBlock.MdtEnergyBuild) node;
                    state.energyJ = Math.min(
                            spec.capacityJ,
                            state.energyJ + legacy.energyBlock().generationJPerSecond
                    );
                }
            }

            if (spec.role == EnergySpec.Role.consumer) {
                // 自动耗电只应用于配置了 consumptionJPerSecond 的通用能源方块。
                if (node instanceof MdtEnergyBlock.MdtEnergyBuild) {
                    MdtEnergyBlock.MdtEnergyBuild legacy = (MdtEnergyBlock.MdtEnergyBuild) node;
                    float consumed = Math.min(
                            state.energyJ,
                            legacy.energyBlock().consumptionJPerSecond
                    );
                    state.energyJ -= consumed;
                }
            }
        }
    }

    /**
     * 在一个连通网络内反复传输 1A 电流包。
     *
     * <p>来源优先级为发电机高于电池；同类来源优先选择荷电比例较高者。
     * 接收端优先级为消费者高于电池；同类接收端优先选择荷电比例较低者。
     * 每个已发送的包都会占用当秒输入输出额度；欠压包会被丢弃，过压包会摧毁接收端。</p>
     */
    private static void routePackets(List<MdtEnergyNode> component) {
        List<MdtEnergyNode> sources = new ArrayList<>();
        List<MdtEnergyNode> sinks = new ArrayList<>();

        // 导线只参加寻路，不作为能量来源或终点。
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

            int socComp = Float.compare(
                    b.energyState().energyJ / Math.max(1, sb.capacityJ),
                    a.energyState().energyJ / Math.max(1, sa.capacityJ)
            );
            return socComp != 0
                    ? socComp
                    : Integer.compare(a.energyBuilding().pos(), b.energyBuilding().pos());
        });

        sinks.sort((a, b) -> {
            EnergySpec sa = a.energySpec();
            EnergySpec sb = b.energySpec();

            int priA = sa.role == EnergySpec.Role.consumer ? 0 : 1;
            int priB = sb.role == EnergySpec.Role.consumer ? 0 : 1;
            if (priA != priB) return Integer.compare(priA, priB);

            int socComp = Float.compare(
                    a.energyState().energyJ / Math.max(1, sa.capacityJ),
                    b.energyState().energyJ / Math.max(1, sb.capacityJ)
            );
            return socComp != 0
                    ? socComp
                    : Integer.compare(a.energyBuilding().pos(), b.energyBuilding().pos());
        });

        for (MdtEnergyNode sink : sinks) {
            boolean moved;
            do {
                moved = false;
                if (!canReceive(sink)) break;

                // 每次从最高优先级的可用来源尝试发送一个包。
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

    /**
     * 判断来源是否有资格向指定接收端发送下一个 1A 包。
     *
     * <p>来源必须有至少一个额定电压对应的能量包、尚未达到输出电流上限。
     * 发电机可向消费者或电池供电；电池只向消费者放电，避免电池之间循环倒能。</p>
     */
    private static boolean canSupply(MdtEnergyNode source, MdtEnergyNode sink) {
        EnergySpec srcSpec = source.energySpec();
        EnergyState srcState = source.energyState();

        if (source == sink
                || source.energyBuilding().dead()
                || sink.energyBuilding().dead()
                || srcState.outputA >= srcSpec.maxOutputA
                || srcState.energyJ + epsilon < srcSpec.voltageV)
            return false;

        if (srcSpec.role == EnergySpec.Role.generator) return true;

        return srcSpec.role == EnergySpec.Role.battery
                && sink.energySpec().role == EnergySpec.Role.consumer;
    }

    /**
     * 判断接收端是否尚有输入电流配额和储能空间。
     */
    private static boolean canReceive(MdtEnergyNode sink) {
        EnergySpec spec = sink.energySpec();
        EnergyState state = sink.energyState();
        return !sink.energyBuilding().dead()
                && state.inputA < spec.maxInputA
                && state.energyJ < spec.capacityJ - epsilon;
    }

    /**
     * 沿最小线损路径传输一个持续一秒的 1A 电流包。
     *
     * <p>只要路径存在，包就会离开发送端：来源扣除一个输出电压对应的能量，
     * 导线增加载流统计，接收端增加输入电流统计。随后才根据到达电压处理结果：</p>
     *
     * <ul>
     *     <li>低于最低输入电压：丢弃该包，不增加接收端缓存；</li>
     *     <li>高于最高输入电压：清空缓存并摧毁接收建筑；</li>
     *     <li>处于输入区间且容量足够：把到达电压对应的能量写入缓存；</li>
     *     <li>处于输入区间但剩余容量不足：丢弃整个包，不进行部分接收。</li>
     * </ul>
     *
     * <p>除了路径不存在之外，传输结果不会回退来源能量和线路电流。这保留了路径、
     * 电流上限和容量边界，但不再由调度器提前保护设备免受欠压或过压。</p>
     */
    private static boolean transferOneAmp(MdtEnergyNode source, MdtEnergyNode sink) {
        Path path = findPath(source, sink);
        if (path == null) return false;

        EnergySpec srcSpec = source.energySpec();
        EnergySpec sinkSpec = sink.energySpec();
        EnergyState sourceState = source.energyState();
        EnergyState sinkState = sink.energyState();

        // 线路损失可能超过来源电压；到达端电压最低按 0V 处理。
        float arrivalVoltage = Math.max(0f, srcSpec.voltageV - path.lossV);

        sourceState.energyJ = Math.max(0f, sourceState.energyJ - srcSpec.voltageV);
        sourceState.outputA++;

        sinkState.inputA++;
        sinkState.lastInputVoltageV = arrivalVoltage;

        // 路径上的每根导线都已经通过了这个 1A 包。
        for (MdtEnergyNode wire : path.wires) {
            wire.energyState().currentA++;
        }

        if (sinkSpec.isUndervoltage(arrivalVoltage)) {
            sinkState.ignoredInputA++;
            return true;
        }

        if (sinkSpec.isOvervoltage(arrivalVoltage)) {
            sinkState.overvoltageA++;
            sink.onOvervoltage(arrivalVoltage);
            return true;
        }

        if (sinkState.energyJ + arrivalVoltage > sinkSpec.capacityJ + epsilon) {
            sinkState.ignoredInputA++;
            return true;
        }

        sinkState.energyJ += arrivalVoltage;
        return true;
    }

    /**
     * 使用 Dijkstra 算法寻找来源到目标的最低总压降路径。
     *
     * <p>只有来源和目标可以是普通设备，中间节点必须是导线。达到载流上限的导线
     * 不参与本次寻路，因此后续电流包可以自动改走仍有容量的其他路径。</p>
     */
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

            // 普通设备不能作为路径中继点。
            if (current != source && !current.isEnergyWire()) continue;

            for (MdtEnergyNode neighbor : adjacentNodes(current)) {
                if (!neighbor.isEnergyWire() && neighbor != target) continue;

                if (neighbor.isEnergyWire()) {
                    EnergySpec wireSpec = neighbor.energySpec();
                    if (neighbor.energyState().currentA >= wireSpec.maxWireCurrentA) continue;
                }

                float addedLoss = neighbor.isEnergyWire()
                        ? neighbor.energySpec().wireLossV
                        : 0f;
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

        // 沿前驱表反向恢复路径，并只记录实际承担电流的导线节点。
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

    /** 优先队列中的 Dijkstra 搜索状态。 */
    private static class PathState {
        final MdtEnergyNode node;
        final float distance;

        PathState(MdtEnergyNode node, float distance) {
            this.node = node;
            this.distance = distance;
        }
    }

    /** 一条已找到的传输路径及其总压降。 */
    private static class Path {
        final float lossV;
        final List<MdtEnergyNode> wires;

        Path(float lossV, List<MdtEnergyNode> wires) {
            this.lossV = lossV;
            this.wires = wires;
        }
    }
}
