package org.example.tree;

import java.util.Collection;

/**
 * TreeHelper支持map和bean作为节点。
 * BeanTreeHelper仅支持bean作为节点。
 * @param <T>
 * @param <N>
 */
public class BeanTreeHelper<T, N> extends TreeHelper<T, N> {
    public BeanTreeHelper(Class<N> nodeClazz, String codeFieldName, String parentCodeFieldName, String childrenFieldName) {
        setNodeCreator(() -> {
            try {
                return nodeClazz.getConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        setCodeGetter(node -> (T) Reflections.getFieldValue(node, codeFieldName));
        setCodeSetter((node, value) -> Reflections.setFieldValue(node, codeFieldName, value));
        setParentCodeGetter(node -> (T) Reflections.getFieldValue(node, parentCodeFieldName));
        setParentCodeSetter((node, value) -> Reflections.setFieldValue(node, parentCodeFieldName, value));
        setChildrenGetter(node -> (Collection<N>) Reflections.getFieldValue(node, childrenFieldName));
        setChildrenSetter((node, value) -> Reflections.setFieldValue(node, childrenFieldName, value));
    }
}
