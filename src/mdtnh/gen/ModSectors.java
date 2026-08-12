package mdtnh.gen;

import mindustry.type.SectorPreset;

public class ModSectors{

    public static SectorPreset landing;

    public static void load(){

        landing = new SectorPreset(
                "landing",
                ModPlanet.myPlanet,
                10
        ){{
            alwaysUnlocked = true;

            difficulty = 1f;
            captureWave = 20;
        }};
    }
}
