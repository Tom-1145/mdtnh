package mdtnh;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.gen.Building;
import mindustry.ui.Bar;
import mindustry.world.Block;

/**
 * Base block for the example discrete MDT electrical network.
 *
 * One simulated second is split into integral 1 A packets. A packet removes
 * voltageV joules from its source, loses wireLossV joules per wire tile and
 * deposits the remaining joules in the receiver.
 */
public class MdtEnergyBlock extends Block {

    public enum EnergyRole {
        generator,
        wire,
        consumer,
        battery
    }

    public EnergyRole role = EnergyRole.battery;

    /** Nominal and maximum safe input voltage. */
    public float voltageV = 12f;
    /** Stored-energy capacity in joules. Ignored by wires. */
    public float capacityJ = 1000f;
    /** Initial fraction of capacity when a new block is placed. */
    public float initialEnergyFraction = 0f;

    /** Integral 1 A packets accepted in one simulated second. */
    public int maxInputA = 1;
    /** Integral 1 A packets emitted in one simulated second. */
    public int maxOutputA = 1;

    /** Automatic energy increase once per simulated second. */
    public float generationJPerSecond = 0f;
    /** Automatic energy decrease once per simulated second. */
    public float consumptionJPerSecond = 0f;

    /** Only used by wire blocks: maximum gross current through this tile. */
    public int maxWireCurrentA = 1;
    /** Only used by wire blocks: voltage lost by each 1 A packet. */
    public float wireLossV = 0f;

    /** Vanilla atlas region used until custom sprites are added. */
    public String fallbackRegion = "battery";

    public MdtEnergyBlock(String name) {
        super(name);

        update = true;
        solid = true;
        destructible = true;
        canOverdrive = false;
        hasPower = false;
        outputsPower = false;
        consumesPower = false;
        conductivePower = false;
        connectedPower = false;
        sync = true;

        buildType = MdtEnergyBuild::new;
    }

    public boolean isWire() {
        return role == EnergyRole.wire;
    }

    @Override
    public void load() {
        super.load();
        region = Core.atlas.find(fallbackRegion);
    }

    @Override
    public void setBars() {
        super.setBars();

        if (isWire()) {
            addBar("mdt-current", build -> {
                MdtEnergyBuild energy = (MdtEnergyBuild) build;
                return new Bar(
                    () -> "Current: " + energy.currentA + " / " + maxWireCurrentA + " A",
                    () -> Color.valueOf("ffd37f"),
                    () -> maxWireCurrentA <= 0 ? 0f : Math.min(1f, energy.currentA / (float) maxWireCurrentA)
                );
            });
        } else {
            addBar("mdt-energy", build -> {
                MdtEnergyBuild energy = (MdtEnergyBuild) build;
                return new Bar(
                    () -> "Energy: " + Math.round(energy.energyJ) + " / " + Math.round(capacityJ) + " J",
                    () -> Color.valueOf("ffd37f"),
                    () -> capacityJ <= 0f ? 0f : Math.min(1f, energy.energyJ / capacityJ)
                );
            });

            addBar("mdt-io", build -> {
                MdtEnergyBuild energy = (MdtEnergyBuild) build;
                int maximum = Math.max(1, Math.max(maxInputA, maxOutputA));
                return new Bar(
                    () -> "I/O: " + energy.inputA + " A in, " + energy.outputA + " A out",
                    () -> Color.valueOf("84f491"),
                    () -> Math.min(1f, Math.max(energy.inputA, energy.outputA) / (float) maximum)
                );
            });
        }
    }

    public class MdtEnergyBuild extends Building {
        /** Persistent stored energy. */
        public float energyJ;

        /** Measurements from the most recent simulated second. */
        public int inputA;
        public int outputA;
        public int currentA;
        public float unmetConsumptionJ;

        public MdtEnergyBlock energyBlock() {
            return (MdtEnergyBlock) block;
        }

        public boolean isWire() {
            return energyBlock().isWire();
        }

        public float soc() {
            float capacity = energyBlock().capacityJ;
            return capacity <= 0f ? 0f : energyJ / capacity;
        }

        @Override
        public void created() {
            super.created();
            MdtEnergyBlock type = energyBlock();
            energyJ = type.isWire() ? 0f : type.capacityJ * type.initialEnergyFraction;
        }

        @Override
        public void draw() {
            if (isWire()) {
                float fraction = energyBlock().maxWireCurrentA <= 0
                    ? 0f
                    : Math.min(1f, currentA / (float) energyBlock().maxWireCurrentA);

                Draw.color(Color.valueOf("ffd37f"));
                Lines.stroke(1.2f + 1.8f * fraction);

                for (int direction = 0; direction < 4; direction++) {
                    Building nearby = tile.nearbyBuild(direction);
                    if (nearby instanceof MdtEnergyBuild
                        && MdtEnergySystem.canConnect(this, (MdtEnergyBuild) nearby)) {
                        Lines.line(x, y, nearby.x, nearby.y);
                    }
                }

                Draw.reset();
            }

            super.draw();
        }

        @Override
        public void write(Writes write) {
            super.write(write);
            write.f(energyJ);
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            MdtEnergyBlock type = energyBlock();
            energyJ = Math.max(0f, Math.min(type.capacityJ, read.f()));
        }

        @Override
        public byte version() {
            return 1;
        }
    }
}
