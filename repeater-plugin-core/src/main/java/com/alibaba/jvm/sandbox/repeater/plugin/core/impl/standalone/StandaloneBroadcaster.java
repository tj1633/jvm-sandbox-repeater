package com.alibaba.jvm.sandbox.repeater.plugin.core.impl.standalone;

import com.alibaba.fastjson.JSON;
import com.alibaba.jvm.sandbox.repeater.plugin.core.impl.AbstractBroadcaster;
import com.alibaba.jvm.sandbox.repeater.plugin.core.impl.api.DefaultBroadcaster;
import com.alibaba.jvm.sandbox.repeater.plugin.core.serialize.JSonSerializer;
import com.alibaba.jvm.sandbox.repeater.plugin.core.serialize.SerializeException;
import com.alibaba.jvm.sandbox.repeater.plugin.core.util.PathUtils;
import com.alibaba.jvm.sandbox.repeater.plugin.core.wrapper.RecordWrapper;
import com.alibaba.jvm.sandbox.repeater.plugin.core.wrapper.SerializerWrapper;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.*;
import com.google.gson.Gson;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * {@link StandaloneBroadcaster} 能够脱机工作，不依赖服务端的实现
 * <p>
 *
 * @author zhaoyb1990
 */
public class StandaloneBroadcaster extends AbstractBroadcaster {

    private String recordSuffix = "record";

    private String repeatSuffix = "repeat";
    private Gson gson = new Gson();

    @Override
    protected void broadcastRecord(RecordModel rm) {
        try {
            String body = SerializerWrapper.jsonSerialize(rm);
            broadcast(body, rm.getTraceId(), recordSuffix);
        } catch (SerializeException e) {
            log.error("broadcast record failed", e);
        } catch (Throwable throwable) {
            log.error("[Error-0000]-broadcast record failed", throwable);
        }
    }

    @Override
    protected void broadcastRepeat(RepeatModel rm) {
        try {
            String body = SerializerWrapper.jsonSerialize(rm);
            broadcast(body, rm.getRepeatId(), repeatSuffix);
        } catch (SerializeException e) {
            log.error("broadcast record failed", e);
        } catch (Throwable throwable) {
            log.error("[Error-0000]-broadcast record failed", throwable);
        }
    }

    @Override
    public RepeaterResult<RecordModel> pullRecord(RepeatMeta meta) {
        ClassLoader swap = Thread.currentThread().getContextClassLoader();
        try {
            String record =
                    FileUtils.readFileToString(new File(assembleFileName(meta.getTraceId(), recordSuffix)), "UTF-8");
            Thread.currentThread().setContextClassLoader(DefaultBroadcaster.class.getClassLoader());
            RecordWrapper wrapper = SerializerWrapper.jsonDeserialize(record, RecordWrapper.class);
            if (meta.isMock() && CollectionUtils.isNotEmpty(wrapper.getSubInvocations())) {
                for (Invocation invocation : wrapper.getSubInvocations()) {
                    SerializerWrapper.inTimeDeserialize(invocation);
                }
            }
            SerializerWrapper.inTimeDeserialize(wrapper.getEntranceInvocation());
            final RepeaterResult<RecordModel> rm =
                    RepeaterResult.builder().success(true).message("operate success").data(wrapper.reTransform())
                            .build();
            log.debug("pullRecord success,traceId={},resp={}", meta.getTraceId(), gson.toJson(rm));
            return rm;
        } catch (Throwable e) {
            log.error("pullRecord failed", e);
            return RepeaterResult.builder().success(false).message(e.getMessage()).build();
        } finally {
            Thread.currentThread().setContextClassLoader(swap);
        }
    }

    private void broadcast(String body, String name, String folder) throws IOException {
        log.info("broadcast success,body={},name={},folder={}", body, name, folder);
        FileUtils.writeStringToFile(new File(assembleFileName(name, folder)), body, "UTF-8");
    }


    private String assembleFileName(String name, String folder) {
        return PathUtils.getModulePath() + File.separator + "repeater-data" + File.separator + folder + File.separator +
                name;
    }

    public static void main(String[] args) throws SerializeException {
        Gson gson = new Gson();
        String record =
                "eyJhcHBOYW1lIjoibmlja3ktdGVzdCIsImVudHJhbmNlSW52b2NhdGlvbiI6eyJhc3luYyI6ZmFsc2UsImJvZHkiOiIiLCJjb250ZW50VHlwZSI6bnVsbCwiZW5kIjoxNzQ1ODI2Mzk0ODQ3LCJlbnRyYW5jZSI6dHJ1ZSwiaGVhZGVycyI6eyJDb29raWUiOiJJZGVhLWYwZjdmM2JiPWRlOTFkYTZhLTQ3NjItNDVkMi04YjQyLWZkNjA5NDYzNGY4MSIsIkFjY2VwdCI6InRleHQvaHRtbCxhcHBsaWNhdGlvbi94aHRtbCt4bWwsYXBwbGljYXRpb24veG1sO3E9MC45LGltYWdlL2F2aWYsaW1hZ2Uvd2VicCxpbWFnZS9hcG5nLCovKjtxPTAuOCxhcHBsaWNhdGlvbi9zaWduZWQtZXhjaGFuZ2U7dj1iMztxPTAuNyIsIlVzZXItQWdlbnQiOiJNb3ppbGxhLzUuMCAoTWFjaW50b3NoOyBJbnRlbCBNYWMgT1MgWCAxMF8xNV83KSBBcHBsZVdlYktpdC81MzcuMzYgKEtIVE1MLCBsaWtlIEdlY2tvKSBDaHJvbWUvMTM1LjAuMC4wIFNhZmFyaS81MzcuMzYiLCJDb25uZWN0aW9uIjoia2VlcC1hbGl2ZSIsIlNlYy1GZXRjaC1EZXN0IjoiZG9jdW1lbnQiLCJTZWMtRmV0Y2gtU2l0ZSI6Im5vbmUiLCJIb3N0IjoibG9jYWxob3N0OjgwODAiLCJBY2NlcHQtRW5jb2RpbmciOiJnemlwLCBkZWZsYXRlLCBiciwgenN0ZCIsIlNlYy1GZXRjaC1Nb2RlIjoibmF2aWdhdGUiLCJhZ2VuY3ktbWljcm8tZmVhdHVyZSI6ImFnZW5jeS1taWNyby1mZWF0dXJlIiwic2VjLWNoLXVhIjoiXCJHb29nbGUgQ2hyb21lXCI7dj1cIjEzNVwiLCBcIk5vdC1BLkJyYW5kXCI7dj1cIjhcIiwgXCJDaHJvbWl1bVwiO3Y9XCIxMzVcIiIsInNlYy1jaC11YS1tb2JpbGUiOiI/MCIsIlVwZ3JhZGUtSW5zZWN1cmUtUmVxdWVzdHMiOiIxIiwic2VjLWNoLXVhLXBsYXRmb3JtIjoiXCJtYWNPU1wiIiwiU2VjLUZldGNoLVVzZXIiOiI/MSIsIkFjY2VwdC1MYW5ndWFnZSI6ImVuLHpoLUNOO3E9MC45LHpoO3E9MC44LHpoLVRXO3E9MC43In0sImlkZW50aXR5Ijp7InVyaSI6Imh0dHA6Ly8vaGVsbG8vIn0sImluZGV4IjoxLCJpbnZva2VJZCI6MTAxOCwibWV0aG9kIjoiR0VUIiwicGFyYW1zTWFwIjp7IlJlcGVhdC1UcmFjZUlkIjpbIjAxMDAwMjA2MzA4NzE3NDU3NDg5MzEzOTUxMDAwN2VkIl19LCJwb3J0Ijo4MDgwLCJwcm9jZXNzSWQiOjEwMTgsInJlcXVlc3RTZXJpYWxpemVkIjoiVzNzaWFHVmhaR1Z5Y3lJNmV5SkRiMjlyYVdVaU9pSkpaR1ZoTFdZd1pqZG1NMkppUFdSbE9URmtZVFpoTFRRM05qSXRORFZrTWkwNFlqUXlMV1prTmpBNU5EWXpOR1k0TVNJc0lrRmpZMlZ3ZENJNkluUmxlSFF2YUhSdGJDeGhjSEJzYVdOaGRHbHZiaTk0YUhSdGJDdDRiV3dzWVhCd2JHbGpZWFJwYjI0dmVHMXNPM0U5TUM0NUxHbHRZV2RsTDJGMmFXWXNhVzFoWjJVdmQyVmljQ3hwYldGblpTOWhjRzVuTENvdktqdHhQVEF1T0N4aGNIQnNhV05oZEdsdmJpOXphV2R1WldRdFpYaGphR0Z1WjJVN2RqMWlNenR4UFRBdU55SXNJbFZ6WlhJdFFXZGxiblFpT2lKTmIzcHBiR3hoTHpVdU1DQW9UV0ZqYVc1MGIzTm9PeUJKYm5SbGJDQk5ZV01nVDFNZ1dDQXhNRjh4TlY4M0tTQkJjSEJzWlZkbFlrdHBkQzgxTXpjdU16WWdLRXRJVkUxTUxDQnNhV3RsSUVkbFkydHZLU0JEYUhKdmJXVXZNVE0xTGpBdU1DNHdJRk5oWm1GeWFTODFNemN1TXpZaUxDSkRiMjV1WldOMGFXOXVJam9pYTJWbGNDMWhiR2wyWlNJc0lsTmxZeTFHWlhSamFDMUVaWE4wSWpvaVpHOWpkVzFsYm5RaUxDSlRaV010Um1WMFkyZ3RVMmwwWlNJNkltNXZibVVpTENKSWIzTjBJam9pYkc5allXeG9iM04wT2pnd09EQWlMQ0pCWTJObGNIUXRSVzVqYjJScGJtY2lPaUpuZW1sd0xDQmtaV1pzWVhSbExDQmljaXdnZW5OMFpDSXNJbE5sWXkxR1pYUmphQzFOYjJSbElqb2libUYyYVdkaGRHVWlMQ0poWjJWdVkza3RiV2xqY204dFptVmhkSFZ5WlNJNkltRm5aVzVqZVMxdGFXTnlieTFtWldGMGRYSmxJaXdpYzJWakxXTm9MWFZoSWpvaVhDSkhiMjluYkdVZ1EyaHliMjFsWENJN2RqMWNJakV6TlZ3aUxDQmNJazV2ZEMxQkxrSnlZVzVrWENJN2RqMWNJamhjSWl3Z1hDSkRhSEp2YldsMWJWd2lPM1k5WENJeE16VmNJaUlzSW5ObFl5MWphQzExWVMxdGIySnBiR1VpT2lJL01DSXNJbFZ3WjNKaFpHVXRTVzV6WldOMWNtVXRVbVZ4ZFdWemRITWlPaUl4SWl3aWMyVmpMV05vTFhWaExYQnNZWFJtYjNKdElqb2lYQ0p0WVdOUFUxd2lJaXdpVTJWakxVWmxkR05vTFZWelpYSWlPaUkvTVNJc0lrRmpZMlZ3ZEMxTVlXNW5kV0ZuWlNJNkltVnVMSHBvTFVOT08zRTlNQzQ1TEhwb08zRTlNQzQ0TEhwb0xWUlhPM0U5TUM0M0luMHNJbkJoY21GdGMwMWhjQ0k2ZXlKU1pYQmxZWFF0VkhKaFkyVkpaQ0k2V3lJd01UQXdNREl3TmpNd09EY3hOelExTnpRNE9UTXhNemsxTVRBd01EZGxaQ0pkZlN3aWJXVjBhRzlrSWpvaVIwVlVJaXdpY0c5eWRDSTZPREE0TUN3aWNtVnhkV1Z6ZEZWU1RDSTZJbWgwZEhBNkx5OXNiMk5oYkdodmMzUTZPREE0TUM5b1pXeHNieUlzSW5KbGNYVmxjM1JWVWtraU9pSXZhR1ZzYkc4aUxDSmliMlI1SWpvaUlpd2lZMjl1ZEdWdWRGUjVjR1VpT201MWJHeDlYUT09IiwicmVxdWVzdFVSSSI6Ii9oZWxsbyIsInJlcXVlc3RVUkwiOiJodHRwOi8vbG9jYWxob3N0OjgwODAvaGVsbG8iLCJyZXNwb25zZVNlcmlhbGl6ZWQiOiJJa2hsYkd4dklEWTRJU0k9Iiwic2VyaWFsaXplVG9rZW4iOiJqZGsuaW50ZXJuYWwubG9hZGVyLkNsYXNzTG9hZGVycyRBcHBDbGFzc0xvYWRlciIsInN0YXJ0IjoxNzQ1ODI2Mzk0ODM1LCJ0aHJvd2FibGVTZXJpYWxpemVkIjpudWxsLCJ0cmFjZUlkIjoiMDEwMDAyMDYzMDg3MTc0NTc0ODkzMTM5NTEwMDA3ZWQiLCJ0eXBlIjp7fX0sImVudmlyb25tZW50IjoiZGV2IiwiaG9zdCI6IjEwLjIuNjMuODciLCJzdWJJbnZvY2F0aW9ucyI6W3siZW5kIjoxNzQ1ODI2Mzk0ODQ2LCJlbnRyYW5jZSI6ZmFsc2UsImlkZW50aXR5Ijp7InVyaSI6Im9raHR0cDovL2h0dHA6Ly9sb2NhbGhvc3QvIn0sImluZGV4IjoxLCJpbnZva2VJZCI6MTAxOSwicHJvY2Vzc0lkIjoxMDE5LCJyZXF1ZXN0U2VyaWFsaXplZCI6Ilczc2ljbVZ4ZFdWemRFaGxZV1JsY25NaU9pSWlMQ0p5WlhGMVpYTjBRbTlrZVNJNklpSXNJbkpsY1hWbGMzUk5aWFJvYjJRaU9pSkhSVlFpTENKeVpYRjFaWE4wVUdGeVlXMXpJanA3ZlgxZCIsInJlc3BvbnNlU2VyaWFsaXplZCI6ImV5SnlaWE53YjI1elpWQnliM1J2WTI5c0lqb2lhSFIwY0M4eExqQWlMQ0p5WlhOd2IyNXpaVWhsWVdSbGNuTWlPbnNpWTI5dWRHVnVkQzEwZVhCbElqcGJJblJsZUhRdmNHeGhhVzRpWFN3aVpHRjBaU0k2V3lKTmIyNHNJREk0SUVGd2NpQXlNREkxSURBM09qUTJPak0wSUVkTlZDSmRMQ0p6WlhKMlpYSWlPbHNpUW1GelpVaFVWRkF2TUM0MklGQjVkR2h2Ymk4ekxqa3VOaUpkZlN3aWNtVnpjRzl1YzJWQ2IyUjVJam9pTmpnaUxDSnlaWE53YjI1elpVMWxjM05oWjJVaU9pSlBTeUlzSW5KbGMzQnZibk5sUTI5a1pTSTZNakF3ZlE9PSIsInNlcmlhbGl6ZVRva2VuIjoiamRrLmludGVybmFsLmxvYWRlci5DbGFzc0xvYWRlcnMkQXBwQ2xhc3NMb2FkZXIiLCJzdGFydCI6MTc0NTgyNjM5NDgzNiwidGhyb3dhYmxlU2VyaWFsaXplZCI6bnVsbCwidHJhY2VJZCI6IjAxMDAwMjA2MzA4NzE3NDU3NDg5MzEzOTUxMDAwN2VkIiwidHlwZSI6e319XSwidGltZXN0YW1wIjoxNzQ1ODI2Mzk0ODM1LCJ0cmFjZUlkIjoiMDEwMDAyMDYzMDg3MTc0NTc0ODkzMTM5NTEwMDA3ZWQifQ==";
        final RecordModel recordModel = SerializerWrapper.jsonDeserialize(record, RecordModel.class);
        System.out.println("1###" + gson.toJson(recordModel));
        for (Invocation invocation : recordModel.getSubInvocations()) {
            SerializerWrapper.inTimeDeserialize(invocation);
        }
        System.out.println("2###" + gson.toJson(recordModel));
//        RecordModel rm = new RecordModel();
//        Invocation entranceInvocation = new Invocation();
//        Identity identity = new Identity("abc");
//        identity.setLocation("nicky");
//        entranceInvocation.setIdentity(identity);
//        rm.setEntranceInvocation(entranceInvocation);
//        final String jsonString = JSON.toJSONString(rm, JSonSerializer.features);
//        System.out.println(jsonString);

    }
}
