package org.example.tree;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * TreeHelper支持map和bean作为节点。
 * MapTreeHelper仅支持Map作为节点。
 * @param <T>
 */
public class MapTreeHelper<T> extends TreeHelper<T, Map<String,Object>> {
    public MapTreeHelper(String codeKeyName, String parentCodeKeyName, String childrenKeyName) {
        setNodeCreator(() -> new HashMap<>());
        setCodeGetter(node -> (T) node.get(codeKeyName));
        setCodeSetter((node, value) -> node.put(codeKeyName,value));
        setParentCodeGetter(node -> (T) node.get(parentCodeKeyName));
        setParentCodeSetter((node, value) -> node.put(parentCodeKeyName,value));
        setChildrenGetter(node -> (Collection<Map<String,Object>>) node.get(childrenKeyName));
        setChildrenSetter((node, value) -> node.put(childrenKeyName,value));
    }
}
