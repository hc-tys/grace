package hc.grace.service;

import grace.anno.json.JSONSerializer;
import grace.anno.json.Json;

/**
 * Created by hechao on 2017/5/1.
 */

@JSONSerializer
public interface People {

    @Json("name")
    String getName();

    @Json("age")
    int getAge();

    @Json("gender")
    String getGender();

    @Json(path = "address",value = "city")
    String getCity();
}
