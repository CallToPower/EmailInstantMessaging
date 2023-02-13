/**
 * EIM, Copyright 2014 Denis Meyer
 */
package com.eim.util;

/*
 import java.io.IOException;
 import java.io.UnsupportedEncodingException;
 import java.security.InvalidKeyException;
 import java.security.NoSuchAlgorithmException;
 import java.security.SecureRandom;
 import java.security.spec.InvalidKeySpecException;
 import javax.crypto.BadPaddingException;
 import javax.crypto.Cipher;
 import javax.crypto.IllegalBlockSizeException;
 import javax.crypto.KeyGenerator;
 import javax.crypto.NoSuchPaddingException;
 import javax.crypto.SecretKey;
 import javax.crypto.SecretKeyFactory;
 import javax.crypto.spec.DESedeKeySpec;
 import javax.crypto.spec.SecretKeySpec;
 import org.apache.logging.log4j.LogManager;
 import org.apache.logging.log4j.Logger;
 */
/**
 * EIMCrypto
 *
 * @author Denis Meyer
 */
public final class EIMCrypto {
    /*

     private static final Logger logger = LogManager.getLogger(EIMCrypto.class.getName());
     private final String algorithm = "DESede";
     private KeyGenerator generator = null;
     private SecretKey key = null;

     public EIMCrypto() throws NoSuchAlgorithmException {
     if(logger.isDebugEnabled()) {
     logger.debug("Initializing EIMCrypto (w/o secret key)");
     }

     generator = KeyGenerator.getInstance(algorithm);
     generator.init(new SecureRandom());

     generateKey();
     }

     public EIMCrypto(byte[] _key) throws NoSuchAlgorithmException, InvalidKeyException, InvalidKeySpecException {
     if(logger.isDebugEnabled()) {
     logger.debug("Initializing EIMCrypto (w/ secret key)");
     }

     DESedeKeySpec keySpec = new DESedeKeySpec(_key);
     SecretKeyFactory factory = SecretKeyFactory.getInstance(algorithm);
     key = factory.generateSecret(keySpec);
     }

     public void generateKey() throws NoSuchAlgorithmException {
     if(logger.isDebugEnabled()) {
     logger.debug("Generating new secret key");
     }
     key = generator.generateKey();
     }

     public byte[] encrypt(String message) throws IllegalBlockSizeException,
     BadPaddingException, NoSuchAlgorithmException,
     NoSuchPaddingException, InvalidKeyException,
     UnsupportedEncodingException {
     Cipher cipher = Cipher.getInstance(algorithm + "/ECB/PKCS5Padding");
     cipher.init(Cipher.ENCRYPT_MODE, key);
     byte[] stringBytes = message.getBytes(EIMConstants.CHARSET);
     return cipher.doFinal(stringBytes);
     }

     public String decrypt(byte[] encrypted) throws InvalidKeyException,
     NoSuchAlgorithmException, NoSuchPaddingException,
     IllegalBlockSizeException, BadPaddingException, IOException {
     Cipher cipher = Cipher.getInstance(algorithm + "/ECB/PKCS5Padding");
     cipher.init(Cipher.DECRYPT_MODE, key);
     byte[] stringBytes = cipher.doFinal(encrypted);
     return new String(stringBytes, EIMConstants.CHARSET);
     }

     public byte[] getEncodedKey() {
     return key.getEncoded();
     }

     public void setKey(byte[] byteKey) {
     if ((byteKey != null) && byteKey.length > 0) {
     setKey(new SecretKeySpec(byteKey, algorithm));
     }
     }

     public void setKey(SecretKey key) {
     if (key != null) {
     this.key = key;
     }
     }
     */
}
