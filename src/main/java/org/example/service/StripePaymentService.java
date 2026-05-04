package org.example.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class StripePaymentService {

    private final Properties fileConfig = loadFileConfig();
    private final String secretKey = readConfig("STRIPE_SECRET_KEY", "");
    private final String successUrl = readConfig("STRIPE_SUCCESS_URL",
            "https://example.com/emonado-payment-success?session_id={CHECKOUT_SESSION_ID}");
    private final String cancelUrl = readConfig("STRIPE_CANCEL_URL",
            "https://example.com/emonado-payment-cancel");
    private final int defaultAmountCents = parseInt(readConfig("STRIPE_CONSULTATION_AMOUNT_CENTS", "50000"), 50000);
    private final String defaultCurrency = readConfig("STRIPE_CURRENCY", "tnd");

    public boolean isConfigured() {
        return secretKey != null && !secretKey.isBlank();
    }

    public int getDefaultAmountCents() {
        return defaultAmountCents;
    }

    public String getDefaultCurrency() {
        return defaultCurrency;
    }

    public StripeCheckoutSession createCheckoutSession(String patientEmail,
                                                       String patientName,
                                                       String description,
                                                       int amountCents,
                                                       String currency,
                                                       String clientReferenceId,
                                                       int paymentId,
                                                       int rendezVousId) throws StripeException {
        ensureConfigured();
        Stripe.apiKey = secretKey;

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .setClientReferenceId(clientReferenceId)
                .setCustomerEmail(patientEmail)
                .putMetadata("payment_id", String.valueOf(paymentId))
                .putMetadata("rendez_vous_id", String.valueOf(rendezVousId))
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency(currency.toLowerCase())
                                                .setUnitAmount((long) amountCents)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName("Consultation psychologique")
                                                                .setDescription(buildDescription(patientName, description))
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                .build();

        Session session = Session.create(params);
        StripeCheckoutSession result = new StripeCheckoutSession();
        result.id = session.getId();
        result.url = session.getUrl();
        result.paymentStatus = session.getPaymentStatus();
        result.paymentIntentId = session.getPaymentIntent();
        return result;
    }

    public StripeCheckoutSession retrieveCheckoutSession(String sessionId) throws StripeException {
        ensureConfigured();
        Stripe.apiKey = secretKey;
        Session session = Session.retrieve(sessionId);
        StripeCheckoutSession result = new StripeCheckoutSession();
        result.id = session.getId();
        result.url = session.getUrl();
        result.paymentStatus = session.getPaymentStatus();
        result.paymentIntentId = session.getPaymentIntent();
        return result;
    }

    private String buildDescription(String patientName, String description) {
        String safePatient = patientName == null || patientName.isBlank() ? "Patient" : patientName.trim();
        String safeDescription = description == null || description.isBlank() ? "Seance validee depuis EmoNado" : description.trim();
        return safePatient + " | " + safeDescription;
    }

    private void ensureConfigured() {
        if (!isConfigured()) {
            throw new IllegalStateException("Configuration Stripe manquante. Renseignez STRIPE_SECRET_KEY dans config/stripe.properties.");
        }
    }

    private Properties loadFileConfig() {
        Properties props = new Properties();
        tryLoad(props, Paths.get("config", "stripe.properties"));

        try {
            URL location = StripePaymentService.class.getProtectionDomain().getCodeSource().getLocation();
            Path classesDir = Paths.get(location.toURI());
            Path projectRoot = classesDir.getParent() == null ? null : classesDir.getParent().getParent();
            if (projectRoot != null) {
                tryLoad(props, projectRoot.resolve(Paths.get("config", "stripe.properties")));
            }
        } catch (URISyntaxException | NullPointerException ignored) {
        }

        if (props.isEmpty()) {
            try (InputStream is = StripePaymentService.class.getClassLoader().getResourceAsStream("stripe.properties")) {
                if (is != null) {
                    props.load(is);
                }
            } catch (IOException ignored) {
            }
        }
        return props;
    }

    private void tryLoad(Properties props, Path path) {
        if (!Files.exists(path)) {
            return;
        }
        try (InputStream is = Files.newInputStream(path)) {
            props.load(is);
        } catch (IOException ignored) {
        }
    }

    private String readConfig(String key, String defaultValue) {
        String value = System.getProperty(key);
        if (value != null && !value.isBlank()) {
            return value.trim();
        }
        value = System.getenv(key);
        if (value != null && !value.isBlank()) {
            return value.trim();
        }
        value = fileConfig.getProperty(key);
        if (value != null && !value.isBlank()) {
            return value.trim();
        }
        return defaultValue;
    }

    private int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static final class StripeCheckoutSession {
        private String id;
        private String url;
        private String paymentStatus;
        private String paymentIntentId;

        public String getId() {
            return id;
        }

        public String getUrl() {
            return url;
        }

        public String getPaymentStatus() {
            return paymentStatus;
        }

        public String getPaymentIntentId() {
            return paymentIntentId;
        }
    }
}
