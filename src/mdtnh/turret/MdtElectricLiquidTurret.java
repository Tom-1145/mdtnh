package mdtnh.turret;

/** 流体弹药 + MDT 电网。 */
public class MdtElectricLiquidTurret extends MdtEnergyLiquidTurret {

    public MdtElectricLiquidTurret(String name) {
        super(name);
        buildType = MdtElectricLiquidTurretBuild::new;
    }

    public class MdtElectricLiquidTurretBuild extends MdtEnergyLiquidTurretBuild {
        @Override
        public boolean canConnectToElectricGrid() {
            return true;
        }
    }
}
