package symbolstruct;

import symbolstruct.entries.AbsVarEntry;
import symbolstruct.entries.Entry;
import symbolstruct.entries.FuncEntry;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;


/**
 * Frame 函数栈帧对象
 * 其存储了当前函数下所有局部变量和函数参数
 * 不同作用域的同名变量通过其所在的Scope来进行区分（这由Entry的hashcode方法支持）
 * 注意函数形参的地址 高于 $ra地址 高于 局部变量地址
 */
public class Frame {
    public ArrayList<Entry> allEntries;         // 栈帧需要存储的所有符号项
    public HashMap<Entry, Integer> offsetMap;   // 符号项对象到偏移量的映射
    public Integer size;                        // 栈帧大小，单位为字节
    public Integer offsetRA;                    // $ra所需要存储的位置的偏移地址

    public ArrayList<Entry> localEntries;
    public ArrayList<Entry> params;

    public Frame(ArrayList<Entry> params, ArrayList<Entry> localEntries) {
        this.localEntries = localEntries;
        this.params = params;

        this.allEntries = new ArrayList<>();
        this.allEntries.addAll(params);
        this.allEntries.addAll(localEntries);

        this.offsetMap = new HashMap<>();

        int offset = 0; //注意参数的排布方式

        for (Entry local : localEntries) {
            if (local instanceof AbsVarEntry) {
                this.offsetMap.put(local, offset);
                offset += ((AbsVarEntry) local).size * 4;
            }
        }
        // 注意我们先将offset建立映射，再将offset+=4
        // 这保证我们的参数这栈帧中的偏移从0一直到(frame.size-4)
        // 这一处与IMExp.PushParaExp联动，后者保证了将函数参数压入栈时，是从-4开始压入的

        offsetRA = offset;  // 将$ra的对应存储地址放在局部变量和参数之间
        offset += 4;

        for (int iter = params.size() - 1; iter >= 0; iter--) {
            Entry param = params.get(iter);
            if (param instanceof AbsVarEntry) {
                this.offsetMap.put(param, offset);
                offset += ((AbsVarEntry) param).size * 4;
            }
        }
        // 注意参数的顺序是反向，这和我们PushPara时的顺序有关。Push时是从左往右，
        // 而我们offset实际上是从低地址到高地址，因此是从右往左依次提高offset的值

        this.size = offset;

        /* 栈帧中存储的大致描述：
         * param 1
         * param 2
         * ______________
         * ra
         * --------------
         * local var 1
         * local var 2      <=== sp
         * */
    }

    public void insertEntry(Entry e) {
        // 将新添加的变量加入局部变量中，然后重新构建映射关系
        this.localEntries.add(e);

        this.allEntries = new ArrayList<>();
        this.allEntries.addAll(params);
        this.allEntries.addAll(localEntries);

        this.offsetMap = new HashMap<>();

        int offset = 0;

        for (Entry local : localEntries) {
            if (local instanceof AbsVarEntry) {
                this.offsetMap.put(local, offset);
                offset += ((AbsVarEntry) local).size * 4;
            }
        }

        this.offsetRA = offset;
        offset += 4;

        for (int iter = params.size() - 1; iter >= 0; iter--) {
            Entry param = params.get(iter);
            if (param instanceof AbsVarEntry) {
                this.offsetMap.put(param, offset);
                offset += ((AbsVarEntry) param).size * 4;
            }
        }

        this.size = offset;
    }
}
