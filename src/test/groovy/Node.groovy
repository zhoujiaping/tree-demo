import groovy.transform.ToString

@ToString
class Node implements Serializable{
    static final long serialVersionUID = 1L;
    Long id
    Long pid
    String name
    List<Node> children
}
