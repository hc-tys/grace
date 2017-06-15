package hc.grace.service;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import grace.anno.http.Config;
import grace.anno.http.Http;
import grace.anno.http.Path;
import grace.anno.http.Query;
import grace.core.http.Call;
import grace.core.http.Executor;
import grace.core.http.Filter;
import grace.core.http.FilterException;
import grace.core.http.Request;
import grace.core.http.Response;

/**
 * Created by hechao on 2017/5/1.
 */
@Config(executor = PeopleProvider.PeopleExecutor.class,filter = PeopleProvider.PeopleFilter.class)
@Http("www.grace.com")
public interface PeopleProvider {

    @Path("/people")
    Call<List<People>> requestPeoples(@Query("size") int size);

    class PeopleFilter implements Filter<String> {
        @Override
        public String filter(Request request, String s) throws FilterException {
            try {
                JSONObject jsonObject = new JSONObject(s);
                int status = jsonObject.optInt("status");
                if( status == 0){
                    return jsonObject.optString("data");
                }else{
                    throw new FilterException(status,jsonObject.optString("message"));
                }
            } catch (JSONException e) {
                e.printStackTrace();
                throw new FilterException(-1,"invalid data format");
            }
        }
    }

    class PeopleExecutor implements Executor.SynExecutor<String> {
        @Override
        public Response<String> executeRequest(Request request) {
            StringBuilder dataBuilder = new StringBuilder();
            dataBuilder.append("{\"status\":0,\"data\":");
            String peoples = "[{\"name\":\"xiaoming\",\"age\":20,\"gender\":\"male\",\"address\":{\"city\":\"Beijing\",\"zipCode\":210049}}," +
                    "{\"name\":\"zhangsan\",\"age\":27,\"gender\":\"male\",\"address\":{\"city\":\"Jining\",\"zipCode\":272100}}]";
            dataBuilder.append(peoples).append("}");
            return new Response.Builder().status(Response.Status.OK).code(200).build(dataBuilder.toString());
        }
    }
}
