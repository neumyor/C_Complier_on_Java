package improve.component;

import global.Error;
import imcode.imexp.*;
import imcode.imitem.VarItem;
import symbolstruct.entries.Entry;

import java.util.Objects;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class ValidExp {
    IMExp exp;

    public ValidExp(IMExp exp) {
        if (exp instanceof AddExp ||
                exp instanceof SubExp ||
                exp instanceof MulExp ||
                exp instanceof DivExp ||
                exp instanceof ModExp) {
            this.exp = exp;
        } else {
            Error.warning("Invalid exp put into ValidExp");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ValidExp validExp = (ValidExp) o;
        IMExp otherExp = validExp.exp;

        if (!exp.getClass().equals(validExp.exp.getClass())) {
            return false;
        }

        if (exp instanceof MulExp || exp instanceof DivExp || exp instanceof ModExp) {
            return exp.item2.equals(otherExp.item2) && exp.item3.equals(otherExp.item3);
        } else {
            return (exp.item2.equals(otherExp.item2) && exp.item3.equals(otherExp.item3)) ||
                    (exp.item3.equals(otherExp.item2) && exp.item2.equals(otherExp.item3));
        }
    }

    @Override
    public int hashCode() {
        if (exp instanceof MulExp || exp instanceof DivExp || exp instanceof ModExp) {
            return Objects.hash(exp.getClass()) + Objects.hash(exp.item2) + Objects.hash(exp.item3);
        } else {
            // 保证 a+b 和 b+a 是一致的
            int hash_a = max(Objects.hash(exp.item2), Objects.hash(exp.item3));
            int hash_b = min(Objects.hash(exp.item2), Objects.hash(exp.item3));
            int ret = Objects.hash(exp.getClass()) + hash_a + hash_b;
            return ret;
        }
    }

    public Boolean in(Entry e) {
        VarItem item2 = (VarItem) exp.item2;
        VarItem item3 = (VarItem) exp.item3;
        return e.equals(item2.entry) || e.equals(item3.entry);
    }
}
