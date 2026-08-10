package mdtnh.turret;

/** 物品弹药 + MDT 电网。 */
public class MdtElectricItemTurret extends MdtEnergyItemTurret {

    public MdtElectricItemTurret(String name) {
        super(name);
        buildType = MdtElectricItemTurretBuild::new;
    }

    public class MdtElectricItemTurretBuild extends MdtEnergyItemTurretBuild {
        @Override
        public boolean canConnectToElectricGrid() {
            return true;
        }
    }
}
