/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

/**
 *
 * @author ssydoruk
 */
public class NameID {

    private String name;
    private int id;

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    public NameID(String name, int id) {
        this.name = name;
        this.id = id;
    }

    @Override
    public String toString() {
        return name;
    }

}
