package grace.compiler.element;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.lang.model.type.DeclaredType;

import grace.compiler.Constants;

/**
 * Created by hechao on 2017/4/6.
 */

class GenCode {

    private static final String JSON_SUFFIX = "_Json";
    private static final String HTTP_SUFFIX = "_Http";

    Map<String,GenType> httpMap = new HashMap<>();

    Map<String,GenType> jsonMap = new HashMap<>();

    GenType getGenHttp(DeclaredType declaredType){
        return httpMap.get(declaredType.toString());
    }

    GenType getGenJson(DeclaredType declaredType){
        return jsonMap.get(declaredType.toString());
    }

    boolean isJsonType(DeclaredType declaredType){
        return jsonMap.get(declaredType.toString()) != null;
    }

    boolean hasGenType(DeclaredType declaredType){
        return httpMap.get(declaredType.toString()) != null || jsonMap.get(declaredType.toString()) != null;
    }

    public GenType addJsonType(DeclaredType declaredType){
        GenType genType =  new GenCode.GenType(Constants.GRACE_CODEGEN_JSON_PACKAGE,createJsonSimpleName(declaredType.asElement().getSimpleName().toString()),declaredType);
        jsonMap.put(declaredType.toString(),genType);
        return genType;
    }

    public GenType addHttpType(DeclaredType declaredType){
        GenType genType =  new GenCode.GenType(Constants.GRACE_CODEGEN_REQUEST_PACKAGE,createHttpSimpleName(declaredType.asElement().getSimpleName().toString()),declaredType);
        httpMap.put(declaredType.toString(),genType);
        return genType;
    }

    private String createJsonSimpleName(String simpleName){
        String target = new StringBuilder().append("_").append(simpleName).append(JSON_SUFFIX).toString();
        Set<String> keys = jsonMap.keySet();
        int i = 0;
        while (keys.contains(target)){
            target = new StringBuilder().append("_").append(simpleName).append(i).append(JSON_SUFFIX).toString();
            i++;
        }
        return target;
    }

    private String createHttpSimpleName(String simpleName){
        String target = new StringBuilder().append("_").append(simpleName).append(HTTP_SUFFIX).toString();
        Set<String> keys = httpMap.keySet();
        int i = 0;
        while (keys.contains(target)){
            target = new StringBuilder().append("_").append(simpleName).append(i).append(HTTP_SUFFIX).toString();
            i++;
        }
        return target;
    }

    static class GenType{
        String packageName;
        String simpleName;
        DeclaredType type;

        private GenType(String packageName, String simpleName,DeclaredType type) {
            this.packageName = packageName;
            this.simpleName = simpleName;
            this.type = type;
        }
    }
}
