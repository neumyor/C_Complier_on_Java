package imcode.imitem;

import global.Error;

public class IMItemFac {
    /**
     * 类型和初始化对象的对应关系：
     * Int -> 数值 Integer
     * Str -> 字符串值 String
     * Var -> 符号项 VarEntry
     * Func -> 符号项 FuncEntry
     * ArrayInit->基本类型DataType 具体数值ArrayList
     *
     * @param type  需要的操作数类型
     * @param objs  传入的初始化对象
     * @return 生成的操作数对象IMItem
     */
    public static IMItem gen(IMItemType type, Object... objs) {
        switch (type) {
            case Int:
                return new IntItem(objs[0]);
            case Str:
                return new StrItem(objs[0]);
            case Var:
                return new VarItem(objs[0]);
            case Func:
                return new FuncItem(objs[0]);
            case Label:
                return new LabelItem(objs[0]);
            case NPos:
                return new PosItem(objs[0]);
            case Space:
                return new SpaceItem(objs[0]);
            default:
                Error.warning("Unknown type IMItem is created in IMItemFac");
                return null;
        }
    }
}
