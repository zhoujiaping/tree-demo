package org.example.tree;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.val;
import lombok.var;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 *
 */
@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TreeHelper<T, N> {
    Class<N> nodeClass;
    Function<N, T> codeGetter;
    BiConsumer<N, T> codeSetter;
    Function<N, T> parentCodeGetter;
    BiConsumer<N, T> parentCodeSetter;
    Function<N, Collection<N>> childrenGetter;
    BiConsumer<N, Collection<N>> childrenSetter;

    @SneakyThrows
    private N newTreeNode() {
        return nodeClass.newInstance();
    }

    private T code(N n) {
        return codeGetter.apply(n);
    }

    private TreeHelper<T, N> code(N node, T code) {
        codeSetter.accept(node, code);
        return this;
    }

    private T parentCode(N n) {
        return parentCodeGetter.apply(n);
    }

    private TreeHelper<T, N> parentCode(N node, T code) {
        parentCodeSetter.accept(node, code);
        return this;
    }

    private Collection<N> children(N n) {
        return childrenGetter.apply(n);
    }

    private TreeHelper<T, N> children(N node, Collection<N> codes) {
        childrenSetter.accept(node, codes);
        return this;
    }

    public N dummyRoot(T dummyRootCode, List<N> nodes) {
        nodes = JSONArray.parseArray(JSON.toJSONString(nodes), nodeClass);
        val dummyRoot = newTreeNode();
        code(dummyRoot, dummyRootCode);
        children(dummyRoot, nodes);
        nodes.forEach(node -> parentCode(node, dummyRootCode));
        return dummyRoot;
    }

    public List<N> flatten(N root) {
        val tree = JSONObject.parseObject(JSON.toJSONString(root), nodeClass);
        val flatten = new ArrayList<N>();
        bfs(tree, node -> flatten.add(node));
        flatten.forEach(it -> children(it, null));
        return flatten;
    }

    public void bfs(N tree, Consumer<N> consumer) {
        //检测循环引用
        val acceptedCodes = new HashSet<T>();
        var queue1 = new LinkedList<N>();
        var queue2 = new LinkedList<N>();
        queue1.add(tree);
        N node;
        Collection<N> children;
        while (!queue1.isEmpty()) {
            node = queue1.poll();
            if (!acceptedCodes.add(code(node))) {
                throw new RuntimeException("multiple code: " + code(node) + ", maybe there is circular reference!");
            }
            children = children(node);
            consumer.accept(node);
            if (children != null) {
                queue2.addAll(children);
            }
            if (queue1.isEmpty()) {
                queue1 = queue2;
                queue2 = new LinkedList<>();
            }
        }
    }

    public void bfsReversed(N tree, Consumer<N> consumer) {
        val nodes = new LinkedList<N>();
        bfs(tree, it -> nodes.add(it));
        while (!nodes.isEmpty()) {
            consumer.accept(nodes.removeLast());
        }
    }

    public void dfsWithPreOrder(N root, Consumer<N> consumer) {
        dfsWithPreOrder0(root, consumer, new HashSet<>());
    }

    private void dfsWithPreOrder0(N root, Consumer<N> consumer, Set<T> visitedCodes) {
        if (!visitedCodes.add(code(root))) {
            throw new RuntimeException("multiple code: " + code(root) + ", maybe there is circular reference!");
        }
        val children = children(root);
        consumer.accept(root);
        if (children != null) {
            for (val child : children) {
                dfsWithPreOrder0(child, consumer, visitedCodes);
            }
        }
    }

    public void dfsWithPostOrder(N root, Consumer<N> consumer) {
        dfsWithPostOrder0(root, consumer, new HashSet<>());
    }

    private void dfsWithPostOrder0(N root, Consumer<N> consumer, Set<T> acceptedCodes) {
        if (!acceptedCodes.add(code(root))) {
            throw new RuntimeException("multiple code: " + code(root) + ", maybe there is circular reference!");
        }
        val children = children(root);
        if (children != null) {
            for (val child : children) {
                dfsWithPostOrder0(child, consumer, acceptedCodes);
            }
        }
        consumer.accept(root);
    }

    public void bfsByLevel(N root, Consumer<List<N>> consumer) {
        val visitedCodes = new HashSet<T>();
        List<N> queue = new LinkedList<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            for (val n : queue) {
                if (!visitedCodes.add(code(n))) {
                    throw new RuntimeException("multiple code: " + code(root) + ", maybe there is circular reference!");
                }
            }
            consumer.accept(queue);
            queue = queue.stream().filter(it -> children(it) != null)
                    .flatMap(it -> children(it).stream())
                    .collect(Collectors.toList());
        }
    }


    public N treeify(List<N> nodes) {
        if (nodes.isEmpty()) {
            return null;
        }
        //copy
        nodes = JSONArray.parseArray(JSON.toJSONString(nodes), nodeClass);
        //code=>node
        val map = nodes.stream().collect(Collectors.toMap(it -> code(it), it -> it));
        //code=>children
        val childrenMap = nodes.stream().filter(it -> parentCode(it) != null)
                .collect(Collectors.groupingBy(it -> parentCode(it)));
        //find roots, which parentCode not in map
        val roots = nodes.stream().filter(it -> parentCode(it) == null || !map.containsKey(code(it)))
                .collect(Collectors.toList());
        if (roots.size() != 1) {
            throw new RuntimeException("error! can't treeify nodes, cause roots.size()==" + roots.size());
        }
        //set children
        nodes.forEach(it -> children(it, childrenMap.get(code(it))));
        return roots.get(0);
    }

    public N treeifyEnableDuplicateCode(Collection<N> nodes) {
        if (nodes.isEmpty()) {
            return null;
        }
        nodes = JSONArray.parseArray(JSON.toJSONString(nodes), nodeClass);
        return treeifyEnableDuplicateCode0(nodes);
    }

    private N treeifyEnableDuplicateCode0(Collection<N> nodes) {
        //code=>node
        val map = new HashMap<T,N>();
        nodes.forEach(it -> map.put(code(it), it));

        //find roots, which parentCode not in map
        val roots = map.values().stream().filter(it ->
                parentCode(it) == null || !map.containsKey(code(it)))
                .collect(Collectors.toList());
        if (roots.size() != 1) {
            throw new RuntimeException("error! can't treeify nodes, cause roots.size()==" + roots.size());
        }
        //code=>children
        val childrenMap = map.values().stream().filter(it -> parentCode(it) != null)
                .collect(Collectors.groupingBy(this::parentCode));
        //set children
        map.values().forEach(it -> children(it, childrenMap.get(code(it))));
        return roots.get(0);
    }

    public N mergeTrees(N oldTree, N newTree) {
        val list1 = flatten(oldTree);
        val list2 = flatten(newTree);
        list1.addAll(list2);
        return treeifyEnableDuplicateCode0(list1);
    }


}
