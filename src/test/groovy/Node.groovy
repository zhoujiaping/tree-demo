import groovy.transform.ToString

@ToString
class Node {
    Long id
    Long pid
    String name
    List<Node> children

}
