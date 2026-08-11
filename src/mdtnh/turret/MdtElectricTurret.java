package mdtnh.turret;

/** 使用 MDT 离散电网供能的固定弹丸炮台。 */
public class MdtElectricTurret extends MdtEnergyTurret {

    public MdtElectricTurret(String name) {
        super(name);
        buildType = MdtElectricTurretBuild::new;
    }

    public class MdtElectricTurretBuild extends MdtEnergyTurretBuild {
        @Override
        public boolean canConnectToElectricGrid() {
            return true;
        }
    }
}
