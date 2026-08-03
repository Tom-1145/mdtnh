package mdtnh.energy;

import arc.util.io.Reads;
import arc.util.io.Writes;
import arc.math.Mathf;

public class EnergyState {
    /** 当前储存能量。 */
    public float energyJ;

    /** 上一个模拟秒的测量值。 */
    public int inputA;
    public int outputA;
    public int currentA;

    public boolean has(float amountJ) {
        return amountJ <= 0f || energyJ + 0.0001f >= amountJ;
    }

    /**
     * 全额消耗。
     * 能量不足时不扣除，并返回 false。
     */
    public boolean consume(float amountJ) {
        if (amountJ <= 0f) return true;
        if (!has(amountJ)) return false;

        energyJ -= amountJ;
        return true;
    }

    public float add(float amountJ, EnergySpec spec) {
        if (amountJ <= 0f) return 0f;

        float accepted = Math.min(amountJ, spec.capacityJ - energyJ);
        energyJ += accepted;
        return accepted;
    }

    public float fraction(EnergySpec spec) {
        return spec.capacityJ <= 0f
                ? 0f
                : Mathf.clamp(energyJ / spec.capacityJ);
    }

    public void write(Writes write) {
        write.f(energyJ);
    }

    public void read(Reads read, EnergySpec spec) {
        energyJ = Mathf.clamp(read.f(), 0f, spec.capacityJ);
    }
}
