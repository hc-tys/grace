package grace.compiler;

import javax.lang.model.element.Element;

/**
 * Created by hechao on 2017/3/7.
 */

public interface ASTFactory {

    void init(ASTContext context);

    AST create(ASTContext context, Element root);

    String identity();
}
