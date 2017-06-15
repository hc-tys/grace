package grace.core.http;

/**
 * Created by hechao on 2017/4/2.
 */

public abstract class NameValuePair {

    public abstract String getName();
    public abstract String getValue();

    public static NameValuePair newPair(String name, String value){
        return new BasicNameValuePair(name, value);
    }

    private static class BasicNameValuePair extends NameValuePair {

        private String name;

        private String value;

        public BasicNameValuePair(String name, String value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getValue() {
            return value;
        }
    }
}