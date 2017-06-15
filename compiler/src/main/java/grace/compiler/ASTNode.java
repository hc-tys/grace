package grace.compiler;

import java.util.List;

/**
 * Created by hechao on 2017/3/3.
 */

public interface ASTNode<T> {

    /**
     * Adds the stated node as a direct child of this node.
     * @param node
     */
    void addChild( ASTNode node);

    /**
     *  Removes the stated node, which must be a direct child of this node
     * @param node
     */
    void removeChild(ASTNode node);

    /**
     *
     * Returns all children nodes.
     */

    List<ASTNode> getChildren();

    /**
     * The ASTNode object that encloses this one.
     * @return
     */
    ASTNode getParent();

    /**
     * set parent
     * @param node
     */
    void setParent(ASTNode node);

    /**
     * Return the name of this node
     *
     */
    String getName();

    /**
     * Return the AST object which this note belongs to
     * @return
     */
    AST getAST();

    /**
     * the type of node
     * @return
     */
    AST.Kind kind();

    /**
     * 节点的值
     * @return
     */
    T getValue();

    /**
     *
     * @param visitor
     */
    void traverse(ASTVisitor visitor);

}
