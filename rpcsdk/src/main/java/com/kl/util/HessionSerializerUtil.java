package com.kl.util;

import com.caucho.hessian.io.*;
import com.caucho.hessian.server.HessianSkeleton;

import java.io.*;
import java.lang.reflect.Method;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * Created by kl on 2016/10/27.
 */
public class HessionSerializerUtil {
    private final static SerializerFactory serializerFactory = new SerializerFactory();

    public static SerializerFactory getSerializerFactory() {
        return serializerFactory;
    }

    /**
     * 转hessian输入流
     * @param is
     * @return
     */
    public static AbstractHessianInput getHessian2Input(InputStream is) {
        AbstractHessianInput in = new Hessian2Input(is);
        in.setSerializerFactory(serializerFactory);
        return in;
    }

    /**
     * 转hessian输出流
     * @param os
     * @return
     */
    public static AbstractHessianOutput getHessianOutput(OutputStream os) {
        AbstractHessianOutput out = new Hessian2Output(os);
        out.setSerializerFactory(serializerFactory);
        return out;
    }
    /**
     * 转hessian输出流
     * @param os
     * @return
     */
    public static AbstractHessianOutput getHessianOutput(HessianInputFactory.HeaderType header, OutputStream os) {
        AbstractHessianOutput out;
        HessianFactory hessianfactory = new HessianFactory();
        switch (header) {
            case CALL_1_REPLY_1:
                out = hessianfactory.createHessianOutput(os);
                break;
            case CALL_1_REPLY_2:
            case HESSIAN_2:
                out = hessianfactory.createHessian2Output(os);
                break;
            default:
                throw new IllegalStateException(header + " is an unknown Hessian call");
        }
        return out;
    }

    /**
     * 生产者响应消息体
     * @param response
     * @param method
     * @param compressed
     * @return
     * @throws Throwable
     */
    public static Object clientResponseBody(byte[] response, Method method, Boolean compressed) throws Throwable {
        AbstractHessianInput in;
        InputStream is = new ByteArrayInputStream(response);
        if (compressed) {
            is = new InflaterInputStream(is, new Inflater(true));
        }
        int code = is.read();
        if (code == 'H') {
            int major = is.read();
            int minor = is.read();
            in = HessionSerializerUtil.getHessian2Input(is);
            return in.readReply(method.getReturnType());
        } else if (code == 'r') {
            int major = is.read();
            int minor = is.read();
            in = HessionSerializerUtil.getHessian2Input(is);
            in.startReplyBody();
            Object value = in.readObject(method.getReturnType());
            in.completeReply();
            return value;
        }else {
            throw new HessianProtocolException("'" + (char) code + "' is an unknown code");
        }
    }

    /**
     * 生产者请求消息体
     * @param method
     * @param args
     * @return
     * @throws IOException
     */
    public static byte[] clientRequestBody(Method method, Object[] args, Boolean isCompressed) throws IOException {
        String methodName = method.getName();
        ByteArrayOutputStream payload = new ByteArrayOutputStream(256);
        OutputStream os;
        if (isCompressed) {
            Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, true);
            os = new DeflaterOutputStream(payload, deflater);
        } else {
            os = payload;
        }
        AbstractHessianOutput out = getHessianOutput(os);
        out.call(methodName, args);
        if (os instanceof DeflaterOutputStream) {
            ((DeflaterOutputStream) os).finish();
        }
        out.flush();
        return payload.toByteArray();
    }

    /**
     * 消费端调用成功响应体
     * @param request
     * @param compressed
     * @return
     * @throws Exception
     */
    public static byte[] serverResponseBody(byte[] request, boolean compressed, Object service, Class apiClass) throws Exception {
        InputStream in = new ByteArrayInputStream(request);
        if (compressed) {
            in = new InflaterInputStream(new ByteArrayInputStream(request), new Inflater(true));
        }
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        OutputStream out;
        if (compressed) {
            Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, true);
            out = new DeflaterOutputStream(bout, deflater);
        } else {
            out = bout;
        }
        HessianSkeleton skeleton = new HessianSkeleton(service, apiClass);
        skeleton.invoke(in, out, serializerFactory);//真正执行消费端方法
        if (out instanceof DeflaterOutputStream) {
            ((DeflaterOutputStream) out).finish();
        }
        out.flush();
        out.close();
        return bout.toByteArray();
    }

    /**
     * 消费端调用异常的响应体
     * @param request
     * @param cause
     * @return
     */
    public static byte[] serverFaultBody(byte[] request, Throwable cause) {
        try {
            ByteArrayInputStream is = new ByteArrayInputStream(request);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            AbstractHessianOutput out = getHessianOutput(new HessianInputFactory().readHeader(is), os);
            out.writeFault(cause.getClass().getSimpleName(), cause.getMessage(), cause);
            out.close();
            return os.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
