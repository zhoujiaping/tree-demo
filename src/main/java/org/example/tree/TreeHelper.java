package org.example.tree;

import lombok.AccessLevel;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 *
 */
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TreeHelper<CODE, NODE> {
    Supplier<NODE> nodeCreator;
    Function<NODE, CODE> codeGetter;
    BiConsumer<NODE, CODE> codeSetter;
    Function<NODE, CODE> parentCodeGetter;
    BiConsumer<NODE, CODE> parentCodeSetter;
    Function<NODE, Collection<NODE>> childrenGetter;
    BiConsumer<NODE, Collection<NODE>> childrenSetter;
    Function<NODE, String> toStringFn = node -> node.toString();

    /**
     * 有的场景，并不是一颗树，而是一个森林。
     * 为了能够应用该类的其他方法，需要一颗虚拟节点，作为根节点。
     * 其中nodes是一个森林。
     * dummyRootCode为虚拟根节点的code。
     * <p>
     * 该方法会修改森林的各个根节点，设置其parentCode。
     */
    public NODE dummyRoot(CODE dummyRootCode, List<NODE> nodes) {
        NODE dummyRoot = nodeCreator.get();
        codeSetter.accept(dummyRoot, dummyRootCode);
        childrenSetter.accept(dummyRoot, nodes);
        nodes.forEach(node -> parentCodeSetter.accept(node, dummyRootCode));
        return dummyRoot;
    }

    /**
     * 将一颗树扁平化。得到一个节点列表。
     *
     * @param root            根节点
     * @param setChildrenNull 是否将各节点的children置为null。
     * @return
     */
    public List<NODE> flatten(NODE root, boolean setChildrenNull) {
        NODE tree = root;
        List<NODE> flatten = new ArrayList<>();
        bfs(tree, (node, level) -> flatten.add(node));
        if (setChildrenNull) {
            flatten.forEach(it -> childrenSetter.accept(it, null));
        }
        return flatten;
    }

    /**
     * 使用广度优先遍历算法遍历一颗树。也就是从上到下，逐层遍历，每一层从左到右遍历。
     * 如果树中存在两个code相同的节点，会抛异常。
     *
     * @param tree     根节点
     * @param consumer
     */
    public void bfs(NODE tree, BiConsumer<NODE, Integer> consumer) {
        //检测循环引用
        Set<CODE> acceptedCodes = new HashSet<>();
        LinkedList<NODE> queue1 = new LinkedList<>();
        LinkedList<NODE> queue2 = new LinkedList<>();
        int level = 0;
        queue1.add(tree);
        NODE node;
        Collection<NODE> children;
        while (!queue1.isEmpty()) {
            node = queue1.poll();
            if (!acceptedCodes.add(codeGetter.apply(node))) {
                throw new RuntimeException("multiple code: " + codeGetter.apply(node) + ", maybe there is circular reference!");
            }
            children = childrenGetter.apply(node);
            consumer.accept(node, level);
            if (children != null) {
                queue2.addAll(children);
            }
            if (queue1.isEmpty()) {
                queue1 = queue2;
                queue2 = new LinkedList<>();
                level++;
            }
        }
    }

    /**
     * 深度优先（先序）算法 遍历树
     *
     * @param root
     * @param consumer
     */
    public void dfsWithPreOrder(NODE root, BiConsumer<NODE, Integer> consumer) {
        dfsWithPreOrder0(root, consumer, new HashSet<>(), 0);
    }

    private void dfsWithPreOrder0(NODE root, BiConsumer<NODE, Integer> consumer, Set<CODE> visitedCodes, int level) {
        if (!visitedCodes.add(codeGetter.apply(root))) {
            throw new RuntimeException("multiple code: " + codeGetter.apply(root) + ", maybe there is circular reference!");
        }
        Collection<NODE> children = childrenGetter.apply(root);
        consumer.accept(root, level);
        if (children != null) {
            for (NODE child : children) {
                dfsWithPreOrder0(child, consumer, visitedCodes, level + 1);
            }
        }
    }

    /**
     * 深度优先（后序）算法遍历树
     *
     * @param root
     * @param consumer
     */
    public void dfsWithPostOrder(NODE root, BiConsumer<NODE, Integer> consumer) {
        dfsWithPostOrder0(root, consumer, new HashSet<>(), 0);
    }

    private void dfsWithPostOrder0(NODE root, BiConsumer<NODE, Integer> consumer, Set<CODE> acceptedCodes, int level) {
        if (!acceptedCodes.add(codeGetter.apply(root))) {
            throw new RuntimeException("multiple code: " + codeGetter.apply(root) + ", maybe there is circular reference!");
        }
        Collection<NODE> children = childrenGetter.apply(root);
        if (children != null) {
            for (NODE child : children) {
                dfsWithPostOrder0(child, consumer, acceptedCodes, level + 1);
            }
        }
        consumer.accept(root, level);
    }

    /**
     * 层序遍历。和bfs不同的是，每次消费的是一层。bfs每次消费的是一个节点。
     *
     * @param root
     * @param consumer
     */
    public void bfsByLevel(NODE root, Consumer<List<NODE>> consumer) {
        Set<CODE> visitedCodes = new HashSet<>();
        List<NODE> queue = new LinkedList<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            for (NODE n : queue) {
                if (!visitedCodes.add(codeGetter.apply(n))) {
                    throw new RuntimeException("multiple code: " + codeGetter.apply(root) + ", maybe there is circular reference!");
                }
            }
            consumer.accept(queue);
            queue = queue.stream().filter(it -> childrenGetter.apply(it) != null)
                    .flatMap(it -> childrenGetter.apply(it).stream())
                    .collect(Collectors.toList());
        }
    }

    /**
     * 将一个节点列表转换成一颗树。
     * 该方法会修改节点的children。
     * 需要4次遍历。
     *
     * @param nodes
     * @return
     */
    public NODE treeify(List<NODE> nodes) {
        if (nodes.isEmpty()) {
            return null;
        }
        //copy
        //code=>node
        Map<CODE, NODE> map = nodes.stream().collect(Collectors.toMap(it -> codeGetter.apply(it), it -> it));
        //code=>children
        Map<CODE, List<NODE>> childrenMap = nodes.stream().filter(it -> parentCodeGetter.apply(it) != null)
                .collect(Collectors.groupingBy(it -> parentCodeGetter.apply(it)));
        //find roots, which parentCode not in map
        List<NODE> roots = nodes.stream().filter(
                it -> parentCodeGetter.apply(it) == null || !map.containsKey(parentCodeGetter.apply(it))
        ).collect(Collectors.toList());
        if (roots.size() != 1) {
            throw new RuntimeException("error! can't treeify nodes, cause roots.size()==" + roots.size());
        }
        //set children
        nodes.forEach(it -> childrenSetter.accept(it, childrenMap.get(codeGetter.apply(it))));
        return roots.get(0);
    }

    /**
     * 快速构建一颗树。
     * 仅需两次遍历，时间复杂度为o(n)
     *
     * @return
     */
    public NODE fastTreeify(List<NODE> nodes) {
        Map<CODE, NODE> visitedNodes = new HashMap<>();
        List<Supplier<?>> todos = new ArrayList<>();
        AtomicReference<NODE> root = new AtomicReference<>();
        nodes.forEach(node -> {
            CODE code = codeGetter.apply(node);
            visitedNodes.put(code, node);
            CODE parentCode = parentCodeGetter.apply(node);
            //childrenSetter.accept(node,new ArrayList<>());
            todos.add(() -> {
                NODE parentNode = visitedNodes.get(parentCode);
                if(parentNode == null){
                    if(!root.compareAndSet(null,node)){
                        throw new RuntimeException("too many root nodes!");
                    }
                    return null;
                }
                Collection<NODE> children = childrenGetter.apply(parentNode);
                if (children == null) {
                    children = new ArrayList<>();
                    childrenSetter.accept(parentNode, children);
                }
                children.add(node);
                if(children.size()>2){
                    throw new RuntimeException("xxx");
                }
                return null;
            });
        });
        todos.forEach(Supplier::get);
        return root.get();
    }

    /**
     * 将节点列表转为一颗树。允许相同code的节点存在。
     * 如果存在相同code的节点，前者会被忽略。
     *
     * @param nodes
     * @return
     */
    public NODE treeifyEnableDuplicateCode(List<NODE> nodes) {
        if (nodes.isEmpty()) {
            return null;
        }
        return treeifyEnableDuplicateCode0(nodes);
    }

    private NODE treeifyEnableDuplicateCode0(Collection<NODE> nodes) {
        //code=>node
        Map<CODE, NODE> map = new HashMap<>();
        nodes.forEach(it -> map.put(codeGetter.apply(it), it));

        //find roots, which parentCode not in map
        List<NODE> roots = map.values().stream().filter(it ->
                        parentCodeGetter.apply(it) == null || !map.containsKey(codeGetter.apply(it)))
                .collect(Collectors.toList());
        if (roots.size() != 1) {
            throw new RuntimeException("error! can't treeify nodes, cause roots.size()==" + roots.size());
        }
        //code=>children
        Map<CODE, List<NODE>> childrenMap = map.values().stream().filter(it -> parentCodeGetter.apply(it) != null)
                .collect(Collectors.groupingBy(parentCodeGetter::apply));
        //set children
        map.values().forEach(it -> childrenSetter.accept(it, childrenMap.get(codeGetter.apply(it))));
        return roots.get(0);
    }

    /**
     * 合并两个树。如果两颗树有相同code的节点，前者的节点会被丢弃。
     * 也就是说，该方法认为该节点发生了迁移，迁移的位置以后者为准。
     *
     * @param oldTree
     * @param newTree
     * @return
     */
    public NODE mergeTrees(NODE oldTree, NODE newTree) {
        List<NODE> list1 = flatten(oldTree, true);
        List<NODE> list2 = flatten(newTree, true);
        list1.addAll(list2);
        return treeifyEnableDuplicateCode0(list1);
    }

    /**
     * 输出漂亮的格式
     *
     * @param root   根节点
     * @param indent 缩进字符串
     * @return
     */
    public String toPrettyString(NODE root, String indent) {
        StringBuilder sb = new StringBuilder();
        List<String> indents = new ArrayList<>();
        indents.add("");
        dfsWithPreOrder(root, (node, level) -> {
            if (indents.size() <= level) {
                indents.add(indents.get(level - 1) + indent);
            }
            sb.append(indents.get(level)).append(toStringFn.apply(node)).append("\n");
        });
        return sb.toString();
    }

}
