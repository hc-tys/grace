package hc.grace;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.List;

import grace.core.Grace;
import grace.core.http.Response;
import hc.grace.service.People;
import hc.grace.service.PeopleProvider;

import static org.junit.Assert.assertEquals;

/**
 * Created by hechao on 2017/5/1.
 */

@RunWith(JUnit4.class)
public class TestCallMapper {

    @Test
    public void testCall(){
        Response<List<People>> response = Grace.from(PeopleProvider.class).requestPeoples(2).execute();

        assertEquals(response.status(), Response.Status.OK);
        assertEquals(response.code(), 200);

        List<People> peopleList = response.body();
        assertEquals(peopleList.size(),2);
        assertEquals(peopleList.get(0).getAge(),20);
        assertEquals(peopleList.get(0).getName(),"xiaoming");
        assertEquals(peopleList.get(0).getGender(),"male");
        assertEquals(peopleList.get(0).getCity(),"Beijing");
        assertEquals(peopleList.get(1).getAge(),27);
        assertEquals(peopleList.get(1).getName(),"zhangsan");
        assertEquals(peopleList.get(1).getGender(),"male");
        assertEquals(peopleList.get(1).getCity(),"Jining");
    }

}
