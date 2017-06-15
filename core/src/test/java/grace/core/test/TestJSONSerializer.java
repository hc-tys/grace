package grace.core.test;

import org.junit.Assert;
import org.junit.Test;

import grace.anno.http.Config;
import grace.anno.json.JSONSerializer;
import grace.anno.json.Json;
import grace.core.Grace;

/**
 * Created by hechao on 2017/5/16.
 */

public class TestJSONSerializer {


    @Test
    public void testJSONSerializer(){
        String data = "{\"age\":10,\"name\":\"Li Ming\"}";
        People people = Grace.from(data).to(People.class);
        Assert.assertEquals(people.age(),10);
        Assert.assertEquals(people.name(),"Li Ming");
    }

    @JSONSerializer
    public interface People{
        @Json
        String name();

        @Json
        int age();
    }

    @Test
    public void testAnnotation(){
        Assert.assertTrue(Config.class.isInterface());
    }
}
