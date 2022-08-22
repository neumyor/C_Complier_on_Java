package component.narray;

import symbolstruct.entries.Entry;
import symbolstruct.entries.VarEntry;

import java.util.ArrayList;

/**
 * 用于描述数组中元素位置的对象
 */
public class NPos {
    private ArrayList<VarEntry> pos;
    public NPos(ArrayList<VarEntry> pos){
        this.pos = pos;
    }

    public Integer getDims(){
        return this.pos.size();
    }

    public VarEntry getOnDim(Integer dim) {
        return this.pos.get(dim);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (VarEntry e:this.pos) {
            builder.append(String.format("[%s]", e.name));
        }
        return builder.toString();
    }
}
