package org.example.service;

import java.util.List;

public interface IService<T> {
    void ajouter(T t);
    void modifier(T t);
    void supprimer(int id);
    List<T> afficherTout();
    T afficherUn(int id);
}
