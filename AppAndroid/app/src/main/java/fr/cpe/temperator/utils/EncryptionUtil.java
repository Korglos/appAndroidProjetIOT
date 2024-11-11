package fr.cpe.temperator.utils;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import android.util.Base64;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import fr.cpe.temperator.exeptions.CipherExeption;

// Class utilitaire pour chiffer les données
public class EncryptionUtil {
    private static final String ALGORITHM = "AES";

    private static final String FIXED_KEY = "2b7e151628aed2a6abf7158809cf4f3c";

    private EncryptionUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    // Récupère la clé fixe
    public static SecretKey getFixedKey() {
        byte[] keyBytes = hexStringToByteArray();
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }

    // Chiffre les données
    public static String encrypt(String data, SecretKey key) throws CipherExeption {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encryptedBytes = cipher.doFinal(data.getBytes());
            return Base64.encodeToString(encryptedBytes, Base64.DEFAULT);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                 BadPaddingException | IllegalBlockSizeException e) {
            throw new CipherExeption("Error while encrypting data", e);
        }
    }

    // Déchiffre les données
    public static String decrypt(String encryptedData, SecretKey key) throws CipherExeption {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decodedBytes = Base64.decode(encryptedData, Base64.DEFAULT);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            return new String(decryptedBytes);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                 BadPaddingException | IllegalBlockSizeException e) {
            throw new CipherExeption("Error while decrypting data", e);
        }
    }

    // Convertit une chaîne hexadécimale en tableau de bytes
    private static byte[] hexStringToByteArray() {
        int len = EncryptionUtil.FIXED_KEY.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(EncryptionUtil.FIXED_KEY.charAt(i), 16) << 4)
                    + Character.digit(EncryptionUtil.FIXED_KEY.charAt(i+1), 16));
        }
        return data;
    }
}