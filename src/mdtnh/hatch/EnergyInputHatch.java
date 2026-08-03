package mdtnh.hatch;

import arc.graphics.Color;
import mdtnh.energy.EnergySpec;
import mdtnh.energy.EnergyState;
import mdtnh.energy.MdtEnergyNode;
import mindustry.gen.Building;
import mindustry.ui.Bar;

public class EnergyInputHatch extends Hatch {
    public final EnergySpec energySpec = new EnergySpec();

    public EnergyInputHatch(String name) {
        super(name);
        // 能源仓不储存物品
        hasItems = false;
        itemCapacity = 0;

        energySpec.role = EnergySpec.Role.consumer;
        energySpec.voltageV = 12f;
        energySpec.capacityJ = 2400f;
        energySpec.maxInputA = 16;
        energySpec.maxOutputA = 0;   // 输入仓不向外电网放电

        buildType = EnergyInputHatchBuild::new;
    }

    @Override
    public void setBars() {
        super.setBars();
        addBar("mdt-energy", raw -> {
            EnergyInputHatchBuild build = (EnergyInputHatchBuild) raw;
            return new Bar(
                    () -> "Energy: " + Math.round(build.energyState.energyJ) + " / " + Math.round(energySpec.capacityJ) + " J",
                    () -> Color.valueOf("ffd37f"),
                    () -> build.energyState.fraction(energySpec)
            );
        });
    }

    public class EnergyInputHatchBuild extends HatchBuild implements MdtEnergyNode {
        public final EnergyState energyState = new EnergyState();

        @Override
        public Building energyBuilding() {
            return this;
        }

        @Override
        public EnergySpec energySpec() {
            return EnergyInputHatch.this.energySpec;
        }

        @Override
        public EnergyState energyState() {
            return energyState;
        }

        @Override
        public byte version() {
            return 1;
        }

        @Override
        public void write(arc.util.io.Writes write) {
            super.write(write);
            energyState.write(write);
        }

        @Override
        public void read(arc.util.io.Reads read, byte revision) {
            super.read(read, revision);
            if (revision >= 1) {
                energyState.read(read, energySpec());
            }
        }
    }
}