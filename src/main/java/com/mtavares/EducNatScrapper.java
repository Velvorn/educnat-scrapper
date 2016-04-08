package com.mtavares;

import com.opencsv.CSVWriter;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class EducNatScrapper {

    private static final String BASE_URL = "http://www.education.gouv.fr";
    private static final int NB_PAGES = 650;
    private static final int NB_SCHOOLS = 65000;

    private List<String> urls;
    private Set<School> schools;

    public EducNatScrapper() {
        this.urls = new ArrayList<>(NB_SCHOOLS);
        this.schools = new HashSet<>(NB_SCHOOLS);
    }

    public void extractAllURLs(String file) {
        IntStream.range(0, NB_PAGES)
                .parallel()
                .forEach(this::extractURLs);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            for (String url : this.urls) {
                bw.write(url);
                bw.newLine();
            }
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void extractURLs(int page) {
        String url = BASE_URL + "/pid24302/annuaire-resultat-recherche.html&page=" + page;
        List<String> realLinks = Collections.emptyList();
        try {
            Connection connection = Jsoup.connect(url).timeout(30000);
            Document doc = connection.get();
            Element annuaire = doc.select("div.annuaire-recherche-resultats").first();
            Elements links = annuaire.select("a[href]:not([class])");
            realLinks = links.stream()
                    .map(element -> BASE_URL + element.attr("href"))
                    .map(link -> link.replaceAll("\\(", "%28")
                            .replaceAll("\\)", "%29")
                            .replaceAll("`", "%60"))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.urls.addAll(realLinks);
    }

    public void extractAllURLsFromFile(String file) {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                urls.add(line);
            }
            br.close();
            Collections.sort(this.urls);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void extractAllSchools(int begin, int end) {
        List<String> copyURLs = this.urls.subList(begin, end);
        //System.out.println("copyURLs = " + copyURLs.size());
        List<List<String>> splitURLs = splitList(copyURLs);
        List<List<School>> collectedSchools = splitURLs.parallelStream()
                .map(this::extractSchools)
                .collect(Collectors.toList());
        this.schools.clear();
        collectedSchools.forEach(this.schools::addAll);
    }

    private List<School> extractSchools(List<String> urls) {
        return urls.stream()
                .map(this::extractSchool)
                .collect(Collectors.toList());
    }

    private School extractSchool(String url) {
        try {
            //System.out.println("url = " + url);
            Connection connection = Jsoup.connect(url).timeout(30000);
            Document doc = connection.get();
            Element fiche = doc.select("div.annuaire-etablissement-fiche").first();
            if (fiche == null) {
                return new School("TO BE DELETED", "", "", "", "", "", "", "", url);
            }
            Element nomFinder = fiche.select("h2 span").first();
            String nom = nomFinder != null ? nomFinder.text() : "";
            Element etabFinder = fiche.select("div.fiche-type-etab").first();
            String etablissement = etabFinder != null ? etabFinder.text() : "";
            Element nbElevesFinder = fiche.select("span.annuaire-nb-eleves").first();
            String nbEleves = nbElevesFinder != null ? nbElevesFinder.text() : "";
            Element zoneFinder = fiche.select("a.annuaire-zone-scolaire").first();
            String zone = zoneFinder != null ? zoneFinder.text() : "";
            Element typeFinder = fiche.select("span.annuaire-type-etab").first();
            String type = typeFinder != null ? typeFinder.text() : "";
            Element codeFinder = fiche.select("span.annuaire-code").first();
            String code = codeFinder != null ? codeFinder.text() : "";
            Element adresseFinder = fiche.select("p.annuaire-etablissement-infos-part3").first();
            String adresse = adresseFinder != null ? adresseFinder.text() : "";
            Element academieFinder = fiche.select("span.academie-lien").first();
            String academie = academieFinder != null ? academieFinder.text() : "";
            return new School(nom, etablissement, nbEleves, zone, type, code, adresse, academie, url);
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
        return null;
    }

    public void writeToFile(String file, boolean append) {
        List<School> schools = new ArrayList<>(this.schools);
        List<List<School>> schoolsSubLists = splitList(schools);
        try (CSVWriter csvWriter = new CSVWriter(new FileWriter(file, append))) {
            if (!append) {
                String[] headers = School.getHeaders();
                csvWriter.writeNext(headers);
            }
            schoolsSubLists.parallelStream()
                    .forEach(subList -> {
                        List<String[]> csvLines = subList.parallelStream()
                                .map(School::toCSV)
                                .collect(Collectors.toList());
                        csvWriter.writeAll(csvLines, true);
                    });
            csvWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private <T> List<List<T>> splitList(List<T> list) {
        final int listSize = list.size();
        int maxSplits = 50;
        final int nbSplits = Math.min(maxSplits, Math.max(listSize / maxSplits, 1));
        final int subListSize = listSize / nbSplits;
        List<List<T>> parts = new ArrayList<>(nbSplits + 1);
        for (int i = 0; i < listSize; i += subListSize) {
            parts.add(new ArrayList<>(
                    list.subList(i, Math.min(listSize, i + subListSize))
            ));
        }
        return parts;
    }

    public static void main(String[] args) {
        EducNatScrapper eductNatScrapper = new EducNatScrapper();
        eductNatScrapper.extractAllURLsFromFile("urls.txt");
        for (int i = 0; i < EducNatScrapper.NB_SCHOOLS; i = i + 1000) {
            eductNatScrapper.extractAllSchools(i, Math.min(i + 1000, EducNatScrapper.NB_SCHOOLS));
            eductNatScrapper.writeToFile("schools.csv", i != 0);
        }
    }
}
