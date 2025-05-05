package com.alibaba.jvm.sandbox.repeater.plugin.elasticsearch;

import com.alibaba.jvm.sandbox.api.event.BeforeEvent;
import com.alibaba.jvm.sandbox.api.event.Event;
import com.alibaba.jvm.sandbox.api.event.ReturnEvent;
import com.alibaba.jvm.sandbox.repeater.plugin.core.impl.api.DefaultInvocationProcessor;
import com.alibaba.jvm.sandbox.repeater.plugin.core.util.HttpUtil;
import com.alibaba.jvm.sandbox.repeater.plugin.core.util.LogUtil;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.Identity;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.Invocation;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.InvokeType;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.ProtocolVersion;
import org.apache.http.RequestLine;
import org.apache.http.StatusLine;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicRequestLine;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.nio.entity.ContentBufferEntity;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 *
 * @author zhaoyb1990
 */
class ElasticSearchProcessor extends DefaultInvocationProcessor {
    private static final Logger log = LoggerFactory.getLogger(ElasticSearchProcessor.class);
    private static final ProtocolVersion HTTP1_1 = new ProtocolVersion("HTTP", 1, 1);


    ElasticSearchProcessor(InvokeType type) {
        super(type);
    }

    @Override
    public Identity assembleIdentity(BeforeEvent event) {
        //TODO 异常会怎么样
        Request request = (Request) event.argumentArray[0];
        return new Identity(InvokeType.ELASTICSEARCH.name(), request.getEndpoint(), "", null);

    }

    @Override
    public Object[] assembleRequest(BeforeEvent event) {

        try {
            Request request = (Request) event.argumentArray[0];
            //TODO 要不要判断一下 NByteArrayEntity
            HttpEntity entity = request.getEntity();
            InputStream inputStream = entity.getContent();
            byte[] bytes = new byte[inputStream.available()];
            inputStream.read(bytes);
            Map<String, Object> params = new HashMap<String, Object>();
            //TODO 替换成request的content-type
            params.put("requestBody", new String(bytes, "UTF-8"));
            return new Object[]{params};
        } catch (Exception e) {
            log.error("es-plugin assembleRequest error, event={}",
                    event.javaClassName + "|" + event.javaMethodName, e);
        }
        return new Object[]{};
    }

    @Override
    public Object assembleResponse(Event event) {
        // assembleResponse可能在before事件中被调用，这里只需要在return时间中获取返回值
        if (event.type == Event.Type.RETURN){
            ReturnEvent returnEvent = (ReturnEvent) event;
            // 获取返回值
            Map<String, Object> responseMap = new HashMap<String, Object>();
            Object responseRaw = returnEvent.object;
            if (responseRaw == null){
                return responseMap;
            }
            Response response = (Response) responseRaw;
            try {
                HttpEntity entity = response.getEntity();
//                String encoding = entity.getContentEncoding().getValue();
//                ContentBufferEntity
                ContentBufferEntity contentBufferEntity = (ContentBufferEntity) entity;
                InputStream inputStream = entity.getContent();
                byte[] bytes = new byte[inputStream.available()];
                inputStream.read(bytes);
                //把数据放回去
                contentBufferEntity.setContent(new ByteArrayInputStream(bytes));
                Header[] respHeaders = response.getHeaders();
                log.info("respHeaders:{}", Arrays.toString(respHeaders));
                responseMap.put("responseBody", new String(bytes, "UTF-8"));
                return responseMap;
            } catch (Exception e) {
                LogUtil.error("okhttp-plugin copy response error",  e);
            }
        }

        return null;
    }

    @Override
    public Object assembleMockResponse(BeforeEvent event, Invocation invocation) {
        //TODO 匹配模式看一下
        //2025-05-05 17:25:31 INFO  find target invocation by PARAMETER_MATCH,identity=elasticsearch:///ams-customer-account/_search/,invocation=com.alibaba.jvm.sandbox.repeater.plugin.domain.Invocation@239a4b4
        try {
            Request request = (Request) event.argumentArray[0];
            // 构建返回body体
            Map<String, Object> responseMap = (Map<String, Object>) invocation.getResponse();
            if (MapUtils.isEmpty(responseMap)){
                new Object();
            }

            String responseStr = (String)responseMap.get("responseBody");
            long responseContentLength = responseStr.getBytes("UTF-8").length;
            Constructor<Response> constructor = Response.class.getDeclaredConstructor(RequestLine.class, HttpHost.class, HttpResponse.class);
            constructor.setAccessible(true);
            StatusLine statusLine = new BasicStatusLine(HttpVersion.HTTP_1_1, 200, "OK");
            HttpResponse httpResponse = new BasicHttpResponse(statusLine);
            BasicHttpEntity entity = new BasicHttpEntity();
            entity.setContentLength(responseContentLength);
            entity.setContentEncoding("UTF-8");
            entity.setContentType("application/json");
            entity.setContent(new ByteArrayInputStream(responseStr.getBytes("UTF-8")));
            httpResponse.setEntity(entity);
            RequestLine requestLine = new BasicRequestLine("POST", request.getEndpoint(), HTTP1_1);
            HttpHost host = new HttpHost("localhost", 9200);
            Response response = constructor.newInstance(requestLine, host, httpResponse);
            return response;
        }catch (Exception e){
            LogUtil.error("es-plugin assembleMockResponse error, event={}",
                    event.javaClassName + "|" + event.javaMethodName, e);
        }


        return null;
    }

}
