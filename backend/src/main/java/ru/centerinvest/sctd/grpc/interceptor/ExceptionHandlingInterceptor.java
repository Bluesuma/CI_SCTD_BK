package ru.centerinvest.sctd.grpc.interceptor;

import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Перехватчик для обработки исключений в gRPC сервисах
 */
public class ExceptionHandlingInterceptor implements ServerInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(ExceptionHandlingInterceptor.class);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {
        
        ServerCall.Listener<ReqT> listener = next.startCall(call, headers);
        
        return new ExceptionHandlingServerCallListener<>(listener, call);
    }

    private class ExceptionHandlingServerCallListener<ReqT, RespT>
            extends ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT> {
        
        private final ServerCall<ReqT, RespT> serverCall;

        ExceptionHandlingServerCallListener(
                ServerCall.Listener<ReqT> listener,
                ServerCall<ReqT, RespT> serverCall) {
            super(listener);
            this.serverCall = serverCall;
        }

        @Override
        public void onHalfClose() {
            try {
                super.onHalfClose();
            } catch (RuntimeException ex) {
                handleException(ex, serverCall, null);
                throw ex;
            }
        }

        @Override
        public void onReady() {
            try {
                super.onReady();
            } catch (RuntimeException ex) {
                handleException(ex, serverCall, null);
                throw ex;
            }
        }

        private void handleException(
                RuntimeException exception,
                ServerCall<ReqT, RespT> serverCall,
                Metadata trailers) {
            
            trailers = trailers == null ? new Metadata() : trailers;
            
            Status status = Status.INTERNAL
                    .withDescription("Внутренняя ошибка сервера: " + exception.getMessage())
                    .withCause(exception);
            
            // Логирование ошибки
            logger.error("Ошибка в gRPC сервисе: {}", exception.getMessage(), exception);
            
            serverCall.close(status, trailers);
        }
    }
} 