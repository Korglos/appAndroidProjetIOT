package fr.cpe.temperator.models;

import androidx.annotation.NonNull;

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

    @NonNull
    public String toString() {
        return nom + " : " + valeur;
    }
}
