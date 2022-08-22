package component.narray;

import component.datatype.Datatype;

import java.util.ArrayList;

public class ConstNArray extends NArray {

    public ConstNArray(Datatype datatype, ArrayList<NArrayItem> values) {
        super(datatype, values);
        for (NArrayItem item : values) {
            assert item instanceof NInt;
        }
    }
}
