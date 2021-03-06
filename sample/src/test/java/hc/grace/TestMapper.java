package hc.grace;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.List;

import grace.core.Grace;
import hc.grace.service.People;

import static org.junit.Assert.assertEquals;

/**
 * Created by hechao on 2017/5/1.
 */
@RunWith(JUnit4.class)
public class TestMapper {

    private static String sData = "";

    @BeforeClass
    public static void initData(){
        sData = "[{\"name\":\"xiaoming\",\"age\":20,\"gender\":\"male\"}," +
                "{\"name\":\"zhangsan\",\"age\":27,\"gender\":\"male\"}]";
    }

    @Test
    public void testListMapper(){
        List<People> peopleList = Grace.from(sData).toList(People.class);

        assertEquals(peopleList.size(),2);
        assertEquals(peopleList.get(0).getAge(),20);
        assertEquals(peopleList.get(0).getName(),"xiaoming");
        assertEquals(peopleList.get(0).getGender(),"male");
        assertEquals(peopleList.get(1).getAge(),27);
        assertEquals(peopleList.get(1).getName(),"zhangsan");
        assertEquals(peopleList.get(1).getGender(),"male");

    }


}
