package mdtnh.gen;

import arc.graphics.Color;
import mindustry.content.Planets;
import mindustry.graphics.g3d.*;
import mindustry.type.Planet;

public class ModPlanet{

    public static Planet myPlanet;

    public static void load(){

        myPlanet = new Planet(
                "my-planet",
                Planets.sun,
                1f,
                3
        ){{
            generator = new MyPlanetGenerator();

            meshLoader = () ->
                    new HexMesh(this, 6);

            cloudMeshLoader = () ->
                    new MultiMesh(
                            new HexSkyMesh(
                                    this,
                                    11,
                                    0.15f,
                                    0.13f,
                                    5,
                                    Color.valueOf("77aaff"),
                                    2,
                                    0.45f,
                                    1.0f,
                                    0.38f
                            )
                    );

            atmosphereColor =
                    Color.valueOf("66aaff");

            atmosphereRadIn = 0.02f;
            atmosphereRadOut = 0.3f;

            startSector = 10;

            accessible = true;
            visible = true;

            allowLaunchSchematics = true;
            allowLaunchLoadout = true;
            alwaysUnlocked = true;
        }};
    }
}
