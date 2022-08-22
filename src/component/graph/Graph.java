package component.graph;

import global.Config;
import mips.register.Register;
import symbolstruct.entries.Entry;

import java.util.*;

public class Graph {
    public HashMap<Entry, Node> nodes;
    public Stack<Node> stack;
    public List<Map.Entry<Entry, Integer>> list;

    public Graph(List<Map.Entry<Entry, Integer>> list) {
        this.nodes = new HashMap<>();
        this.stack = new Stack<>();
        this.list = list;
    }

    public void addNode(Entry entry) {
        if (!nodes.containsKey(entry)) {
            this.nodes.put(entry, new Node(entry));
        }
    }

    public void addEdge(Entry a, Entry b) {
        Node nodeA = nodes.get(a);
        Node nodeB = nodes.get(b);
        nodeA.connects.add(nodeB);
        nodeB.connects.add(nodeA);
    }

    public void executeDistribution() {
        ArrayList<Register> regs = Config.getGlbReg();

        if(this.nodes.size() == 0) {
            return;
        }

        // 删除连接数小于K的节点，直到剩下最后一个节点
        while (nodes.size() > 1) {
            Node toRemove = null;
            for (Map.Entry entry : this.nodes.entrySet()) {
                Node node = (Node) entry.getValue();
                int counter = 0;
                for (Node otherNode : node.connects) {
                    if (nodes.containsKey(otherNode.entry)) {
                        counter++;
                    }
                }
                if (counter < regs.size()) {
                    toRemove = node;
                    break;
                }
            }

            if (toRemove != null) {
                stack.add(toRemove);
                nodes.remove(toRemove.entry);
            } else {
                // 如果检查一遍发现没有任何节点连接数小于k，那么遍历引用计数找到计数最少的一个Entry，并移除
                Entry noDistribute = null;
                for (Map.Entry entry : this.list) {
                    if (this.nodes.containsKey(entry.getKey())) {
                        noDistribute = (Entry) entry.getKey();
                        break;
                    }
                }
                nodes.remove(noDistribute);
            }
        }

        Node lastNode = (Node) this.nodes.values().toArray()[0];
        lastNode.reg = regs.get(0);

        // 按移除的顺序的逆序，将节点一个个取出
        while (stack.size() > 0) {
            Node toAdd = stack.pop();
            HashSet<Register> tmpRegs = new HashSet<>(regs);
            for (Node connNode : toAdd.connects) {
                // 对需要添加的节点的每条边，如果另一方处在当前图中，则移除其对应的寄存器
                if (nodes.containsKey(connNode.entry)) {
                    tmpRegs.remove(connNode.reg);
                }
            }
            // 选取剩下的寄存器中的最后一个
            toAdd.reg = (Register) tmpRegs.toArray()[0];
            // 将需要添加的点加入图中
            nodes.put(toAdd.entry, toAdd);
        }
    }

    public HashMap<Entry, Register> dump() {
        HashMap<Entry, Register> distribution = new HashMap<>();
        for (Entry entry : this.nodes.keySet()) {
            distribution.put(entry, this.nodes.get(entry).reg);
        }
        return distribution;
    }
}
