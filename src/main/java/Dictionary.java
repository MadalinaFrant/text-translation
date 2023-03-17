import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class Dictionary {

    private HashMap<String, ArrayList<Word>> map = new HashMap<>();

    /* citire din dictionare si salvare informatii intr-un HashMap */
    private void readDictionary(File dir) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            String fileName = file.getName();
            /* extrage limba din numele fisierului */
            String language = fileName.substring(0, 2);

            Gson gson = new Gson();
            Reader reader = new FileReader(file);
            /* se defineste tipul lista de cuvinte (de tip Word) */
            TypeToken<ArrayList<Word>> type = new TypeToken<>(){};

            /* citeste din fisier lista de cuvinte utilizand gson */
            ArrayList<Word> words = gson.fromJson(reader, type.getType());
            /* adauga in map perechea limba - lista de cuvinte */
            map.put(language, words);
            reader.close();
        }
    }

    /* afisarea dictionarului (HashMap-ului in care au fost salvate cuvintele) */
    private void printDictionary() {
        for (HashMap.Entry<String, ArrayList<Word>> entry : map.entrySet()) {
            System.out.println(entry.getKey() + " " + entry.getValue().toString());
        }
        System.out.println();
    }

    /* metoda pentru adaugarea unui cuvant in dictionar - intoarce true daca
    s-a adaugat cuvantul in dictionar sau false daca exista deja cuvantul
    in dictionar */
    private boolean addWord(Word word, String language) throws Exception {
        if (map.containsKey(language)) { // limba exista in dictionar
            for (Word w : map.get(language)) {
                /* in forms se vor retine toate formele posibile ale cuvantului */
                ArrayList<String> forms = new ArrayList<>();
                forms.add(w.getWord());
                forms.addAll(w.getSingular());
                forms.addAll(w.getPlural());
                for (String f : forms) { // cauta in toate formele cuvantului
                    if (f.equals(word.getWord())) { // cuvant gasit
                        /* added devine true daca este adaugata o defintie cuvantului */
                        boolean added = false;
                        for (Definition d : word.getDefinitions()) {
                            if (addDefinitionForWord(w.getWord(), language, d)) {
                                added = true; // a fost adaugata definitie
                            }
                        }
                        /* true = modificare (adaugare defintie), false = cuvant deja existent */
                        return added;
                    }
                }
            }
            /* nu exista deja cuvantul in lista corespunzatoare => se adauga la aceasta lista */
            map.get(language).add(word);
        } else { // limba nu exista in dictionar
            ArrayList<Word> newList = new ArrayList<>(); // creare lista noua
            newList.add(word); // adaugare cuvant in noua lista
            map.put(language, newList); // adaugare in map perechea limba - lista
        }
        return true; // cuvantul a fost adaugat
    }

    /* metoda pentru stergerea unui cuvant din dictionar - intoarce true daca
    s-a sters cuvantul in dictionar sau false daca nu exista cuvantul in
    dictionar */
    private boolean removeWord(String word, String language) throws Exception {
        if (!map.containsKey(language)) { // limba nu exista in dictionar
            throw new Exception("Language " + language + " doesn't exist in dictionary\n");
        }

        boolean found = false; // found devine true daca este gasit cuvantul
        for (Word w : map.get(language)) {
            /* in forms se vor retine toate formele posibile ale cuvantului */
            ArrayList<String> forms = new ArrayList<>();
            forms.add(w.getWord());
            forms.addAll(w.getSingular());
            forms.addAll(w.getPlural());
            for (String f : forms) { // cauta in toate formele cuvantului
                if (f.equals(word)) { // cuvant gasit
                    found = true;
                    map.get(language).remove(w); // sterge cuvantul din lista
                    break;
                }
            }
            if (found) // cuvant gasit => nu mai trebuie cautat => iesire din for
                break;
        }

        if (!found) { // cuvantul nu exista in dictionar
            return false;
        }

        /* daca dupa stergerea cuvantului lista corespunzatoare limbii ramane goala,
        aceasta va fi stearsa, eliminandu-se limba din cheile dictionarului */
        if (map.get(language).isEmpty()) {
            map.remove(language);
        }

        return true; // cuvantul a fost sters
    }

    /* metoda pentru adaugarea unei noi definitii pentru un cuvant dat ca parametru -
    intoarce true daca s-a adaugat definitia sau false daca exista o definitie din
    acelasi dictionar (dict) sau daca cuvantul nu exista in dictionar */
    private boolean addDefinitionForWord(String word, String language, Definition definition) throws Exception {
        if (!map.containsKey(language)) { // limba nu exista in dictionar
            throw new Exception("Language " + language + " doesn't exist in dictionary\n");
        }

        for (Word w : map.get(language)) {
            /* in forms se vor retine toate formele posibile ale cuvantului */
            ArrayList<String> forms = new ArrayList<>();
            forms.add(w.getWord());
            forms.addAll(w.getSingular());
            forms.addAll(w.getPlural());
            for (String f : forms) { // cauta in toate formele cuvantului
                if (f.equals(word)) { // cuvant gasit
                    for (Definition d : w.getDefinitions()) {
                        if (d.getDict().equals(definition.getDict()) &&
                                d.getDictType().equals(definition.getDictType()) &&
                                d.getYear() == definition.getYear()) { // definitie gasita

                            /* added devine true daca este adaugata o defintie cuvantului */
                            boolean added = false;
                            for (String t : definition.getText()) {
                                /* daca este gasita o definitie/un sinonim care nu exista,
                                se va adauga la lista definitiilor/sinonimelor */
                                if (!d.getText().contains(t)) {
                                    added = true;
                                    d.getText().add(t);
                                }
                            }
                            /* true = modificare (adaugare defintie/sinonim),
                            false = definitie deja existenta */
                            return added;
                        }
                    }
                    /* nu exista deja definitia => se adauga la lista definitiilor */
                    w.getDefinitions().add(definition);
                    return true; // a fost adaugata definitia
                }
            }
        }
        return false; // cuvantul nu exista in lista corespunzatoare
    }

    /* metoda pentru stergerea unei definitii a unui cuvant dat ca parametru -
    intoarce true daca s-a sters definitia sau false daca nu exista o definitie
    din dictionarul primit ca parametru */
    private boolean removeDefinition(String word, String language, String dictionary) throws Exception {
        if (!map.containsKey(language)) { // limba nu exista in dictionar
            throw new Exception("Language " + language + " doesn't exist in dictionary\n");
        }

        String[] id = dictionary.split("@"); // nume@tip@an
        String dict = id[0]; // nume
        String type = id[1]; // tip
        int year = Integer.parseInt(id[2]); // an

        for (Word w : map.get(language)) {
            /* in forms se vor retine toate formele posibile ale cuvantului */
            ArrayList<String> forms = new ArrayList<>();
            forms.add(w.getWord());
            forms.addAll(w.getSingular());
            forms.addAll(w.getPlural());
            for (String f : forms) { // cauta in toate formele cuvantului
                if (f.equals(word)) { // cuvant gasit
                    for (Definition d : w.getDefinitions()) {
                        /* definitie gasita */
                        if (d.getDict().equals(dict) && d.getDictType().equals(type) && d.getYear() == year) {
                            w.getDefinitions().remove(d); // stergere definitie din lista definitiilor cuvantului
                            return true;
                        }
                    }
                }
            }
        }
        return false; // definitia nu exista
    }

    /* metoda pentru traducerea unui cuvant - intoarce traducerea cuvantului word
    din limba fromLanguage in limba toLanguage */
    private String translateWord(String word, String fromLanguage, String toLanguage) throws Exception {
        /* se verifica daca limbile exista in dictionar */
        if (!map.containsKey(fromLanguage) && !fromLanguage.equals("en")) {
            throw new Exception("Language " + fromLanguage + " doesn't exist in dictionary\n");
        }
        if (!map.containsKey(toLanguage) && !toLanguage.equals("en")) {
            throw new Exception("Language " + toLanguage + " doesn't exist in dictionary\n");
        }

        /* cuvant in engleza => nu exista o intrare in dictionar pt. engleza */
        if (fromLanguage.equals("en")) {
            for (Word t : map.get(toLanguage)) {
                if (t.getWord_en().equals(word)) { // a fost gasit cuvantul corespunzator
                    return t.getWord(); // se returneaza traducerea
                }
            }
            return null; // cuvantul corespunzator nu exista in dictionarul limbii de tradus
        }

        for (Word w : map.get(fromLanguage)) {
            if (w.getWord().equals(word)) { // cuvant de tradus = campul word al unui cuvant
                if (toLanguage.equals("en")) { // limba in care se traduce este engleza =>
                    return w.getWord_en(); // se returneaza campul reprezentand traducerea in engleza
                }
                /* orice alta limba din dictionar => se verifica pt. cuvintele din dictionarul
                limbii in care se traduce */
                for (Word t : map.get(toLanguage)) {
                    if (t.getWord_en().equals(w.getWord_en())) { // a fost gasit cuvantul corespunzator
                        return t.getWord(); // se returneaza traducerea
                    }
                }
            }
            int i = 0; // index pentru a obtine forma corespunzatoare
            /* in forms se vor retine formele de singular si plural ale cuvantului */
            ArrayList<String> forms = new ArrayList<>();
            forms.addAll(w.getSingular());
            forms.addAll(w.getPlural());
            for (String s : forms) { // cauta in formele de singular si plural ale cuvantului
                if (s.equals(word)) { // cuvant gasit
                    if (toLanguage.equals("en")) { // de tradus in engleza
                        return w.getWord_en();
                    }
                    for (Word t : map.get(toLanguage)) {
                        if (t.getWord_en().equals(w.getWord_en())) { // legatura gasita (cuvant corespunzator)
                            /* nr va lua valoarea 3 in cazul unei verb si 1 in cazul unui substantiv; cu
                            ajutorul acestuia se va putea determina indexul corespunzator formei de tradus */
                            int nr;
                            if (t.getType().equals("verb"))
                                nr = 3; // lista de 3 cuvinte in cazul unui verb
                            else
                                nr = 1; // lista de 1 cuvant in cazul unui substantiv

                            if (i < nr) // => forma de singular
                                return t.getSingular().get(i);
                            else // => forma de plural
                                return t.getPlural().get(i - nr);
                        }
                    }
                }
                i++;
            }
        }

        return null; // nu a fost gasita o traducere
    }

    /* metoda pentru traducerea unei propozitii - intoarce traducerea propozitiei
    sentence din limba fromLanguage in limba toLanguage */
    private String translateSentence(String sentence, String fromLanguage, String toLanguage) throws Exception {
        /* se verifica daca limbile exista in dictionar */
        if (!map.containsKey(fromLanguage) && !fromLanguage.equals("en")) {
            throw new Exception("Language " + fromLanguage + " doesn't exist in dictionary\n");
        }
        if (!map.containsKey(toLanguage) && !toLanguage.equals("en")) {
            throw new Exception("Language " + toLanguage + " doesn't exist in dictionary\n");
        }

        StringBuilder t = new StringBuilder();
        for (String s : sentence.split(" ")) { // pentru fiecare cuvant din propozitie
            String word = translateWord(s, fromLanguage, toLanguage);
            if (word != null) // daca exista traducere se adauga la Stringul ce va fi returnat
                t.append(word);
            else // daca nu exista traducere se adauga la String forma netradusa
                t.append(s);
            t.append(" ");
        }

        return t.toString();
    }

    /* metoda pentru traducerea unei propozitii si furnizarea a 3 variante de traducere
    folosind sinonimele cuvintelor - intoarce traducerea propozitiei sentence din limba
    fromLanguage in limba toLanguage */
    private ArrayList<String> translateSentences(String sentence, String fromLanguage, String toLanguage) throws Exception {
        String t = translateSentence(sentence, fromLanguage, toLanguage); // traducerea propozitiei

        ArrayList<String> translated = new ArrayList<>();
        translated.add(t); // se adauga traducerea la lista variantelor de traducere

        if (toLanguage.equals("en")) // daca s-a tradus in engleza => nu exista sinonime pt. cuvinte
            return translated;

        for (int i = 0; i < 2; i++) { // inca alte 2 variante de traducere
            StringBuilder str = new StringBuilder();
            for (String s : t.split(" ")) { // pentru fiecare cuvant al propozitiei traduse
                boolean found = false; // found devine true daca este gasit cuvantul
                for (Word w : map.get(toLanguage)) {
                    /* in forms se vor retine toate formele posibile ale cuvantului */
                    ArrayList<String> forms = new ArrayList<>();
                    forms.add(w.getWord());
                    forms.addAll(w.getSingular());
                    forms.addAll(w.getPlural());
                    for (String f : forms) { // cauta in toate formele cuvantului
                        if (f.equals(s)) { // cuvant gasit
                            found = true;
                            /* hasSynonym devine true daca este gasit un sinonim al cuvantului */
                            boolean hasSynonym = false;
                            for (Definition d : w.getDefinitions()) {
                                if (d.getDictType().equals("synonyms")) { // dictionar de tip sinonime
                                    if (i < d.getText().size()) { // daca mai exista sinonime
                                        hasSynonym = true;
                                        /* se adauga la String sinonimul corespunzator indexului */
                                        str.append(d.getText().get(i)).append(" ");
                                        break;
                                    }
                                }
                            }
                            if (!hasSynonym) // daca nu exista un sinonim se adauga cuvantul initial
                                str.append(s).append(" ");
                        }
                        if (found) // cuvant gasit => nu mai trebuie cautat => iesire din for (forme cuvant)
                            break;
                    }
                    if (found) // cuvant gasit => nu mai trebuie cautat => iesire din for (cuvinte limba)
                        break;
                }
                if (!found) // cuvantul nu este din limba toLanguage =>
                    return translated; // se returneaza traducerea incompleta
            }
            /* Stringul creat devine egal cu traducerea initiala (nu mai exista sinonime) */
            if (str.toString().equals(t))
                break;
            translated.add(str.toString()); // adaugare la lista de traduceri
        }
        return translated;
    }

    /* metoda pentru intoarcerea definitiilor si sinonimelor unui cuvant - definitiile
    sunt sortate crescator dupa anul de aparitie al dictionarului */
    private ArrayList<Definition> getDefinitionsForWord(String word, String language) throws Exception {
        if (!map.containsKey(language)) { // limba nu exista in dictionar
            throw new Exception("Language " + language + " doesn't exist in dictionary\n");
        }

        for (Word w : map.get(language)) {
            /* in forms se vor retine toate formele posibile ale cuvantului */
            ArrayList<String> forms = new ArrayList<>();
            forms.add(w.getWord());
            forms.addAll(w.getSingular());
            forms.addAll(w.getPlural());
            for (String f : forms) { // cauta in toate formele cuvantului
                if (f.equals(word)) { // cuvant gasit
                    Collections.sort(w.getDefinitions()); // sorteaza definitiile
                    return w.getDefinitions();
                }
            }
        }

        return null; // cuvant negasit
    }

    /* metoda pentru exportarea unui dictionar in format JSON - Cuvintele din JSON sunt
     ordonate alfabetic, iar definitiile sunt ordonate dupa anul de aparitie al dictionarului */
    private void exportDictionary(String language) throws Exception {
        if (!map.containsKey(language)) { // limba nu exista in dictionar
            throw new Exception("Language " + language + " doesn't exist in dictionary\n");
        }

        for (Word w : map.get(language)) {
            Collections.sort(w.getDefinitions()); // sortare definitii
        }
        Collections.sort(map.get(language)); // sortare cuvinte

        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        File dir = new File("./out");
        if (!dir.exists()) {
            if (!dir.mkdir()) {
                return;
            }
        }
        String fileName = "./out/" + language + "_dict.json";
        File file = new File(fileName);
        if (file.exists()) {
            if (!file.delete()) {
                return;
            }
        }
        Files.createFile(file.toPath());
        FileWriter fw = new FileWriter(file);
        gson.toJson(map.get(language), fw);
        fw.close();
    }

    /* metode de testare a metodelor implementate */

    private void testAddWord(String word, String word_en, String type, ArrayList<String> singular, ArrayList<String> plural,
                            ArrayList<Definition> definitions, String language) {
        Word w = new Word(word, word_en, type, singular, plural, definitions);
        System.out.println("Add word " + word + " to dictionary " + language + " =>");
        try {
            if (!addWord(w, language))
                System.out.println("Word " + word + " already exists in dictionary " + language + "\n");
            else
                printDictionary();
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void testRemoveWord(String word, String language) {
        System.out.println("Remove word " + word + " from dictionary " + language + " =>");
        try {
            if (!removeWord(word, language))
                System.out.println("Word " + word + " doesn't exist in dictionary " + language + "\n");
            else
                printDictionary();
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void testAddDefinitionForWord(String word, String language, Definition definition) {
        System.out.println("Add definition " + definition.toString() + " for word " + word + " from dictionary " + language + " =>");
        try {
            if (!addDefinitionForWord(word, language, definition))
                System.out.println("Definition " + definition.getDict() + " " + definition.getDictType() + " " + definition.getYear()
                        + " already exists for word " + word + "\n");
            else
                printDictionary();
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void testRemoveDefinition(String word, String language, String dictionary) {
        System.out.println("Remove definition " + dictionary + " for word " + word + " from dictionary " + language + " =>");
        try {
            if (!removeDefinition(word, language, dictionary))
                System.out.println("Definition " + dictionary + " doesn't exist for word " + word + "\n");
            else
                printDictionary();
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void testTranslateWord(String word, String fromLanguage, String toLanguage) {
        System.out.println("Translate " + word + " from " + fromLanguage + " to " + toLanguage + " =>");
        try {
            String translatedWord = translateWord(word, fromLanguage, toLanguage);

            if (translatedWord == null)
                System.out.println("Translation not found\n");
            else
                System.out.println(word + " = " + translatedWord + "\n");
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void testTranslateSentence(String sentence, String fromLanguage, String toLanguage) {
        System.out.println("Translate " + sentence + " from " + fromLanguage + " to " + toLanguage + " =>");
        try {
            String translatedSentence = translateSentence(sentence, fromLanguage, toLanguage);
            System.out.println(sentence + " = " + translatedSentence + "\n");
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }

    private void testTranslateSentences(String sentence, String fromLanguage, String toLanguage) {
        System.out.println("Translate " + sentence + " from " + fromLanguage + " to " + toLanguage + " in 3 ways =>");
        try {
            ArrayList<String> translatedSentences = translateSentences(sentence, fromLanguage, toLanguage);
            System.out.println(sentence + " = " + translatedSentences + "\n");
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void testGetDefinitionsForWord(String word, String language) {
        System.out.println("Get definitions for word " + word + " from dictionary " + language + " =>");
        try {
            ArrayList<Definition> definitions = getDefinitionsForWord(word, language);
            if (definitions == null) {
                System.out.println("Word " + word + " doesn't have any definitions\n");
                return;
            }

            for (Definition d : definitions)
                System.out.println(d.toString());
            System.out.println();
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void testExportDictionary(String language) {
        System.out.print("Export dictionary " + language + " => ");
        try {
            exportDictionary(language);
            System.out.println("dictionary exported\n");
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public static void main(String[] args) throws Exception {
        Dictionary dictionary = new Dictionary();
        dictionary.readDictionary(new File("./dict"));

        System.out.println();
        dictionary.printDictionary();

        dictionary.testAddWord("essen", "eat", "verb", new ArrayList<>(List.of("esse", "isst", "isst")), new ArrayList<>(List.of("essen", "esst", "essen")),
                new ArrayList<>(List.of(new Definition("Duden", "definitions", 2020, new ArrayList<>(List.of("Nahrung zu sich nehmen"))))),
                "de");
        dictionary.testAddWord("chat", "cat", "noun", new ArrayList<>(List.of("chat")), new ArrayList<>(List.of("chats")),
                new ArrayList<>(List.of(new Definition("Larousse", "synonyms", 2000, new ArrayList<>(List.of("greffier", "mistigri", "matou", "minet"))))),
                "fr");

        dictionary.testRemoveWord("jeu", "fr");
        dictionary.testRemoveWord("chat", "de");

        dictionary.testAddDefinitionForWord("essen", "de", new Definition("Duden", "definitions", 2020,
                new ArrayList<>(List.of("Nahrung zu sich nehmen", "Nahrungsaufnahme in einen bestimmten Zustand bringen"))));
        dictionary.testAddDefinitionForWord("chat", "fr", new Definition("Larousse", "synonyms", 2000,
                new ArrayList<>(List.of("greffier", "mistigri", "matou", "minet"))));

        dictionary.testRemoveDefinition("chat", "fr", "Larousse@definitions@2000");
        dictionary.testRemoveDefinition("pisică", "ro", "Larousse@synonyms@2000");

        dictionary.testTranslateWord("esst", "de", "ro");
        dictionary.testTranslateWord("câine", "ro", "fr");

        dictionary.testTranslateSentence("chats mangent", "fr", "ro");
        dictionary.testTranslateSentence("pisică mănâncă", "ro", "de");

        dictionary.testTranslateSentences("pisică mănâncă", "ro", "fr");
        dictionary.testTranslateSentences("chats mangent", "fr", "de");

        dictionary.testGetDefinitionsForWord("mănâncă", "ro");
        dictionary.testGetDefinitionsForWord("kot", "ru");

        dictionary.testExportDictionary("ro");
        dictionary.testExportDictionary("de");

    }

}