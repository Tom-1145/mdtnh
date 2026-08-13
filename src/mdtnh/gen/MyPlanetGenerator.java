package mdtnh.gen;

import arc.graphics.Color;
import arc.math.Rand;
import arc.math.geom.Vec3;
import arc.util.Log;
import arc.util.noise.Simplex;
import mindustry.content.Blocks;
import mindustry.maps.generators.PlanetGenerator;
import mindustry.type.Sector;
import mindustry.world.Block;
import mindustry.world.TileGen;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.*;

public class MyPlanetGenerator extends PlanetGenerator {

    public float scl = 1.2f;
    public float waterLevel = 0.45f;
    public float moistScl = 0.6f;

    @Override
    public int getSectorSize(Sector sector) {
        return 200;
    }
    private float computeHeight(Vec3 pos) {
        return Simplex.noise3d(
                seed,
                6,
                0.56f,
                scl,
                pos.x,
                pos.y,
                pos.z
        );
    }

    // 根据位置判断生物群落
    private Block getBiome(Vec3 pos) {
        float height = computeHeight(pos);
        float temperature = 1f - abs(pos.y)
                - height * 0.05f
                + 0.1f * Simplex.noise3d(
                seed + 5,
                5,
                0.5f,
                moistScl,
                pos.x,
                pos.y,
                pos.z
        );
        float moisture = Simplex.noise3d(
                seed + 1,
                2,
                0.5f,
                moistScl,
                pos.x,
                pos.y,
                pos.z
        );

        if (temperature < 0.25f) return Blocks.snow;
        if (height > 0.8f) return Blocks.stone;
        if (temperature > 0.65f && moisture < 0.75f) return Blocks.sand;
        if (moisture > 0.5f) return Blocks.grass;
        return Blocks.grass;
    }
    private Block getSurface(Vec3 position) {
        float height = computeHeight(position);
        if (height < waterLevel) {
            return Blocks.water;
        }
        return getBiome(position);
    }
    @Override
    public float getHeight(Vec3 position) {
        float height = computeHeight(position);
        if (height < waterLevel) {
            return waterLevel * 0.08f;
        }
        return height * 0.08f;
    }
    @Override
    public void getColor(Vec3 position, Color out) {
        Block block = getSurface(position);
        out.set(block.mapColor).a(1f - block.albedo);
    }
    @Override
    protected void genTile(Vec3 position, TileGen tile) {
        tile.floor = getSurface(position);
        tile.block = Blocks.air;
    }
    @Override
    public void generateSector(Sector sector) {
        super.generateSector(sector);
        float latitude = abs(sector.tile.v.y);
        sector.generateEnemyBase = latitude < 0.5f && sector.id % 4 == 0;
    }
    @Override
    protected void generate() {
        int size = sector.getSize();
        Vec3 sectorPos = sector.tile.v;
        int sectorSeed=seed+sector.id;
        //基础地形
        pass((x, y) -> {
            float worldX = sectorPos.x + (x - size / 2f) * 0.004f;
            float worldY = sectorPos.y + (y - size / 2f) * 0.004f;
            float worldZ = sectorPos.z;
            Vec3 worldPos = new Vec3(worldX, worldY, worldZ);

            float height = computeHeight(worldPos);

            if (height < waterLevel) {
                float depth = (waterLevel - height) / (waterLevel + 0.3f);
                if (depth < 0.05f) floor = Blocks.sandWater;
                else if (depth < 0.2f) floor = Blocks.water;
                else floor = Blocks.deepwater;
                block = Blocks.air;
            } else {
                floor = getBiome(worldPos);
                block = Blocks.air;
                float wallNoise = Simplex.noise3d(
                        sectorSeed + 300,
                        2,
                        0.5f,
                        0.12f,
                        worldPos.x,
                        worldPos.y,
                        worldPos.z
                );
                if (height > 0.6f && wallNoise > 0.35f) {
                    Block wallBlock;
                    if (floor == Blocks.snow) {
                        wallBlock = Blocks.snowWall;
                    } else if (floor == Blocks.sand) {
                        wallBlock = Blocks.sandWall;
                    } else if (floor == Blocks.grass) {
                        wallBlock = Blocks.stoneWall;
                    } else {
                        wallBlock = Blocks.stoneWall;
                    }
                    block = wallBlock;
                }
            }
        });

        // 河流生成
        pass((x, y) -> {
            if (floor == Blocks.water || floor == Blocks.deepwater || floor == Blocks.sandWater) {
                return;
            }
            float river = Simplex.noise3d(
                    sectorSeed + 100,
                    2,
                    0.6f,
                    0.06f,
                    x * 0.05f + 100,
                    y * 0.05f + 100,
                    0
            );
            if (river > 0.42f && river < 0.58f) {
                floor = Blocks.water;
                block = Blocks.air;
            } else if (river > 0.38f && river < 0.62f) {
                if (floor == Blocks.ice) {
                    floor = Blocks.darksandWater;
                } else {
                    floor = Blocks.sandWater;
                }
                block = Blocks.air;
            }
        });
        Rand rnd=new Rand(sectorSeed);
        List<MineralVein> mv = new ArrayList<>();
        mv.add(MineralVeins.test);
        int sum = 0;
        for(var i:mv)sum+=i.weight;
        final int sumWeight = sum;
        pass((x,y)->{
            if(x/4%4!=0||y/4%4!=0)return;
            if (floor == Blocks.water || floor == Blocks.deepwater || floor == Blocks.sandWater) {
                return;
            }
            if(block!=Blocks.air)return;
            MineralVein vein=null;
            int summ = 0;
            int c=rnd.nextInt(sumWeight)+1;
            for(var i:mv){
                summ+=i.weight;
                if(summ>=c)vein=i;
            }
            if(vein==null)return;
            if(!rnd.chance(vein.density))return;
            ore=vein.generateOre(rnd);
        });

        // 地形扭曲
        distort(8f, 10f);
    }
}