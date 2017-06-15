package grace.core;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import grace.core.mapper.Converter;
import grace.core.http.FilterException;
import grace.core.mapper.GraceRuleMapper;
import grace.core.util.GLogger;

/**
 * Created by hechao on 2017/5/3.
 */

class GraceFilterMapperImpl extends GraceMapperImpl implements GraceRuleMapper {

    private List<Rule.IRule> rules;

    private JsonNode sourceNode;

    public GraceFilterMapperImpl(String source, Converter.Factory converterFactory) {
        super(source,converterFactory);
        this.rules = new ArrayList<>();
        try {
            this.sourceNode = new ObjectMapper().readTree(source);
        } catch (IOException e) {
            e.printStackTrace();
            sourceNode = null;
        }
    }

    @Override
    public GraceRuleMapper alias(String originMode, String targetMode) {
        rules.add(new Rule.Alias(originMode,targetMode));
        return this;
    }

    @Override
    public GraceRuleMapper array(String regexMode, String targetMode) {
        rules.add(new Rule.Collect(regexMode,targetMode));
        return this;
    }

    @Override
    public GraceRuleMapper include(String... originMode) {
        rules.add(new Rule.Include(originMode));
        return this;
    }

    @Override
    public GraceRuleMapper exclude(String... originMode) {
        rules.add(new Rule.Exclude(originMode));
        return this;
    }

    @Override
    public GraceRuleMapper get(String originMode) {
        rules.add(new Rule.Get(originMode));
        return this;
    }

    @Override
    public GraceRuleMapper get(int originMode) {
        rules.add(new Rule.Get(originMode));
        return this;
    }

    @Override
    public GraceRuleMapper map(String keyMode, String valueMode) {
        rules.add(new Rule.Mapper(keyMode, valueMode));
        return this;
    }

    @Override
    public GraceRuleMapper collectMap(String keyMode, String valueMode) {
        rules.add(new Rule.MapCollect(keyMode, valueMode));
        return this;
    }

    @Override
    public GraceRuleMapper assertEqual(String assertMode, String value) {
        return assertEqual(assertMode,null,value);
    }

    @Override
    public GraceRuleMapper assertEqual(String assertMode, int value) {
        return assertEqual(assertMode,null,value);
    }

    @Override
    public GraceRuleMapper assertEqual(String assertMode, long value) {
        return assertEqual(assertMode,null,value);
    }

    @Override
    public GraceRuleMapper assertEqual(String assertMode, boolean value) {
        return assertEqual(assertMode,null,value);
    }

    @Override
    public GraceRuleMapper assertEqual(String assertMode, float value) {
        return null;
    }

    @Override
    public GraceRuleMapper assertEqual(String assertMode, String errorMode, String value) {
        rules.add(new Rule.Assert(assertMode,errorMode,value));
        return this;
    }

    @Override
    public GraceRuleMapper assertEqual(String assertMode, String errorMode, int value) {
        assertEqual(assertMode,errorMode,String.valueOf(value));
        return this;
    }

    @Override
    public GraceRuleMapper assertEqual(String assertMode, String errorMode, long value) {
        assertEqual(assertMode,errorMode,String.valueOf(value));
        return this;
    }

    @Override
    public GraceRuleMapper assertEqual(String assertMode, String errorMode, float value) {
        return null;
    }

    @Override
    public GraceRuleMapper assertEqual(String assertMode, String errorMode, boolean value) {
        assertEqual(assertMode,errorMode,String.valueOf(value));
        return this;
    }

    @Override
    public String apply() throws FilterException {

        if(sourceNode == null)
            throw new FilterException("Filter failed for load source",source);

        JsonNode node = sourceNode;
        for (Rule.IRule rule :rules){
            JsonNode temp = node;
            node = rule.apply(temp);
            if(node == null){
                GLogger.info("Result is null when source %s apply %s",temp,rule);
                break;
            }
        }
        return node != null ? node.toString() : null;
    }

    @Override
    public String quietApply() {
        try {
            return apply();
        } catch (FilterException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    Object transformTarget(Object source) {
        return quietApply();
    }

    private static class Rule{

        interface IRule{
            JsonNode apply(JsonNode jsonNode) throws FilterException;
        }

        static class Assert implements IRule{

            private String assertMode;

            private String errorMode;

            private String expectValue;

            public Assert(String assertMode,String errorMode,String expectValue) {
                this.assertMode = assertMode;
                this.errorMode = errorMode;
                this.expectValue = expectValue;
            }

            @Override
            public JsonNode apply(JsonNode jsonNode) throws FilterException {
                if(assertMode == null || assertMode.isEmpty())
                    throw new FilterException("%s failed for mode is empty",this);

                JsonNode real = jsonNode.get(assertMode);

                if(real == null)
                    throw new FilterException("%s failed for no matched assert mode in source : %s",this,jsonNode);

                String value = real.toString();

                if( (value == null && expectValue == null ) || (value != null && value.equals(expectValue)))
                    return jsonNode;


                if(errorMode != null && !errorMode.isEmpty()){
                    JsonNode errorNode = jsonNode.get(errorMode);
                    String message = errorNode != null ? errorNode.toString() : "no matched error mode";
                    throw new FilterException("%s failed with actual value %s ,and message is :%s",this,value,message);
                }
                throw new FilterException("%s failed with actual value %s ",this,value);
            }

            @Override
            public String toString() {
                final StringBuilder sb = new StringBuilder("Assert rule (");
                sb.append("'").append(assertMode).append('\'');
                sb.append("==").append(expectValue).append(")");
                return sb.toString();
            }
        }

        static class Alias implements IRule {

            private String originMode;

            private String targetMode;

            public Alias(String originMode, String targetMode) {
                this.originMode = originMode;
                this.targetMode = targetMode;
            }

            @Override
            public JsonNode apply(JsonNode jsonNode) throws FilterException {
                if(originMode == null || originMode.isEmpty())
                    throw new FilterException("%s failed for origin mode is null",this);

                if(targetMode == null || targetMode.isEmpty())
                    throw new FilterException("%s failed for target mode is null",this);

                switch (jsonNode.getNodeType()){
                    case ARRAY:
                        ArrayNode arrayNode = (ArrayNode) jsonNode;
                        int size = arrayNode.size();
                        for (int i = 0 ;i < size ; i++){
                            JsonNode temp = arrayNode.get(i);
                            if(temp == null) continue;
                            if(temp.getNodeType() != JsonNodeType.OBJECT){
                                throw new FilterException("%s can't apply to source : %s , type : %s",this,jsonNode,jsonNode.getNodeType());
                            }
                            aliasNode((ObjectNode) temp);
                        }
                        break;
                    case OBJECT:
                        aliasNode((ObjectNode) jsonNode);
                        break;
                    default:
                        throw new FilterException("%s can't apply to source : %s , type : %s",this,jsonNode,jsonNode.getNodeType());
                }
                return jsonNode;
            }

            private void aliasNode(ObjectNode objectNode) throws FilterException {
                if(objectNode.has(originMode)) {
                    JsonNode value = objectNode.remove(originMode);
                    objectNode.set(targetMode, value);
                }
            }

            @Override
            public String toString() {
                final StringBuilder sb = new StringBuilder("Alias rule(");
                sb.append("'").append(originMode).append('\'');
                sb.append(" ==> '").append(targetMode).append('\'');
                sb.append(')');
                return sb.toString();
            }
        }

        static class Include implements IRule{

            String[] originModes;

            public Include(String[] originModes) {
                this.originModes = originModes == null ? new String[0] : originModes;
            }

            @Override
            public JsonNode apply(JsonNode jsonNode) throws FilterException {
                switch (jsonNode.getNodeType()){
                    case ARRAY:
                        ArrayNode target = JsonNodeFactory.instance.arrayNode();
                        ArrayNode arrayNode = (ArrayNode) jsonNode;
                        int size = arrayNode.size();
                        for (int i = 0 ;i < size ; i++){
                            JsonNode temp = arrayNode.get(i);
                            if(temp == null) continue;
                            if(temp.getNodeType() != JsonNodeType.OBJECT) {
                                throw new FilterException("%s can't apply to source : %s ,type : %s",this,jsonNode,jsonNode.getNodeType());
                            }
                            target.add(includeNode((ObjectNode) temp));
                        }
                        return target;
                    case OBJECT:
                        return includeNode((ObjectNode) jsonNode);
                    default:
                        throw new FilterException("%s can't apply to source : %s ,type : %s",this,jsonNode,jsonNode.getNodeType());
                }
            }

            JsonNode includeNode(ObjectNode jsonNode){
                ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
                for (String mode : originModes){
                    if(mode != null && jsonNode.has(mode)){
                        objectNode.set(mode,jsonNode.get(mode));
                    }
                }
                return objectNode;
            }

            @Override
            public String toString() {
                final StringBuilder sb = new StringBuilder("Include rule (");
                sb.append(Arrays.toString(originModes));
                sb.append(')');
                return sb.toString();
            }
        }

        static class Exclude implements IRule{

            String[] originModes;

            public Exclude(String[] originModes) {
                this.originModes = originModes == null ? new String[0] : originModes;
            }

            @Override
            public JsonNode apply(JsonNode jsonNode) throws FilterException {
                switch (jsonNode.getNodeType()){
                    case ARRAY:
                        ArrayNode arrayNode = (ArrayNode) jsonNode;
                        int size = arrayNode.size();
                        for (int i = 0 ;i < size ; i++){
                            JsonNode temp = arrayNode.get(i);
                            if(temp == null) continue;
                            if(temp.getNodeType() != JsonNodeType.OBJECT) {
                                throw new FilterException ("%s can't apply to source : %s , type : %s",this,temp,jsonNode.getNodeType());
                            }
                            excludeNode((ObjectNode) temp);
                        }
                        break;
                    case OBJECT:
                        excludeNode((ObjectNode) jsonNode);
                        break;
                    default:
                        throw new FilterException("%s can't apply to source : %s, type : %s",this,jsonNode,jsonNode.getNodeType());
                }
                return jsonNode;
            }

            void excludeNode(ObjectNode node){
                for (String mode : originModes){
                    if(mode != null && node.has(mode)) node.remove(mode);
                }
            }

            @Override
            public String toString() {
                final StringBuilder sb = new StringBuilder("Exclude rule (");
                sb.append(Arrays.toString(originModes));
                sb.append(')');
                return sb.toString();
            }
        }

        static class Get implements IRule{

            private String originMode;

            private int indexMode = -1;

            private boolean isIndex = false;

            public Get(String originMode) {
                this.originMode = originMode;
                this.isIndex = false;
            }

            private Get(int indexMode){
                this.indexMode = indexMode;
                this.isIndex = true;
            }

            @Override
            public JsonNode apply(JsonNode jsonNode) throws FilterException {
                switch (jsonNode.getNodeType()){
                    case ARRAY:
                        ArrayNode arrayNode = (ArrayNode) jsonNode;
                        if(isIndex){
                            if(indexMode >= 0 && indexMode < arrayNode.size()){
                                return jsonNode.get(indexMode);
                            }else{
                                throw new FilterException(" %s apply to source ( %s ) failed for exceed : %d : %d",this,jsonNode,indexMode,arrayNode.size());
                            }
                        }else{
                            ArrayNode targetArray = JsonNodeFactory.instance.arrayNode();
                            for (int i = 0 ;i < arrayNode.size() ; i++){
                                JsonNode temp = jsonNode.get(i);
                                if(temp == null) continue;
                                if(temp.getNodeType() != JsonNodeType.OBJECT) {
                                    throw new FilterException("%s can't apply to source : %s , type : %s",this,temp,temp.getNodeType());
                                }
                                if(originMode != null && temp.has(originMode)) targetArray.add(temp.get(originMode));
                            }
                            return targetArray;
                        }
                    case OBJECT:
                        if(isIndex){
                            throw new FilterException(" %s apply to source ( %s ) failed for invalid node type : %s",this,jsonNode,indexMode,jsonNode.getNodeType());
                        }else{
                            return originMode != null && jsonNode.has(originMode)? jsonNode.get(originMode) : JsonNodeFactory.instance.objectNode();
                        }
                    default:
                        throw new FilterException("%s can't apply to source : %s,type : %s",this,jsonNode,jsonNode.getNodeType());
                }
            }

            @Override
            public String toString() {
                final StringBuilder sb = new StringBuilder("Get rule(");
                if(originMode != null && !originMode.isEmpty())
                    sb.append("'").append(originMode).append('\'');
                if(indexMode >= 0)
                    sb.append(",").append(indexMode);
                sb.append(')');
                return sb.toString();
            }
        }

        static class Collect implements IRule{

            private String regexMode;

            private String targetMode;

            public Collect(String regexMode, String targetMode) {
                this.regexMode = regexMode;
                this.targetMode = targetMode;
            }

            @Override
            public JsonNode apply(JsonNode jsonNode) throws FilterException {

                if(regexMode == null)
                    throw new FilterException("%s failed for regex mode is null",this);

                if(targetMode == null || targetMode.isEmpty())
                    throw new FilterException("%s failed for target mode is null",this);

                switch (jsonNode.getNodeType()){
                    case OBJECT:
                        return collect((ObjectNode) jsonNode);
                    case ARRAY:
                        ArrayNode arrayNode = (ArrayNode) jsonNode;
                        ArrayNode targetArray = JsonNodeFactory.instance.arrayNode();
                        for (int i = 0 ;i < arrayNode.size() ; i++){
                            JsonNode temp = arrayNode.get(i);
                            if(temp == null) continue;
                            if(temp.getNodeType() != JsonNodeType.OBJECT) {
                                throw new FilterException ("%s can't apply to source : %s , type : %s",this,temp,jsonNode.getNodeType());
                            }
                            targetArray.add(collect((ObjectNode) temp));
                        }
                        return targetArray;
                    default:
                        throw new FilterException("%s can't apply to source : %s,type : %s",this,jsonNode,jsonNode.getNodeType());
                }
            }

            private ObjectNode collect(ObjectNode jsonNode){
                ObjectNode targetNode = JsonNodeFactory.instance.objectNode();
                ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
                Iterator<Map.Entry<String,JsonNode>> iterator = jsonNode.fields();
                while (iterator.hasNext()){
                    Map.Entry<String,JsonNode> entry = iterator.next();
                    String fieldName = entry.getKey();
                    if(fieldName == null || fieldName.isEmpty()) continue;
                    JsonNode value = entry.getValue();
                    if(value == null) continue;

                    if(fieldName.matches(regexMode)){
                        arrayNode.add(value);
                    }else {
                        targetNode.set(fieldName,value);
                    }
                }
                targetNode.set(targetMode,arrayNode);
                return targetNode;
            }
        }

        static class Mapper implements IRule {

            private String keyMode;

            private String valueMode;

            public Mapper(String keyMode, String valueMode) {
                this.keyMode = keyMode;
                this.valueMode = valueMode;
            }

            @Override
            public JsonNode apply(JsonNode jsonNode) throws FilterException {
                if(valueMode == null || valueMode.isEmpty())
                    throw new FilterException("%s failed for value mode is null",this);

                if(keyMode == null || keyMode.isEmpty())
                    throw new FilterException("%s failed for key mode is null",this);

                switch (jsonNode.getNodeType()){
                    case ARRAY:
                        ArrayNode arrayNode = (ArrayNode) jsonNode;
                        int size = arrayNode.size();
                        for (int i = 0 ;i < size ; i++){
                            JsonNode temp = arrayNode.get(i);
                            if(temp == null) continue;
                            if(temp.getNodeType() != JsonNodeType.OBJECT){
                                throw new FilterException("%s can't apply to source : %s , type : %s",this,jsonNode,jsonNode.getNodeType());
                            }
                            aliasNode((ObjectNode) temp);
                        }
                        break;
                    case OBJECT:
                        aliasNode((ObjectNode) jsonNode);
                        break;
                    default:
                        throw new FilterException("%s can't apply to source : %s , type : %s",this,jsonNode,jsonNode.getNodeType());
                }
                return jsonNode;
            }

            private void aliasNode(ObjectNode objectNode) throws FilterException {

                String key = null;
                if(objectNode.has(keyMode)) {
                    JsonNode value = objectNode.remove(keyMode);
                    JsonNodeType nodeType = value.getNodeType();
                    if( nodeType != JsonNodeType.NUMBER || nodeType != JsonNodeType.STRING){
                        throw new FilterException("%s apply to source (%s) failed for invalid value type (%s) as key",this,objectNode,nodeType);
                    }
                    key = value.asText();
                }
                JsonNode value = null;
                if(objectNode.has(valueMode)) {
                    value = objectNode.remove(valueMode);
                }
                if(key != null && value != null){
                    objectNode.set(key,value);
                }
            }

            @Override
            public String toString() {
                final StringBuilder sb = new StringBuilder("Map rule(");
                sb.append("'").append(keyMode).append('\'');
                sb.append(" , '").append(valueMode).append('\'');
                sb.append(')');
                return sb.toString();
            }
        }

        static class MapCollect implements IRule {

            private String keyMode;

            private String valueMode;

            public MapCollect(String keyMode, String valueMode) {
                this.keyMode = keyMode;
                this.valueMode = valueMode;
            }

            @Override
            public JsonNode apply(JsonNode jsonNode) throws FilterException {
                if(valueMode == null || valueMode.isEmpty())
                    throw new FilterException("%s failed for value mode is null",this);

                if(keyMode == null || keyMode.isEmpty())
                    throw new FilterException("%s failed for key mode is null",this);

                switch (jsonNode.getNodeType()){
                    case ARRAY:
                        ArrayNode arrayNode = (ArrayNode) jsonNode;
                        ObjectNode targetObject = JsonNodeFactory.instance.objectNode();
                        int size = arrayNode.size();
                        for (int i = 0 ;i < size ; i++){
                            JsonNode temp = arrayNode.get(i);
                            if(temp == null) continue;
                            if(temp.getNodeType() != JsonNodeType.OBJECT){
                                throw new FilterException("%s can't apply to source : %s , type : %s",this,jsonNode,jsonNode.getNodeType());
                            }
                            Pair<String,JsonNode> pair = doMapCollect((ObjectNode) temp);
                            if(pair != null) targetObject.set(pair.key,pair.value);
                        }
                        return targetObject;
                    case OBJECT:
                        Pair<String,JsonNode> pair = doMapCollect((ObjectNode) jsonNode);
                        if(pair == null) return null;
                        ObjectNode target = JsonNodeFactory.instance.objectNode();
                        target.set(pair.key,pair.value);
                        return target;
                    default:
                        throw new FilterException("%s can't apply to source : %s , type : %s",this,jsonNode,jsonNode.getNodeType());
                }
            }

            private Pair<String,JsonNode> doMapCollect(ObjectNode objectNode) throws FilterException {

                String key = null;
                if(objectNode.has(keyMode)) {
                    JsonNode value = objectNode.remove(keyMode); // remove key mode
                    JsonNodeType nodeType = value.getNodeType();
                    if( nodeType != JsonNodeType.NUMBER && nodeType != JsonNodeType.STRING){
                        throw new FilterException("%s apply to source (%s) failed for invalid value type (%s) as key",this,objectNode,nodeType);
                    }
                    key = value.asText();
                }
                JsonNode value = null;
                if(objectNode.has(valueMode)) {
                    value = objectNode.remove(valueMode);
                }
                if(key != null && value != null){
                    return new Pair<>(key,value);
                }else {
                    return null;
                }
            }

            @Override
            public String toString() {
                final StringBuilder sb = new StringBuilder("Map rule(");
                sb.append("'").append(keyMode).append('\'');
                sb.append(" , '").append(valueMode).append('\'');
                sb.append(')');
                return sb.toString();
            }
        }
    }

    private static class Pair<K,V>{
        K key;
        V value;

        public Pair(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }
}
