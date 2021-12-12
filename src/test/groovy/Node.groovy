import groovy.transform.ToString

/**
 * 实现Serializable不是必须的。
 * 这里实现它仅仅是因为测试的时候需要深度拷贝一份数据。
 *
 * 如果希望节点的子节点是有序的，则需要定义children集合的类型为TreeSet。
 * 然后实现Comparable接口。然后设置TreeHelper/BeanTreeHelper/MapTreeHelper的childrenCollectionFactory为TreeSet::new
 *
 * 如果不在乎子节点的顺序，可以使用List
 */
@ToString
class Node implements Serializable,Comparable<Node>{
    static final long serialVersionUID = 1L
    Long id
    Long pid
    String name
    TreeSet<Node> children// = new TreeSet<>()

    @Override
    int compareTo(Node o) {
        id <=> o.id
    }
}
