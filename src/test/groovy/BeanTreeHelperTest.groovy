import org.example.tree.BeanTreeHelper
import org.junit.jupiter.api.Test

class BeanTreeHelperTest {
    /**
     * src
     * --main
     * ----java(main)
     * ----resource(main)
     * --test
     * ----java(test)
     * ----resource(test)
     */
    @Test
    void testBfs(){
        def helper = new BeanTreeHelper<Integer,Node>(Node,"id","pid","children")
        helper.toStringFn = {
            "${it.id},${it.name}".toString()
        }
        def nodes = [
                new Node(id:1,pid:-1,name: 'src'),
                new Node(id:2,pid:1,name: 'main'),
                new Node(id:3,pid:2,name: 'java(main)'),
                new Node(id:4,pid:2,name: 'resource(main)'),
                new Node(id:5,pid:1,name: 'test'),
                new Node(id:6,pid:5,name: 'java(test)'),
                new Node(id:7,pid:5,name: 'resource(test)')
        ]
        (0..<10).each{
            println "*"*20
            def ns = ObjectUtils.deepClone(nodes)
            //洗牌，打乱顺序
            ns.shuffle()
            //def root = helper.treeify(ns)
            def root = helper.fastTreeify(ns)
            helper.bfs(root){
                node,level->
                println "${node.id},${node.name}"
            }
            helper.dfsWithPreOrder(root){
                node,level->
                println "${node.id},${node.name}"
            }
            println helper.toPrettyString(root,"--")
        }
    }
}
