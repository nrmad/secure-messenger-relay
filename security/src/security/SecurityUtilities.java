package security;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509v1CertificateBuilder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v1CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import javax.security.auth.x500.X500Principal;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Date;

public class SecurityUtilities {

    private static final String ASYMMETRIC_KEY_ALG = "RSA";
    private static final String SYMMETRIC_KEY_ALG = "AES";
    private static final String SYMMETRIC_KEY_ALG_MODE_PAD = SYMMETRIC_KEY_ALG + "/ECB/PKCS7Padding";
    private static final String PROVIDER = "BC";
    private static final String HASH_DIGEST_ALG = "SHA3-512";
    private static final String CERT_FACTORY = "X.509";
    private static final String KEYSTORE_TYPE = "BCFKS";
    private static final String SIGNATURE_ALG = "SHA384with" + ASYMMETRIC_KEY_ALG;
    private static final String TRUSTSTORE_NAME_POSTFIX = "-truststore";
    private static final String KEYSTORE_NAME_POSTFIX = "-keystore";
    private static final String SECURE_RANDOM_ALG = "SHA1PRNG";
    private static final String AUTH_HASH_DIGEST_ALG = "PBKDF2WithHmacSHA512";
    private static final int NUM_ITERATIONS = 100000;

    private static long serialNumberBase = System.currentTimeMillis();

    static X509Certificate makeV1Certificate(PrivateKey caSignerKey, PublicKey caPublicKey, String issuer, String subject)
            throws GeneralSecurityException, OperatorCreationException
    {
        X509v1CertificateBuilder v1CertBldr = new JcaX509v1CertificateBuilder(
                new X500Name("CN=" + issuer),
                calculateSerialNumber(),
                calculateDate(0),
                calculateDate(24 * 365 * 100),
                new X500Name("CN=" + subject),
                caPublicKey);

        JcaContentSignerBuilder signerBuilder = new JcaContentSignerBuilder(SIGNATURE_ALG).setProvider(PROVIDER);
        return new JcaX509CertificateConverter().setProvider(PROVIDER).getCertificate(v1CertBldr.build(signerBuilder.build(caSignerKey)));
    }

    static X509Certificate makeV3Certificate( X509Certificate caCertificate, PrivateKey caPrivateKey, PublicKey eePublicKey, String subject)
            throws GeneralSecurityException, CertIOException, OperatorCreationException
    {
        X509v3CertificateBuilder v3CertBldr = new JcaX509v3CertificateBuilder(
                caCertificate.getSubjectX500Principal(),
                calculateSerialNumber(),
                calculateDate(0),
                calculateDate(24 * 365 * 100),
                new X500Principal("CN=" + subject),
                eePublicKey);

        JcaX509ExtensionUtils extUtils = new JcaX509ExtensionUtils();
        v3CertBldr.addExtension(
                // Identifies the public key certified by this certificate (e.g. where more than one exist to a subject)
                Extension.subjectKeyIdentifier,
                false,
                extUtils.createSubjectKeyIdentifier(eePublicKey));
        v3CertBldr.addExtension(
                // Identifies the public key which relates to the signing private key (e.g. more than one exists to issues)
                Extension.authorityKeyIdentifier,
                false,
                extUtils.createAuthorityKeyIdentifier(caCertificate));
        v3CertBldr.addExtension(
                // Used in identifying the CA in a cert chain
                Extension.basicConstraints,
                true,
                new BasicConstraints(false));

        JcaContentSignerBuilder signerBuilder = new JcaContentSignerBuilder(SIGNATURE_ALG)
                .setProvider(PROVIDER);

        return new JcaX509CertificateConverter().setProvider(PROVIDER)
                .getCertificate(v3CertBldr.build(signerBuilder.build(caPrivateKey)));
    }

    private static Date calculateDate(int hoursInFuture){

        long secs = System.currentTimeMillis() / 1000;

        return new Date((secs + (hoursInFuture * 60 * 60)) * 1000);
    }

    private static synchronized BigInteger calculateSerialNumber(){
        return BigInteger.valueOf(serialNumberBase++);
    }

}
