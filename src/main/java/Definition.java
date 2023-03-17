import java.util.ArrayList;

public class Definition implements Comparable<Definition> {

    private String dict;
    private String dictType;
    private int year;
    private ArrayList<String> text;

    public Definition(String dict, String dictType, int year, ArrayList<String> text) {
        this.dict = dict;
        this.dictType = dictType;
        this.year = year;
        this.text = text;
    }

    public String getDict() {
        return dict;
    }

    public String getDictType() {
        return dictType;
    }

    public int getYear() {
        return year;
    }

    public ArrayList<String> getText() {
        return text;
    }

    @Override
    public int compareTo(Definition d) {
        return getYear() - d.getYear();
    }

    @Override
    public String toString() {
        return "Definition {" +
                "dict = " + dict +
                ", dictType = " + dictType +
                ", year = " + year +
                ", text = " + text +
                "}";
    }

}