package fr.cpe.temperator.exeptions;

// Exception pour les erreurs de chiffrement et déchiffrement
public class CipherExeption extends Exception {
    public CipherExeption(String message, Throwable cause) {
        super(message, cause);
    }
}
