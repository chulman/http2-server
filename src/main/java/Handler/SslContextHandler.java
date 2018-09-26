package Handler;

import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.ssl.*;
import io.netty.handler.ssl.util.SelfSignedCertificate;

import javax.net.ssl.KeyManagerFactory;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.Security;


public class SslContextHandler {

    private SslContext sslCtx;
    private SocketChannel sc;

    private String certPath;
    private String certPassword;

    public SslContextHandler(String certPath, String certPassword, SocketChannel sc){
        this.certPath = certPath;
        this.certPassword = certPassword;
        this.sc = sc;

        createSslContext();
    }
    public SslHandler getHandler() {
        return sslCtx.newHandler(sc.alloc());
    }


    private void createSslContext() {

        try {
            //SelfSignedCertificate
//            SelfSignedCertificate ssc = new SelfSignedCertificate();

            SslProvider provider = OpenSsl.isAlpnSupported() ? SslProvider.OPENSSL : SslProvider.JDK;

            String algorithm = Security.getProperty("ssl.KeyManagerFactory.algorithm");
            if (algorithm == null) {
                algorithm = "SunX509";
            }
            KeyStore ks = KeyStore.getInstance("JKS");

            ks.load(new FileInputStream(certPath),certPassword.toCharArray());
            final KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
            kmf.init(ks, certPassword.toCharArray());

            sslCtx = SslContextBuilder.forServer(kmf)
                    .sslProvider(provider)
                    .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
                    .applicationProtocolConfig(new ApplicationProtocolConfig(
                            ApplicationProtocolConfig.Protocol.ALPN, ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                            ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                            ApplicationProtocolNames.HTTP_2,
                            ApplicationProtocolNames.HTTP_1_1))
                    .build();

        } catch (Exception e) {
            sslCtx = null;
            e.printStackTrace();
        }
    }
}
