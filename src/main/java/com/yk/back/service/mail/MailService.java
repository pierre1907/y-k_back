package com.yk.back.service.mail;

import com.yk.back.config.AppProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {

    private final JavaMailSender mailSender;
    private final AppProperties appProperties;

    // ── Auth ─────────────────────────────────────────────────────────────────

    @Async
    public void sendWelcome(String to, String fullName) {
        String subject = "Bienvenue sur Y&K Platform";
        String body = """
                <h2>Bonjour %s,</h2>
                <p>Votre compte a été créé avec succès sur <strong>Y&K Platform</strong>.</p>
                <p>Vous pouvez dès maintenant vous connecter via le lien ci-dessous :</p>
                <p><a href="%s/login">Accéder à ma plateforme</a></p>
                <hr/>
                <p style="color:#888;font-size:12px;">Cet email a été envoyé automatiquement, merci de ne pas y répondre.</p>
                """.formatted(fullName, appProperties.getMail().getBaseUrl());
        send(to, subject, body);
    }

    @Async
    public void sendPasswordReset(String to, String fullName, String resetToken) {
        String resetUrl = appProperties.getMail().getBaseUrl() + "/reset-password?token=" + resetToken;
        String subject = "Réinitialisation de votre mot de passe";
        String body = """
                <h2>Bonjour %s,</h2>
                <p>Vous avez demandé la réinitialisation de votre mot de passe.</p>
                <p>Cliquez sur le lien ci-dessous (valable 1 heure) :</p>
                <p><a href="%s">Réinitialiser mon mot de passe</a></p>
                <p>Si vous n'êtes pas à l'origine de cette demande, ignorez cet email.</p>
                <hr/>
                <p style="color:#888;font-size:12px;">Cet email a été envoyé automatiquement, merci de ne pas y répondre.</p>
                """.formatted(fullName, resetUrl);
        send(to, subject, body);
    }

    @Async
    public void sendPasswordChanged(String to, String fullName) {
        String subject = "Votre mot de passe a été modifié";
        String body = """
                <h2>Bonjour %s,</h2>
                <p>Votre mot de passe a été modifié avec succès.</p>
                <p>Si vous n'êtes pas à l'origine de cette action, contactez immédiatement le support.</p>
                <hr/>
                <p style="color:#888;font-size:12px;">Cet email a été envoyé automatiquement, merci de ne pas y répondre.</p>
                """.formatted(fullName);
        send(to, subject, body);
    }

    @Async
    public void sendLoginAlert(String to, String fullName, String ipAddress) {
        String subject = "Nouvelle connexion détectée sur votre compte";
        String body = """
                <h2>Bonjour %s,</h2>
                <p>Une nouvelle connexion a été détectée sur votre compte depuis l'adresse IP <strong>%s</strong>.</p>
                <p>Si ce n'était pas vous, réinitialisez immédiatement votre mot de passe.</p>
                <p><a href="%s/login">Accéder à ma plateforme</a></p>
                <hr/>
                <p style="color:#888;font-size:12px;">Cet email a été envoyé automatiquement, merci de ne pas y répondre.</p>
                """.formatted(fullName, ipAddress, appProperties.getMail().getBaseUrl());
        send(to, subject, body);
    }

    // ── Compte ───────────────────────────────────────────────────────────────

    @Async
    public void sendAccountActivated(String to, String fullName) {
        String subject = "Votre compte a été activé";
        String body = """
                <h2>Bonjour %s,</h2>
                <p>Votre compte est maintenant <strong>actif</strong>.</p>
                <p><a href="%s/login">Accéder à ma plateforme</a></p>
                <hr/>
                <p style="color:#888;font-size:12px;">Cet email a été envoyé automatiquement, merci de ne pas y répondre.</p>
                """.formatted(fullName, appProperties.getMail().getBaseUrl());
        send(to, subject, body);
    }

    @Async
    public void sendAccountDeactivated(String to, String fullName) {
        String subject = "Votre compte a été désactivé";
        String body = """
                <h2>Bonjour %s,</h2>
                <p>Votre compte a été <strong>désactivé</strong>. Contactez le support pour plus d'informations.</p>
                <hr/>
                <p style="color:#888;font-size:12px;">Cet email a été envoyé automatiquement, merci de ne pas y répondre.</p>
                """.formatted(fullName);
        send(to, subject, body);
    }

    // ── Tenant / Merchant ────────────────────────────────────────────────────

    @Async
    public void sendTenantCreated(String to, String adminName, String tenantName) {
        String subject = "Nouveau tenant créé : " + tenantName;
        String body = """
                <h2>Bonjour %s,</h2>
                <p>Le tenant <strong>%s</strong> vient d'être créé sur Y&K Platform.</p>
                <p><a href="%s/admin/tenants">Gérer les tenants</a></p>
                <hr/>
                <p style="color:#888;font-size:12px;">Cet email a été envoyé automatiquement, merci de ne pas y répondre.</p>
                """.formatted(adminName, tenantName, appProperties.getMail().getBaseUrl());
        send(to, subject, body);
    }

    @Async
    public void sendTenantAdminInvite(String to, String tenantName, String token) {
        String setupUrl = appProperties.getMail().getBaseUrl() + "/reset-password?token=" + token;
        String subject = "Votre espace " + tenantName + " est prêt — définissez votre mot de passe";
        String body = """
                <h2>Bienvenue sur Y&K Platform,</h2>
                <p>Le tenant <strong>%s</strong> vient d'être créé et vous avez été désigné comme administrateur.</p>
                <p>Définissez votre mot de passe pour activer votre compte (lien valable 24h) :</p>
                <p><a href="%s">Définir mon mot de passe</a></p>
                <hr/>
                <p style="color:#888;font-size:12px;">Cet email a été envoyé automatiquement, merci de ne pas y répondre.</p>
                """.formatted(tenantName, setupUrl);
        send(to, subject, body);
    }

    @Async
    public void sendMerchantCreated(String to, String adminName, String merchantName, String tenantName) {
        String subject = "Nouveau merchant créé : " + merchantName;
        String body = """
                <h2>Bonjour %s,</h2>
                <p>Le merchant <strong>%s</strong> a été créé sous le tenant <strong>%s</strong>.</p>
                <p><a href="%s/admin/merchants">Gérer les merchants</a></p>
                <hr/>
                <p style="color:#888;font-size:12px;">Cet email a été envoyé automatiquement, merci de ne pas y répondre.</p>
                """.formatted(adminName, merchantName, tenantName, appProperties.getMail().getBaseUrl());
        send(to, subject, body);
    }

    // ── Abonnement ────────────────────────────────────────────────────────────

    @Async
    public void sendSubscriptionActivated(String to, String fullName, String planName) {
        String subject = "Abonnement activé — " + planName;
        String body = """
                <h2>Bonjour %s,</h2>
                <p>Votre abonnement <strong>%s</strong> est maintenant actif.</p>
                <p><a href="%s/billing">Voir mon abonnement</a></p>
                <hr/>
                <p style="color:#888;font-size:12px;">Cet email a été envoyé automatiquement, merci de ne pas y répondre.</p>
                """.formatted(fullName, planName, appProperties.getMail().getBaseUrl());
        send(to, subject, body);
    }

    @Async
    public void sendSubscriptionExpiring(String to, String fullName, String planName, String expiryDate) {
        String subject = "Votre abonnement expire bientôt";
        String body = """
                <h2>Bonjour %s,</h2>
                <p>Votre abonnement <strong>%s</strong> expirera le <strong>%s</strong>.</p>
                <p>Renouvelez-le dès maintenant pour ne pas interrompre votre service.</p>
                <p><a href="%s/billing">Renouveler mon abonnement</a></p>
                <hr/>
                <p style="color:#888;font-size:12px;">Cet email a été envoyé automatiquement, merci de ne pas y répondre.</p>
                """.formatted(fullName, planName, expiryDate, appProperties.getMail().getBaseUrl());
        send(to, subject, body);
    }

    @Async
    public void sendSubscriptionExpired(String to, String fullName, String planName) {
        String subject = "Votre abonnement a expiré";
        String body = """
                <h2>Bonjour %s,</h2>
                <p>Votre abonnement <strong>%s</strong> a expiré. Votre accès est maintenant limité.</p>
                <p><a href="%s/billing">Renouveler mon abonnement</a></p>
                <hr/>
                <p style="color:#888;font-size:12px;">Cet email a été envoyé automatiquement, merci de ne pas y répondre.</p>
                """.formatted(fullName, planName, appProperties.getMail().getBaseUrl());
        send(to, subject, body);
    }

    // ── Facturation ───────────────────────────────────────────────────────────

    @Async
    public void sendInvoiceAvailable(String to, String fullName, String invoiceNumber, String amount) {
        String subject = "Nouvelle facture disponible — " + invoiceNumber;
        String body = """
                <h2>Bonjour %s,</h2>
                <p>Votre facture <strong>%s</strong> d'un montant de <strong>%s</strong> est disponible.</p>
                <p><a href="%s/billing/invoices">Voir mes factures</a></p>
                <hr/>
                <p style="color:#888;font-size:12px;">Cet email a été envoyé automatiquement, merci de ne pas y répondre.</p>
                """.formatted(fullName, invoiceNumber, amount, appProperties.getMail().getBaseUrl());
        send(to, subject, body);
    }

    @Async
    public void sendPaymentConfirmed(String to, String fullName, String invoiceNumber, String amount) {
        String subject = "Paiement confirmé — " + invoiceNumber;
        String body = """
                <h2>Bonjour %s,</h2>
                <p>Le paiement de <strong>%s</strong> pour la facture <strong>%s</strong> a bien été reçu.</p>
                <p>Merci pour votre confiance.</p>
                <p><a href="%s/billing/invoices">Voir mes factures</a></p>
                <hr/>
                <p style="color:#888;font-size:12px;">Cet email a été envoyé automatiquement, merci de ne pas y répondre.</p>
                """.formatted(fullName, amount, invoiceNumber, appProperties.getMail().getBaseUrl());
        send(to, subject, body);
    }

    @Async
    public void sendPaymentFailed(String to, String fullName, String invoiceNumber) {
        String subject = "Échec du paiement — " + invoiceNumber;
        String body = """
                <h2>Bonjour %s,</h2>
                <p>Le paiement de la facture <strong>%s</strong> a échoué.</p>
                <p>Veuillez mettre à jour vos informations de paiement pour régulariser votre situation.</p>
                <p><a href="%s/billing">Gérer ma facturation</a></p>
                <hr/>
                <p style="color:#888;font-size:12px;">Cet email a été envoyé automatiquement, merci de ne pas y répondre.</p>
                """.formatted(fullName, invoiceNumber, appProperties.getMail().getBaseUrl());
        send(to, subject, body);
    }

    // ── Envoi bas niveau ──────────────────────────────────────────────────────

    private void send(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(
                    appProperties.getMail().getFrom(),
                    appProperties.getMail().getFromName()
            );
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.debug("Mail [{}] envoyé à {}", subject, to);
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("Échec envoi mail à {} : {}", to, e.getMessage());
        }
    }
}
