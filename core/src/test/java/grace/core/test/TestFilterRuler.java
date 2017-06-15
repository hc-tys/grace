package grace.core.test;

import org.junit.Assert;
import org.junit.Test;

import grace.core.Grace;
import grace.core.http.FilterException;

/**
 * Created by hechao on 2017/5/2.
 */

public class TestFilterRuler {

    @Test
    public void testAssertRule() throws FilterException {

        String value = "{\"status\":true,\"data\":[{\"name\":\"hc\"},{\"name\":\"ty\"}],\"1\":{\"name\":\"john\"},\"2\":{\"name\":\"tom\"}}";

        Assert.assertEquals("{\"data\":[{\"name\":\"hc\"},{\"name\":\"ty\"}]}",Grace.filter(value).assertEqual("status",true).include("data").apply());

        Assert.assertEquals("[{\"name\":\"hc\"},{\"name\":\"ty\"}]",Grace.filter(value).assertEqual("status",true).get("data").apply());

        Assert.assertEquals("[{\"value\":\"hc\"},{\"value\":\"ty\"}]",Grace.filter(value).get("data").alias("name","value").apply());

        Assert.assertEquals("[\"hc\",\"ty\"]",Grace.filter(value).get("data").alias("name","value").get("value").apply());

    }

    @Test
    public void testIncludeRule() throws FilterException {

        String value = "{\"status\":true,\"data\":[{\"name\":\"hc\",\"gender\":\"male\"},{\"name\":\"ty\",\"gender\":\"female\"}]}";

        Assert.assertEquals("[{\"name\":\"hc\"},{\"name\":\"ty\"}]",Grace.filter(value).assertEqual("status",true).get("data").include("name").apply());

    }

    @Test
    public void testArrayRule() throws FilterException {

        String value = "{\"1\":{\"name\":\"john\"},\"2\":{\"name\":\"tom\"}}";

        Assert.assertEquals("{\"data\":[{\"name\":\"john\"},{\"name\":\"tom\"}]}",Grace.filter(value).array("[0-9]","data").apply());

    }

    @Test
    public void testMapCollect() throws Exception {

//        String value = "[{\"id\":\"xiaomi\",\"value\":23},{\"id\":\"huawei\",\"value\":10}]";
//        String target = Grace.apply(value).collectMap("id","value").quietApply();
//        assertEquals("{\"xiami\":23,\"huawei\":10}",target);
    }
}
