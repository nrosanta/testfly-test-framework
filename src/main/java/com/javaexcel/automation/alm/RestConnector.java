package com.javaexcel.automation.alm;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.uri.UriComponent;

public class RestConnector
{
    private String host;
    private String port;
    private String domain;
    private String project;
    private Map<String, String> cookies = new HashMap<String, String>();
   //private Map<String, NewCookie> cookies = new HashMap<String, NewCookie>();

    private RestConnector()
    {
    }

    private static class RestConnectorHolder
    {
        static final RestConnector Instance = new RestConnector();
    }

    public static RestConnector instance()
    {
        return RestConnectorHolder.Instance;
    }

    public void init(String host, String port, String domain, String project)
    {
        this.host = host;
        this.port = port;
        this.domain = domain;
        this.project = project;
    }

    public static MultivaluedMap<String, Object> createBasicAuthHeader(String username, String password)
    {
        byte[] credBytes = (username + ":" + password).getBytes();
        String credEncodedString = "Basic " + Base64.encodeBase64String(credBytes);
        System.out.println("Authorization: "+credEncodedString);

        MultivaluedMap<String, Object> authHeader = new MultivaluedHashMap<String, Object>();
        authHeader.add(HttpHeaders.AUTHORIZATION, credEncodedString);

        return authHeader;
    }
    
    public static MultivaluedMap<String, Object> createBasicAuthHeader(String authToken)
    {

        //System.out.println("Authorization: "+authToken);

        MultivaluedMap<String, Object> authHeader = new MultivaluedHashMap<String, Object>();
        authHeader.add(HttpHeaders.AUTHORIZATION, "Basic "+authToken);

        return authHeader;
    }

    public String host()
    {
        return host;
    }

    public String port()
    {
        return port;
    }

    public String domain()
    {
        return domain;
    }

    public String project()
    {
        return project;
    }

    public String buildUrl(String path) throws Exception
    {
        //if(StringUtils.isNotBlank(host) && StringUtils.isNotBlank(port))
        if(StringUtils.isNotBlank(host))
        {
            path = path.startsWith("/") ? path.substring(1) : path;
            //System.out.println("URL: "+String.format("%s%s/%s", host, port, path));
            return String.format("%s%s/%s", host, port, path);
        }

        throw new Exception("Host/Port are invalid. Call init() to initialize them properly.");
    }

    public String buildEntityCollectionUrl(String entityType) throws Exception
    {
        if(StringUtils.isNotBlank(domain) && StringUtils.isNotBlank(project))
        {
            return String.format("/qcbin/rest/domains/%s/projects/%s/%s", domain, project, entityType);
        }

        throw new Exception("Domain/Project are invalid. Call init() to initialize them properly.");
    }

    public String buildEntityUrl(String entityType, String id) throws Exception
    {
        return buildEntityCollectionUrl(entityType) + "/" + id;
    }

    public <T> T get(
            String path,
            Class<T> entityType,
            MultivaluedMap<String, Object> headers,
            Map<String, String> queryParams) throws Exception
    {
        //Log.debug("GET: {}", path);
    	

        return call(HttpMethod.GET, path, headers, queryParams, null, entityType);
    }

    public <T> T post(
            String path,
            Class<T> entityType,
            MultivaluedMap<String, Object> headers,
            Map<String, String> queryParams,
            Object payload) throws Exception
    {
        //Log.debug("POST: {}", path);
        return call(HttpMethod.POST, path, headers, queryParams, payload, entityType);
    }

    public <T> T post(
            String path,
            Class<T> entityType,
            MultivaluedMap<String, Object> headers,
            Map<String, String> queryParams,
            Object payload,
            String contentType) throws Exception
    {
        
        return call(HttpMethod.POST, path, headers, queryParams, payload, contentType, entityType);
    }

    public <T> T put(
            String path,
            Class<T> entityType,
            MultivaluedMap<String, Object> headers,
            Map<String, String> queryParams,
            Object payload) throws Exception
    {
      
        return call(HttpMethod.PUT, path, headers, queryParams, payload, entityType);
    }

    public Response delete(
            String path,
            MultivaluedMap<String, Object> headers,
            Map<String, String> queryParams) throws Exception
    {
     
        return call(HttpMethod.DELETE, path, headers, queryParams, null);
    }

    private static WebTarget createWebTarget(String uri, Map<String, String> queryParams) throws URISyntaxException
    {
        WebTarget webTarget = null;
        URI u = new URI(uri);
        Client client = ClientBuilder.newClient();
        //client.register(new LoggingFilter());
        client.register(LoggingFilter.class);
        //client.register(JacksonFeature.class);
        
        webTarget = client.target(u); 
        
        

        if (MapUtils.isNotEmpty(queryParams))
        {
            for (Entry<String, String> entry : queryParams.entrySet())
            {
                if (StringUtils.isNotBlank(entry.getKey()) && StringUtils.isNotBlank(entry.getValue()))
                {
                    String value = UriComponent.encode(
                            entry.getValue(),
                            UriComponent.Type.QUERY_PARAM_SPACE_ENCODED);

                    webTarget = webTarget.queryParam(entry.getKey(), value);
                }
            }
        }

        return webTarget;
    }

    private static boolean isStatusCodeOK(int statusCode)
    {
        return statusCode >= Status.OK.getStatusCode() &&
               statusCode <= Status.PARTIAL_CONTENT.getStatusCode();
    }

    private <T> T call(
            String methodName,
            String path,
            MultivaluedMap<String, Object> headers,
            Map<String, String> queryParams,
            Object payload,
            Class<T> entityType) throws Exception
    {

        return call(methodName, path, headers, queryParams, payload, MediaType.APPLICATION_XML, entityType);
    }

    private <T> T call(
            String methodName,
            String path,
            MultivaluedMap<String, Object> headers,
            Map<String, String> queryParams,
            Object payload,
            String contentType,
            Class<T> entityType) throws Exception
    {
        Response res = call(methodName, path, headers, queryParams, payload, contentType);
        

        if(!res.hasEntity())
        {
            return null;
        }
        
        
    	return (T) res.readEntity(entityType);
    }

    private Response call(
            String methodName,
            String path,
            MultivaluedMap<String, Object> headers,
            Map<String, String> queryParams,
            Object payload) throws Exception
    {
        return call(methodName, path, headers, queryParams, payload, MediaType.APPLICATION_XML);
    }

    private Response call(
            String methodName,
            String path,
            MultivaluedMap<String, Object> headers,
            Map<String, String> queryParams,
            Object payload,
            String contentType) throws Exception
    {
    	
    	if(path.contains("site-session")){
        	contentType = MediaType.APPLICATION_JSON;
        }
    	
    	WebTarget webTarget = createWebTarget(buildUrl(path), queryParams);
        Builder builder = webTarget.request().headers(headers);
        
        
        if (MapUtils.isNotEmpty(cookies))
        {
            for (String cookie : cookies.keySet())
            {
            	builder.cookie(cookie, cookies.get(cookie));
            }
            
        }
        
        Response res = builder.method(
                methodName, Entity.entity(payload, contentType), Response.class);
        
        if(path.contains("run")){
        //System.out.println(">>>>"+Entity.entity(payload, contentType).getEntity().toString());
        }
        
        int statusCode = res.getStatus();
        res.bufferEntity();
        
        
        if (!isStatusCodeOK(statusCode) && statusCode!=401)
        {
        	System.err.println("\nDebug Log:\n"+res.readEntity(String.class));
        	throw new ResponseException(res, buildUrl(path));
        } else if (statusCode==401){
        	throw new ResponseException(res, buildUrl(path));
        }
        
        
        updateCookies(res);
        
        return res;
    }

   
    private void updateCookies(Response response)
    {
    	String[] newCookies = response.getHeaders().get("Set-Cookie").toString().replace("[", "").replace("]", "").split(",");
        if (newCookies.length>0)
        {
        	 for (String cookie : newCookies)
             {
                 String cookieKey = cookie.split("=")[0];
                 String cookieValue = cookie.split("=")[1];
                 cookies.put(cookieKey, cookieValue);
             }
        }

    }
    


}
