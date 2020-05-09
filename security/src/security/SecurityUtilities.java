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

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.security.auth.x500.X500Principal;
import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.Base64;
import java.util.Date;

public class SecurityUtilities {

    private static final String ASYMMETRIC_KEY_ALG = "RSA";
    private static final String SYMMETRIC_KEY_ALG = "AES";
    private static final String SYMMETRIC_KEY_ALG_MODE_PAD = SYMMETRIC_KEY_ALG + "/ECB/PKCS7Padding";
    private static final String PROVIDER = "BC";
    private static final String HASH_DIGEST_ALG = "SHA3-512";
    private static final String CERT_FACTORY = "X.509";
    private static final String KEYSTORE_TYPE = "PKCS12";
    private static final String SIGNATURE_ALG = "SHA384with" + ASYMMETRIC_KEY_ALG;
    private static final String TRUSTSTORE_NAME = "truststore.p12";
    private static final String KEYSTORE_NAME = "keystore.p12";
    private static final String SECURE_RANDOM_ALG = "SHA1PRNG";
    private static final String AUTH_HASH_DIGEST_ALG = "PBKDF2WithHmacSHA512";
    private static final int NUM_ITERATIONS = 100000;

    private static long serialNumberBase = System.currentTimeMillis();

    /**
     * Calls deleteEntry with truststore name
     * @param storePassword the store password
     * @param network_alias the network alias
     * @throws GeneralSecurityException
     * @throws IOException
     */
    public static void deleteCertificate(String storePassword, String network_alias)
            throws GeneralSecurityException, IOException
    {
        deleteEntry(storePassword, network_alias, TRUSTSTORE_NAME);
    }

    /**
     * Calls deleteEntry with the keystore name
     * @param storePassword the store password
     * @param network_alias the network alias
     * @throws GeneralSecurityException
     * @throws IOException
     */
    public static void deletePrivateKeyEntry(String storePassword, String network_alias)
            throws GeneralSecurityException, IOException
    {
        deleteEntry(storePassword, network_alias, KEYSTORE_NAME);
    }

    /**
     * Delete the entry of the provided network_alias from the provided storeName
     * @param storePassword the store password
     * @param network_alias the network_alias
     * @param storeName the store name
     * @throws GeneralSecurityException
     * @throws IOException
     */
    private static void deleteEntry(String storePassword, String network_alias, String storeName)
            throws GeneralSecurityException, IOException
    {
        char[] password = storePassword.toCharArray();
        KeyStore store = KeyStore.getInstance(storeName, PROVIDER);
        store.load(new FileInputStream(storeName), password);
        store.deleteEntry(network_alias);
        try(FileOutputStream os = new FileOutputStream(storeName)) {
            store.store(os, password);
        }

    }

    /**
     * Store the trusted certificate for a particular network to the truststore.p12 file
     * @param storePassword the truststore password
     * @param trustedCert the trusted certificate
     * @param network_alias the network alias for identification
     * @throws GeneralSecurityException
     * @throws IOException
     */
    public static void storeCertificate(String storePassword, X509Certificate trustedCert, String network_alias)
            throws GeneralSecurityException, IOException
    {
        char[] password = storePassword.toCharArray();
        KeyStore truststore = KeyStore.getInstance(KEYSTORE_TYPE, PROVIDER);
        truststore.load(null, null);
        truststore.setCertificateEntry(network_alias, trustedCert);
        try(FileOutputStream os = new FileOutputStream( TRUSTSTORE_NAME)) {
            truststore.store(os, password);
        }
    }

    /**
     * Store the private key and certificate chain for a network in the keystore.p12 file
     * @param storePassword the password of the keystore
     * @param eeKey the the private key to be stored
     * @param eeCertChain the certificate chain to be stored
     * @param network_alias the network alias for identification
     * @throws GeneralSecurityException
     * @throws IOException
     */
    public static void storePrivateKeyEntry(String storePassword, PrivateKey eeKey, X509Certificate[] eeCertChain, String network_alias)
            throws GeneralSecurityException, IOException
    {
        char[] password = storePassword.toCharArray();

        KeyStore keystore = KeyStore.getInstance(KEYSTORE_TYPE, PROVIDER);
        keystore.load(null, null);

        keystore.setKeyEntry(network_alias, eeKey, null, eeCertChain);

        try (OutputStream os = new FileOutputStream(KEYSTORE_NAME)) {
            keystore.store(os, password);
        }
    }

    /**
     * Generate an RSA keypair
     * @return return the keypair
     * @throws GeneralSecurityException
     */
    public static KeyPair generateKeyPair()
            throws GeneralSecurityException
    {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(ASYMMETRIC_KEY_ALG, PROVIDER);
        keyPairGenerator.initialize(new RSAKeyGenParameterSpec(3072, RSAKeyGenParameterSpec.F4));
        return keyPairGenerator.generateKeyPair();
    }

    /**
     * This method creates SHA3-512 (512 bit) digests of the input data bytes and then base64 encodes them to a fingerprint
     * @param data The byte array representation of a public key
     * @return the hash of those bytes
     */
    public static String calculateFingerprint(byte[] data)
            throws GeneralSecurityException
    {
        MessageDigest hash = MessageDigest.getInstance(HASH_DIGEST_ALG, PROVIDER);
        return Base64.getEncoder().encodeToString(hash.digest(data));
    }

    /**
     * Generate a self signed V1 X509Certificate for use by the server to authenticate and sign new users into the network
     * it pertains to.
     * @param caPrivateKey The private key for use in signing
     * @param caPublicKey the public key of the certificate
     * @param name The name of the self signing party
     * @return The Certificate
     * @throws GeneralSecurityException
     * @throws OperatorCreationException
     */
    public static X509Certificate makeV1Certificate(PrivateKey caPrivateKey, PublicKey caPublicKey, String name)
            throws GeneralSecurityException, OperatorCreationException
    {
        X509v1CertificateBuilder v1CertBldr = new JcaX509v1CertificateBuilder(
                new X500Name("CN=" + name),
                calculateSerialNumber(),
                calculateDate(0),
                calculateDate(24 * 365 * 100),
                new X500Name("CN=" + name),
                caPublicKey);

        JcaContentSignerBuilder signerBuilder = new JcaContentSignerBuilder(SIGNATURE_ALG).setProvider(PROVIDER);
        return new JcaX509CertificateConverter().setProvider(PROVIDER).getCertificate(v1CertBldr.build(signerBuilder.build(caPrivateKey)));
    }

    /**
     * Generate a V3 X509Certificate for use by the disparate hosts to authenticate to the server
     * @param caCertificate the Certificate of the CA
     * @param caPrivateKey the CA private key
     * @param eePublicKey the requesting parties public key
     * @param subject the subjects name
     * @return  the Certificate
     * @throws GeneralSecurityException
     * @throws CertIOException
     * @throws OperatorCreationException
     */
    public static X509Certificate makeV3Certificate( X509Certificate caCertificate, PrivateKey caPrivateKey, PublicKey eePublicKey, String subject)
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

    /**
     * A date utilitiy for calculating how much time in the future a certificate will be valid for
     * @param hoursInFuture the number of hours you want the certificate to be valid for
     * @return the Date of that number of hours in the future from the current time
     */
    private static Date calculateDate(int hoursInFuture){

        long secs = System.currentTimeMillis() / 1000;

        return new Date((secs + (hoursInFuture * 60 * 60)) * 1000);
    }

    /**
     * A method for soliciting a distinct serial number for certificate generation for multiple threads
     * @return the SerialNumber
     */
    private static synchronized BigInteger calculateSerialNumber(){
        return BigInteger.valueOf(serialNumberBase++);
    }

    /**
     * produce a salt value for use in password hashing
     * @return salt
     * @throws NoSuchAlgorithmException missing boi
     */
    private static byte[] getSalt()
            throws NoSuchAlgorithmException
    {
        SecureRandom secureRandom = SecureRandom.getInstance(SECURE_RANDOM_ALG);
        byte[] salt = new byte[64];
        secureRandom.nextBytes(salt);
        return salt;
    }

    /**
     * Performs a hash + salt operation on the user provided password
     * @param password the user provided password
     * @return the hashed and salted password
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    static String getAuthennticationHash(String password)
            throws  NoSuchAlgorithmException, InvalidKeySpecException
    {
        return getAuthenticationHash(password,  NUM_ITERATIONS);
    }


    static String getAuthenticationHash(String password, int iterations)
            throws  NoSuchAlgorithmException, InvalidKeySpecException
    {
        byte[] salt = getSalt();
        char[] chars = password.toCharArray();

        PBEKeySpec spec = new PBEKeySpec(chars, salt, iterations, 512);
        SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(AUTH_HASH_DIGEST_ALG);
        byte[] hashWithSalt = secretKeyFactory.generateSecret(spec).getEncoded();

        String authHash= Base64.getEncoder().encodeToString(hashWithSalt);
        authHash += ":";
        authHash += Base64.getEncoder().encodeToString(salt);
        authHash += ":";
        authHash += Integer.toString(iterations);

        return authHash;
    }

}
