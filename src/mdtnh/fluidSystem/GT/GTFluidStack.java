package mdtnh.fluidSystem.GT;

import arc.struct.Seq;

public class GTFluidStack implements Comparable<GTFluidStack> {
    public GTFluid fluid;
    public int amount;
    public GTFluidStack(GTFluid fluid, int amount){
        this.fluid = fluid;
        this.amount = amount;
    }
    public GTFluidStack set(GTFluid fluid, int amount){
        this.fluid = fluid;
        this.amount = amount;
        return this;
    }
    public GTFluidStack copy(){
        return new GTFluidStack(fluid, amount);
    }
    public boolean equals(GTFluidStack other){
        return other != null && other.fluid == fluid && other.amount == amount;
    }
    public static GTFluidStack[] mult(GTFluidStack[] stacks, int amount){
        GTFluidStack[] copy = new GTFluidStack[stacks.length];
        for(int i = 0; i < copy.length; i++){
            copy[i] = new GTFluidStack(stacks[i].fluid, stacks[i].amount * amount);
        }
        return copy;
    }
    public static GTFluidStack[] with(Object... items){
        GTFluidStack[] stacks = new GTFluidStack[items.length / 2];
        for(int i = 0; i < items.length; i += 2){
            stacks[i / 2] = new GTFluidStack((GTFluid)items[i], ((Number)items[i + 1]).intValue());
        }
        return stacks;
    }
    public static Seq<GTFluidStack> list(Object... items){
        Seq<GTFluidStack> stacks = new Seq<>(items.length / 2);
        for(int i = 0; i < items.length; i += 2){
            stacks.add(new GTFluidStack((GTFluid)items[i], ((Number)items[i + 1]).intValue()));
        }
        return stacks;
    }
    @Override
    public int compareTo(GTFluidStack fluidStack){
        return fluid.compareTo(fluidStack.fluid);
    }
    @Override
    public boolean equals(Object o){
        if(this == o) return true;
        if(!(o instanceof GTFluidStack stack)) return false;
        return amount == stack.amount && fluid == stack.fluid;
    }
    @Override
    public String toString(){
        return "GTFluidStack{" +
                "fluid=" + fluid +
                ", amount=" + amount +
                '}';
    }
}
