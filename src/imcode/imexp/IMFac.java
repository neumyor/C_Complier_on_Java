package imcode.imexp;

import component.Label;
import component.narray.NArray;
import component.narray.NPos;
import global.Error;
import imcode.imitem.IMItem;
import imcode.imitem.IMItemFac;
import imcode.imitem.IMItemType;
import imcode.imitem.VarItem;
import symbolstruct.entries.AbsVarEntry;
import symbolstruct.entries.FuncEntry;
import symbolstruct.entries.SpaceEntry;
import symbolstruct.entries.VarEntry;

public abstract class IMFac {
    public static IMExp gen(IMExpType type, Object... items) {
        switch (type) {
            case ParaDef:
                assert items[0] instanceof VarEntry;
                return new ParaDefExp(convert(items[0]));
            case Beq:
                assert items[0] instanceof VarEntry;
                assert items[1] != null;
                assert items[2] != null;
                return new BeqExp(convert(items[0]), convert(items[1]), convert(items[2]));
            case Bge:
                assert items[0] instanceof VarEntry;
                assert items[1] != null;
                assert items[2] != null;
                return new BgeExp(convert(items[0]), convert(items[1]), convert(items[2]));
            case Bgt:
                assert items[0] instanceof VarEntry;
                assert items[1] != null;
                assert items[2] != null;
                return new BgtExp(convert(items[0]), convert(items[1]), convert(items[2]));
            case Bne:
                assert items[0] instanceof VarEntry;
                assert items[1] != null;
                assert items[2] != null;
                return new BneExp(convert(items[0]), convert(items[1]), convert(items[2]));
            case ConJump:
                assert items[0] instanceof Label;
                assert items[1] instanceof VarEntry;
                return new ConJumpExp(convert(items[0]), convert(items[1]));
            case ConNotJump:
                assert items[0] instanceof Label;
                assert items[1] instanceof VarEntry;
                return new ConNotJumpExp(convert(items[0]), convert(items[1]));
            case PushPara:
                assert items[0] instanceof AbsVarEntry;
                assert items[0] instanceof Integer;
                return new PushParaExp(convert(items[0]), convert(items[1]));
            case Call:
                assert items[0] instanceof FuncEntry;
                return new CallExp(convert(items[0]));
            case Return:
                if (items == null) {
                    return new ReturnExp(null);
                } else {
                    assert items[0] instanceof VarEntry;
                    return new ReturnExp(convert(items[0]));
                }
            case AssignByRet:
                assert items[0] instanceof VarEntry;
                return new AssignByRetExp(convert(items[0]));
            case Jump:
                assert items[0] instanceof Label;
                return new JumpExp(convert(items[0]));
            case Label:
                assert items[0] instanceof Label;
                return new LabelExp(convert(items[0]));
            case AssignByVarAddr:
                assert items[0] instanceof AbsVarEntry;
                assert items[1] instanceof SpaceEntry;
                return new AssignByVarAddrExp(convert(items[0]), convert(items[1]));
            case AssignByVar:
                assert items[0] instanceof VarEntry;
                assert items[1] instanceof VarEntry;
                return new AssignByVarExp(convert(items[0]), convert(items[1]));
            case AssignByGetInt:
                assert items[0] instanceof VarEntry;
                return new AssignByGetInt(convert(items[0]));
            case AssignToAddr:
                assert items[0] instanceof VarEntry;
                assert items[1] instanceof VarEntry;
                return new AssignToAddrExp(convert(items[0]), convert(items[1]));
            case AssignFromAddr:
                assert items[0] instanceof VarEntry;
                assert items[1] instanceof VarEntry;
                return new AssignFromAddrExp(convert(items[0]), convert(items[1]));
            case PrintVar:
                assert items[0] instanceof VarEntry;
                return new PrintVarExp(convert(items[0]));
            case PrintStr:
                assert items[0] instanceof String;
                return new PrintStrExp(convert(items[0]));
            case Add:
                assert items[0] instanceof AbsVarEntry; // ?????????????????????ArrayEntry+4????????????ArrayEntry[1]???????????????
                assert items[1] != null;
                assert items[2] != null;
                return new AddExp(convert(items[0]), convert(items[1]), convert(items[2]));
            case Div:
                assert items[0] instanceof VarEntry;
                assert items[1] != null;
                assert items[2] != null;
                return new DivExp(convert(items[0]), convert(items[1]), convert(items[2]));
            case Mod:
                assert items[0] instanceof VarEntry;
                assert items[1] != null;
                assert items[2] != null;
                return new ModExp(convert(items[0]), convert(items[1]), convert(items[2]));
            case Mul:
                assert items[0] instanceof VarEntry;
                assert items[1] != null;
                assert items[2] != null;
                return new MulExp(convert(items[0]), convert(items[1]), convert(items[2]));
            case Sub:
                assert items[0] instanceof AbsVarEntry;
                assert items[1] != null;
                assert items[2] != null;
                return new SubExp(convert(items[0]), convert(items[1]), convert(items[2]));
            default:
                Error.warning("An Unknown Type Of IMExp created!");
                return null;
        }
    }

    /**
     * ???Object???????????????????????????????????????IMItem
     * Integer -> Int   ??????
     * AbsVarEntry -> Var   ???????????????????????????????????????
     * ArrayList -> Array   ??????????????????????????????????????????IMItem???
     *
     * @param o ?????????????????????
     * @return ???????????????????????????
     */
    private static IMItem convert(Object o) {
        if (o instanceof Integer) {
            return IMItemFac.gen(IMItemType.Int, o);
        } else if (o instanceof FuncEntry) {
            return IMItemFac.gen(IMItemType.Func, o);
        } else if (o instanceof NArray) {
            return IMItemFac.gen(IMItemType.Array, o);
        } else if (o instanceof NPos) {
            return IMItemFac.gen(IMItemType.NPos, o);
        } else if (o instanceof String) {
            return IMItemFac.gen(IMItemType.Str, o);
        } else if (o instanceof SpaceEntry) {
            return IMItemFac.gen(IMItemType.Space, o);
        } else if (o instanceof AbsVarEntry) {
            return IMItemFac.gen(IMItemType.Var, o);
        } else if (o instanceof Label) {
            return IMItemFac.gen(IMItemType.Label, o);
        } else {
            Error.warning("Unsupported type of Object to covert");
            return null;
        }
    }
}
