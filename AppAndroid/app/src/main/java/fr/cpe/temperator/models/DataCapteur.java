package fr.cpe.temperator.models;

import androidx.annotation.NonNull;

import java.util.Objects;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataCapteur {
    private String id;
    private String nom;
    private String valeur;
    private String unite;

    @NonNull
    public String toString() {
        if (this.getValeur() == null) {
            this.setValeur("no value");
            this.setUnite("");
        }
        return nom + " : " + valeur + " " + unite;
    }
}
