package com.mtavares;

import com.google.common.base.Objects;

public class School {

    public String nom;
    public String etablissement;
    public String nbEleves;
    public String zone;
    public String type;
    public String code;
    public String adresse;
    public String academie;
    public String url;

    public School(String nom, String etablissement, String nbEleves, String zone, String type, String code, String adresse, String academie, String url) {
        this.nom = nom;
        this.etablissement = etablissement;
        this.nbEleves = nbEleves;
        this.zone = zone;
        this.type = type;
        this.code = code;
        this.adresse = adresse;
        this.academie = academie;
        this.url = url;
    }

    public String[] toCSV() {
        return new String[]{
                nom, etablissement, nbEleves, zone, type, code, adresse, academie, url
        };
    }

    public static String[] getHeaders() {
        return new String[]{
                "Nom", "Etablissement", "Nombre d'élèves", "Zone",
                "Type", "Code", "Adresse", "Académie", "URL"
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        School school = (School) o;
        return Objects.equal(nom, school.nom) &&
                Objects.equal(etablissement, school.etablissement) &&
                Objects.equal(nbEleves, school.nbEleves) &&
                Objects.equal(zone, school.zone) &&
                Objects.equal(type, school.type) &&
                Objects.equal(code, school.code) &&
                Objects.equal(adresse, school.adresse) &&
                Objects.equal(academie, school.academie) &&
                Objects.equal(url, school.url);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(nom, etablissement, nbEleves, zone, type, code, adresse, academie, url);
    }
}

