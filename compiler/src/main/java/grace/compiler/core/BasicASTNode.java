package grace.compiler.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import grace.compiler.AST;
import grace.compiler.AST.Kind;
import grace.compiler.ASTNode;
import grace.compiler.ASTVisitor;

/**
 * Created by hechao on 2017/3/3.
 */

public class BasicASTNode<T> implements ASTNode<T> {

    private AST mAst;

    private List<ASTNode> mChildren;

    private T mValue;

    private ASTNode mParent;

    private Kind mKind;

    private String name;

    public BasicASTNode(AST ast, T value, List<ASTNode> children, Kind kind, String name) {
        this.mAst = ast;
        this.mValue = value;
        this.mKind = kind;
        this.name = name;
        this.mChildren = children != null ? new ArrayList(children) : new ArrayList<>();
        for (ASTNode child : this.mChildren) {
            child.setParent(this);
        }
    }

    @Override
    public void addChild(ASTNode node) {
        mChildren.add(node);
    }

    @Override
    public void removeChild(ASTNode node) {
        mChildren.remove(node);
    }

    @Override
    public List<ASTNode> getChildren() {
        return Collections.unmodifiableList(mChildren);
    }

    @Override
    public ASTNode getParent() {
        return mParent;
    }

    @Override
    public void setParent(ASTNode node) {
        mParent = node;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public AST getAST() {
        return mAst;
    }

    @Override
    public Kind kind() {
        return mKind;
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public T getValue(){
        return mValue;
    }

    @Override
    public void traverse(ASTVisitor visitor) {

    }
}
