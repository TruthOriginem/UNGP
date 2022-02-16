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
    private static final String FEEDBACK_SENT_KEY = "UNGP_feedbackSent";
    private static final String FEEDBACK_LIST_KEY = "UNGP_feedbackList";

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
     * only called after inherit and repick.
     *
     * @param rules
     */
    public static void setFeedBackList(List<UNGP_RulesManager.URule> rules) {
        StringBuilder sb = new StringBuilder();
        for (UNGP_RulesManager.URule rule : rules) {
            sb.append(rule.getId());
            sb.append("|");
        }
        sb.deleteCharAt(sb.length() - 1);
        Global.getSector().getPersistentData().put(FEEDBACK_LIST_KEY, sb.toString());
    }

    /**
     * Send how players pick rules to certain server for analysis.
     * The server only runs one time per 5 seconds.
     * Player SHOULD be able to cancel this.
     * <p>
     * Has request times limitation
     *
     * @param rules
     */
    public static void sendPlayerRulesToServer(List<UNGP_RulesManager.URule> rules) {
        final String apiUrl = UNGP_RulesManager.rules_i18n.get("button_feedback_url");
        StringBuilder sb = new StringBuilder(apiUrl);

        String ruleString = (String) Global.getSector().getPersistentData().get(FEEDBACK_LIST_KEY);
        if (ruleString != null) {
            sb.append(ruleString);
        } else {
            if (rules.isEmpty()) return;
            for (UNGP_RulesManager.URule rule : rules) {
                sb.append(rule.getId());
                sb.append("|");
            }
            sb.deleteCharAt(sb.length() - 1);
        }
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
                    LOGGER.info("Error connecting to " + apiUrl);
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
