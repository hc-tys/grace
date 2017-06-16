package grace.compiler;

import java.util.List;

/**
 * Created by hechao on 2017/3/3.
 */

public interface ASTNode<T> {

    /**
     * Adds the stated node as a direct child of this node.
     * @param node node object
     */
    void addChild( ASTNode node);

    /**
     *  Removes the stated node, which must be a direct child of this node
     * @param node node object
     */
    void removeChild(ASTNode node);

    /**
     *
     * Returns all children nodes.
     * @return child nodes
     */

    List<ASTNode> getChildren();

    /**
     * The ASTNode object that encloses this one.
     * @return parent node
     */
    ASTNode getParent();

    /**
     * set parent
     * @param node parent node
     */
    void setParent(ASTNode node);

    /**
     * Return the name of this node
     * @return node name
     */
    String getName();

    /**
     * Return the AST object which this note belongs to
     * @return ast object
     */
    AST getAST();

    /**
     * the type of node
     * @return node kind
     */
    AST.Kind kind();

    /**
     * 节点的值
     * @return node value
     */
    T getValue();

    /**
     * traverse this node
     * @param visitor callback
     */
    void traverse(ASTVisitor visitor);

}
