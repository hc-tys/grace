package grace.compiler.element;


import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeVariableName;
import com.squareup.javapoet.WildcardTypeName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.SimpleTypeVisitor7;

import grace.compiler.ASTContext;
import grace.compiler.ASTPrinter;


/**
 * Created by hechao on 2017/4/7.
 */

public class TypeMapper {

    public static boolean hasJSONTypeKey(ASTContext context,TypeMirror mirror){
        for (String type : context.getAttribute(GenCode.class).jsonMap.keySet()){
            if(mirror.toString().contains(type)) {
                return true;
            }
        }
        return false;
    }

    public static TypeName getRawType(ASTContext context,DeclaredType declaredType){
        TypeName typeName = getTypeName(context,declaredType);
        if(typeName instanceof ParameterizedTypeName){
            return ((ParameterizedTypeName)typeName).rawType;
        }
        return typeName;
    }

    public static TypeName getTypeName(ASTContext context,TypeMirror mirror) {
        for (String type : context.getAttribute(GenCode.class).jsonMap.keySet()){
            if(mirror.toString().contains(type)) {
                context.getAttribute(ASTPrinter.class).printInfo("map type start for %s",mirror);
                TypeName mapperType = _getTypeName(context,mirror);
                context.getAttribute(ASTPrinter.class).printInfo("map type end %s",mapperType);
                return mapperType;
            }
        }
        return TypeName.get(mirror);
    }

    private static TypeName _getTypeName(ASTContext context,TypeMirror mirror) {
        context.setAttribute(new TypeVariableMap());
        TypeName typeName = get(mirror,context);
        context.removeAttribute(TypeVariableMap.class);
        return typeName;
    }

    private static TypeName get(TypeMirror mirror,ASTContext context) {
        return mirror.accept(new SimpleTypeVisitor7<TypeName, ASTContext>() {
            @Override public TypeName visitPrimitive(PrimitiveType t, ASTContext context) {
                context.getAttribute(ASTPrinter.class).printInfo("visitor primitive type :%s",t);

                switch (t.getKind()) {
                    case BOOLEAN:
                        return TypeName.BOOLEAN;
                    case BYTE:
                        return TypeName.BYTE;
                    case SHORT:
                        return TypeName.SHORT;
                    case INT:
                        return TypeName.INT;
                    case LONG:
                        return TypeName.LONG;
                    case CHAR:
                        return TypeName.CHAR;
                    case FLOAT:
                        return TypeName.FLOAT;
                    case DOUBLE:
                        return TypeName.DOUBLE;
                    default:
                        throw new AssertionError();
                }
            }

            @Override public TypeName visitDeclared(DeclaredType t,final ASTContext context) {

                context.getAttribute(ASTPrinter.class).printInfo("visitor declare type :%s",t);
                GenCode.GenType jsonType = context.getAttribute(GenCode.class).getGenJson(t);
                ClassName rawType = jsonType != null ? ClassName.get(jsonType.packageName,jsonType.simpleName) : ClassName.get((TypeElement) t.asElement());
                TypeMirror enclosingType = t.getEnclosingType();

                TypeName enclosing =
                        (enclosingType.getKind() != TypeKind.NONE)
                                && !t.asElement().getModifiers().contains(Modifier.STATIC)
                                ? enclosingType.accept(this, null)
                                : null;
                if (t.getTypeArguments().isEmpty() && !(enclosing instanceof ParameterizedTypeName)) {
                    return rawType;
                }

                List<TypeName> typeArgumentNames = new ArrayList<>();
                for (TypeMirror typeMirror : t.getTypeArguments()){
                    typeArgumentNames.add(get(typeMirror,context));
                }
                return enclosing instanceof ParameterizedTypeName
                        ? ((ParameterizedTypeName) enclosing).nestedClass(rawType.simpleName(), typeArgumentNames)
                        : ParameterizedTypeName.get(rawType, typeArgumentNames.toArray(new TypeName[typeArgumentNames.size()]));
            }

            @Override public TypeName visitError(ErrorType t, ASTContext context) {
                context.getAttribute(ASTPrinter.class).printInfo("visitor error type :%s",t);
                return visitDeclared(t, context);
            }

            @Override public ArrayTypeName visitArray(ArrayType t,ASTContext context) {
                context.getAttribute(ASTPrinter.class).printInfo("visitor array :%s",t);
                return ArrayTypeName.of(get(t.getComponentType(), context));
            }

            @Override public TypeName visitTypeVariable(TypeVariable t,final ASTContext context) {
                context.getAttribute(ASTPrinter.class).printInfo("visitor type variable:%s",t);
                TypeParameterElement element = (TypeParameterElement) t.asElement();
                final TypeVariableMap typeVariables = context.getAttribute(TypeVariableMap.class);
                TypeVariableName typeVariableName = typeVariables.get(element);
                if (typeVariableName == null) {
                    // Since the bounds field is public, we need to make it an unmodifiableList. But we control
                    // the List that that wraps, which means we can change it before returning.
                    List<TypeName> bounds = new ArrayList<>();
                    List<TypeName> visibleBounds = Collections.unmodifiableList(bounds);
                    typeVariableName = TypeVariableName.get(element.getSimpleName().toString(), visibleBounds.toArray(new TypeName[visibleBounds.size()]));
                    typeVariables.put(element, typeVariableName);
                    List<TypeName> typeNames = new ArrayList<>();
                    for (TypeMirror typeMirror : element.getBounds()){
                        typeNames.add(get(typeMirror,context));
                    }
                    bounds.remove(TypeName.OBJECT);
                }
                return typeVariableName;
            }

            @Override public TypeName visitWildcard(WildcardType t,ASTContext context) {
                context.getAttribute(ASTPrinter.class).printInfo("visit wild card : %s",t);
                TypeMirror extendsBound = t.getExtendsBound();
                if (extendsBound == null) {
                    TypeMirror superBound = t.getSuperBound();
                    if (superBound == null) {
                        return WildcardTypeName.subtypeOf(Object.class);
                    }else{
                        return WildcardTypeName.supertypeOf(get(superBound, context));
                    }
                } else {
                    if(extendsBound.getKind() == TypeKind.DECLARED){
                        GenCode.GenType jsonType = context.getAttribute(GenCode.class).getGenJson((DeclaredType) extendsBound);
                        if(jsonType != null){
                            return ClassName.get(jsonType.packageName,jsonType.simpleName);
                        }
                    }
                    return WildcardTypeName.subtypeOf(get(extendsBound, context));
                }
            }

            @Override public TypeName visitNoType(NoType t,ASTContext context) {
                context.getAttribute(ASTPrinter.class).printInfo("visitor no type :%s",t);
                if (t.getKind() == TypeKind.VOID) return TypeName.VOID;
                return super.visitUnknown(t, context);
            }

            @Override protected TypeName defaultAction(TypeMirror e, ASTContext context) {
                throw new IllegalArgumentException("Unexpected type mirror: " + e);
            }
        }, context);
    }

    private static class TypeVariableMap extends HashMap<TypeParameterElement, TypeVariableName>{
    }

}
