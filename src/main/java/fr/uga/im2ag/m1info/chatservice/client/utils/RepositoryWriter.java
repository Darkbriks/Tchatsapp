package fr.uga.im2ag.m1info.chatservice.client.utils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public class RepositoryWriter<T> {
    private final String filePath;

    public RepositoryWriter(String filePath) {
        Path dossier = null;
        try {
            dossier = Files.createTempDirectory("tchatsapp");
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.filePath = dossier.toAbsolutePath().toString() + File.separator + filePath + ".dat";
        System.out.println(this.filePath);
    }

    public void writeData(Set<T> dataSet) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
            oos.writeObject(dataSet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeData(T data) {
        Set<T> existingData = readData();
        existingData.add(data);
        writeData(existingData);
    }

    public Set<T> readData() {
        File file = new File(filePath);
        if (!file.exists()) {
            return new HashSet<T>();
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filePath))) {
            Object obj = ois.readObject();
            if (obj instanceof Set) {
                return (Set<T>) obj;
            }
            return new HashSet<T>();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return new HashSet<T>();
        }
    }

    public T readData(Predicate<T> condition) {
        Set<T> allData = readData();
        return allData.stream()
                .filter(condition)
                .findFirst()
                .orElse(null);
    }

    public void updateData(Predicate<T> condition, T newData) {
        Set<T> allData = readData();
        
        T toRemove = null;
        for (T item : allData) {
            if (condition.test(item)) {
                toRemove = item;
                break;
            }
        }
        
        if (toRemove != null) {
            allData.remove(toRemove);
            allData.add(newData);
            writeData(allData);
        }
    }

    public void removeData(Predicate<T> condition) {
        Set<T> allData = readData();
        boolean removed = allData.removeIf(condition);
        
        if (removed) {
            writeData(allData);
        }
    }

    public boolean deleteCache() {
        File file = new File(filePath);
        return file.exists() && file.delete();
    }
}
