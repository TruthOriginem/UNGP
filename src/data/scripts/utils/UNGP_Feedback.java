package data.scripts.utils;

import com.fs.starfarer.api.Global;
import data.scripts.campaign.specialist.rules.UNGP_RulesManager;
import org.apache.log4j.Logger;

import javax.net.ssl.*;
import java.net.Socket;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

public class UNGP_Feedback {
    private static final Logger LOGGER = Global.getLogger(UNGP_Feedback.class);
    /**
     * Has request times limitation
     */
    private static final String API_URL = "https://originem.jnxyp.net/api/ungp/sc?rules=";
    private static final String FEEDBACK_SENT_KEY = "UNGP_feedbackSent";

    public static boolean getFeedbackSent() {
        return Global.getSector().getPersistentData().containsKey(FEEDBACK_SENT_KEY);
    }

    public static void resetFeedbackSent() {
        Global.getSector().getPersistentData().remove(FEEDBACK_SENT_KEY);
    }

    public static void setFeedBackSent() {
        Global.getSector().getPersistentData().put(FEEDBACK_SENT_KEY, true);
    }

    /**
     * Send how players pick rules to certain server for analysis.
     * The server only runs one time per 5 seconds.
     * Player SHOULD be able to cancel this.
     *
     * @param rules
     */
    public static void sendPlayerRulesToServer(List<UNGP_RulesManager.URule> rules) {
        if (rules.isEmpty()) return;
        StringBuilder sb = new StringBuilder(API_URL);
        for (UNGP_RulesManager.URule rule : rules) {
            sb.append(rule.getId());
            sb.append("|");
        }
        sb.deleteCharAt(sb.length() - 1);
        final String url = sb.toString();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL connURL = new URL(url);
                    HttpsURLConnection conn = (HttpsURLConnection) connURL.openConnection();
                    SSLContext ctx = MyX509TrustManagerUtils();
                    conn.setSSLSocketFactory(ctx.getSocketFactory());
                    conn.setHostnameVerifier(new HostnameVerifier() {
                        @Override
                        public boolean verify(String s, SSLSession sslSession) {
                            return true;
                        }
                    });
                    conn.getInputStream().close();
                    LOGGER.info("Successfully send " + url);
                } catch (Exception e) {
                    LOGGER.info("Error connecting to " + API_URL);
                    LOGGER.info(e);
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private static SSLContext MyX509TrustManagerUtils() {
        TrustManager[] tm = {new MyX509TrustManager()};
        SSLContext ctx = null;
        try {
            ctx = SSLContext.getInstance("TLS");
            ctx.init(null, tm, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ctx;
    }

    private static class MyX509TrustManager extends X509ExtendedTrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s, Socket socket) throws CertificateException {

        }

        @Override
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s, Socket socket) throws CertificateException {

        }

        @Override
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s, SSLEngine sslEngine) throws CertificateException {

        }

        @Override
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s, SSLEngine sslEngine) throws CertificateException {

        }

        @Override
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

        }

        @Override
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
}
