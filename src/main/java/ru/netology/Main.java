package ru.netology;

import java.io.File;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        FormatConvertor formatConvertor = new FormatConvertor("");
        formatConvertor.convertXmlTo("data.xml", FileFormat.JSON, FileCreateOption.NEW_NAME);
        formatConvertor.convertCsvTo("data.csv", ';', FileFormat.JSON);

        printList(formatConvertor.convertJsonToList("data_778.json"));
    }

    private static void printList(List<Employee> list) {
        list.stream().forEach(System.out::println);
    }
}