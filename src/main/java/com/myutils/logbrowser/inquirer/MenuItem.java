/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import Utils.Pair;

/**
 *
 * @author ssydoruk
 */
public class MenuItem extends Pair<String, Object> {

    public MenuItem(String key, Object value) {
        super(key, value);
    }

    @Override
    public String toString() {
        return getKey();
    }

}
