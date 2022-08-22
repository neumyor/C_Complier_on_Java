package mips.exps;

import mips.register.Register;

/**
 * MIPS R型指令
 * 接收三个寄存器变量，一个立即数变量（用于移位指令）
 */
public abstract class MRType extends MIPSExp{
    private Register rs;
    private Register rt;
    private Register rd;

    private Integer shamt;

    public abstract String toString();
}
