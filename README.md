# Grace

This project provides a framework for deserialization and serialization.
You can use its convenient flat APIs to serialize POJO or deserialize source to POJO. These sources can be from http request.

## Get it

**maven**

```
<dependency>
  <groupId>com.github.hc-tys</groupId>
  <artifactId>grace-core</artifactId>
  <version>1.0.0</version>
</dependency>
```
or **gradle**:

```
dependencies {
    compile 'com.github.hc-tys:grace-core:1.0.0'
}
```

## How to use

The following six methods are the core of Grace:

* **GraceMapper from(byte[] value)**
* **GraceMapper from(String value)**
* **GraceHttpCallMapper from(Request request)**
* **GraceHttpCallMapper from(URL url)**
* **GraceHttpCallMapper fromUrl(String url)**
* **GraceRuleMapper filter(String value)**

There is also a conveninet method for POJO to String:

* **String toString(Object value)**

Grace also provide a method like [retrofit](https://github.com/square/retrofit) to implement http request, but unlike [retrofit](https://github.com/square/retrofit) depends on [okhttp](https://github.com/square/okhttp),you can use other low-level http implementations.But mostly implementations are referred to [retrofit](https://github.com/square/retrofit):

* **S from(Class service)**

### Http request
Grace provides default implementation for http request ,but you can implement by yourself ,just only implement one of the following interfaces and set it use Grace.

The interface:

```
public interface Executor<F>{

	 //synchronize request
    interface SynExecutor<F> extends Executor<F>{
        Response<F> executeRequest(Request request);
    }
	
	 // asynchonize request
    interface ASynExecutor<F> extends Executor<F>{
        void executeRequest(Request request,Callback<F> callback);
    }
}
```

Set it to Grace:

```
void setDefaultExecutor(Executor executor)
```

### Convert
Grace use [jackson-databind](https://github.com/FasterXML/jackson-databind/edit/master/README.md) as its defaut convertor,you also can custom it by implement the following interface and set it to Grace.

The interface:

```
public interface Converter<F,T>{
    T convert(F value) throws IOException;

    interface Factory {

        <F> Converter<F,?> create(Class<F> clazz,Type type);

        <F,T> Converter<F,T> create(Class<F> clazz,Class<T> raw,Type... typeParams);

    }
}
```
Set it to Grace:

```
void setDefaultConverterFactory(Converter.Factory converterFactory)
```
## Expand

If use default convertor , that is [jackson-databind](https://github.com/FasterXML/jackson-databind/edit/master/README.md) ,you can go to its main page for more infomations about how to implement POJOs.

Here grace provide another convenient way to implement POJOs, adopted interface programming philosophy,but finally convert to [jackson-databind](https://github.com/FasterXML/jackson-databind/edit/master/README.md) POJOs.

for example:

```
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
String data = "[{\"name\":\"xiaoming\",\"age\":20,\"gender\":\"male\"}," +
                "{\"name\":\"zhangsan\",\"age\":27,\"gender\":\"male\"}]";
List<People> peopleList = Grace.from(sData).toList(People.class);
```

### Principle

Use annotations provided by grace-anno, grace can generate concrete classes for these interfaces annotated with @JSONSerializer at runtime or compile time. When grace meet these interfaces ,it use the generated concrete classes to replace them for [jackson-databind](https://github.com/FasterXML/jackson-databind/edit/master/README.md)

for android , currently only support generate code at compile time:

```
annotationProcessor com.github.hc-tys:grace-compiler:1.0.0

```

## Http service

This idea is from [retrofit](https://github.com/square/retrofit) , but there are two main differences

* you can implement yourself HttpURLConnection
* except use reflect at runtime ,support generate code at compile time,also use grace-compiler:

```
annotationProcessor com.github.hc-tys:grace-compiler:1.0.0

```

for example:

```
@Http("www.grace.com")
public interface PeopleProvider {
    @Path("/people")
    Call<List<People>> requestPeoples(@Query("size") int size);
}
Response<List<People>> response = Grace.from(PeopleProvider.class).requestPeoples(2).execute();
```

### Individuation

Except **Executor** interface, Grace provides another two interfaces to customize http request:

```
public interface Filter<F> {

    /**
     * this method can only be called when the response of the request meets that
     * the status  get by {@link Response#status()} is {@link grace.core.http.Response.Status#OK},
     * the code get by {@link Response#code()} is 200 and the body get by {@link Response#body()} is not empty.
     * @param request request
     * @param data http response data
     * @return filter data
     * @throws FilterException filter data failed
     */
    F filter(Request request,F data) throws FilterException;
}

public interface Interceptor{
    Request intercept(Request request);
}

```

It also support compile time use annotation **@Config**

```
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
```

## Proguard

If you are using ProGuard you might need to add the following options:

```
-keepnames public class grace.codegen.**{*;}
```

